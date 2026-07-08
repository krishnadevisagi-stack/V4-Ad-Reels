package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.ReelAdvertisementItem
import com.example.data.model.ReelsConfig
import com.example.data.model.ReelsAnalytics
import com.example.ui.theme.PremiumGold
import com.example.viewmodel.AdViewModel
import com.example.viewmodel.ReelsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class, ExperimentalFoundationApi::class)
@Composable
fun ReelsScreen(
    adViewModel: AdViewModel, // Retained for compatibility/sharing layout signatures
    modifier: Modifier = Modifier
) {
    // Instantiate our new dedicated decoupled ReelsViewModel
    val reelsViewModel: ReelsViewModel = viewModel()

    val ads by reelsViewModel.reelsAds.collectAsStateWithLifecycle()
    val config by reelsViewModel.configState.collectAsStateWithLifecycle()
    val activeIdx by reelsViewModel.activeIndex.collectAsStateWithLifecycle()
    val preparedIdx by reelsViewModel.preparedIndex.collectAsStateWithLifecycle()
    val rewardedAds by reelsViewModel.rewardedAdIds.collectAsStateWithLifecycle()
    val isLoading by reelsViewModel.isLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Move state reads and animation controls to the top of Composable to avoid nested lambdas glitches
    val userProfile by adViewModel.userProfile.collectAsStateWithLifecycle()
    val autoPlayOption by adViewModel.autoPlayOption.collectAsStateWithLifecycle()
    val autoMuteOption by adViewModel.autoMuteOption.collectAsStateWithLifecycle()
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

    DisposableEffect(Unit) {
        val startTime = System.currentTimeMillis()
        com.example.data.adengine.ApplicationEventBus.emit(
            com.example.data.adengine.ApplicationEventBus.Event.ReelsOpened(startTime)
        )
        onDispose {}
    }

    // Configuration Bottom Sheet State
    var showConfigSheet by remember { mutableStateOf(false) }

    if (isLoading || ads.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { ads.size })

    // Sync selected page back to ViewModel
    LaunchedEffect(pagerState.currentPage) {
        reelsViewModel.onPageSelected(pagerState.currentPage)
    }

    // Auto Swipe to Next Video when active ad ends
    var shouldAutoMoveToNext by remember { mutableStateOf(false) }
    LaunchedEffect(shouldAutoMoveToNext) {
        if (shouldAutoMoveToNext) {
            shouldAutoMoveToNext = false
            if (config.autoSwipe && pagerState.currentPage + 1 < ads.size) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
            VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("reels_vertical_pager"),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            val ad = ads[pageIndex]
            val isActive = activeIdx == pageIndex
            val isPreload = preparedIdx == pageIndex

            var isAdLoaded by remember(ad.id) { mutableStateOf(false) }
            LaunchedEffect(isActive, isAdLoaded) {
                if (isActive && isAdLoaded) {
                    adViewModel.logAdStat("REEL", ad.id, "VIDEO_VIEW")
                }
            }

            val overriddenConfig = remember(config, autoPlayOption, autoMuteOption) {
                config.copy(
                    autoPlay = (autoPlayOption != "Disabled"),
                    muteDefault = autoMuteOption
                )
            }

            ReelItemView(
                ad = ad,
                isActive = isActive,
                isPreload = isPreload,
                config = overriddenConfig,
                onProgressUpdate = { elapsedMs ->
                    if (isActive) {
                        reelsViewModel.updatePlaybackProgress(ad.id, elapsedMs)
                    }
                },
                onVideoCompleted = {
                    if (isActive) {
                        shouldAutoMoveToNext = true
                        adViewModel.logAdStat("REEL", ad.id, "COMPLETED")
                    }
                },
                isRewarded = rewardedAds.contains(ad.id),
                onCtaClicked = {
                    val proceed = com.example.data.adengine.SecurityAndAntiFraudManager.registerAndEvaluateClick(ad.id, ad.brandName)
                    if (proceed) {
                        reelsViewModel.onCtaClicked(ad)
                        adViewModel.logAdStat("REEL", ad.id, "CLICK")
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.destinationUrl))
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Opening destination...", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onToggleSave = {
                    reelsViewModel.onAdSaved(ad) {
                        // Map ReelAdvertisementItem back to DummyAd for global state compat
                        val dummy = com.example.data.model.DummyAd(
                            id = ad.id,
                            category = ad.category,
                            brandName = ad.brandName,
                            logo = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=100",
                            mediaUrl = ad.videoUrl,
                            isVideo = true,
                            title = ad.title,
                            description = ad.description,
                            rewardCoins = ad.rewardCoins,
                            duration = 15,
                            ctaText = ad.ctaText,
                            sponsoredStatus = ad.isSponsored,
                            productName = ad.productName
                        )
                        adViewModel.toggleSaveAd(dummy)
                        Toast.makeText(context, "Ad Bookmarked", Toast.LENGTH_SHORT).show()
                    }
                    adViewModel.logAdStat("REEL", ad.id, "SAVE")
                },
                onShare = {
                    reelsViewModel.onAdShared(ad)
                    adViewModel.logAdStat("REEL", ad.id, "SHARE")
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, ad.title)
                        putExtra(Intent.EXTRA_TEXT, "Watch ${ad.brandName} on AdReels! Earn points watching videos: ${ad.title}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share advertisement"))
                },
                onLogAdStat = { action ->
                    adViewModel.logAdStat("REEL", "ca-app-pub-6715807412270192/7621643175", action)
                },
                onLoaded = {
                    isAdLoaded = true
                }
            )
        }

        // Floating Header Overlay: Logo, Config Cog, Wallet balance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Settings Cog trigger
                IconButton(
                    onClick = { showConfigSheet = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("reels_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Reels Config",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "Sponsored Reels",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = "Aesthetic Commercials Engine",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                contentAlignment = Alignment.TopEnd
            ) {
                // 1. Static Wallet Pill with highly-visible Premium Gold styling
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.2.dp, PremiumGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("reels_wallet_indicator")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Wallet Balance",
                            tint = PremiumGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currentCoins Coins",
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
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
        }
        }

        // Config Sheet Dialog
        if (showConfigSheet) {
            ReelsConfigBottomSheet(
                config = config,
                onDismiss = { showConfigSheet = false },
                onSave = { updated ->
                    reelsViewModel.updateConfig(updated)
                    showConfigSheet = false
                }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ReelItemView(
    ad: ReelAdvertisementItem,
    isActive: Boolean,
    isPreload: Boolean,
    config: ReelsConfig,
    onProgressUpdate: (Long) -> Unit,
    onVideoCompleted: () -> Unit,
    isRewarded: Boolean,
    onCtaClicked: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onLogAdStat: (action: String) -> Unit,
    onLoaded: () -> Unit = {}
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(config.muteDefault) }
    var isBuffering by remember { mutableStateOf(true) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackMs by remember { mutableLongStateOf(0L) }
    var isAdLoading by remember { mutableStateOf(false) }
    var adError by remember { mutableStateOf<String?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Setup ExoPlayer instances ONLY if the page is Active or Preload.
    // Disposes previous videos immediately to prevent memory leaks and dropped frames.
    val playerInstance = remember(isActive, isPreload) {
        if (isActive || isPreload) {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
        } else null
    }

    // Connect source to ExoPlayer
    LaunchedEffect(playerInstance) {
        if (playerInstance != null) {
            val mediaItem = MediaItem.fromUri(ad.videoUrl)
            playerInstance.setMediaItem(mediaItem)
            playerInstance.prepare()

            playerInstance.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY) {
                        durationMs = playerInstance.duration
                        hasError = false
                        errorMsg = null
                        onLoaded()
                    }
                    if (state == Player.STATE_ENDED) {
                        onVideoCompleted()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    hasError = true
                    errorMsg = error.message ?: "Failed to play video advertisement"
                    isBuffering = false
                }
            })

            if (isActive) {
                playerInstance.playWhenReady = if (ad.id.startsWith("admob_reel_rewarded_test_")) true else config.autoPlay
                playerInstance.volume = if (ad.id.startsWith("admob_reel_rewarded_test_")) 0f else (if (isMuted) 0f else 1f)
            } else {
                playerInstance.playWhenReady = false
                playerInstance.volume = 0f
            }
        }
    }

    // Keep active volumes in sync on tap gestures
    LaunchedEffect(isMuted, isActive) {
        if (isActive && playerInstance != null) {
            playerInstance.volume = if (ad.id.startsWith("admob_reel_rewarded_test_")) 0f else (if (isMuted) 0f else 1f)
        }
    }

    // Seamless fallback duration if video fails to load or buffers slowly
    LaunchedEffect(isActive, hasError) {
        if (isActive && !hasError) {
            delay(2000)
            if (durationMs == 0L && !hasError) {
                durationMs = 15000L // Default fallback duration (15 seconds)
            }
        }
    }

    // Continuous watch updates while playing (ONLY if playing without error)
    LaunchedEffect(isActive, playerInstance, hasError) {
        if (isActive && playerInstance != null && !hasError) {
            while (isActive && !hasError) {
                val isPlaying = playerInstance.isPlaying
                val playbackState = playerInstance.playbackState
                if (isPlaying && playbackState == Player.STATE_READY) {
                    val pos = playerInstance.currentPosition
                    playbackMs = pos
                    onProgressUpdate(pos)
                }
                delay(250L)
            }
        }
    }

    DisposableEffect(playerInstance) {
        onDispose {
            playerInstance?.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isMuted = !isMuted }
                )
            }
    ) {
        // Video View
        if (playerInstance != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerInstance
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Playback Error Overlay
        if (hasError && !ad.id.startsWith("admob_reel_rewarded_test_")) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Video Load Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Video Playback Error",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg ?: "An error occurred while loading this video advertisement.",
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No reward coins will be credited for failed or unplayed media.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Circular Loader overlay when buffering
        if (isBuffering && (isActive || isPreload) && !ad.id.startsWith("admob_reel_rewarded_test_")) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }

        // Google AdMob auto-load rewarded overlay as per user request
        if (ad.id.startsWith("admob_reel_rewarded_test_")) {
            val loadAndShowAd = {
                isAdLoading = true
                adError = null
                // Log CALLED
                onLogAdStat("CALLED")
                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    context,
                    "ca-app-pub-6715807412270192/7621643175",
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isAdLoading = false
                            adError = "Code ${loadAdError.code}: ${loadAdError.message}\n(AdMob requires real devices, emulators, or test device registration.)"
                            Toast.makeText(context, "AdMob Load Failed: ${loadAdError.message}", Toast.LENGTH_LONG).show()
                        }

                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            isAdLoading = false
                            // Log OPENED
                            onLogAdStat("OPENED")
                            onLoaded()
                            val activity = context as? Activity
                            if (activity != null) {
                                rewardedAd.show(activity) { rewardItem ->
                                    // Log SEEN on successful view
                                    onLogAdStat("SEEN")
                                    onVideoCompleted()
                                    Toast.makeText(context, "Completed! Credited +${ad.rewardCoins} Coins successfully.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                adError = "Context is not an Activity (Unable to launch fullscreen ad)"
                            }
                        }
                    }
                )
            }

            // Trigger load immediately on page activation
            LaunchedEffect(isActive) {
                if (isActive) {
                    loadAndShowAd()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    if (isAdLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Loading AdMob Video Ad...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sponza is loading a premium rewarded advertisement. Watch to earn +${ad.rewardCoins} Coins.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else if (adError != null) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "AdMob Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AdMob Loading Blocked",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = adError ?: "",
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { loadAndShowAd() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry Loading Ad", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick mute action status overlay
        AnimatedVisibility(
            visible = isMuted && !ad.id.startsWith("admob_reel_rewarded_test_"),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeOff,
                    contentDescription = "Audio Muted",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom Transparent Shader Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        val safeAdvertiser = ad.brandName.trim().takeIf { it.isNotBlank() } ?: "Sponsored Advertisement"

        // Floating Vertical Button Panel on Right Side
        Column(
            modifier = Modifier
                .padding(end = 16.dp, bottom = 24.dp)
                .align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reward indicator
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PremiumGold, Color(0xFFFF8F00))))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Payout value",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "+${ad.rewardCoins}c",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }

            // Save / Bookmark
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("reels_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Reel",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Save", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("reels_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Reel",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Share", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Report ad
            IconButton(
                onClick = {
                    Toast.makeText(context, "Advertisement flagged and reported. Thank you!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .testTag("reels_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Report Reel",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Commercial Details overlay on Left Bottom (optimized spacing to fit compact screen heights)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 80.dp, bottom = 12.dp)
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Headline Missing Strategy: Hide Headline if empty/blank
            if (ad.title.isNotBlank()) {
                Text(
                    text = ad.title,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.clickable { onCtaClicked() }
                )
            }

            // Body Missing Strategy: Hide/collapse layout if description is blank
            if (ad.description.isNotBlank()) {
                Text(
                    text = ad.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                    modifier = Modifier.clickable { onCtaClicked() }
                )
            }

            // Unified CTA, brand sponsor, and category metadata row (placed below, right next to each other)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // CTA Button (solid, high-contrast and fully visible!)
                if (ad.ctaText.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onCtaClicked() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("reels_cta_pill")
                    ) {
                        Text(
                            text = "Open",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowOutward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Metadata Row to cleanly list advertiser, AD tag, and category in a single horizontal row without overlap
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Brand Name
                    Text(
                        text = safeAdvertiser,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Sponsored / AD Label with solid, highly visible Premium Gold background
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PremiumGold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Category Tag
                    if (ad.category.isNotBlank()) {
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ad.category.uppercase(),
                                color = Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Reward Milestone Indicators & Watch elapsed display (Compact and Premium Gold highlights)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val secondsWatched = (playbackMs / 1000).toInt()
                val minThreshold = config.minimumWatchSeconds

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ Reward Eligibility",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${secondsWatched}s / ${minThreshold}s required",
                        color = if (isRewarded) PremiumGold else Color.Yellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Min threshold achieved indicator
                    val thresholdMet = secondsWatched >= minThreshold || isRewarded
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (thresholdMet) PremiumGold.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (thresholdMet) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = "Threshold milestone",
                            tint = if (thresholdMet) PremiumGold else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tier 1: ${minThreshold}s (+${config.rewardTier1}c)",
                            color = if (thresholdMet) PremiumGold else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // High watch reward (Tier 2, 15s)
                    val highMet = secondsWatched >= 15 || isRewarded
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (highMet) PremiumGold.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (highMet) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = "15s milestone",
                            tint = if (highMet) PremiumGold else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tier 2: 15s (+${config.rewardTier2}c)",
                            color = if (highMet) PremiumGold else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Seamless linear bottom scrubber timeline
        if (isActive && durationMs > 0L) {
            val progressVal = (playbackMs.toFloat() / durationMs).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressVal },
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsConfigBottomSheet(
    config: ReelsConfig,
    onDismiss: () -> Unit,
    onSave: (ReelsConfig) -> Unit
) {
    var autoSwipeEnabled by remember { mutableStateOf(config.autoSwipe) }
    var autoPlayEnabled by remember { mutableStateOf(config.autoPlay) }
    var muteDefaultEnabled by remember { mutableStateOf(config.muteDefault) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🔧 Reels Engine Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider()

            // Switches for user experience
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Auto Swipe on Ended", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Swipes immediately when ad completes", color = Color.Gray, fontSize = 10.sp)
                }
                Switch(checked = autoSwipeEnabled, onCheckedChange = { autoSwipeEnabled = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Auto Play", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Plays automatically when ad viewport is active", color = Color.Gray, fontSize = 10.sp)
                }
                Switch(checked = autoPlayEnabled, onCheckedChange = { autoPlayEnabled = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mute by Default", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Mutes the advertisement upon loading", color = Color.Gray, fontSize = 10.sp)
                }
                Switch(checked = muteDefaultEnabled, onCheckedChange = { muteDefaultEnabled = it })
            }

            Button(
                onClick = {
                    onSave(
                        config.copy(
                            minimumWatchSeconds = 5,
                            rewardTier1 = 15,
                            rewardTier2 = 40,
                            autoSwipe = autoSwipeEnabled,
                            autoPlay = autoPlayEnabled,
                            muteDefault = muteDefaultEnabled
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Configuration Engine", fontWeight = FontWeight.Bold)
            }
        }
    }
}

class LocalCoinParticle(
    val id: String,
    val x: Animatable<Float, AnimationVector1D>,
    val y: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>,
    val angle: Double,
    val burstSpeed: Float
)
