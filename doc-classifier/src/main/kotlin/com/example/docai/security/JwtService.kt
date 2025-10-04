package com.example.docai.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.issuer}") private val issuer: String,
    @Value("\${jwt.access-token-ttl-minutes}") private val accessTokenTtlMinutes: Long,
    @Value("\${jwt.refresh-token-ttl-days}") private val refreshTokenTtlDays: Long
) {

    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generateAccessToken(email: String, role: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .claim("type", "access")
            .setIssuer(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun generateRefreshToken(email: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .setSubject(email)
            .claim("type", "refresh")
            .setIssuer(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(refreshTokenTtlDays, ChronoUnit.DAYS)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extractClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    fun extractEmail(token: String): String? {
        return try {
            extractClaims(token).subject
        } catch (e: Exception) {
            null
        }
    }

    fun extractRole(token: String): String? {
        return try {
            extractClaims(token)["role"] as? String
        } catch (e: Exception) {
            null
        }
    }

    fun isTokenType(token: String, type: String): Boolean {
        return try {
            extractClaims(token)["type"] == type
        } catch (e: Exception) {
            false
        }
    }
}
