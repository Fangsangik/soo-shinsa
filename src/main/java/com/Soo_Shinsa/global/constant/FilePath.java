package com.Soo_Shinsa.global.constant;

public class FilePath {
    public static final String IMAGE_BASE_DIR = "images/";
    public static final String PRODUCT_DIR = IMAGE_BASE_DIR + "product/";
    public static final String REVIEW_DIR = IMAGE_BASE_DIR + "review/";
    public static final String BRAND_DIR = IMAGE_BASE_DIR + "brand/";

    public static final int MAX_FILE_SIZE_MB = 10;

    // 인스턴스화 방지
    private FilePath() {
    }
}
