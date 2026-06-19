package com.fpt.elearning.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.elearning.entity.*;
import com.fpt.elearning.repository.EnrollmentRepository;
import com.fpt.elearning.repository.LessonRepository;
import com.fpt.elearning.repository.QuestionRepository;
import com.fpt.elearning.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ProgressService progressService;
    private final ObjectMapper objectMapper;

    @Value("${ollama.model}")
    private String model;
    @Value("${ollama.url}")
    private String ollamaUrl;
    @Value("${quiz.pass-score:70}")
    private int passScore;
    @Value("${quiz.num-questions:5}")
    private int numQuestions;

    // ====== Ket qua cham diem tra ve cho controller ======
    public record QuizResult(int score, boolean passed, int correct, int total, int passScore) {}

    // ====== Cau truc JSON nhan tu Ollama ======
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GenChoiceQuiz(List<GenQuestion> questions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GenQuestion(String question, List<String> choices, Integer correctIndex) {}

    // ============ SINH CAU HOI BANG OLLAMA ============
    @Transactional
    public int generateForLesson(Long lessonId, String instruction) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bai hoc"));

        String plain = Jsoup.parse(lesson.getContent() == null ? "" : lesson.getContent()).text().trim();
        if (plain.isBlank()) {
            throw new IllegalStateException("Bai hoc chua co noi dung de AI tao cau hoi.");
        }
        if (plain.length() > 4000) {
            plain = plain.substring(0, 4000);
        }

        // Goi AI toi da 3 lan, loai cau co dap an placeholder (A/B/C/D, rong), giu bo tot nhat
        List<GenQuestion> best = java.util.Collections.emptyList();
        for (int attempt = 1; attempt <= 3; attempt++) {
            GenChoiceQuiz gen;
            try {
                gen = objectMapper.readValue(callOllama(plain, instruction), GenChoiceQuiz.class);
            } catch (Exception ex) {
                log.warn("Lan {}: khong parse duoc JSON tu Ollama", attempt);
                continue;
            }
            if (gen == null || gen.questions() == null) {
                continue;
            }
            List<GenQuestion> good = gen.questions().stream().filter(this::isValidQuestion).toList();
            if (good.size() > best.size()) {
                best = good;
            }
            if (best.size() >= 3) {
                break; // du dung cho 1 bai kiem tra
            }
        }
        if (best.isEmpty()) {
            throw new IllegalStateException("AI tao cau hoi chua dat (dap an khong hop le). Thu lai hoac them cau hoi thu cong.");
        }

        // Xoa cau hoi cu (cascade xoa luon dap an) roi luu bo moi
        questionRepository.deleteAll(questionRepository.findByLesson_IdOrderByIdAsc(lessonId));

        int saved = 0;
        for (GenQuestion gq : best) {
            int correctIdx = gq.correctIndex() == null ? 0 : gq.correctIndex();
            Question q = Question.builder().lesson(lesson).content(cleanText(gq.question())).build();
            List<String> choices = gq.choices();
            for (int i = 0; i < choices.size(); i++) {
                String text = cleanText(choices.get(i));
                if (text.isBlank()) continue;
                q.getChoices().add(Choice.builder()
                        .question(q).content(text).correct(i == correctIdx).build());
            }
            if (q.getChoices().stream().noneMatch(Choice::isCorrect)) {
                q.getChoices().get(0).setCorrect(true);
            }
            questionRepository.save(q);
            saved++;
        }
        return saved;
    }

    /** Cau hoi hop le: >=2 lua chon, khong rong, khong phai nhan A/B/C/D, co it nhat 2 lua chon khac nhau */
    private boolean isValidQuestion(GenQuestion q) {
        if (q == null || q.question() == null || cleanText(q.question()).isBlank()) {
            return false;
        }
        if (q.choices() == null || q.choices().size() < 2) {
            return false;
        }
        java.util.Set<String> distinct = new java.util.HashSet<>();
        for (String c : q.choices()) {
            if (c == null) {
                return false;
            }
            String t = cleanText(c);
            if (t.isBlank() || t.matches("(?i)^[a-d]$")) {
                return false; // rong hoac chi la nhan A/B/C/D
            }
            distinct.add(t.toLowerCase());
        }
        return distinct.size() >= 2;
    }

    /** Bo LaTeX/markdown ($...$, \command, **...**) khoi text AI sinh ra */
    private String cleanText(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\\\[a-zA-Z]+\\s*", " ")  // bo \rightarrow, \to ...
                .replace("$", "")
                .replace("**", "")
                .replace("\\", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String callOllama(String lessonText, String instruction) {
        String system = "Ban la giao vien. Dua CHI tren noi dung bai hoc, tao " + numQuestions
                + " cau hoi trac nghiem tieng Viet de kiem tra nguoi hoc. "
                + "Moi cau co dung 4 lua chon, va MOI LUA CHON PHAI LA NOI DUNG DAP AN CU THE - "
                + "TUYET DOI KHONG dung 'A', 'B', 'C', 'D' hay nhan/so/chu cai lam lua chon. "
                + "Chi 1 dap an dung; correctIndex la chi so (bat dau tu 0) cua dap an dung trong mang choices. "
                + "Viet plain text tieng Viet co dau, KHONG dung LaTeX, markdown hay ky hieu (khong dung \\rightarrow, $...$, **...**). "
                + "Vi du dung mot cau: "
                + "{\"question\":\"Dang qua khu (V2) cua dong tu 'go' la gi?\", "
                + "\"choices\":[\"went\",\"goed\",\"gone\",\"going\"], \"correctIndex\":0}";
        String user = "NOI DUNG BAI HOC:\n" + lessonText;
        if (instruction != null && !instruction.isBlank()) {
            user += "\n\nYEU CAU THEM TU GIANG VIEN (uu tien tuan theo, nhung VAN giu dung quy tac dinh dang o tren): "
                    + instruction.trim();
        }

        try {
            // JSON Schema ep Ollama tra ve dung cau truc {"questions":[{question,choices,correctIndex}]}
            Map<String, Object> itemSchema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "question", Map.of("type", "string"),
                            "choices", Map.of("type", "array", "items", Map.of("type", "string")),
                            "correctIndex", Map.of("type", "integer")
                    ),
                    "required", List.of("question", "choices", "correctIndex")
            );
            Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of("questions", Map.of("type", "array", "items", itemSchema)),
                    "required", List.of("questions")
            );

            Map<String, Object> body = Map.of(
                    "model", model,
                    "stream", false,
                    "format", schema,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)
                    )
            );
            String requestBody = objectMapper.writeValueAsString(body);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Ollama HTTP {}: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Dich vu AI gap su co (HTTP " + response.statusCode() + ").");
            }
            // Voi format=json, truong message.content la chuoi JSON hop le
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("message").path("content").asText();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Loi goi Ollama tao quiz", ex);
            throw new IllegalStateException("Khong ket noi duoc AI (Ollama). Kiem tra Ollama dang chay.");
        }
    }

    // ============ CHAM DIEM ============
    @Transactional
    public QuizResult grade(User user, Long courseId, Long lessonId, Map<Long, Long> answers) {
        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_Id(user.getId(), courseId)
                .orElseThrow(() -> new IllegalStateException("Ban chua so huu khoa hoc nay"));
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bai hoc"));

        List<Question> questions = questionRepository.findByLesson_IdOrderByIdAsc(lessonId);
        if (questions.isEmpty()) {
            throw new IllegalStateException("Bai hoc chua co cau hoi.");
        }

        int correct = 0;
        for (Question q : questions) {
            Long chosen = answers.get(q.getId());
            if (chosen == null) continue;
            boolean ok = q.getChoices().stream()
                    .anyMatch(c -> c.getId().equals(chosen) && c.isCorrect());
            if (ok) correct++;
        }
        int total = questions.size();
        int score = (int) Math.round(correct * 100.0 / total);
        boolean passed = score >= passScore;

        quizAttemptRepository.save(QuizAttempt.builder()
                .student(user).lesson(lesson).score(score).passed(passed).build());

        if (passed) {
            progressService.markComplete(enrollment, lesson);
        }
        return new QuizResult(score, passed, correct, total, passScore);
    }
}
