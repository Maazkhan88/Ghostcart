package com.example.ghostcart.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.common.PrimaryButton

/**
 * Blocking first-run (and post-version-bump) gate: the user must explicitly accept that
 * Ghost Cart is a simulation before reaching any other screen. There is deliberately no
 * back button, dismiss gesture, or "skip" - see AppViewModel.acceptSimulationConsent(),
 * only ever invoked from this screen's own button.
 */
@Composable
fun SimulationConsentScreen(consentText: String, onAccept: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Wide tablets otherwise stretch the body text/button edge-to-edge - same fix
        // applied to AuthScreen's sign-in form for the same reason.
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GhostMascotPose("wave", modifier = Modifier.size(96.dp))
            Text(
                "Before we begin",
                color = Ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                "Ghost Cart is a simulation-only app. It does not process real payments, place real orders or deliver products. It helps you pause, cool and reconsider impulse purchases.",
                color = MutedText,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = true, onCheckedChange = null)
                Text(
                    "I understand that Ghost Cart is only a simulation.",
                    color = Ink,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            PrimaryButton(
                text = "I understand — continue",
                onClick = onAccept,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
