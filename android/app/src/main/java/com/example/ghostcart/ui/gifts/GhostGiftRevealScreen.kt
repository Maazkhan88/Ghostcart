package com.example.ghostcart.ui.gifts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.GhostGiftRepository
import com.example.ghostcart.data.RevealedGhostGift
import com.example.ghostcart.theme.DangerRed
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.DirhamAmount
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.common.GhostTopBar
import com.example.ghostcart.ui.common.PrimaryButton
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun GhostGiftRevealScreen(
    token: String,
    onBack: () -> Unit,
    onGhostGift: (RevealedGhostGift) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var privacyAccepted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var gift by remember { mutableStateOf<RevealedGhostGift?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 108.dp)
    ) {
        GhostTopBar(title = "Your gift", onBack = onBack)

        if (gift == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Ink),
                contentAlignment = Alignment.Center
            ) {
                GhostMascotPose("cart", Modifier.size(160.dp))
            }
            Text(
                "A private gift is waiting.",
                color = Ink,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                "The product stays hidden until you choose to reveal it. Ghost Cart will not identify you to the sender.",
                color = MutedText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftGray)
                    .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(checked = privacyAccepted, onCheckedChange = { privacyAccepted = it; error = null })
                Text(
                    "I understand this is a simulation-only gift. Revealing it records that this private invitation was opened; it creates no purchase, payment, or delivery.",
                    color = Ink,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 11.dp)
                )
            }
            error?.let {
                Text(it, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            }
            PrimaryButton(
                text = if (loading) "Revealing…" else "Reveal gift",
                onClick = {
                    if (!privacyAccepted) {
                        error = "Accept the privacy notice to continue"
                    } else if (!loading) {
                        loading = true
                        error = null
                        scope.launch {
                            GhostGiftRepository.reveal(context, token, true)
                                .onSuccess { gift = it }
                                .onFailure { error = it.message ?: "Unable to reveal this gift" }
                            loading = false
                        }
                    }
                },
                containerColor = GhostGreen,
                contentColor = Ink,
                modifier = Modifier.padding(top = 18.dp)
            )
            if (loading) {
                CircularProgressIndicator(color = GhostGreen, modifier = Modifier.size(28.dp).align(Alignment.CenterHorizontally).padding(top = 12.dp))
            }
        } else {
            val revealed = requireNotNull(gift)
            Text(
                "${revealed.senderName} thought of you.",
                color = GhostGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 18.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (revealed.imageUrl != null) {
                    AsyncImage(
                        model = revealed.imageUrl,
                        contentDescription = revealed.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(18.dp)
                    )
                } else {
                    GhostMascotPose("cart", Modifier.size(150.dp))
                }
            }
            Text(revealed.category, color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
            Text(revealed.title, color = Ink, fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
            if (revealed.amountCents > 0) {
                DirhamAmount(
                    amount = String.format(Locale.US, "%.2f", revealed.amountCents / 100.0),
                    tint = Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    glyphSize = 16.dp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftGray)
                    .padding(16.dp)
            ) {
                Text("A thought, not a purchase", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Text(revealed.disclosure, color = MutedText, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 5.dp))
            }
            PrimaryButton(
                text = "Ghost it too — start 24-hour cooldown",
                onClick = { onGhostGift(revealed) },
                containerColor = GhostGreen,
                contentColor = Ink,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                "The sender can see that the invitation was revealed, but not what you decide next.",
                color = MutedText,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
