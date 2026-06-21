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

    // Quy uoc: phan tu DAU TIEN trong 'choices' luon la dap an DUNG (do AI sinh ra),
    // server se danh dau dung roi XAO TRON vi tri truoc khi luu.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GenQuestion(String question, List<String> choices) {}

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
            String qContent = cleanText(gq.question());
            List<String> raw = gq.choices();

            // Lam sach + loc trung/rong/trung cau hoi. Phan tu DAU TIEN hop le = dap an DUNG.
            String correctText = null;
            List<String> cleaned = new java.util.ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String rc : raw) {
                String text = cleanText(rc);
                if (text.isBlank()) continue;
                if (text.equalsIgnoreCase(qContent)) continue;        // bo lua chon trung cau hoi
                if (!seen.add(text.toLowerCase())) continue;          // bo lua chon trung nhau
                if (correctText == null) {
                    correctText = text;                               // dau tien hop le = dap an dung
                }
                cleaned.add(text);
            }
            if (cleaned.size() < 2) {
                continue; // bo cau khong du lua chon hop le
            }

            // XAO TRON vi tri de dap an dung khong con co dinh o dau (A)
            java.util.Collections.shuffle(cleaned);

            Question q = Question.builder().lesson(lesson).content(qContent).build();
            for (String text : cleaned) {
                q.getChoices().add(Choice.builder()
                        .question(q).content(text)
                        .correct(text.equals(correctText)).build());
            }
            questionRepository.save(q);
            saved++;
        }
        if (saved == 0) {
            throw new IllegalStateException("AI tao cau hoi chua dat. Thu lai hoac them cau hoi thu cong.");
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
        String qc = cleanText(q.question());
        java.util.Set<String> distinct = new java.util.HashSet<>();
        for (String c : q.choices()) {
            if (c == null) {
                continue;
            }
            String t = cleanText(c);
            if (t.isBlank() || t.matches("(?i)^[a-d]$") || t.equalsIgnoreCase(qc)) {
                continue; // bo qua: rong / nhan A,B,C,D / trung cau hoi
            }
            distinct.add(t.toLowerCase());
        }
        return distinct.size() >= 2; // con it nhat 2 dap an thuc su khac nhau
    }

    /** Bo LaTeX/markdown (\textbf{x}, extbf{x} do JSON nuot \t, $...$, **...**) khoi text AI */
    private String cleanText(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\t", " ").replace("\r", " ").replace("\n", " ")
                // lenh{noi dung} hoac extbf{noi dung} -> giu lai noi dung
                .replaceAll("(?i)\\\\?[a-z]+\\s*\\{([^{}]*)\\}", "$1")
                .replaceAll("\\\\[a-zA-Z]+\\s*", " ")  // bo \rightarrow, \to ...
                .replace("$", "")
                .replace("**", "")
                .replace("{", "")
                .replace("}", "")
                .replace("\\", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String callOllama(String lessonText, String instruction) {
        String system = "You are a teacher who writes multiple-choice quizzes. Based ONLY on the provided LESSON CONTENT, "
                + "create " + numQuestions + " multiple-choice questions written in ENGLISH, clear and grammatically correct.\n"
                + "MANDATORY RULES:\n"
                + "1) Each question must be clear and strictly based on the knowledge in the lesson; do NOT ask about anything outside the lesson.\n"
                + "2) Each question has exactly 4 options that are CONCRETE, short, and DIFFERENT answers. "
                + "Do NOT use 'A', 'B', 'C', 'D' or any label/number as an option. Do NOT repeat the question text inside the options.\n"
                + "3) IMPORTANT: The FIRST element of the 'choices' array MUST be the CORRECT answer "
                + "(factually correct - double-check it). The other 3 elements must be plausible but INCORRECT distractors. "
                + "Do NOT mention which one is correct anywhere else - just put it first. "
                + "(The application will automatically shuffle the positions, so the correct answer will NOT stay first for students.)\n"
                + "4) Write everything in plain English. "
                + "Do NOT use any LaTeX/markdown (\\textbf{}, \\rightarrow, $...$, **, curly braces {}).\n"
                + "5) If there is an 'EXTRA INSTRUCTION FROM THE TEACHER', use it only to adjust the focus/difficulty within the lesson scope; "
                + "if it is outside the lesson, ignore it.\n"
                + "Example (first choice 'went' is the correct answer): "
                + "{\"question\":\"What is the past simple (V2) form of the verb 'go'?\","
                + "\"choices\":[\"went\",\"goed\",\"gone\",\"going\"]}";
        String user = "LESSON CONTENT:\n" + lessonText;
        if (instruction != null && !instruction.isBlank()) {
            user += "\n\nEXTRA INSTRUCTION FROM THE TEACHER (only adjust focus within the lesson scope; ignore if outside the lesson): "
                    + instruction.trim();
        }

        try {
            // JSON Schema ep Ollama tra ve dung cau truc {"questions":[{question,choices}]}
            // Quy uoc: choices[0] luon la dap an dung.
            Map<String, Object> itemSchema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "question", Map.of("type", "string"),
                            "choices", Map.of("type", "array", "items", Map.of("type", "string"))
                    ),
                    "required", List.of("question", "choices")
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
                    "options", Map.of("temperature", 0.3),
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
