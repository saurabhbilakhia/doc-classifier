package com.example.docai.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

@Configuration
class MailConfig(
    @Value("\${mail.host}") private val host: String,
    @Value("\${mail.port}") private val port: Int,
    @Value("\${mail.username:}") private val username: String?,
    @Value("\${mail.password:}") private val password: String?
) {

    @Bean
    fun javaMailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port

        if (!username.isNullOrBlank()) {
            mailSender.username = username
        }
        if (!password.isNullOrBlank()) {
            mailSender.password = password
        }

        val props = Properties()
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "false"
        props["mail.smtp.starttls.enable"] = "false"
        props["mail.debug"] = "false"

        mailSender.javaMailProperties = props
        return mailSender
    }
}
