package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.adengine.SecurityThreats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecurityBlockScreen(
    threats: SecurityThreats,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F121E)) // Elegant deep cosmic slate background
            .windowInsetsPadding(WindowInsets.safeContent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Secure Shield Warning Icon Header
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF4D4D).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (threats.isAppModified) Icons.Default.Info else Icons.Default.Warning,
                    contentDescription = "Security Threat Warning",
                    tint = Color(0xFFFF4D4D),
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("security_threat_icon")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Access Restricted Title
            Text(
                text = if (threats.isAppModified) "Application Tampered" else "Access Restricted",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("security_screen_title")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "To preserve reward integrity, Sponza prevents earning in environments that block ads or use modified software.",
                fontSize = 14.sp,
                color = Color(0xFF8E99B5),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card list of detected issues
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E233C))
                    .border(1.dp, Color(0xFF2E375C), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Detected Security Failures",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFECF0F6)
                )

                Divider(color = Color(0xFF2E375C), thickness = 1.dp)

                // 1. App Modification
                if (threats.isAppModified) {
                    SecurityIssueRow(
                        title = "Modified / Non-Original Application",
                        description = "This app has been modified or re-signed. Running modified packages is strictly blocked. Please install the official build.",
                        severity = "Critical Block"
                    )
                }

                // 2. VPN Detection
                if (threats.hasVpn) {
                    SecurityIssueRow(
                        title = "VPN Active",
                        description = "You are connected to a Virtual Private Network (VPN). Please disconnect your VPN to resume using Sponza.",
                        severity = "Connection Blocked"
                    )
                }

                // 3. Proxy Detection
                if (threats.hasProxy) {
                    SecurityIssueRow(
                        title = "Network Proxy Active",
                        description = "An HTTP or SOCKS proxy has been configured on your device or connection. Please turn off proxies in your network settings.",
                        severity = "Proxy Blocked"
                    )
                }

                // 4. Ad Blocker Apps
                if (threats.detectedAdBlockers.isNotEmpty()) {
                    SecurityIssueRow(
                        title = "Ad-Blocking App Installed",
                        description = "The following suspicious ad-blocking or patching tools were found: ${threats.detectedAdBlockers.joinToString(", ")}. You must uninstall or disable them to continue.",
                        severity = "App Conflict"
                    )
                }

                // 5. DNS Ad Blocking
                if (threats.dnsAdBlockActive && !threats.hasVpn && !threats.hasProxy) {
                    SecurityIssueRow(
                        title = "Local DNS / Hosts Ad Block",
                        description = "Our connections to standard ad providers are being filtered or redirected (e.g., via Pi-hole or AdGuard DNS). Please use standard DNS settings.",
                        severity = "DNS Filtered"
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Re-scan Button
            Button(
                onClick = {
                    if (!isScanning) {
                        coroutineScope.launch {
                            isScanning = true
                            delay(1200) // Simulating high-fidelity scanning feedback
                            onRescan()
                            isScanning = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("rescan_security_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Color(0xFF1E233C) else Color(0xFFFF4D4D),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !isScanning
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analyzing Environment...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Re-scan Device",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityIssueRow(
    title: String,
    description: String,
    severity: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF4D4D))
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF4D4D).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = severity.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF4D4D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF8E99B5),
                lineHeight = 16.sp
            )
        }
    }
}
