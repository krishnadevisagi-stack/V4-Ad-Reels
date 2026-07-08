package com.example.data.utils

import java.security.MessageDigest
import java.util.regex.Pattern

object SecurityUtils {
    fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            password // Fallback in case of unexpected environment errors, though SHA-256 is standard
        }
    }
}

object EmailValidator {
    private val EMAIL_PATTERN = Pattern.compile(
        "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" +
                "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
    )

    fun isValid(email: String): Boolean {
        return email.isNotBlank() && EMAIL_PATTERN.matcher(email).matches()
    }
}

object PasswordValidator {
    fun isValid(password: String): Boolean {
        // Minimum 6 characters, has at least one letter and one number
        if (password.length < 6) return false
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }
}

object MobileValidator {
    private val MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$") // Standard 10 digit Indian mobile

    fun isValid(mobile: String): Boolean {
        return mobile.isNotBlank() && MOBILE_PATTERN.matcher(mobile).matches()
    }
}

object ValidationManager {
    fun validateRegistration(
        fullName: String,
        email: String,
        mobile: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        if (fullName.isBlank()) {
            return ValidationResult.Error("Full Name cannot be empty.")
        }
        if (!EmailValidator.isValid(email)) {
            return ValidationResult.Error("Please enter a valid email address.")
        }
        if (!MobileValidator.isValid(mobile)) {
            return ValidationResult.Error("Please enter a valid 10-digit mobile number starting with 6-9.")
        }
        if (!PasswordValidator.isValid(password)) {
            return ValidationResult.Error("Password must be at least 6 characters and contain both letters and digits.")
        }
        if (password != confirmPassword) {
            return ValidationResult.Error("Passwords do not match.")
        }
        return ValidationResult.Success
    }

    fun validateLogin(emailOrMobile: String, password: String): ValidationResult {
        if (emailOrMobile.isBlank()) {
            return ValidationResult.Error("Please enter your Email or Mobile number.")
        }
        if (password.isBlank()) {
            return ValidationResult.Error("Please enter your password.")
        }
        return ValidationResult.Success
    }
}

sealed interface ValidationResult {
    object Success : ValidationResult
    data class Error(val message: String) : ValidationResult
}
