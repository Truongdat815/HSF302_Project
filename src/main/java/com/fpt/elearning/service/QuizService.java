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
    public int generateForLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bai hoc"));

        String plain = Jsoup.parse(lesson.getContent() == null ? "" : lesson.getContent()).text().trim();
        if (plain.isBlank()) {
            throw new IllegalStateException("Bai hoc chua co noi dung de AI tao cau hoi.");
        }
        if (plain.length() > 4000) {
            plain = plain.substring(0, 4000);
        }

        String generatedJson = callOllama(plain);
        GenChoiceQuiz gen;
        try {
            gen = objectMapper.readValue(generatedJson, GenChoiceQuiz.class);
        } catch (Exception ex) {
            log.error("Khong parse duoc JSON tu Ollama: {}", generatedJson, ex);
            throw new IllegalStateException("AI tra ve du lieu khong hop le, thu lai hoac them cau hoi thu cong.");
        }
        if (gen == null || gen.questions() == null || gen.questions().isEmpty()) {
            throw new IllegalStateException("AI khong tao duoc cau hoi nao, thu lai.");
        }

        // Xoa cau hoi cu (cascade xoa luon dap an)
        questionRepository.deleteAll(questionRepository.findByLesson_IdOrderByIdAsc(lessonId));

        int saved = 0;
        for (GenQuestion gq : gen.questions()) {
            if (gq.question() == null || gq.choices() == null || gq.choices().size() < 2) {
                continue;
            }
            int correctIdx = gq.correctIndex() == null ? 0 : gq.correctIndex();
            Question q = Question.builder().lesson(lesson).content(gq.question().trim()).build();
            for (int i = 0; i < gq.choices().size(); i++) {
                String text = gq.choices().get(i);
                if (text == null || text.isBlank()) continue;
                q.getChoices().add(Choice.builder()
                        .question(q)
                        .content(text.trim())
                        .correct(i == correctIdx)
                        .build());
            }
            // Bao dam co it nhat 1 dap an dung
            if (q.getChoices().stream().noneMatch(Choice::isCorrect) && !q.getChoices().isEmpty()) {
                q.getChoices().get(0).setCorrect(true);
            }
            if (q.getChoices().size() >= 2) {
                questionRepository.save(q);
                saved++;
            }
        }
        if (saved == 0) {
            throw new IllegalStateException("AI khong tao duoc cau hoi hop le, thu lai hoac them thu cong.");
        }
        return saved;
    }

    private String callOllama(String lessonText) {
        String system = "Ban la giao vien. Dua CHI tren noi dung bai hoc, tao " + numQuestions
                + " cau hoi trac nghiem tieng Viet, moi cau 4 lua chon, dung 1 dap an dung. "
                + "Tra ve DUNG JSON dang: "
                + "{\"questions\":[{\"question\":\"...\",\"choices\":[\"A\",\"B\",\"C\",\"D\"],\"correctIndex\":0}]}. "
                + "correctIndex la chi so (bat dau tu 0) cua dap an dung. Khong them chu nao ngoai JSON.";
        String user = "NOI DUNG BAI HOC:\n" + lessonText;

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "stream", false,
                    "format", "json",
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
