package com.example.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager

class FifandraisanaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val isDark by ThemeManager.isDarkMode.collectAsState()
                MyApplicationTheme(darkTheme = isDark) {
                    FifandraisanaScreen(
                        isDark = isDark,
                        onBackClick = {
                            (activity as? MainActivity)?.closeFifandraisana()
                        }
                    )
                }
            }
        }
    }

    fun applyThemeSync(isDark: Boolean) {
    }
}

@Composable
fun FifandraisanaScreen(
    isDark: Boolean = ThemeManager.isDarkMode.value,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current

    val plusJakartaSans = remember {
        FontFamily(
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
            Font(R.font.inter, FontWeight.SemiBold),
            Font(R.font.inter, FontWeight.Bold)
        )
    }

    val bgColor = MaterialTheme.colorScheme.background
    val cardSurface = MaterialTheme.colorScheme.surface
    val cardSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF781C2E)
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val accentColor = if (isDark) Color(0xFFD4AF37) else Color(0xFF781C2E)
    val emeraldSquircleBg = if (isDark) Color(0xFF0F382A) else Color(0xFFECFDF5)
    val emeraldSquircleBorder = if (isDark) Color(0xFF1B5E43) else Color(0xFFD1FAE5)
    val emeraldIconColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF047857)
    val colorEmerald600 = Color(0xFF059669)
    val colorEmerald700 = Color(0xFF047857)
    val roseSquircleBg = if (isDark) Color(0xFF351019) else Color(0xFFFFF1F2)
    val roseSquircleBorder = if (isDark) Color(0xFF59021A) else Color(0xFFFFE4E6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("fifandraisana_screen_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 430.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Capsule Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(cardSurface)
                    .border(1.dp, borderColor, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Circular Back Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cardSurfaceVariant)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = accentColor),
                            onClick = onBackClick
                        )
                        .testTag("fifandraisana_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    SvgStrokedIcon(
                        paths = listOf("M15 19l-7-7 7-7"),
                        size = 20.dp,
                        color = accentColor,
                        strokeWidthDp = 2.5.dp
                    )
                }

                // Center: Title
                Text(
                    text = "FIFANDRAISANA",
                    fontFamily = plusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.9.sp,
                    color = titleColor,
                    textAlign = TextAlign.Center
                )

                // Right: Emblem Anchor
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cardSurfaceVariant)
                        .border(1.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SvgFilledIcon(
                        pathData = "M12 2L1 8.5V11H23V8.5L12 2zm0 3.236l5.5 3.264H6.5L12 5.236zM3 13h4v8H3v-8zm7 0h4v8h-4v-8zm7 0h4v8h-4v-8zM1 21h22v2H1v-2z",
                        size = 20.dp,
                        color = accentColor
                    )
                }
            }

            // 1. HERO BENTO CARD (Collaboration & Improvements)
            val heroCardShape = RoundedCornerShape(28.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(heroCardShape)
                    .background(cardSurface)
                    .border(1.dp, borderColor, heroCardShape)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Medallion
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(cardSurfaceVariant)
                        .border(2.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SvgStrokedIcon(
                        paths = listOf(
                            "M22 2L11 13",
                            "M22 2L15 22L11 13L2 9L22 2Z"
                        ),
                        size = 28.dp,
                        color = accentColor,
                        strokeWidthDp = 2.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Fiaraha-miasa & Fanatsarana",
                    fontFamily = plusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.4).sp,
                    color = titleColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Raha misy fiaraha-miasa tiana hatao na fanatsarana ny application dia aza misalasala mifandray aminay amin'ny adiresy etsy ambany :",
                    fontFamily = interFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 22.75.sp,
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 330.dp)
                )
            }

            // 2. INTERACTIVE CONTACT CHANNELS
            // CARD A: WHATSAPP
            val actionCardShape = RoundedCornerShape(24.dp)
            val openWhatsApp: () -> Unit = {
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/261385462153")
                    )
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(actionCardShape)
                    .background(cardSurface)
                    .border(1.dp, borderColor, actionCardShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = colorEmerald600),
                        onClick = openWhatsApp
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Crisp Emerald Squircle
                    val squircleShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(squircleShape)
                            .background(emeraldSquircleBg)
                            .border(1.dp, emeraldSquircleBorder, squircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SvgFilledIcon(
                            pathData = "M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766.001-3.187-2.575-5.77-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.299.045-.677.063-1.092-.069-.252-.08-.575-.187-.988-.365-1.739-.751-2.874-2.502-2.961-2.617-.087-.116-.708-.94-.708-1.793s.448-1.273.607-1.446c.159-.173.346-.217.462-.217l.332.006c.106.005.249-.04.39.298.144.347.491 1.2.534 1.287.043.087.072.188.014.304-.058.116-.087.188-.173.289l-.26.304c-.087.086-.177.18-.076.354.101.174.449.741.964 1.201.662.591 1.221.774 1.394.86s.274.072.376-.043c.101-.116.433-.506.549-.68.116-.173.231-.145.39-.087s1.011.477 1.184.564.289.13.332.202c.045.072.045.419-.1.824zm-3.423-14.416c-6.627 0-12 5.373-12 12 0 2.625.846 5.059 2.284 7.033l-1.492 5.451 5.581-1.464c1.914 1.258 4.197 1.98 6.627 1.98 6.627 0 12-5.373 12-12s-5.373-12-12-12z",
                            size = 24.dp,
                            color = emeraldIconColor
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WHATSAPP",
                            fontFamily = plusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.6.sp,
                            color = subtitleColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+261 38 54 621 53",
                            fontFamily = plusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            letterSpacing = (-0.3).sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Action Pill Button
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colorEmerald600)
                        .border(1.dp, colorEmerald700, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = openWhatsApp
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("fifandraisana_whatsapp_button"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Handefa",
                        fontFamily = plusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.White
                    )
                    SvgStrokedIcon(
                        paths = listOf(
                            "M5 12h14",
                            "M12 5l7 7-7 7"
                        ),
                        size = 14.dp,
                        color = Color.White,
                        strokeWidthDp = 2.5.dp
                    )
                }
            }

            // CARD B: EMAIL
            val emailCtaBg = if (isDark) Color(0xFF800020) else Color(0xFF781C2E)
            val emailCtaBorder = if (isDark) Color(0xFF59021A) else Color(0xFF59021A)
            val openEmail: () -> Unit = {
                try {
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:fanovah@gmail.com")
                    )
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(actionCardShape)
                    .background(cardSurface)
                    .border(1.dp, borderColor, actionCardShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = emailCtaBg),
                        onClick = openEmail
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Crisp Garnet Squircle
                    val squircleShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(squircleShape)
                            .background(roseSquircleBg)
                            .border(1.dp, roseSquircleBorder, squircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SvgStrokedIcon(
                            paths = listOf(
                                "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
                                "M22 7l-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"
                            ),
                            size = 24.dp,
                            color = accentColor,
                            strokeWidthDp = 2.dp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EMAIL",
                            fontFamily = plusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.6.sp,
                            color = subtitleColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "fanovah@gmail.com",
                            fontFamily = plusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            letterSpacing = (-0.3).sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(emailCtaBg)
                        .border(1.dp, emailCtaBorder, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = openEmail
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("fifandraisana_email_button"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Handefa",
                        fontFamily = plusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.White
                    )
                    SvgStrokedIcon(
                        paths = listOf(
                            "M5 12h14",
                            "M12 5l7 7-7 7"
                        ),
                        size = 14.dp,
                        color = Color.White,
                        strokeWidthDp = 2.5.dp
                    )
                }
            }

            // 3. EXTRA BENTO CARD - COMMITMENT / DEVOTION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(actionCardShape)
                    .background(cardSurface)
                    .border(1.dp, borderColor, actionCardShape)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                val squircleShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(40.dp)
                        .clip(squircleShape)
                        .background(cardSurfaceVariant)
                        .border(1.dp, borderColor, squircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SvgStrokedIcon(
                        paths = listOf(
                            "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1-2.5-2.5Z",
                            "M6 6h10",
                            "M6 10h10"
                        ),
                        size = 20.dp,
                        color = accentColor,
                        strokeWidthDp = 2.dp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fifandraisana",
                        fontFamily = plusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = titleColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ny fanamarihana momba ny tononkira, fanitsiana laharana, na hevitra vaovao hampandrosoana ny application dia raisina am-pifaliana hatrany.",
                        fontFamily = interFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 19.5.sp,
                        color = subtitleColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SvgFilledIcon(
    pathData: String,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    val parsedPath = remember(pathData) {
        PathParser().parsePathString(pathData).toPath().apply {
            fillType = PathFillType.NonZero
        }
    }
    Canvas(modifier = modifier.size(size)) {
        val scaleX = this.size.width / 24f
        val scaleY = this.size.height / 24f
        scale(scaleX = scaleX, scaleY = scaleY, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(
                path = parsedPath,
                color = color,
                style = Fill
            )
        }
    }
}

@Composable
private fun SvgStrokedIcon(
    paths: List<String>,
    size: Dp,
    color: Color,
    strokeWidthDp: Dp,
    modifier: Modifier = Modifier
) {
    val parsedPaths = remember(paths) {
        paths.map { PathParser().parsePathString(it).toPath() }
    }
    Canvas(modifier = modifier.size(size)) {
        val scaleX = this.size.width / 24f
        val scaleY = this.size.height / 24f
        val scaledStrokeWidth = (strokeWidthDp.toPx()) / ((scaleX + scaleY) / 2f)
        scale(scaleX = scaleX, scaleY = scaleY, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            parsedPaths.forEach { path ->
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = scaledStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
