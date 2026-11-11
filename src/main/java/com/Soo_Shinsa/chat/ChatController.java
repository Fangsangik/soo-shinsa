package com.Soo_Shinsa.chat;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.CommonResponse;
import com.Soo_Shinsa.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final BrandRepository brandRepository;
    private final ChatHistory history;

    public record Room(Long brandId, String brandName, ChatMessage last) { }

    /** 들어갈 수 있는 상담방 목록 + 마지막 메시지. VENDOR 는 자기 브랜드, 나머지는 전체 브랜드. */
    @GetMapping("/rooms")
    public ResponseEntity<CommonResponse<List<Room>>> rooms(@AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        List<Brand> brands = user.isVendor() ? brandRepository.findAllByUserUserId(user.getUserId()) : brandRepository.findAll(PageRequest.of(0, 200)).getContent();
        List<Room> rooms = brands.stream().map(b -> new Room(b.getId(), b.getName(), history.last(String.valueOf(b.getId())))).toList();
        return ResponseEntity.ok(new CommonResponse<>("상담방 목록", rooms));
    }
}
