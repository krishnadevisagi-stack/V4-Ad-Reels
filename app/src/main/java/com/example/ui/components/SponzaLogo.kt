package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

import androidx.compose.ui.platform.LocalContext

@Composable
fun SponzaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    transparent: Boolean = true,
    showText: Boolean = false
) {
    val context = LocalContext.current
    var pngResId = context.resources.getIdentifier("img_sponza_logo", "drawable", context.packageName)
    if (pngResId == 0) {
        pngResId = context.resources.getIdentifier("sponza_logo_png", "drawable", context.packageName)
    }
    val painter = if (pngResId != 0) {
        painterResource(id = pngResId)
    } else {
        painterResource(id = if (transparent) R.drawable.sponza_logo_transparent else R.drawable.sponza_logo)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painter,
            contentDescription = "Sponza Logo",
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )

        if (showText) {
            Spacer(modifier = Modifier.height(12.dp))

            // The wordmark "sponza" in white/slate with an amber-glowing 'o'
            val wordColor = if (transparent) Color(0xFF1E233C) else Color.White
            val oColor = Color(0xFFFF9800) // Vibrant Gold/Orange

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "sp",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.24f).sp,
                        color = wordColor
                    )
                )
                Text(
                    text = "o",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.24f).sp,
                        color = oColor
                    )
                )
                Text(
                    text = "nza",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.24f).sp,
                        color = wordColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Colored and segmented tagline: WATCH. DISCOVER. EARN.
            val tagline = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFFE040FB), fontWeight = FontWeight.Bold)) {
                    append("WATCH. ")
                }
                withStyle(SpanStyle(color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)) {
                    append("DISCOVER. ")
                }
                withStyle(SpanStyle(color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)) {
                    append("EARN.")
                }
            }

            Text(
                text = tagline,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.08f).sp,
                    letterSpacing = 1.8.sp
                )
            )
        }
    }
}

