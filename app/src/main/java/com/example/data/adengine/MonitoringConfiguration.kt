package com.example.data.adengine

/**
 * MonitoringConfiguration
 * Holds all configuration flags and thresholds for analytics, logging, performance, and error reporting.
 */
data class MonitoringConfiguration(
    val loggingEnabled: Boolean = true,
    val verboseLoggingInDev: Boolean = true,
    val analyticsTrackingEnabled: Boolean = true,
    val performanceMonitoringEnabled: Boolean = true,
    val errorReportingEnabled: Boolean = true,
    val debugDashboardVisible: Boolean = true, // Development mode debug stats
    val statsRetentionPeriodDays: Int = 30,    // Days to keep history
    val slowQueryThresholdMs: Long = 50,       // Threshold to flag slow database queries
    val frameDropThresholdMs: Long = 16,       // Normal frame time (60 FPS ~16.6ms)
    val criticalErrorAutoRecovery: Boolean = true
) {
    companion object {
        val Default = MonitoringConfiguration()
    }
}
