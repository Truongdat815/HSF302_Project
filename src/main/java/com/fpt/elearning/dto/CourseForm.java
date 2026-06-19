package com.fpt.elearning.dto;

import com.fpt.elearning.entity.enums.CourseStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
public class CourseForm {
    private Long id;
    private String title;
    private String shortDescription;
    // HTML rich text tu CKEditor
    private String description;
    private BigDecimal price;
    // Mac dinh PUBLISHED de khoa hoc moi tao hien ngay cho hoc vien
    private CourseStatus status = CourseStatus.PUBLISHED;
    private Long categoryId;
    // Ảnh thumbnail (co the rong khi sua ma khong doi anh)
    private MultipartFile thumbnail;
}
