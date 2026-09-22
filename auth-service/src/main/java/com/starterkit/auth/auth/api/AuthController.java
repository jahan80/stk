package com.starterkit.auth.auth.api;

import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.api.response.ApiResponse;
import com.starterkit.auth.shared.api.response.ApiResponseFactory;
import com.starterkit.auth.auth.application.dto.LoginRequest;
import com.starterkit.auth.auth.application.dto.LoginResponse;
import com.starterkit.auth.auth.application.dto.RegisterRequest;
import com.starterkit.auth.auth.application.dto.UserResponse;
import com.starterkit.auth.auth.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ApiResponse<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse userResponse = authService.register(request);

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
}