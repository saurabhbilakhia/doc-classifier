package com.example.docai.auth.service

import com.example.docai.auth.dto.*
import com.example.docai.auth.entities.PasswordResetToken
import com.example.docai.auth.entities.User
import com.example.docai.auth.repositories.PasswordResetTokenRepository
import com.example.docai.auth.repositories.UserRepository
import com.example.docai.common.enums.Role
import com.example.docai.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class AuthService(
    private val userRepo: UserRepository,
    private val passwordResetTokenRepo: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailService: EmailService
) {

    @Transactional
    fun register(request: RegisterRequest): MessageResponse {
        if (userRepo.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            role = Role.USER
        )
        userRepo.save(user)

        return MessageResponse("User registered successfully")
    }

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepo.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!user.enabled) {
            throw IllegalArgumentException("Account is disabled")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        val accessToken = jwtService.generateAccessToken(user.email, user.role.name)
        val refreshToken = jwtService.generateRefreshToken(user.email)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = user.email,
            role = user.role.name
        )
    }

    fun refresh(request: RefreshRequest): LoginResponse {
        if (!jwtService.validateToken(request.refreshToken) ||
            !jwtService.isTokenType(request.refreshToken, "refresh")
        ) {
            throw IllegalArgumentException("Invalid refresh token")
        }

        val email = jwtService.extractEmail(request.refreshToken)
            ?: throw IllegalArgumentException("Invalid token")

        val user = userRepo.findByEmail(email)
            ?: throw IllegalArgumentException("User not found")

        val accessToken = jwtService.generateAccessToken(user.email, user.role.name)
        val refreshToken = jwtService.generateRefreshToken(user.email)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = user.email,
            role = user.role.name
        )
    }

    @Transactional
    fun forgotPassword(request: ForgotPasswordRequest): MessageResponse {
        val user = userRepo.findByEmail(request.email)
            ?: return MessageResponse("If the email exists, a reset link has been sent")

        val token = UUID.randomUUID().toString()
        val resetToken = PasswordResetToken(
            user = user,
            token = token,
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        passwordResetTokenRepo.save(resetToken)

        emailService.sendPasswordResetEmail(user.email, token)

        return MessageResponse("If the email exists, a reset link has been sent")
    }

    @Transactional
    fun resetPassword(request: ResetPasswordRequest): MessageResponse {
        val resetToken = passwordResetTokenRepo.findByToken(request.token)
            ?: throw IllegalArgumentException("Invalid or expired token")

        if (resetToken.used) {
            throw IllegalArgumentException("Token already used")
        }

        if (resetToken.expiresAt.isBefore(Instant.now())) {
            throw IllegalArgumentException("Token expired")
        }

        val user = resetToken.user
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepo.save(user)

        resetToken.used = true
        passwordResetTokenRepo.save(resetToken)

        return MessageResponse("Password reset successfully")
    }
}
