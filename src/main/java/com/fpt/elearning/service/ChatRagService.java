package com.fpt.elearning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Chat AI theo mô hình RAG (Retrieval-Augmented Generation):
 * 1. RETRIEVAL  - lấy khóa học liên quan từ DB
 * 2. AUGMENT    - ghép dữ liệu DB vào prompt
 * 3. GENERATION - gọi Ollama API trả lời, chỉ dựa trên dữ liệu cung cấp
 */
@Service
@RequiredArgsConstructor
public class ChatRagService {

    private static final Logger log = LoggerFactory.getLogger(ChatRagService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CourseRepository courseRepository;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.url}")
    private String ollamaUrl;

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý tư vấn khóa học của website E-learning.
            CHỈ trả lời dựa trên DANH SÁCH KHÓA HỌC được cung cấp bên dưới.
            Nếu DANH SÁCH KHÓA HỌC trống, hãy nói rõ hiện chưa có dữ liệu phù hợp.
            TUYỆT ĐỐI không bịa tên khóa học, giá, hay thông tin nằm ngoài danh sách.
            Trả lời ngắn gọn, thân thiện, bằng tiếng Việt có dấu.
            """;

    public String answer(String question) {
        // Chỉ truy xuất dữ liệu khóa học liên quan trong DB của app.
        List<Course> courses = courseRepository.searchPublished(CourseStatus.PUBLISHED, question);

        String context = buildContext(courses);
        String userContent = "DANH SÁCH KHÓA HỌC:\n" + context
                + "\n\nCÂU HỎI CỦA NGƯỜI DÙNG: " + question;

        try {
            String requestBody = "{"
                    + "\"model\":\"" + escapeJson(model) + "\","
                    + "\"messages\":["
                    + "  {\"role\":\"system\",\"content\":\"" + escapeJson(SYSTEM_PROMPT) + "\"},"
                    + "  {\"role\":\"user\",\"content\":\"" + escapeJson(userContent) + "\"}"
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
                JsonNode content = JSON.readTree(response.body()).path("message").path("content");
                if (!content.isMissingNode()) {
                    return content.asText();
                }
                return "Lỗi đọc phản hồi từ hệ thống AI.";
            }

            log.error("Ollama HTTP Error: {}, body: {}", response.statusCode(), response.body());
            return "Dịch vụ AI tạm thời gặp sự cố.";
        } catch (Exception ex) {
            log.error("Lỗi kết nối Ollama ({})", ollamaUrl, ex);
            return "Hiện tại trợ lý AI không hoạt động. Vui lòng kiểm tra Ollama.";
        }
    }

    private String buildContext(List<Course> courses) {
        if (courses.isEmpty()) {
            return "(không có khóa học phù hợp)";
        }
        return courses.stream().map(c -> {
            String instructor = c.getInstructor() != null ? c.getInstructor().getFullName() : "N/A";
            String category = c.getCategory() != null ? c.getCategory().getName() : "N/A";
            return "- Tên: " + c.getTitle()
                    + " | Danh mục: " + category
                    + " | Giá: " + c.getPrice() + " VND"
                    + " | Giảng viên: " + instructor
                    + " | Mô tả: " + (c.getShortDescription() != null ? c.getShortDescription() : "");
        }).collect(Collectors.joining("\n"));
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
