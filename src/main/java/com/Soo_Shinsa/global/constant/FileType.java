package com.Soo_Shinsa.global.constant;

import java.util.Arrays;

public enum FileType {
    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    JPEG("jpeg", "image/jpeg");

    private final String extension;
    private final String mimeType;

    FileType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }


    /**
     * 확장자와 MIME 타입의 유효성을 검증
     * @param extension 파일 확장자
     * @param mimeType MIME 타입
     * @return 유효한 경우 true
     */
    public static boolean isValid(String extension, String mimeType) {
        return Arrays.stream(values())
                .anyMatch(fileType -> fileType.extension.equals(extension) && fileType.mimeType.equals(mimeType));
    }
}

