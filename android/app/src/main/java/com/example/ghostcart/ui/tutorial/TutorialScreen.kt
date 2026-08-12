package com.example.ghostcart.ui.tutorial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.TUTORIAL_COOLDOWN_MILLIS
import com.example.ghostcart.data.TUTORIAL_PRODUCT_ID
import com.example.ghostcart.data.AlmostBuy
import com.example.ghostcart.data.AlmostBuyStatus
import com.example.ghostcart.data.GhostDeliveryState
import com.example.ghostcart.data.GhostOrderResolution
import com.example.ghostcart.data.ghostDeliverySnapshot
import com.example.ghostcart.data.TutorialDecision
import com.example.ghostcart.data.TutorialState
import com.example.ghostcart.data.TutorialStep
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.DirhamAmount
import com.example.ghostcart.ui.GhostCartWordmark
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.SecondaryButton
import com.example.ghostcart.ui.delivery.GhostDeliveryTrackerScreen
import com.ghostcart.app.R
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun TutorialScreen(
    state: TutorialState,
    onContinueWelcome: () -> Unit,
    onSkip: () -> Unit,
    onOpenPracticeProduct: () -> Unit,
    onAddToCart: () -> Unit,
    onOpenCooldown: () -> Unit,
    onSelectTutorialCooldown: () -> Unit,
    onContinueToCheckout: () -> Unit,
    onCompleteFakeCheckout: () -> Unit,
    onFinishCooling: () -> Unit,
    onChooseDecision: (TutorialDecision) -> Unit,
    onContinueFromReceipt: () -> Unit,
    onDeliveryFinished: () -> Unit,
    onReplay: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    Box(modifier.fillMaxSize().background(Paper)) {
        AnimatedContent(
            targetState = state.currentStep,
            label = "tutorial_step",
            modifier = Modifier.fillMaxSize()
        ) { step ->
            when (step) {
                TutorialStep.WELCOME -> WelcomeStep(onContinueWelcome, onSkip)
                TutorialStep.PRACTICE_INTRO -> PracticeIntroStep(onOpenPracticeProduct)
                TutorialStep.PRODUCT -> ProductStep(onAddToCart)
                TutorialStep.CART -> CartStep(onOpenCooldown)
                TutorialStep.COOLDOWN -> CooldownPickerStep(
                    selected = state.selectedCooldownMillis == TUTORIAL_COOLDOWN_MILLIS,
                    onSelectTutorial = onSelectTutorialCooldown,
                    onContinue = onContinueToCheckout
                )
                TutorialStep.FAKE_CHECKOUT -> FakeCheckoutStep(onCompleteFakeCheckout)
                TutorialStep.COOLING -> CoolingStep(
                    coolingEndsAt = state.coolingEndsAt,
                    onFinished = onFinishCooling
                )
                TutorialStep.DECISION -> DecisionStep(onChooseDecision)
                TutorialStep.GHOST_RECEIPT -> ReceiptStep(state.decision, onContinueFromReceipt)
                TutorialStep.COMPLETE -> CompleteStep(onDeliveryFinished, onReplay)
                TutorialStep.DELIVERY -> TutorialDeliveryTracker(
                    state = state,
                    onBack = { showExitDialog = true },
                    onChooseDecision = onChooseDecision
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave the tutorial?") },
            text = { Text("The practice coffee and donut will disappear, but you can replay the tutorial later from Profile.") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Continue tutorial") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) { Text("Exit tutorial") }
            }
        )
    }
}

@Composable
private fun TutorialPage(
    step: TutorialStep,
    title: String,
    subtitle: String? = null,
    hero: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            TutorialProgress(step)
            if (hero != null) {
                Box(Modifier.fillMaxWidth().padding(top = 22.dp), contentAlignment = Alignment.Center) { hero() }
            }
            Text(
                title,
                color = Ink,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            )
            subtitle?.let {
                Text(
                    it,
                    color = MutedText,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            content()
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun TutorialProgress(step: TutorialStep) {
    val progress = ((step.ordinal + 1).toFloat() / TutorialStep.entries.size).coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { progress },
        color = GhostGreen,
        trackColor = FaintBorder,
        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
    )
}

@Composable
private fun WelcomeStep(onStart: () -> Unit, onSkip: () -> Unit) {
    TutorialPage(
        step = TutorialStep.WELCOME,
        title = "Welcome to Ghost Cart 👻",
        subtitle = "Pause the craving. Keep the control.",
        hero = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GhostCartWordmark(modifier = Modifier.width(220.dp).height(64.dp), tint = Ink)
                TutorialTeacherImage(
                    imageRes = R.drawable.tutorial_teacher_board,
                    contentDescription = "Ghost Cart teacher welcoming the user",
                    size = 150.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    ) {
        Text(
            "Ghost Cart gives everything you almost bought somewhere safe to go while you decide.",
            color = Ink,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        )
        PrimaryButton("Start the tutorial", onStart, modifier = Modifier.padding(top = 28.dp), containerColor = GhostGreen, contentColor = Color.Black)
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("Skip for now", color = MutedText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PracticeIntroStep(onContinue: () -> Unit) {
    TutorialPage(
        step = TutorialStep.PRACTICE_INTRO,
        title = "Your first Ghost is on the house ☕🍩",
        subtitle = "Thank you for trying Ghost Cart. We’ve added a fictional coffee-and-donut combo so you can practise the complete flow without buying anything.",
        hero = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TutorialTeacherImage(
                    imageRes = R.drawable.tutorial_teacher_checklist,
                    contentDescription = "Ghost Cart teacher introducing the practice steps",
                    size = 116.dp
                )
                Spacer(Modifier.width(10.dp))
                TutorialProductImage(180.dp)
            }
        }
    ) {
        DisclosureCard("Tutorial item only. No real order, payment or delivery.")
        PrimaryButton("Ghost this combo", onContinue, modifier = Modifier.padding(top = 18.dp), containerColor = GhostGreen, contentColor = Color.Black)
    }
}

@Composable
private fun ProductStep(onAddToCart: () -> Unit) {
    TutorialPage(
        step = TutorialStep.PRODUCT,
        title = "Coffee and donut",
        subtitle = "A warm coffee and glazed donut, on the house for your first Ghost Cart practice run.",
        hero = { TutorialProductImage(250.dp) }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ghost Café · Tutorial", color = GhostGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text("Food & delivery", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            DirhamAmount("31.00", fontSize = 20.sp, glyphSize = 17.dp)
        }
        CoachMark("Start by Ghosting the item.", modifier = Modifier.padding(top = 20.dp)) {
            PrimaryButton(
                text = "Ghost it",
                onClick = onAddToCart,
                leadingIcon = Icons.Filled.ShoppingCart,
                containerColor = GhostGreen,
                contentColor = Color.Black
            )
        }
        DisclosureCard("Simulation only. This tutorial product cannot be purchased.", Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun CartStep(onProceed: () -> Unit) {
    TutorialPage(
        step = TutorialStep.CART,
        title = "Your practice Ghost Cart",
        subtitle = "The practice item is isolated from your real cart."
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TutorialProductImage(82.dp)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Coffee and donut", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Tutorial item · Qty 1", color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                DirhamAmount("31.00", fontSize = 14.sp, glyphSize = 12.dp)
            }
        }
        CoachMark("This is your simulated cart. Nothing here will be ordered or charged.", Modifier.padding(top = 18.dp)) {
            PrimaryButton("Proceed to checkout", onProceed, containerColor = GhostGreen, contentColor = Color.Black)
        }
    }
}

@Composable
private fun CooldownPickerStep(
    selected: Boolean,
    onSelectTutorial: () -> Unit,
    onContinue: () -> Unit
) {
    TutorialPage(
        step = TutorialStep.COOLDOWN,
        title = "Give the craving a little time",
        subtitle = "Cooling gives the impulse time to fade. For this practice run, choose 10 seconds.",
        hero = {
            TutorialTeacherImage(
                imageRes = R.drawable.tutorial_teacher_board,
                contentDescription = "Ghost Cart teacher explaining cooldown time",
                size = 140.dp
            )
        }
    ) {
        CoachMark("Choose the special tutorial duration.") {
            CooldownOptionRow(
                label = "10 seconds — Tutorial",
                supporting = "Recommended for this practice run",
                selected = selected,
                enabled = true,
                onClick = onSelectTutorial
            )
        }
        CooldownOptionRow("24 hours", "Default for real Ghost items", false, false, {})
        CooldownOptionRow("3 days", "For a longer pause", false, false, {})
        PrimaryButton(
            "Continue to Fake Checkout",
            onClick = { if (selected) onContinue() },
            modifier = Modifier.padding(top = 18.dp),
            containerColor = if (selected) GhostGreen else FaintBorder,
            contentColor = if (selected) Color.Black else MutedText
        )
        if (!selected) {
            Text("Select the 10-second tutorial option to continue.", color = MutedText, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}

@Composable
private fun CooldownOptionRow(
    label: String,
    supporting: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) GreenTint else SoftGray)
            .border(if (selected) 2.dp else 1.dp, if (selected) GhostGreen else FaintBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, color = if (enabled) Ink else MutedText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(supporting, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun FakeCheckoutStep(onComplete: () -> Unit) {
    TutorialPage(
        step = TutorialStep.FAKE_CHECKOUT,
        title = "Fake Checkout",
        subtitle = "Complete the checkout feeling without spending real money."
    ) {
        DisclosureCard("Simulation only. No real payment or delivery.")
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CheckoutSummaryRow("Practice item", "Coffee and donut")
                CheckoutSummaryRow("Cooldown", "10 seconds — Tutorial")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Simulated total", color = Ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    DirhamAmount("31.00", fontSize = 18.sp, glyphSize = 15.dp)
                }
            }
        }
        CoachMark("Complete the checkout feeling without spending real money.", Modifier.padding(top = 18.dp)) {
            PrimaryButton("Complete Fake Checkout", onComplete, containerColor = GhostGreen, contentColor = Color.Black, leadingIcon = Icons.Filled.Lock)
        }
    }
}

@Composable
private fun CheckoutSummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MutedText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CoolingStep(coolingEndsAt: Long?, onFinished: () -> Unit) {
    var now by remember(coolingEndsAt) { mutableLongStateOf(System.currentTimeMillis()) }
    val remaining = ((coolingEndsAt ?: now) - now).coerceAtLeast(0L)
    val progressTarget = (1f - remaining.toFloat() / TUTORIAL_COOLDOWN_MILLIS).coerceIn(0f, 1f)
    val progress by animateFloatAsState(progressTarget, tween(180), label = "tutorial_cooling_progress")

    LaunchedEffect(coolingEndsAt) {
        if (coolingEndsAt == null) return@LaunchedEffect
        while (System.currentTimeMillis() < coolingEndsAt) {
            now = System.currentTimeMillis()
            delay(100L)
        }
        now = coolingEndsAt
        onFinished()
    }

    TutorialPage(
        step = TutorialStep.COOLING,
        title = "Let it cool…",
        subtitle = "Take a breath. The item will still be here when the timer ends.",
        hero = {
            Box(contentAlignment = Alignment.BottomEnd) {
                TutorialProductImage(230.dp)
                TutorialTeacherImage(
                    imageRes = R.drawable.tutorial_teacher_pointer,
                    contentDescription = "Ghost Cart teacher guiding the cooldown",
                    size = 96.dp
                )
            }
        }
    ) {
        Text(
            "${ceil(remaining / 1000.0).toInt()}s",
            color = Ink,
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${ceil(remaining / 1000.0).toInt()} seconds remaining" }
        )
        LinearProgressIndicator(
            progress = { progress },
            color = GhostGreen,
            trackColor = FaintBorder,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(10.dp).clip(CircleShape)
        )
        SecondaryButton("Skip countdown for tutorial", onFinished, modifier = Modifier.padding(top = 18.dp))
        DisclosureCard("This in-session timer creates no background job or notification.", Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun DecisionStep(onChoose: (TutorialDecision) -> Unit) {
    TutorialPage(
        step = TutorialStep.DECISION,
        title = "Still want it?",
        subtitle = "Any answer is useful. Ghost Cart helps you make an intentional decision, not a perfect one.",
        hero = {
            TutorialTeacherImage(
                imageRes = R.drawable.tutorial_teacher_checklist,
                contentDescription = "Ghost Cart teacher helping with the decision",
                size = 140.dp
            )
        }
    ) {
        DecisionCard("Ghost it", "Skip the simulated purchase.", recommended = true) { onChoose(TutorialDecision.GHOSTED) }
        DecisionCard("Send it around again", "Give the decision more time.") { onChoose(TutorialDecision.COOL_LONGER) }
        DecisionCard("I’d still buy it", "That’s okay — the goal is an intentional decision.") { onChoose(TutorialDecision.STILL_BUY) }
        DisclosureCard("Your practice choice will not affect Wallet, Money Kept, history or leaderboards.", Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun DecisionCard(title: String, supporting: String, recommended: Boolean = false, onClick: () -> Unit) {
    Surface(
        color = if (recommended) GreenTint else SoftGray,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(if (recommended) 2.dp else 1.dp, if (recommended) GhostGreen else FaintBorder),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (recommended) Icons.Filled.CheckCircle else Icons.Filled.AccessTime, null, tint = if (recommended) GhostGreen else MutedText, modifier = Modifier.size(23.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(supporting, color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (recommended) Text("Recommended", color = GhostGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ReceiptStep(decision: TutorialDecision?, onContinue: () -> Unit) {
    val result = when (decision) {
        TutorialDecision.GHOSTED -> "Ghosted — skipped the simulated purchase"
        TutorialDecision.COOL_LONGER -> "Chose more cooling time"
        TutorialDecision.STILL_BUY -> "Would still buy — an intentional decision"
        null -> "Practice decision complete"
    }
    TutorialPage(
        step = TutorialStep.GHOST_RECEIPT,
        title = "Your first Ghost Receipt",
        subtitle = "A one-time tutorial record",
        hero = {
            TutorialTeacherImage(
                imageRes = R.drawable.tutorial_teacher_checklist,
                contentDescription = "Ghost Cart teacher presenting the tutorial receipt",
                size = 136.dp
            )
        }
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("TUTORIAL GHOST RECEIPT", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                CheckoutSummaryRow("Product", "Coffee and donut")
                CheckoutSummaryRow("Result", result)
                CheckoutSummaryRow("Cooling period", "10-second tutorial")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Simulated price", color = MutedText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    DirhamAmount("31.00", fontSize = 15.sp, glyphSize = 13.dp)
                }
                Text(
                    "Tutorial record only. No purchase, payment or delivery occurred.",
                    color = MutedText,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        PrimaryButton("Continue", onContinue, modifier = Modifier.padding(top = 18.dp))
    }
}

@Composable
private fun CompleteStep(onExplore: () -> Unit, onReplay: () -> Unit) {
    TutorialPage(
        step = TutorialStep.COMPLETE,
        title = "You’re ready to Ghost 👻",
        subtitle = "You’ve learned the full Ghost Cart flow: discover it, choose a Ghost Delivery time, track it and decide when the impulse is quieter.",
        hero = {
            TutorialTeacherImage(
                imageRes = R.drawable.tutorial_teacher_confetti,
                contentDescription = "Ghost Cart teacher celebrating tutorial completion",
                size = 150.dp
            )
        }
    ) {
        DisclosureCard("The practice product and Ghost Order now vanish. They never enter your Wallet, Orders, history or Leaderboard.")
        PrimaryButton("Explore Ghost Cart", onExplore, modifier = Modifier.padding(top = 18.dp), containerColor = GhostGreen, contentColor = Color.Black)
        SecondaryButton("Replay tutorial", onReplay, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun TutorialDeliveryTracker(
    state: TutorialState,
    onBack: () -> Unit,
    onChooseDecision: (TutorialDecision) -> Unit
) {
    val context = LocalContext.current
    val end = state.coolingEndsAt ?: (System.currentTimeMillis() + TUTORIAL_COOLDOWN_MILLIS)
    val start = end - TUTORIAL_COOLDOWN_MILLIS
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.sessionId, end) {
        while (now < end) {
            delay(250L)
            now = System.currentTimeMillis()
        }
    }
    val tutorialSnapshot = ghostDeliverySnapshot(now, start, end, GhostDeliveryState.PLACED)
    val tutorialOrder = remember(state.sessionId, end) {
        AlmostBuy(
            id = "tutorial_delivery_${state.sessionId.orEmpty()}",
            name = "Coffee and donut",
            amountCents = 3_100,
            category = "Food & delivery",
            trigger = "Tutorial",
            createdAtMillis = start,
            coolingUntilMillis = end,
            status = AlmostBuyStatus.COOLING,
            imageUrl = "android.resource://${context.packageName}/${R.drawable.tutorial_coffee_donut_combo}",
            sourceKind = "tutorial",
            ghostOrderId = "TUTORIAL-GHOST-ORDER",
            coolingStartedAtMillis = start,
            coolingDurationMillis = TUTORIAL_COOLDOWN_MILLIS,
            productId = TUTORIAL_PRODUCT_ID,
            sourceMerchant = "Ghost Café · Tutorial",
            deliveryState = GhostDeliveryState.PLACED,
            deliveryStartedAtMillis = start,
            deliveryEndsAtMillis = end,
            selectedDeliveryDurationMillis = TUTORIAL_COOLDOWN_MILLIS,
            routeSeed = 31L,
            tutorialOnly = true
        )
    }
    Box(Modifier.fillMaxSize()) {
        GhostDeliveryTrackerScreen(
            item = tutorialOrder,
            onBack = onBack,
            onResolve = { resolution ->
                onChooseDecision(
                    when (resolution) {
                        GhostOrderResolution.SKIPPED -> TutorialDecision.GHOSTED
                        GhostOrderResolution.BUY_FROM_SOURCE,
                        GhostOrderResolution.BOUGHT_ALREADY -> TutorialDecision.STILL_BUY
                    }
                )
            },
            onRestart = { onChooseDecision(TutorialDecision.COOL_LONGER) },
            onOpenSource = { onChooseDecision(TutorialDecision.STILL_BUY) },
            onShare = { onChooseDecision(TutorialDecision.COOL_LONGER) },
            tutorialMode = true
        )
        AnimatedContent(
            targetState = tutorialSnapshot.state,
            label = "tutorial_stage_banner",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp, start = 24.dp, end = 24.dp)
        ) { stage ->
            Surface(
                color = Ink.copy(alpha = 0.94f),
                contentColor = Paper,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.tutorial_teacher_pointer),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = tutorialStageNotification(stage),
                        color = Paper,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}

private fun tutorialStageNotification(state: GhostDeliveryState): String = when (state) {
    GhostDeliveryState.PLACED -> "Ghost Order placed · your decision is cooling"
    GhostDeliveryState.PREPARING -> "Your Ghost Order is being prepared"
    GhostDeliveryState.RIDER_PICKING_UP -> "Ghost Rider is picking up your order"
    GhostDeliveryState.OUT_FOR_DELIVERY -> "Out for Ghost Delivery"
    GhostDeliveryState.RIDER_NEARBY -> "Your Ghost Rider is nearby"
    GhostDeliveryState.DELIVERED -> "Your Ghost Order has arrived"
    else -> "Tutorial decision recorded"
}

@Composable
private fun TutorialDeliveryStep(onFinished: () -> Unit) {
    var remainingSeconds by remember { mutableLongStateOf(8L) }
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0L) {
            delay(1_000L)
            remainingSeconds -= 1L
        }
    }
    val progress by animateFloatAsState(
        targetValue = ((8L - remainingSeconds).toFloat() / 8f).coerceIn(0f, 1f),
        animationSpec = tween(450),
        label = "tutorial_delivery_progress"
    )
    val status = when {
        remainingSeconds >= 6L -> "Preparing your imaginary order"
        remainingSeconds >= 3L -> "Ghost Rider is gliding your way"
        remainingSeconds > 0L -> "Almost at your imaginary doorstep"
        else -> "Simulated delivery complete"
    }

    TutorialPage(
        step = TutorialStep.DELIVERY,
        title = status,
        subtitle = "Nothing is being purchased or delivered. This is the final tutorial simulation.",
        hero = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingTutorialTeacher(
                    modifier = Modifier.size(150.dp),
                    imageRes = if (remainingSeconds == 0L) {
                        R.drawable.tutorial_teacher_confetti
                    } else {
                        R.drawable.tutorial_teacher_pointer
                    },
                    contentDescription = if (remainingSeconds == 0L) {
                        "Ghost Cart teacher celebrating tutorial completion with confetti"
                    } else {
                        "Ghost Cart teacher guiding the simulated delivery"
                    }
                )
                TutorialProductImage(190.dp)
            }
        }
    ) {
        LinearProgressIndicator(
            progress = { progress },
            color = GhostGreen,
            trackColor = FaintBorder,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
        )
        Text(
            text = if (remainingSeconds > 0L) "$remainingSeconds seconds" else "The coffee and donut have vanished like a proper ghost.",
            color = if (remainingSeconds > 0L) Ink else GhostGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
        if (remainingSeconds == 0L) {
            PrimaryButton(
                "Explore Ghost Cart",
                onFinished,
                modifier = Modifier.padding(top = 18.dp),
                containerColor = GhostGreen,
                contentColor = Color.Black
            )
        }
    }
}

@Composable
private fun TutorialTeacherImage(
    imageRes: Int,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(imageRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
    )
}

@Composable
private fun TutorialProductImage(size: androidx.compose.ui.unit.Dp) {
    Image(
        painter = painterResource(R.drawable.tutorial_coffee_donut_combo),
        contentDescription = "Ghost Cart-branded coffee and glazed donut tutorial combo",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(6.dp)
    )
}

@Composable
private fun CoachMark(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GreenTint)
            .border(2.dp, GhostGreen, RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Text(text, color = Ink, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        Box(Modifier.padding(top = 8.dp)) { content() }
    }
}

@Composable
private fun DisclosureCard(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(SoftGray)
            .border(1.dp, FaintBorder, RoundedCornerShape(15.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
        Text(text, color = MutedText, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(start = 9.dp))
    }
}
