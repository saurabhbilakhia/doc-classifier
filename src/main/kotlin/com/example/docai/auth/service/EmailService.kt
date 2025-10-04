package com.example.docai.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${mail.from}") private val fromEmail: String
) {

    fun sendPasswordResetEmail(email: String, token: String) {
        val resetLink = "http://localhost:8080/reset?token=$token"
        val message = SimpleMailMessage()
        message.from = fromEmail
        message.setTo(email)
        message.subject = "Password Reset Request"
        message.text = """
            You requested a password reset.

            Click the link below to reset your password:
            $resetLink

            This link will expire in 1 hour.

            If you did not request this, please ignore this email.
        """.trimIndent()

        try {
            mailSender.send(message)
        } catch (e: Exception) {
            // Log error but don't fail the request
            println("Failed to send email: ${e.message}")
        }
    }
}
