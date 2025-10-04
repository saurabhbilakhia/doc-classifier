package com.example.docai.auth.entities

import com.example.docai.common.enums.Role
import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false, length = 320)
    var email: String,

    @Column(nullable = false, length = 100)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = Role.USER,

    @Column(nullable = false)
    var enabled: Boolean = true
)
