package com.fpt.elearning.service;
import com.fpt.elearning.entity.Course;
import com.fpt.elearning.entity.enums.CourseStatus;
import com.fpt.elearning.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat AI theo mo hinh RAG (Retrieval-Augmented Generation):
 * 1. RETRIEVAL  - lấy khóa học liên quan từ DB
 * 2. AUGMENT    - ghep du lieu DB vao prompt
 * 3. GENERATION - goi Ollama API tra loi, CHI dua tren du lieu cung cap
 */
@Service
@RequiredArgsConstructor
public class ChatRagService {

    private static final Logger log = LoggerFactory.getLogger(ChatRagService.class);

    private final CourseRepository courseRepository;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.url}")
    private String ollamaUrl;

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý tư vấn khóa học của website E-learning.
            CHỈ trả lời dựa trên DANH SÁCH KHÓA HỌC được cung cấp bên dưới.
            Nếu không có khóa học phù hợp, hãy trả lời: "Hiện chưa có khóa học phù hợp với yêu cầu của bạn."
            TUYỆT ĐỐI không bịa tên khóa học, giá, hay thông tin nằm ngoài danh sách.
            Tra loi ngan gon, than thien, bang tieng Viet.
            """;

    public String answer(String question) {
        // 1. RETRIEVAL: tìm khóa học liên quan (fallback: lấy tất cả PUBLISHED)
        List<Course> courses = courseRepository.searchPublished(CourseStatus.PUBLISHED, question);
        if (courses.isEmpty()) {
            courses = courseRepository.findByStatus(CourseStatus.PUBLISHED);
        }

        String context = buildContext(courses);

        // 2. AUGMENT: ghep context + cau hoi
        String userContent = "DANH SACH KHOA HOC:\n" + context
                + "\n\nCAU HOI CUA NGUOI DUNG: " + question;

        // 3. GENERATION: goi Ollama API chat
        try {
            String escapedUserContent = escapeJson(userContent);
            String escapedSystemPrompt = escapeJson(SYSTEM_PROMPT);

            String requestBody = "{"
                    + "\"model\":\"" + model + "\","
                    + "\"messages\":["
                    + "  {\"role\":\"system\",\"content\":\"" + escapedSystemPrompt + "\"},"
                    + "  {\"role\":\"user\",\"content\":\"" + escapedUserContent + "\"}"
                    + "],"
                    + "\"stream\":false"
                    + "}";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Simple parsing to extract "content" from {"message":{"role":"assistant","content":"..."}}
                int contentIndex = body.indexOf("\"content\":\"");
                if (contentIndex != -1) {
                    int start = contentIndex + 11;
                    int end = body.indexOf("\"", start);
                    while (end != -1 && body.charAt(end - 1) == '\\') {
                        end = body.indexOf("\"", end + 1);
                    }
                    if (end != -1) {
                        String rawContent = body.substring(start, end);
                        return unescapeJson(rawContent);
                    }
                }
                return "Lỗi đọc phản hồi từ hệ thống AI.";
            } else {
                log.error("Ollama HTTP Error: " + response.statusCode() + ", body: " + response.body());
                return "Dịch vụ AI tạm thời gặp sự cố.";
            }
        } catch (Exception ex) {
            log.error("Lỗi kết nối Ollama (http://localhost:11434)", ex);
            return "Hiện tại trợ lý AI không hoạt động. Vui lòng kiểm tra Ollama.";
        }
    }

    private String buildContext(List<Course> courses) {
        if (courses.isEmpty()) {
            return "(không có khóa học nào)";
        }
        return courses.stream().map(c -> {
            String instructor = c.getInstructor() != null ? c.getInstructor().getFullName() : "N/A";
            String category = c.getCategory() != null ? c.getCategory().getName() : "N/A";
            return "- Tên: " + c.getTitle()
                    + " | Danh mục: " + category
                    + " | Giá: " + c.getPrice() + " VND"
                    + " | Giảng vien: " + instructor
                    + " | Mô tả: " + (c.getShortDescription() != null ? c.getShortDescription() : "");
        }).collect(Collectors.joining("\n"));
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
