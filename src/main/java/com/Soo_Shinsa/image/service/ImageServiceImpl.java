package com.Soo_Shinsa.image.service;

import com.Soo_Shinsa.global.constant.TargetType;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.global.utils.S3Uploader;
import com.Soo_Shinsa.image.model.Image;
import com.Soo_Shinsa.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.Soo_Shinsa.global.exception.ErrorCode.FAIL_UPLOAD_FILE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    @Override
    public Image uploadImage(MultipartFile file, TargetType targetType, Long targetId) {

        try {
            // Image 클래스의 determinePath 메서드 사용
            String dirName = Image.determinePath(targetType);
            // S3에 파일 업로드
            String imageUrl = s3Uploader.upload(file, dirName);
            // 비즈니스 로직: 데이터베이스에 이미지 정보 저장
            Image image = new Image(targetId, targetType, file.getOriginalFilename(), imageUrl);

            return imageRepository.save(image);

        } catch (IOException e) {
            // 업로드 중 예외 처리
            log.error("이미지 업로드 실패: {}", e.getMessage());

            // 적절한 사용자 정의 예외 던지기
            throw new InvalidInputException(FAIL_UPLOAD_FILE);
        }
    }

    @Transactional
    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl != null) {
            s3Uploader.deleteFile(imageUrl);
        }
    }

    @Transactional
    @Override
    public Image updateImage(MultipartFile newFile, String oldImageUrl, TargetType targetType) {
        try {

            String dirName = Image.determinePath(targetType);
            String newImageUrl = s3Uploader.updateFile(newFile, oldImageUrl, dirName);
            Image image = new Image(newFile.getOriginalFilename(), targetType);
            image.assignImageUrl(newImageUrl);

            return imageRepository.save(image);
        } catch (IOException e) {
            log.error("이미지 업데이트 실패: {}", e.getMessage());
            throw new InvalidInputException(FAIL_UPLOAD_FILE);
        }
    }
}
