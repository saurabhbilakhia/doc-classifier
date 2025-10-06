package com.example.docai.auth.entities

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(unique = true, nullable = false)
    var token: String,

    @Column(nullable = false)
    var expiresAt: Instant,

    @Column(nullable = false)
    var used: Boolean = false
)
