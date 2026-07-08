package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.PremiumGold
import com.example.ui.theme.SoftGold
import com.example.ui.theme.DarkGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.ErrorRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// CENTRALIZED DESIGN TOKENS
// ==========================================
object DesignTokens {
    val CornerLarge = 24.dp
    val CornerMedium = 16.dp
    val CornerSmall = 12.dp
    
    val PaddingLarge = 20.dp
    val PaddingMedium = 16.dp
    val PaddingSmall = 12.dp
    val PaddingTiny = 8.dp
    
    val ElevationSoft = 1.dp
    val ElevationMedium = 4.dp
    
    val AnimFastMs = 150
    val AnimMediumMs = 300
    val AnimSlowMs = 500
    val AnimWalletMs = 800

    val GradientPremium = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E24), Color(0xFF0F0F12))
    )
    val GradientGoldCard = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2C2512), Color(0xFF141209))
    )
}

// Centralized configuration can be referenced if needed, otherwise we use standard Material theme values
// ==========================================
// METADATA HEADER
// ==========================================
@Composable
fun MetadataHeader(
    brandName: String,
    category: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    // Non-Clickable header and safe advertiser formatting
    val finalAdvertiser = brandName.trim().takeIf { it.isNotBlank() } ?: "Sponsored Advertisement"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.PaddingMedium, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = finalAdvertiser,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                // Elegant, small Material Chip for Sponsored badge (always visible)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = tint.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "SPONSORED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = tint,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                if (category.isNotBlank()) {
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = category.trim(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==========================================
// ADVERTISEMENT CARD
// ==========================================
@Composable
fun AdvertisementCard(
    brandName: String,
    category: String,
    imageUrl: String,
    title: String,
    description: String,
    ctaText: String,
    isValidated: Boolean,
    onCtaClicked: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    isSaved: Boolean,
    modifier: Modifier = Modifier,
    adId: String = "",
    isFullyVisible: Boolean = false,
    onLoadedChanged: ((Boolean) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    var imageLoaded by remember(imageUrl) { mutableStateOf(imageUrl.isBlank()) }
    var imageError by remember(imageUrl) { mutableStateOf<String?>(null) }

    val progress = remember(adId, isValidated) { Animatable(if (isValidated) 1f else 0f) }
    LaunchedEffect(adId, isValidated, isFullyVisible, imageLoaded) {
        if (!isValidated) {
            if (isFullyVisible && imageLoaded) {
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    com.example.data.adengine.SecurityAndAntiFraudManager.recordWatchProgress(adId, elapsed)
                    val currentVal = (elapsed.toFloat() / 3000f).coerceAtMost(1f)
                    progress.snapTo(currentVal)
                    if (currentVal >= 1f) {
                        break
                    }
                    delay(100)
                }
            } else {
                progress.snapTo(0f)
                com.example.data.adengine.SecurityAndAntiFraudManager.recordWatchProgress(adId, 0L)
            }
        } else {
            progress.snapTo(1f)
            com.example.data.adengine.SecurityAndAntiFraudManager.recordWatchProgress(adId, 3000L)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("brand_ad_card_$adId"),
        shape = RoundedCornerShape(DesignTokens.CornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.ElevationSoft)
    ) {
        Column {
            // Header is completely non-clickable to keep it safe and compliant
            MetadataHeader(brandName = brandName, category = category)

            // Image Content area (never cropped awkwardly, uses coil placeholder)
            if (imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageError != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Error Loading Ad",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ad Media Failed to Load",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Impression and rewards suspended.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                             )
                        }
                    } else {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title.takeIf { it.isNotBlank() } ?: "Sponsored Media Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onSuccess = {
                                imageLoaded = true
                                onLoadedChanged?.invoke(true)
                            },
                            onError = { error ->
                                imageLoaded = false
                                imageError = error.result.throwable.message ?: "Failed to load image"
                                onLoadedChanged?.invoke(false)
                            }
                        )
                    }
                }
            }

            // Extremely thin, clean, minimal impression progress indicator line
            LinearProgressIndicator(
                progress = progress.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp),
                color = if (isValidated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                trackColor = if (isValidated) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // Description and CTA Buttons
            Column(modifier = Modifier.padding(DesignTokens.PaddingMedium)) {
                // Headline Missing Strategy: Hide Headline if empty/blank
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Body Missing Strategy: Hide/collapse layout if description is blank
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // CTA Missing Strategy: Hide/remove CTA button area completely if ctaText is empty/blank
                    if (ctaText.isNotBlank()) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val proceed = com.example.data.adengine.SecurityAndAntiFraudManager.registerAndEvaluateClick(adId, brandName)
                                if (proceed) {
                                    onCtaClicked()
                                }
                            },
                            shape = RoundedCornerShape(DesignTokens.CornerSmall),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("ad_cta_button_$adId"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Open",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowOutward,
                                contentDescription = "CTA out",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleSave()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save Ad",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShare()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Ad",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PREMIUM WALLET CARD
// ==========================================
@Composable
fun WalletCard(
    totalCoins: Int,
    usdEquivalent: Double,
    todayCoins: Int,
    pendingCoins: Int,
    redeemableCoins: Int,
    lifetimeCoins: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wallet_balance_card"),
        shape = RoundedCornerShape(DesignTokens.CornerLarge),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DesignTokens.GradientPremium)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(DesignTokens.CornerLarge))
                .padding(DesignTokens.PaddingLarge)
        ) {
            // Gold Chip / Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = PremiumGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPONZA COIN",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PremiumGold.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SECURED",
                        color = PremiumGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$totalCoins",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = String.format("≈ ₹%.2f Rupya", usdEquivalent),
                        color = PremiumGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(20.dp))

            // Sub balances Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SubBalanceItem(title = "Today", count = todayCoins, color = PremiumGold)
                SubBalanceItem(title = "Pending", count = pendingCoins, color = ErrorRed)
                SubBalanceItem(title = "Redeemable", count = redeemableCoins, color = SuccessGreen)
                SubBalanceItem(title = "Lifetime", count = lifetimeCoins, color = SoftGold)
            }
        }
    }
}

@Composable
private fun RowScope.SubBalanceItem(
    title: String,
    count: Int,
    color: Color
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$count",
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ==========================================
// MICRO-INTERACTIVE COIN ANIMATION
// ==========================================
@Composable
fun CoinAnimation(
    coinsEarned: Int,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isStarted by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            isStarted = true
            delay(800) // 800ms fade transition
            isStarted = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isStarted,
        enter = fadeIn(animationSpec = tween(150)) + slideInVertically(
            initialOffsetY = { 30 },
            animationSpec = tween(200, easing = EaseOutBack)
        ),
        exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(
            targetOffsetY = { -30 },
            animationSpec = tween(250, easing = EaseInBack)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PremiumGold)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                tint = Color(0xFF1E1E24),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "+$coinsEarned Coins",
                color = Color(0xFF1E1E24),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}

// ==========================================
// REWARD PROGRESS INDICATOR
// ==========================================
@Composable
fun RewardProgress(
    progress: Float,
    label: String = "Reward Progress",
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    }
}

// ==========================================
// STATISTICS CARD
// ==========================================
@Composable
fun StatisticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(DesignTokens.CornerMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    letterSpacing = 0.25.sp
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ==========================================
// COIN HISTORY ITEM
// ==========================================
@Composable
fun HistoryItem(
    title: String,
    timestamp: String,
    coins: Int,
    type: String, // "FEED", "REELS", "CASHOUT", "SIGNUP"
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        "FEED" -> Icons.Default.DynamicFeed
        "REELS" -> Icons.Default.PlayCircleOutline
        "CASHOUT" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.Stars
    }

    val iconColor = when (type) {
        "FEED" -> MaterialTheme.colorScheme.primary
        "REELS" -> MaterialTheme.colorScheme.secondary
        "CASHOUT" -> ErrorRed
        else -> SuccessGreen
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timestamp,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }

        val prefix = if (coins > 0) "+" else ""
        val textColor = if (coins > 0) SuccessGreen else ErrorRed
        val bgTint = if (coins > 0) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgTint)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$prefix$coins",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ==========================================
// REDEEM CARD
// ==========================================
@Composable
fun RedeemCard(
    methodName: String,
    methodIcon: ImageVector,
    rateDescription: String,
    minCoinsRequired: Int,
    onRedeemClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRedeemClicked()
            },
        shape = RoundedCornerShape(DesignTokens.CornerMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = methodIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = methodName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rateDescription,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Min Payout",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$minCoinsRequired Coins",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==========================================
// VOUCHER CARD WITH DASHED DIVIDER
// ==========================================
@Composable
fun VoucherCard(
    voucherId: String,
    brand: String,
    title: String,
    description: String,
    discountCode: String,
    status: String, // "REDEEMED", "CLAIMED", "EXPIRED"
    onCopyCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isRedeemed = status == "REDEEMED"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerMedium),
        colors = CardDefaults.cardColors(
            containerColor = if (isRedeemed) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Custom dashed divider line between Voucher info and Discount Code box
                    val stroke = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.25f),
                        start = Offset(0f, size.height - 70.dp.toPx()),
                        end = Offset(size.width, size.height - 70.dp.toPx()),
                        strokeWidth = stroke.width,
                        pathEffect = stroke.pathEffect
                    )
                }
                .padding(DesignTokens.PaddingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = brand.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isRedeemed) SuccessGreen.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRedeemed) SuccessGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(28.dp)) // Clearance for dashed line and gap

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = discountCode,
                    fontSize = 15.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCopyCode()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SETTINGS TILE
// ==========================================
@Composable
fun SettingsTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    },
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = 14.dp, horizontal = DesignTokens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        if (trailing != null) {
            Box(modifier = Modifier.padding(start = 12.dp)) {
                trailing()
            }
        }
    }
}

// ==========================================
// ANALYTICS CARD
// ==========================================
@Composable
fun AnalyticsCard(
    sessionCount: Int,
    impressionCount: Int,
    clickCount: Int,
    conversionRate: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(DesignTokens.PaddingMedium)) {
            Text(
                text = "PERFORMANCE ANALYTICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.75.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsStatCol(title = "Sessions", value = "$sessionCount", modifier = Modifier.weight(1f))
                AnalyticsStatCol(title = "Impressions", value = "$impressionCount", modifier = Modifier.weight(1f))
                AnalyticsStatCol(title = "Clicks", value = "$clickCount", modifier = Modifier.weight(1f))
                AnalyticsStatCol(title = "CTR", value = String.format("%.2f%%", conversionRate), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AnalyticsStatCol(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==========================================
// LOADING SKELETON (SHIMMER ACTION)
// ==========================================
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    width: Dp = Dp.Unspecified,
    cornerRadius: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .then(
                if (width != Dp.Unspecified) Modifier.width(width) else Modifier.fillMaxWidth()
            )
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

// ==========================================
// ERROR VIEW
// ==========================================
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Oops! Something went wrong",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Try Again", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// EMPTY VIEW WITH ILLUSTRATIVE BADGE
// ==========================================
@Composable
fun EmptyView(
    message: String,
    buttonText: String = "Update Interests",
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DesignTokens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Advertisements Found",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onActionClick,
            shape = RoundedCornerShape(DesignTokens.CornerSmall),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = buttonText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
