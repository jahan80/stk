package com.starterkit.auth.auth.api;

import com.starterkit.auth.auth.api.dto.ForgotPasswordRequest;
import com.starterkit.auth.auth.api.dto.LoginRequest;
import com.starterkit.auth.auth.api.dto.ResetPasswordRequest;
import com.starterkit.auth.auth.api.dto.LoginResponse;
import com.starterkit.auth.auth.api.dto.RefreshTokenRequest;
import com.starterkit.auth.auth.api.dto.RegisterRequest;
import com.starterkit.auth.auth.api.dto.RegisterResponse;
import com.starterkit.auth.auth.api.dto.ResendVerificationRequest;
import com.starterkit.auth.auth.api.dto.VerifyEmailRequest;
import com.starterkit.auth.auth.api.dto.VerifyMobileRequest;
import com.starterkit.auth.auth.api.dto.ResendMobileVerificationRequest;
import com.starterkit.auth.auth.api.dto.UserResponse;
import com.starterkit.auth.auth.application.service.AuthService;
import com.starterkit.auth.auth.application.service.EmailVerificationService;
import com.starterkit.auth.auth.application.service.MobileVerificationService;
import com.starterkit.auth.auth.application.service.PasswordResetService;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
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
    private final EmailVerificationService emailVerificationService;
    private final MobileVerificationService mobileVerificationService;
    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);

        return responseFactory.success(
                ApiCode.USER_REGISTERED,
                response
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

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        LoginResponse response = authService.refresh(request);

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

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(request);

        return responseFactory.success(
                ApiCode.SUCCESS,
                null
        );
    }


    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        emailVerificationService.verifyCode(
                request.getEmail(),
                request.getCode()
        );

        return responseFactory.success(
                ApiCode.SUCCESS,
                null
        );
    }

    @PostMapping("/email/resend-verification")
    public ApiResponse<Void> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        // Find user by email
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.starterkit.auth.auth.application.exception.InvalidVerificationCodeException());

        emailVerificationService.sendVerificationCode(user);

        return responseFactory.success(
                ApiCode.SUCCESS,
                null
        );
    }


    @PostMapping("/mobile/verify")
    public ApiResponse<Void> verifyMobile(
            @Valid @RequestBody VerifyMobileRequest request
    ) {
        mobileVerificationService.verifyCode(
                request.getMobileNumber(),
                request.getCode()
        );

        return responseFactory.success(
                ApiCode.SUCCESS,
                null
        );
    }

    @PostMapping("/mobile/resend-verification")
    public ApiResponse<Void> resendMobileVerification(
            @Valid @RequestBody ResendMobileVerificationRequest request
    ) {
        var user = userRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new com.starterkit.auth.auth.application.exception.InvalidMobileVerificationCodeException());

        mobileVerificationService.sendVerificationCode(user);

        return responseFactory.success(
                ApiCode.SUCCESS,
                null
        );
    }


    @PostMapping("/password/forgot")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.sendResetCode(request.getEmail());

        return responseFactory.success(
                ApiCode.SUCCESS,
                null
        );
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword()
        );

        return responseFactory.success(
                ApiCode.SUCCESS,
                null
        );
    }

}
