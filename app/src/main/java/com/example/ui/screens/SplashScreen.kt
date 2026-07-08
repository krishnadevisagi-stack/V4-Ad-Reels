package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AdViewModel
import kotlinx.coroutines.delay
import com.example.ui.components.SponzaLogo

@Composable
fun SplashScreen(
    viewModel: AdViewModel,
    onNavigateNext: (targetRoute: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    var startDelayTriggered by remember { mutableStateOf(false) }

    // Start delay to show the stunning splash screen
    LaunchedEffect(Unit) {
        delay(3000) // Show for 3 seconds
        startDelayTriggered = true
    }

    // Handle navigation once delay finishes and profile is loaded
    LaunchedEffect(startDelayTriggered, userProfile) {
        if (startDelayTriggered) {
            val profile = userProfile
            if (profile != null) {
                if (profile.isLoggedIn) {
                    if (profile.selectedCategories.isBlank()) {
                        onNavigateNext("interests/${profile.username}/${profile.email}")
                    } else {
                        onNavigateNext("main")
                    }
                } else {
                    onNavigateNext("login")
                }
            } else {
                // If no profile found, double check after a tiny extra wait or go to login
                delay(300)
                if (viewModel.userProfile.value == null) {
                    onNavigateNext("login")
                } else {
                    val p = viewModel.userProfile.value
                    if (p != null) {
                        if (p.isLoggedIn) {
                            if (p.selectedCategories.isBlank()) {
                                onNavigateNext("interests/${p.username}/${p.email}")
                            } else {
                                onNavigateNext("main")
                            }
                        } else {
                            onNavigateNext("login")
                        }
                    } else {
                        onNavigateNext("login")
                    }
                }
            }
        }
    }

    // Animation state for scaling logo
    val infiniteTransition = rememberInfiniteTransition(label = "logo_scale")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale_val"
    )

    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val gradientColors = if (isDarkTheme) {
        listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF0F172A)
        )
    } else {
        listOf(
            Color(0xFFF8FAFC),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // High-fidelity custom Sponza logo
            SponzaLogo(
                size = 140.dp * logoScale,
                transparent = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sponza",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Transforming Your Attention into Real Coin Rewards",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Warning Banner for Ad Platform (Strictly Transparent as requested)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.9f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Warning Info Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HONEST AD-REWARDS PLATFORM",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This application strictly operates on a transparent ad-for-rewards model. Every programmatic ad or vertical reel ad you view here awards premium coins directly creditable to your secure ledger wallet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Preparing curated advertisement feeds...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun DisclaimerScreen(
    viewModel: AdViewModel,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val gradientColors = if (isDarkTheme) {
        listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF0F172A)
        )
    } else {
        listOf(
            Color(0xFFF8FAFC),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Large Warning/Shield Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    .border(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Alert Icon",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Platform Disclosure & Terms",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Please read and consent to proceed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.9f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "IMPORTANT DISCLOSURE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "You are about to enter the live dashboard of Sponza.\n\n" +
                               "As an active member of this platform, you will be shown personalized and sponsored programmatic advertisements, including premium native video ads, interactive banners, and rewarded vertical video reels.\n\n" +
                               "In direct exchange for your attention and engagement, you will receive secure digital coins deposited directly into your user ledger wallet, which can be accumulated and redeemed for premium rewards.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "By clicking 'Proceed', you confirm you are at least 18 years of age, consent to viewing programmatic ads, and agree to the Coin Redemption Guidelines. If you do not consent, click 'Exit App'.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exit App Button
                OutlinedButton(
                    onClick = {
                        // Log user out so they don't auto-login next time, and finish activity
                        viewModel.logout()
                        val activity = context as? Activity
                        activity?.finish()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Exit App icon",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit App", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Proceed Button
                Button(
                    onClick = {
                        onProceed()
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Proceed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Proceed arrow",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
