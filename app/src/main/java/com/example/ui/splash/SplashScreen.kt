package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.util.AssetImageLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Exact Design Tokens from HTML SplashScreen (100% Opaque, Zero Blur)
private val GarnetPrimary = Color(0xFF781C2E)
private val Garnet80 = Color(0xCC781C2E)
private val Garnet35 = Color(0x59781C2E)
private val Garnet25 = Color(0x40781C2E)
private val GoldPrimary = Color(0xFFD4AF37)
private val Gold50 = Color(0x80D4AF37)
private val Gold40 = Color(0x66D4AF37)

// Sacerdotal Light Theme Gradient Stops (160deg, 100% Opaque)
private val BgStop0 = Color(0xFFFFF8F8)
private val BgStop22 = Color(0xFFFAF5F6)
private val BgStop45 = Color(0xFFFAF4E8)
private val BgStop65 = Color(0xFFF8EFF1)
private val BgStop82 = Color(0xFFF7EBED)
private val BgStop100 = Color(0xFFF1DFE3)

// cubic-bezier(0.16, 1, 0.3, 1)
private val EaseSacred = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit = {},
    onSplashFinished: () -> Unit = onNavigateNext,
    modifier: Modifier = Modifier,
    splashDurationMs: Long = 2850L
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val logoBitmap = remember(context) {
        AssetImageLoader.loadBitmapSync(context, "logo_transparent.png", 512, 512)
            ?: AssetImageLoader.loadBitmapSync(context, "logo.png", 512, 512)
    }

    val plusJakartaSans = remember {
        FontFamily(
            Font(R.font.plus_jakarta_sans, FontWeight.Normal),
            Font(R.font.plus_jakarta_sans, FontWeight.Medium),
            Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
            Font(R.font.plus_jakarta_sans, FontWeight.Bold),
            Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
        )
    }
    val interFont = remember {
        FontFamily(
            Font(R.font.inter, FontWeight.Normal),
            Font(R.font.inter, FontWeight.Medium),
            Font(R.font.inter, FontWeight.SemiBold)
        )
    }
    val cormorantGaramond = remember {
        FontFamily(
            Font(R.font.cormorant_garamond, FontWeight.Medium, FontStyle.Italic),
            Font(R.font.cormorant_garamond, FontWeight.SemiBold, FontStyle.Italic)
        )
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Block keyboard from automatically opening on startup
    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    // Redirect to FandraisanaScreen after splash choreography completes
    LaunchedEffect(Unit) {
        delay(splashDurationMs)
        onSplashFinished()
    }

    // One-shot staggered entrance Animatables matching CSS keyframes & delays
    val medallionScaleEntrance = remember { Animatable(0.6f) }
    val medallionAlpha = remember { Animatable(0f) }

    val titleMainProgress = remember { Animatable(0f) }
    val titleSubProgress = remember { Animatable(0f) }
    val dividerProgress = remember { Animatable(0f) }
    val captionProgress = remember { Animatable(0f) }
    val verseProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. medallionEntrance: 0% scale(0.6) opacity(0) -> 65% scale(1.05) opacity(1) -> 100% scale(1)
        launch {
            medallionAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 715, easing = EaseSacred)
            )
        }
        launch {
            medallionScaleEntrance.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 1100
                    0.6f at 0 using EaseSacred
                    1.05f at 715 using EaseSacred
                    1.0f at 1100
                }
            )
        }

        // 2. anim-title-main (delay 0.35s, duration 1.1s)
        launch {
            delay(350)
            titleMainProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = EaseSacred)
            )
        }

        // 3. anim-title-sub (delay 0.5s, duration 1.1s)
        launch {
            delay(500)
            titleSubProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = EaseSacred)
            )
        }

        // 4. anim-divider (delay 0.65s, duration 1.0s)
        launch {
            delay(650)
            dividerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = EaseSacred)
            )
        }

        // 5. anim-caption (delay 0.8s, duration 1.0s)
        launch {
            delay(800)
            captionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = EaseSacred)
            )
        }

        // 6. anim-verse (delay 1.05s, duration 1.2s)
        launch {
            delay(1050)
            verseProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = EaseSacred)
            )
        }
    }

    // Continuous 60 FPS Infinite Animations (0% Blur)
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")

    // liveSacerdotalGradient: 14s ease infinite (7s half-cycle with Reverse)
    val bgShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_shift"
    )

    // rot-ring-1: slowRotate1 54s linear infinite (0deg -> 360deg)
    val rotRing1Degrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 54000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot_ring_1"
    )

    // rot-ring-2: slowRotate2 70s linear infinite (360deg -> 0deg)
    val rotRing2Degrees by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 70000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot_ring_2"
    )

    // medallionBreathe: 4.5s ease-in-out infinite after 1.1s (scale 1.0 <-> 1.025)
    val medallionBreatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(1100, StartOffsetType.Delay)
        ),
        label = "medallion_breathe"
    )

    // ripple-1: rippleWave 3.4s cubic-bezier(0.16, 1, 0.3, 1) infinite 0.6s
    val ripple1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = EaseSacred),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(600, StartOffsetType.Delay)
        ),
        label = "ripple_1"
    )

    // ripple-2: rippleWave 3.4s cubic-bezier(0.16, 1, 0.3, 1) infinite 1.7s
    val ripple2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = EaseSacred),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1700, StartOffsetType.Delay)
        ),
        label = "ripple_2"
    )

    // 3 Pulsating Dots (pulseWaveDot 1.6s ease-in-out infinite, delays 0.9s, 1.15s, 1.4s)
    val dot1Wave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(900, StartOffsetType.Delay)
        ),
        label = "dot_1"
    )
    val dot2Wave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(1150, StartOffsetType.Delay)
        ),
        label = "dot_2"
    )
    val dot3Wave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(1400, StartOffsetType.Delay)
        ),
        label = "dot_3"
    )

    val backgroundBrush = Brush.linearGradient(
        0.0f to BgStop0,
        0.22f to BgStop22,
        0.45f to BgStop45,
        0.65f to BgStop65,
        0.82f to BgStop82,
        1.0f to BgStop100,
        start = Offset(x = 180f * bgShift, y = 120f * bgShift),
        end = Offset(x = 900f + 240f * bgShift, y = 1900f - 180f * bgShift)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("splash_screen_root"),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ==========================================
        // CENTRAL CONTEMPLATIVE COMPOSITION (<main>)
        // ==========================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Medallion & Concentric Rings Stack
                // Rings are positioned at top-[92px] left-1/2 of the 192.dp (w-48 h-48) medallion
                Box(
                    modifier = Modifier.size(192.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 1. CRISP CONCENTRIC RINGS & RIPPLES (NO BLUR)
                    // Expanding delicate concentric ripple ring 1 in Gold (#D4AF37/50)
                    val ripple1Scale = 0.85f + (1.45f - 0.85f) * ripple1Progress
                    val ripple1Alpha = 0.7f * (1f - ripple1Progress)
                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .requiredSize(210.dp)
                            .graphicsLayer {
                                scaleX = ripple1Scale
                                scaleY = ripple1Scale
                                alpha = ripple1Alpha
                            }
                            .border(1.dp, Gold50, CircleShape)
                    )

                    // Expanding delicate concentric ripple ring 2 in Garnet (#781C2E/35)
                    val ripple2Scale = 0.85f + (1.45f - 0.85f) * ripple2Progress
                    val ripple2Alpha = 0.7f * (1f - ripple2Progress)
                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .requiredSize(210.dp)
                            .graphicsLayer {
                                scaleX = ripple2Scale
                                scaleY = ripple2Scale
                                alpha = ripple2Alpha
                            }
                            .border(1.dp, Garnet35, CircleShape)
                    )

                    // Outer precise dashed circle (rot-ring-2: 310px, counter-rotation)
                    Canvas(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .requiredSize(310.dp)
                            .graphicsLayer {
                                rotationZ = rotRing2Degrees
                            }
                    ) {
                        val strokePx = 1.dp.toPx()
                        val dashPx = 5.dp.toPx()
                        drawCircle(
                            color = Garnet25,
                            radius = (size.minDimension - strokePx) / 2f,
                            style = Stroke(
                                width = strokePx,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx), 0f)
                            )
                        )
                    }

                    // Inner clean geometric circle (rot-ring-1: 260px, clockwise rotation)
                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .requiredSize(260.dp)
                            .graphicsLayer {
                                rotationZ = rotRing1Degrees
                            }
                            .border(1.dp, Gold40, CircleShape)
                    )

                    // 2. SOLID OPAQUE LOGO MEDALLION (w-48 h-48 = 192.dp)
                    val combinedMedallionScale = medallionScaleEntrance.value * medallionBreatheScale
                    Box(
                        modifier = Modifier
                            .size(192.dp)
                            .graphicsLayer {
                                scaleX = combinedMedallionScale
                                scaleY = combinedMedallionScale
                                alpha = medallionAlpha.value
                            }
                            .clip(CircleShape)
                            .background(GoldPrimary)
                            .border(1.dp, GarnetPrimary, CircleShape)
                            .padding(2.5.dp)
                            .testTag("splash_medallion")
                    ) {
                        // Inner White Ring with 2px Garnet Border
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, GarnetPrimary, CircleShape)
                                .padding(3.dp)
                        ) {
                            // 100% Pure Opaque White Medallion Interior (p-4 = 16.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoBitmap != null) {
                                    Image(
                                        bitmap = logoBitmap.asImageBitmap(),
                                        contentDescription = "Logo Jesosy Mamonjy",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_logo_jesosy_mamonjy),
                                        contentDescription = "Logo Jesosy Mamonjy",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. SACRED TYPOGRAPHY CHOREOGRAPHY (mt-9 = 36.dp)
                Spacer(modifier = Modifier.height(36.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Noble Primary Title (Deep Garnet #781C2E)
                    val titleMainTranslatePx = with(density) { (22f * (1f - titleMainProgress.value)).dp.toPx() }
                    val titleMainTrackingEm = 0.18f + (0.25f - 0.18f) * titleMainProgress.value
                    Text(
                        text = "JESOSY MAMONJY",
                        fontFamily = plusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (20f * titleMainTrackingEm).sp,
                        color = GarnetPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = titleMainProgress.value
                            translationY = titleMainTranslatePx
                        }
                    )

                    // Subtitle "M. A. DAOUD" (-mt-0.5 = -2.dp)
                    val titleSubTranslatePx = with(density) { (18f * (1f - titleSubProgress.value)).dp.toPx() }
                    val titleSubTrackingEm = 0.22f + (0.32f - 0.22f) * titleSubProgress.value
                    Text(
                        text = "M. A. DAOUD",
                        fontFamily = plusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = (15f * titleSubTrackingEm).sp,
                        color = GarnetPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .offset(y = (-2).dp)
                            .graphicsLayer {
                                alpha = titleSubProgress.value
                                translationY = titleSubTranslatePx
                            }
                    )

                    // Refined Gold & Garnet Geometric Divider (my-3.5 = 14.dp, space-x-2.5 = 10.dp)
                    val dividerTranslatePx = with(density) { (10f * (1f - dividerProgress.value)).dp.toPx() }
                    Row(
                        modifier = Modifier
                            .padding(vertical = 14.dp)
                            .graphicsLayer {
                                alpha = dividerProgress.value
                                translationY = dividerTranslatePx
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(1.5.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, GoldPrimary, GarnetPrimary)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GarnetPrimary)
                        )
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(1.5.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(GarnetPrimary, GoldPrimary, Color.Transparent)
                                    )
                                )
                        )
                    }

                    // Refined Subtitle in Garnet 80% ("Fihirana rehetra")
                    val captionTranslatePx = with(density) { (10f * (1f - captionProgress.value)).dp.toPx() }
                    Text(
                        text = "Fihirana rehetra",
                        fontFamily = interFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.96.sp,
                        color = Garnet80,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = captionProgress.value
                            translationY = captionTranslatePx
                        }
                    )
                }
            }
        }

        // ==========================================
        // 4. FOOTER & STATUS INDICATORS (<footer>)
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // 3 Pulsating Dots in Garnet #781C2E (space-x-2.5 = 10.dp, mb-3.5 = 14.dp)
            Row(
                modifier = Modifier.padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1Wave, dot2Wave, dot3Wave).forEach { wave ->
                    val dotScale = 1f + 0.25f * wave
                    val dotAlpha = 0.35f + 0.65f * wave
                    val dotTranslateY = with(density) { (-4f * wave).dp.toPx() }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                scaleX = dotScale
                                scaleY = dotScale
                                alpha = dotAlpha
                                translationY = dotTranslateY
                            }
                            .clip(CircleShape)
                            .background(GarnetPrimary)
                    )
                }
            }

            // Solemn Sacred Dedication (Cormorant Garamond italic, 15px, semibold, tracking-wide, #781C2E, mb-4)
            val verseTranslatePx = with(density) { (10f * (1f - verseProgress.value)).dp.toPx() }
            Text(
                text = "« Ho voninahitr'Andriamanitra irery ihany »",
                fontFamily = cormorantGaramond,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.375.sp,
                color = GarnetPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        alpha = verseProgress.value
                        translationY = verseTranslatePx
                    }
            )

            // iOS 18 Home Indicator Bar (w-32 = 128.dp, h-1 = 4.dp, bg-[#781C2E]/35)
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Garnet35)
            )
        }
    }
}
