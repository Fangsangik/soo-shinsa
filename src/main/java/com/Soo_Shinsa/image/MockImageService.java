package com.Soo_Shinsa.image;

import com.Soo_Shinsa.constant.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 로컬/개발용 이미지 서비스. S3를 호출하지 않고 mock 이미지 URL(picsum)을 저장한다.
 * app.image.mock=true 일 때만 활성화 (application-h2 / application-local).
 */
@Primary
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.image.mock", havingValue = "true")
public class MockImageService implements ImageService {

    private final ImageRepository imageRepository;

    @Override
    public Image uploadImage(MultipartFile file, TargetType targetType, Long targetId) {
        String url = "https://picsum.photos/seed/" + UUID.randomUUID() + "/600/600";
        return imageRepository.save(new Image(targetId, targetType, file.getOriginalFilename(), url));
    }

    @Override
    public void deleteImage(String imageUrl) {
        // mock: nothing to delete
    }

    @Override
    public Image updateImage(MultipartFile newFile, String oldImageUrl, TargetType targetType) {
        return uploadImage(newFile, targetType, null);
    }
}
