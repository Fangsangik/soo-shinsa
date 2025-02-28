package com.Soo_Shinsa.global.utils;

import com.Soo_Shinsa.global.exception.InvalidInputException;

import static com.Soo_Shinsa.global.exception.ErrorCode.NO_EXTENSION;

public class FileUtils {

    /**
     * 파일 이름에서 확장자를 추출
     *
     * @param originName 원본 파일명
     * @return 확장자 (소문자)
     * @throws IllegalArgumentException
     */
    public static String extractFileExtension(String originName) {
        int dotIndex = originName.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex == originName.length() - 1) {
            throw new InvalidInputException(NO_EXTENSION);
        }
        return originName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 파일 이름에서 확장자를 제외한 이름만 추출
     *
     * @param originName 원본 파일명
     * @return 확장자를 제외한 파일명
     */
    public static String extractFileName(String originName) {
        int dotIndex = originName.lastIndexOf(".");
        if (dotIndex == -1) {
            return originName; // 확장자가 없는 경우 전체 파일명 반환
        }
        return originName.substring(0, dotIndex); // 확장자 이전의 파일명 반환
    }
}
