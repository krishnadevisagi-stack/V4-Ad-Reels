package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PremiumGold
import com.example.viewmodel.WalletViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CoinAnimationOverlay(
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val animEvents by walletViewModel.notificationEngine.coinAnimFlow.collectAsState(initial = null)
    
    // Maintain a list of currently active flying coin particles
    var activeCoins by remember { mutableStateOf<List<CoinParticleState>>(emptyList()) }

    // Screen dimensions in pixels
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // When a new event triggers, spawn a burst of coin particles
    LaunchedEffect(animEvents) {
        val event = animEvents ?: return@LaunchedEffect
        
        // Spawn 8 coins per burst
        val newParticles = List(8) { index ->
            val angleOffset = kotlin.random.Random.nextInt(0, 16)
            val angle = (index * (360f / 8) + angleOffset).toDouble() * (Math.PI / 180.0)
            val burstSpeed = kotlin.random.Random.nextFloat() * 10f + 8f // 8f to 18f
            
            CoinParticleState(
                id = "${event.id}_$index",
                x = Animatable(screenWidthPx / 2f),
                y = Animatable(screenHeightPx / 2f),
                scale = Animatable(0f),
                alpha = Animatable(1f),
                angle = angle,
                burstSpeed = burstSpeed
            )
        }
        
        // Append to active particles list
        activeCoins = activeCoins + newParticles

        // Animate each particle individually
        newParticles.forEachIndexed { idx, particle ->
            coroutineScope.launch {
                // Phase 1: Burst outwards from center with scale-in
                val burstX = (screenWidthPx / 2f) + (particle.burstSpeed * 15f * cos(particle.angle)).toFloat()
                val burstY = (screenHeightPx / 2f) + (particle.burstSpeed * 15f * sin(particle.angle)).toFloat()
                
                // Animate scale and position in parallel
                launch {
                    val targetScale = kotlin.random.Random.nextFloat() * 0.4f + 1.0f // 1.0f to 1.4f
                    particle.scale.animateTo(
                        targetValue = targetScale,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                }
                
                launch {
                    particle.x.animateTo(
                        targetValue = burstX,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                }
                
                particle.y.animateTo(
                    targetValue = burstY,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )

                // Small suspension delay at burst position to feel organic
                val suspensionDelay = kotlin.random.Random.nextLong(50, 151)
                delay(suspensionDelay)

                // Phase 2: Fly to top right corner (Wallet target)
                val targetX = screenWidthPx - with(density) { 40.dp.toPx() }
                val targetY = with(density) { 60.dp.toPx() }

                // Determine flying animation specs (staggered delay based on index)
                val flyDuration = kotlin.random.Random.nextInt(450, 651)
                
                launch {
                    particle.scale.animateTo(
                        targetValue = 0.5f,
                        animationSpec = tween(durationMillis = flyDuration, easing = LinearOutSlowInEasing)
                    )
                }

                launch {
                    particle.alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = flyDuration, easing = FastOutLinearInEasing)
                    )
                }

                launch {
                    particle.x.animateTo(
                        targetValue = targetX,
                        animationSpec = tween(durationMillis = flyDuration, easing = FastOutSlowInEasing)
                    )
                }

                particle.y.animateTo(
                    targetValue = targetY,
                    animationSpec = tween(durationMillis = flyDuration, easing = FastOutSlowInEasing)
                )

                // Remove once completed
                activeCoins = activeCoins.filter { it.id != particle.id }
            }
        }
    }

    if (activeCoins.isNotEmpty()) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
        ) {
            activeCoins.forEach { coin ->
                val xVal = coin.x.value
                val yVal = coin.y.value
                val scaleVal = coin.scale.value
                val alphaVal = coin.alpha.value
                
                if (alphaVal > 0f && scaleVal > 0f) {
                    val baseRadius = 24f * scaleVal
                    
                    // Draw outer premium gold circle with shadow/border
                    drawCircle(
                        color = PremiumGold,
                        radius = baseRadius,
                        center = Offset(xVal, yVal),
                        alpha = alphaVal
                    )
                    
                    // Draw inner circle border for depth
                    drawCircle(
                        color = Color.Yellow,
                        radius = baseRadius * 0.8f,
                        center = Offset(xVal, yVal),
                        alpha = alphaVal
                    )

                    // Draw a tiny shine center
                    drawCircle(
                        color = Color.White,
                        radius = baseRadius * 0.4f,
                        center = Offset(xVal - (baseRadius * 0.2f), yVal - (baseRadius * 0.2f)),
                        alpha = alphaVal
                    )
                }
            }
        }
    }
}

// Data class to track individual animated state of each coin particle
class CoinParticleState(
    val id: String,
    val x: Animatable<Float, AnimationVector1D>,
    val y: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>,
    val angle: Double,
    val burstSpeed: Float
)
