package com.Soo_Shinsa.wish.controller;

import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.wish.dto.WishResponseDto;
import com.Soo_Shinsa.wish.service.WishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishes")
@RequiredArgsConstructor
@Tag(name = "Wish API", description = "찜 관련 API")
public class WishController {

    private final WishService wishService;

    @PostMapping("/{productId}")
    @Operation(summary = "찜 추가", description = "상품을 찜 목록에 추가합니다.")
    public ResponseEntity<CommonResponse<WishResponseDto>> add(@AuthenticationPrincipal UserDetails userDetails,
                                                               @PathVariable Long productId) {
        User user = UserUtils.getUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResponse<>("찜에 추가했습니다.", wishService.add(user, productId)));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "찜 해제", description = "상품을 찜 목록에서 제거합니다.")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long productId) {
        wishService.remove(UserUtils.getUser(userDetails), productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "내 찜 목록", description = "내가 찜한 상품 목록을 조회합니다.")
    public ResponseEntity<CommonResponse<List<WishResponseDto>>> findMine(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                new CommonResponse<>("찜 목록 조회에 성공했습니다.", wishService.findMine(UserUtils.getUser(userDetails))));
    }
}
