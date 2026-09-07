package com.Soo_Shinsa.order.controller;

import com.Soo_Shinsa.order.service.TossPaymentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 결제사가 브라우저를 돌려보내는 콜백.
 *
 * 예전에는 "success" / "cancel" 같은 뷰 이름을 반환했는데 templates 디렉터리 자체가 없어서
 * 결제 승인 직후 500 으로 끝났다. 이 프로젝트의 화면은 static/index.html 한 장이므로
 * 템플릿을 새로 만들지 않고 그 페이지로 결과를 붙여 돌려보낸다.
 *
 * 주소(/api/success, /api/fail)는 토스 쪽에 등록된 값이라 바꾸지 않는다.
 * ApiPathConfig 의 /api/v1 접두사 대상이 아니도록 @Controller 로 둔다.
 */
@Slf4j
@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class TossCallbackController {

    private final TossPaymentsService tossPaymentsService;

    @GetMapping("/success")
    public String success(@RequestParam String paymentKey,
                          @RequestParam String orderId,
                          @RequestParam Long amount) {
        try {
            tossPaymentsService.approvePayment(paymentKey, orderId, amount);
            return redirect("success", orderId, null);
        } catch (Exception e) {
            // 승인 실패를 JSON 예외로 뱉으면 사용자는 결제창에서 날것의 오류를 본다
            log.warn("결제 승인 실패 orderId={}", orderId, e);
            return redirect("fail", orderId, e.getMessage());
        }
    }

    @GetMapping("/fail")
    public String fail(@RequestParam(required = false) String orderId,
                       @RequestParam(required = false) String message) {
        log.info("결제 실패 콜백 orderId={} message={}", orderId, message);
        return redirect("fail", orderId, message);
    }

    private String redirect(String result, String orderId, String message) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/").queryParam("payment", result);
        if (orderId != null) {
            uri.queryParam("orderId", orderId);
        }
        if (message != null) {
            uri.queryParam("message", message);
        }
        return "redirect:" + uri.build().encode().toUriString();
    }
}
