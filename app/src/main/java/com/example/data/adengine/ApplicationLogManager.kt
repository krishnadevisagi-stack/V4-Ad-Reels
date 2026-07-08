package com.example.data.adengine

import android.util.Log

/**
 * ApplicationLogManager
 * Responsibilities:
 *  - Handles verbose logs in development, minimal in production.
 *  - Automatically scrubs sensitive info (Passwords, UPI IDs, Secrets, Tokens).
 */
object ApplicationLogManager {

    private var isDevelopment: Boolean = true // Flag to toggle verbose logging

    fun setDevelopmentMode(enabled: Boolean) {
        isDevelopment = enabled
    }

    /**
     * Log info messages. Under production, only critical logs are output.
     */
    fun i(tag: String, message: String) {
        val sanitized = sanitize(message)
        if (isDevelopment) {
            Log.i(tag, sanitized)
        } else {
            // In production, keep info logs minimal or skip them
            Log.d(tag, "[PROD] $sanitized")
        }
    }

    /**
     * Log debug messages. Skip completely or make extremely minimal in production.
     */
    fun d(tag: String, message: String) {
        if (isDevelopment) {
            Log.d(tag, sanitize(message))
        }
    }

    /**
     * Log error messages. Always log error messages but make sure they are sanitized.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = sanitize(message)
        if (throwable != null) {
            Log.e(tag, sanitized, throwable)
        } else {
            Log.e(tag, sanitized)
        }
    }

    /**
     * Sanitizes strings to prevent leaking personal info, UPI, password, tokens, or wallet secrets.
     */
    fun sanitize(message: String): String {
        var result = message
        // Regex to scrub UPI ids or phone numbers
        val upiRegex = "[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}".toRegex()
        val phoneRegex = "\\b\\d{10}\\b".toRegex()
        val secretsRegex = "(?i)(password|secret|token|upiId|key|credentials)\\s*[:=]\\s*[^\\s,]+".toRegex()

        result = result.replace(upiRegex, "[UPI_SCRUBBED]")
        result = result.replace(phoneRegex, "[PHONE_SCRUBBED]")
        result = result.replace(secretsRegex) { match ->
            val param = match.groupValues[1]
            "$param=[SCRUBBED]"
        }
        return result
    }
}
