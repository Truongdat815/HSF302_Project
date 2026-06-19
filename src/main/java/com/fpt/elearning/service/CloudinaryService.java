package com.fpt.elearning.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Upload anh len Cloudinary, tra ve secure_url de luu vao DB.
 */
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * @param file   anh tu form (multipart)
     * @param folder thu muc tren Cloudinary, vd: "elearning/courses"
     * @return secure_url cua anh da upload
     */
    @SuppressWarnings("unchecked")
    public String upload(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image"
        );
        Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
        return (String) result.get("secure_url");
    }
}
