package com.dawn.catalog.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Value("${dawn.cloudinary.folderName}")
    private String folderName;

    private final Cloudinary cloudinary;

    public Map<String, Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InternalServiceException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InternalServiceException("File too large, max 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InternalServiceException("Only image files are allowed");
        }
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folderName,
                    "resource_type", "auto"
            );

            return this.cloudinary
                    .uploader()
                    .upload(file.getBytes(), params);
        } catch (IOException e) {
            throw new InternalServiceException("Image upload fail", e);
        }
    }
}
