package com.Soo_Shinsa.image.service;

import com.Soo_Shinsa.global.constant.TargetType;
import com.Soo_Shinsa.image.model.Image;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    Image uploadImage(MultipartFile file, TargetType targetType, Long targetId);
    void deleteImage(String imageUrl);
    Image updateImage(MultipartFile newFile, String oldImageUrl, TargetType targetType);
}
