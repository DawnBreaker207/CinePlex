package com.dawn.catalog.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    @Value("${dawn.cloudinary.folderName}")
    private String folderName;

    private final Cloudinary cloudinary;

    public Map upload(MultipartFile file) {
        try {
            Map params = ObjectUtils.asMap(
                    "folder", folderName,
                    "resource_type", "auto"
            );

            Map data = this.cloudinary
                    .uploader()
                    .upload(file.getBytes(), params);
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Image upload fail");
        }
    }
}
