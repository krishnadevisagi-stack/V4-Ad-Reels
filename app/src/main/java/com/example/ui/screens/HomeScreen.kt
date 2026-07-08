package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AdvertisementItem
import com.example.data.model.FeedConfig
import com.example.data.model.FeedAnalytics
import com.example.ui.components.*
import com.example.viewmodel.FeedViewModel
import com.example.viewmodel.AdViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AdViewModel, // kept for signature compatibility with MainActivity routing
    modifier: Modifier = Modifier
) {
    // Instantiate our beautifully structured FeedViewModel
    val feedViewModel: FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val context = LocalContext.current
    val feedAds by feedViewModel.feedAds.collectAsState()
    val userProfile by feedViewModel.userProfile.collectAsState()
    val feedConfig by feedViewModel.feedConfig.collectAsState()
    val feedAnalytics by feedViewModel.feedAnalytics.collectAsState()
    val isLoading by feedViewModel.isLoading.collectAsState()
    val isScrolling by feedViewModel.isScrolling.collectAsState()
    val validatedAdIds by feedViewModel.validatedAdIds.collectAsState()

    val currentCoins = userProfile?.coins ?: 0
    var previousCoins by remember { mutableStateOf(currentCoins) }
    var showBonusAnimation by remember { mutableStateOf(false) }
    var bonusAmount by remember { mutableStateOf(0) }

    LaunchedEffect(currentCoins) {
        if (currentCoins > previousCoins) {
            bonusAmount = currentCoins - previousCoins
            showBonusAnimation = true
            delay(1000)
            showBonusAnimation = false
        }
        previousCoins = currentCoins
    }

    var showEngineConsole by remember { mutableStateOf(false) }

    // Coroutine scope for animations
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val startTime = System.currentTimeMillis()
        com.example.data.adengine.ApplicationEventBus.emit(
            com.example.data.adengine.ApplicationEventBus.Event.FeedOpened(startTime)
        )
        onDispose {
            val duration = System.currentTimeMillis() - startTime
            com.example.data.adengine.ApplicationEventBus.emit(
                com.example.data.adengine.ApplicationEventBus.Event.FeedClosed(duration)
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val isDarkTheme by viewModel.isDarkMode.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SponzaLogo(
                            size = 32.dp,
                            transparent = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sponza Feed",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    // Engine Console settings icon
                    IconButton(
                        onClick = { showEngineConsole = true },
                        modifier = Modifier.testTag("engine_console_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Engine Console",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Active Coins indicator in App Bar with subtle increment animation
                    Box(
                        modifier = Modifier.padding(end = 12.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        // 1. Static Wallet Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Coin icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$currentCoins",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // 2. Animated floating "+Coins" indicator that starts below and slides up to the wallet
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBonusAnimation,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it + 40 }) + expandVertically(),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it - 40 }) + shrinkVertically(),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(y = 24.dp, x = (-4).dp)
                        ) {
                            Text(
                                text = "+$bonusAmount",
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF4CAF50),
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isAdsBlockedState by com.example.data.adengine.SecurityAndAntiFraudManager.isAdsBlocked.collectAsState()
            val blockMsgState by com.example.data.adengine.SecurityAndAntiFraudManager.fraudBlockMsg.collectAsState()

            if (isAdsBlockedState) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Access Restricted",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Access Temporarily Restricted",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = blockMsgState ?: "Ads and rewards are temporarily suspended on this account due to suspicious clicking patterns.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                if (isLoading && feedAds.isEmpty()) {
                // Skeleton Screen Loading
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(feedConfig.skeletonCount) {
                        ShimmerAdCard()
                    }
                }
            } else if (feedAds.isEmpty()) {
                // Empty state fallback when no selected categories matching
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyView(
                        message = "No advertisements available in this category. Please select other interests in your Profile tab to begin earning rewards.",
                        buttonText = "View Profile Instructions",
                        onActionClick = {
                            Toast.makeText(context, "Tap the 'Profile' tab at the bottom to customize your interests and unlock premium ad feeds!", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } else {
                // Interactive advertisement feed
                val listState = rememberLazyListState()
                val visibleIndices = remember {
                    derivedStateOf {
                        listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
                    }
                }
                var lastOffset by remember { mutableStateOf(0) }
                var lastIndex by remember { mutableStateOf(0) }

                // Tracking scroll distances and notify ViewModel of viewport item
                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                        .collect { (index, offset) ->
                            val delta = if (index == lastIndex) {
                                kotlin.math.abs(offset - lastOffset)
                            } else {
                                350 // standard item height jump delta estimation
                            }
                            if (delta > 0 && delta < 2000) {
                                feedViewModel.addScrollMetrics(delta.toFloat() / 150f)
                            }
                            lastIndex = index
                            lastOffset = offset
                            
                            feedViewModel.onUserScrolledToItem(index, listState.isScrollInProgress)
                        }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_feed_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Feed Items
                    itemsIndexed(
                        items = feedAds,
                        key = { _, ad -> ad.id }
                    ) { index, ad ->
                        val isAlreadyValidated = validatedAdIds.contains(ad.id)
                        val isFullyVisible = index in visibleIndices.value
                        
                        FeedAdItemCard(
                            ad = ad,
                            isValidated = isAlreadyValidated,
                            isFullyVisible = isFullyVisible,
                            onValidateImpression = {
                                feedViewModel.onAdImpressionValidated(ad.id)
                                viewModel.logAdStat("FEED", ad.id, "IMPRESSION")
                            },
                            onLogAdStat = { adUnitId, action ->
                                viewModel.logAdStat("FEED", adUnitId, action)
                            },
                            onCtaClicked = {
                                feedViewModel.onAdClicked(ad)
                                viewModel.logAdStat("FEED", ad.id, "CLICK")
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.destinationUrl))
                                context.startActivity(browserIntent)
                            },
                            onToggleSave = {
                                feedViewModel.toggleSaveAd(ad)
                                viewModel.logAdStat("FEED", ad.id, "SAVE")
                            },
                            onShare = {
                                feedViewModel.onAdShared(ad)
                                viewModel.logAdStat("FEED", ad.id, "SHARE")
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, ad.title)
                                    putExtra(Intent.EXTRA_TEXT, "Check out ${ad.brandName} on Sponza: ${ad.title}! Detail URL: ${ad.destinationUrl}")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Ad via"))
                            },
                            isAdSavedFlow = feedViewModel.isAdSaved(ad.id)
                        )
                    }

                    // Silent footer buffer loading indicator
                    if (isScrolling) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Buffering personalized advertisements...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // ADMINISTRATIVE ENGINE CONFIGURATION & ANALYTICS DIALOG
    if (showEngineConsole) {
        AlertDialog(
            onDismissRequest = { showEngineConsole = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Engine Console",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sponza Engine Console",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "LIVE PERFORMANCE METRICS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnalyticsRow(label = "Total Feed Sessions", value = "${feedAnalytics.totalFeedSessions}")
                                AnalyticsRow(label = "Feed Open Count", value = "${feedAnalytics.feedOpenCount}")
                                AnalyticsRow(label = "Impressions Counted", value = "${feedAnalytics.feedImpressions}")
                                AnalyticsRow(label = "Advertisement Clicks", value = "${feedAnalytics.adClicks}")
                                AnalyticsRow(label = "Advertisement Saves", value = "${feedAnalytics.adSaves}")
                                AnalyticsRow(label = "Advertisement Shares", value = "${feedAnalytics.adShares}")
                                AnalyticsRow(
                                    label = "Scroll Distance Traveled",
                                    value = String.format(Locale.US, "%.1f m", feedAnalytics.feedScrollDistance)
                                )
                                val avgSessionSec = if (feedAnalytics.totalFeedSessions > 0) {
                                    (feedAnalytics.totalSessionDurationMs / 1000) / feedAnalytics.totalFeedSessions
                                } else 0
                                AnalyticsRow(label = "Avg Session Duration", value = "${avgSessionSec}s")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ENGINE CONFIGURATIONS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Feed Buffer Size:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${feedConfig.feedBufferSize} prepared ads", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Lazy Loading Threshold:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${feedConfig.lazyLoadingThreshold} items from end", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Intelligent LRU Cache:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${feedConfig.cacheSize} items max", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reward Ratio (Milestone):", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${feedConfig.rewardRatio} impressions/coin event", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                                
                                Text(
                                    text = "🔒 System logic configurations have been placed in the non-editable category. These settings are hardcoded in the backend to ensure system security and prevent administrative tampering.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Button(
                                    onClick = {
                                        feedViewModel.resetConfigToDefault()
                                        Toast.makeText(context, "Configurations reset to default successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reset Configurations to Default", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ANTI-FRAUD & SECURITY CONTROLS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Suspicious Activity Block Duration",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "⚡ Calculated automatically based on suspicion levels (15 minutes up to 24 hours per offense).",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )

                                val isBlocked by com.example.data.adengine.SecurityAndAntiFraudManager.isAdsBlocked.collectAsState()
                                val rewardsSuspended by com.example.data.adengine.SecurityAndAntiFraudManager.areRewardsSuspended.collectAsState()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Account Status:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (isBlocked) "RESTRICTED (Click Spam)" else if (rewardsSuspended) "SUSPENDED (VPN/Proxy/AdBlock)" else "SECURE & ACTIVE",
                                        color = if (isBlocked || rewardsSuspended) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    feedViewModel.clearAllStatsAndCache()
                                    viewModel.resetAllAnalyticsLogsAndCache()
                                    Toast.makeText(context, "Analytics reset successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Analytics", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEngineConsole = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply & Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AdMobBannerCard(
    adUnitId: String,
    modifier: Modifier = Modifier,
    onLog: (action: String) -> Unit,
    onValidateImpression: () -> Unit
) {
    var adLoaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ad",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Google AdMob",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sponsored • Banner Ad",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (loadError != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "AdMob Banner Load Error",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = loadError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            onLog("CALLED")
                            AdView(context).apply {
                                setAdSize(AdSize.MEDIUM_RECTANGLE)
                                setAdUnitId(adUnitId)
                                adListener = object : com.google.android.gms.ads.AdListener() {
                                    override fun onAdLoaded() {
                                        adLoaded = true
                                        onLog("OPENED")
                                        onLog("SEEN")
                                        onValidateImpression()
                                    }

                                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                        loadError = "Code ${error.code}: ${error.message}\n(AdMob requires real device/emulator setup or test device ID in release builds.)"
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeedAdItemCard(
    ad: AdvertisementItem,
    isValidated: Boolean,
    isFullyVisible: Boolean,
    onValidateImpression: () -> Unit,
    onLogAdStat: (adUnitId: String, action: String) -> Unit,
    onCtaClicked: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    isAdSavedFlow: kotlinx.coroutines.flow.Flow<Boolean>
) {
    val context = LocalContext.current
    val isSaved by isAdSavedFlow.collectAsState(initial = false)

    var isLoaded by remember(ad.id) { mutableStateOf(false) }

    // Log VISIBLE only when both fully visible and successfully loaded
    LaunchedEffect(ad.id, isFullyVisible, isLoaded) {
        if (!ad.id.startsWith("admob_feed_native_test_") && !ad.id.startsWith("ca-app-pub-")) {
            if (isFullyVisible && isLoaded) {
                onLogAdStat(ad.id, "VISIBLE")
            }
        }
    }

    // Tracks visible layout block to fire continuous timer (validated impressions after 3 seconds)
    LaunchedEffect(ad.id, isFullyVisible, isValidated, isLoaded) {
        if (!ad.id.startsWith("admob_feed_native_test_")) {
            if (!isValidated && isFullyVisible && isLoaded) {
                delay(3000) // 3 seconds continuous layout view validation
                onValidateImpression()
            }
        }
    }

    if (ad.id.startsWith("admob_feed_native_test_") || ad.id.startsWith("ca-app-pub-")) {
        AdMobBannerCard(
            adUnitId = ad.id,
            modifier = Modifier.padding(bottom = 4.dp),
            onLog = { action -> onLogAdStat(ad.id, action) },
            onValidateImpression = onValidateImpression
        )
    } else {
        AdvertisementCard(
            brandName = ad.brandName,
            category = ad.category,
            imageUrl = ad.imageUrl ?: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=600",
            title = ad.title,
            description = ad.description,
            ctaText = ad.ctaText,
            isValidated = isValidated,
            onCtaClicked = onCtaClicked,
            onToggleSave = onToggleSave,
            onShare = onShare,
            isSaved = isSaved,
            modifier = Modifier.padding(bottom = 4.dp),
            adId = ad.id,
            isFullyVisible = isFullyVisible,
            onLoadedChanged = { loaded -> isLoaded = loaded }
        )
    }
}

@Composable
fun ShimmerAdCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LoadingSkeleton(width = 36.dp, height = 36.dp, cornerRadius = 18.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    LoadingSkeleton(width = 100.dp, height = 12.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LoadingSkeleton(width = 60.dp, height = 8.dp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LoadingSkeleton(height = 220.dp, cornerRadius = 16.dp)
            Spacer(modifier = Modifier.height(16.dp))
            LoadingSkeleton(width = 200.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(8.dp))
            LoadingSkeleton(height = 10.dp)
            Spacer(modifier = Modifier.height(6.dp))
            LoadingSkeleton(width = 250.dp, height = 10.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingSkeleton(width = 120.dp, height = 40.dp, cornerRadius = 12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoadingSkeleton(width = 36.dp, height = 36.dp, cornerRadius = 18.dp)
                    LoadingSkeleton(width = 36.dp, height = 36.dp, cornerRadius = 18.dp)
                }
            }
        }
    }
}

@Composable
fun AnalyticsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ConfigSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = displayValue, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
