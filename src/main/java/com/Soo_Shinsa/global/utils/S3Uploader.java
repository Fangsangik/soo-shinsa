package com.Soo_Shinsa.global.utils;

import com.Soo_Shinsa.global.constant.FilePath;
import com.Soo_Shinsa.global.constant.FileType;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.UUID;

import static com.Soo_Shinsa.global.exception.ErrorCode.FAIL_IMAGE_DELETE;
import static com.Soo_Shinsa.global.exception.ErrorCode.MAX_10MB_SIZE;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {
    private final AmazonS3 amazonS3;
    private final String bucket;

    private static final long MAX_FILE_SIZE_BYTES = FilePath.MAX_FILE_SIZE_MB * 1024L * 1024L; // 10MB


    /**
     * S3로 파일 업로드
     * @param multipartFile
     * @param dirName
     * @return uploadImageUrl
     * @throws IOException
     */
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        validateFile(multipartFile);

        String originalFileName = multipartFile.getOriginalFilename();
        // UUId를 통해 파일명 추가
        String uuid = UUID.randomUUID().toString();
        String uniqueFileName = uuid + "_" + originalFileName.replaceAll("\\s", "_");

        String fileName = dirName + "/" + uniqueFileName;
        log.info("fileName: {}", fileName);
        File uploadFile = convert(multipartFile);

        String uploadImageUrl = putS3(uploadFile, fileName);
        removeNewFile(uploadFile);
        return uploadImageUrl;
    }


    public void deleteFile (String fileName) {
        try {
            // URL 디코딩을 통해 원래의 파일 이름을 가져옵니다.
            String decodedFileName = URLDecoder.decode(fileName, "UTF-8");
            log.info("S3 파일 삭제: " + decodedFileName);
            amazonS3.deleteObject(bucket, decodedFileName);
        } catch (UnsupportedEncodingException e) {
            log.error("디코딩 하는 중 파일 이름이 잘못되었습니다.: {}", e.getMessage());
        }
    }

    public String updateFile(MultipartFile multipartFile, String oldFileName, String dirName) throws IOException {
        // 기존 파일 삭제
        deleteFile(oldFileName);
        // 새로운 파일 업로드
        return upload(multipartFile, dirName);
    }

    /**
     * MultipartFile을 File로 변환
     * @param multipartFile
     * @return convertFile
     * @throws IOException
     */
    private File convert(MultipartFile multipartFile) throws IOException {
        String originalFileName = Objects.requireNonNull(multipartFile.getOriginalFilename(), "파일명은 필수입니다.");
        String uuid = UUID.randomUUID().toString();
        String uniqueFileName = uuid + "_" + originalFileName.replaceAll("\\s", "_");

        File convertFile = new File(uniqueFileName);
        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(multipartFile.getBytes());
            } catch (IOException e) {
                log.error("파일 변환 중 오류 발생: {}", e.getMessage());
                throw e;
            }
            return convertFile;
        }

        throw new IllegalArgumentException(String.format("파일 변환 실패: %s", originalFileName));
    }

    /**
     * 로컬에 생성된 파일 삭제
     * @param uploadFile
     */
    private void removeNewFile(File uploadFile) {
        if (!uploadFile.delete()) {
            log.error("로컬 파일이 삭제되지 않았습니다.");
            throw new InvalidInputException(FAIL_IMAGE_DELETE);
        }
        log.info("로컬 파일이 삭제되었습니다.");
    }

    private String putS3(File uploadFile, String fileName) {
        amazonS3.putObject(new PutObjectRequest(bucket, fileName, uploadFile)
                .withCannedAcl(CannedAccessControlList.PublicRead));
        return amazonS3.getUrl(bucket, fileName).toString();
    }

    private void validateFile(MultipartFile file) {
        String originName = Objects.requireNonNull(file.getOriginalFilename(), "파일명은 필수입니다.");
        if (originName.trim().isEmpty() || originName.lastIndexOf(".") <= 0) {
            throw new IllegalArgumentException("잘못된 파일명입니다: " + originName);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidInputException(MAX_10MB_SIZE);
        }

        String extension = FileUtils.extractFileExtension(originName);
        String mimeType = file.getContentType();

        if (!FileType.isValid(extension, mimeType)) {
            throw new IllegalArgumentException("확장자와 MIME 타입이 일치하지 않습니다: 확장자=" + extension + ", MIME 타입=" + mimeType);
        }
    }
}
