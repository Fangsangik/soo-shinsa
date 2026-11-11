package com.Soo_Shinsa.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 프론트(app.js api())는 실패 시 json.message 를 토스트로 띄운다 — 모든 예외가 {httpStatus, message} JSON 으로 나가는지. */
class GlobalExceptionControllerTest {
    MockMvc mvc;

    @RestController
    static class Boom {
        @GetMapping("/nf") void nf() { throw new NotFoundException(ErrorCode.NOT_FOUND_ORDER); }
        @GetMapping("/bad") void bad() { throw new InvalidInputException(ErrorCode.INVALID_ORDER_STATUS); }
        @GetMapping("/auth") void auth() { throw new NoAuthorizedException(ErrorCode.NO_AUTHORITY); }
        @PostMapping("/valid") void valid(@Valid @RequestBody Body b) {}
        static class Body { @NotNull(message = "수량을 입력해주세요.") public Integer quantity; }
    }

    @BeforeEach
    void setUp() { mvc = MockMvcBuilders.standaloneSetup(new Boom()).setControllerAdvice(new GlobalExceptionController()).build(); }

    @Test
    void 도메인_예외는_ErrorCode_의_상태와_메시지로() throws Exception {
        mvc.perform(get("/nf")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("오더를 찾을 수 없습니다."));
        mvc.perform(get("/bad")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(ErrorCode.INVALID_ORDER_STATUS.getMessage()));
        mvc.perform(get("/auth")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.httpStatus").value("UNAUTHORIZED"));
    }

    @Test
    void 검증_실패는_첫_필드_메시지를_400으로() throws Exception {
        mvc.perform(post("/valid").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("수량을 입력해주세요."));
    }
}
