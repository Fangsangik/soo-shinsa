package com.Soo_Shinsa.image.model;

import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.global.constant.FilePath;
import com.Soo_Shinsa.global.constant.TargetType;
import com.Soo_Shinsa.global.utils.FileUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Image extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    private String originName;
    private String name;

    private String path;

    private String extension;

    public Image(Long targetId, TargetType targetType, String originName, String path) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.originName = originName;
        this.name = UUID.randomUUID().toString();
        this.path = path;
        this.extension = FileUtils.extractFileExtension(originName);
    }

    public Image(String originName, TargetType targetType) {
        // 안전한 확장자 추출
        this.originName = FileUtils.extractFileName(Objects.requireNonNull(originName, "파일명은 필수입니다."));
        this.extension = FileUtils.extractFileExtension(originName);

        // 저장 파일명은 UUID로 생성
        this.name = UUID.randomUUID().toString();
        this.path = determinePath(targetType);
    }

    /**
     * TargetType에 따라 적절한 경로를 반환
     * @param targetType 이미지의 대상 타입
     * @return 적절한 경로
     */
    public static String determinePath(TargetType targetType) {
        return switch (targetType) {
            case PRODUCT -> FilePath.PRODUCT_DIR;
            case REVIEW -> FilePath.REVIEW_DIR;
            case BRAND -> FilePath.BRAND_DIR;

            default -> throw new IllegalArgumentException("지원하지 않는 대상 타입입니다.");
        };
    }

    public void assignImageUrl(String imageUrl) {
        if (imageUrl == null) {
            throw new IllegalArgumentException("이미지 URL은 필수입니다.");
        }
        this.path = imageUrl;
    }
}
