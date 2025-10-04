package com.example.docai.auth.controller

import com.example.docai.auth.dto.*
import com.example.docai.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<MessageResponse> {
        return ResponseEntity.ok(authService.register(request))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<LoginResponse> {
        return ResponseEntity.ok(authService.refresh(request))
    }

    @PostMapping("/forgot")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<MessageResponse> {
        return ResponseEntity.ok(authService.forgotPassword(request))
    }

    @PostMapping("/reset")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<MessageResponse> {
        return ResponseEntity.ok(authService.resetPassword(request))
    }
}
