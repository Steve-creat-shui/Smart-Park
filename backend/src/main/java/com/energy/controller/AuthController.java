package com.energy.controller;

import com.energy.dto.*;
import com.energy.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-code")
    public ApiResponse<Map<String, String>> sendCode(@Valid @RequestBody SendCodeRequest req) {
        authService.sendCode(req.getPhone());
        return ApiResponse.success(Map.of("message", "验证码已发送，请查看后端控制台"));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse resp = authService.register(req);
        return ApiResponse.success(resp);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req);
        return ApiResponse.success(resp);
    }
}
