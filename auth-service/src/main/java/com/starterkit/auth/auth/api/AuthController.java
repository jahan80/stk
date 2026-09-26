package com.starterkit.auth.auth.api;

import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.api.response.ApiResponse;
import com.starterkit.auth.shared.api.response.ApiResponseFactory;
import com.starterkit.auth.auth.api.dto.LoginRequest;
import com.starterkit.auth.auth.api.dto.LoginResponse;
import com.starterkit.auth.auth.api.dto.RegisterRequest;
import com.starterkit.auth.auth.api.dto.RegisterResponse;
import com.starterkit.auth.auth.api.dto.UserResponse;
import com.starterkit.auth.auth.application.service.AuthService;
import com.starterkit.auth.shared.infrastructure.jwt.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ApiResponseFactory responseFactory;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse userResponse = authService.register(request);

        return responseFactory.success(
                ApiCode.USER_REGISTERED,
                userResponse
        );
    }
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return responseFactory.success(
                ApiCode.SUCCESS,
                response
        );
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserResponse response = authService.me(principal.getId());

        return responseFactory.success(
                ApiCode.SUCCESS,
                response
        );
    }
}
