package com.example.ghostcart.ui.onboarding

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.ghostcart.data.AuthRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ghostcart.app.BuildConfig
import com.example.ghostcart.theme.DangerRed
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.common.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit,
    onGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignIn by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    fun signInWithGoogle() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            errorMessage = "Google Sign-In needs the Ghost Cart OAuth client ID."
            return
        }
        loading = true
        errorMessage = ""
        scope.launch {
            runCatching {
                val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = credentialManager.getCredential(context, request).credential
                require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                GoogleIdTokenCredential.createFrom(credential.data).id
            }.onSuccess { googleEmail ->
                loading = false
                onAuthSuccess(googleEmail)
            }.onFailure { error ->
                loading = false
                errorMessage = error.message ?: "Google Sign-In was cancelled or unavailable."
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 24.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            GhostMascotPose(poseName = "wallet", modifier = Modifier.size(80.dp))

            Text(
                text = if (isSignIn) "Welcome Back" else "Create Real Account",
                color = Ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = if (isSignIn) "Sign in to keep tracking your craved saves." else "Sign up now. Even though it is a fake shop, your account will be real.",
                color = MutedText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tab selection switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FaintBorder)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSignIn) Ink else Color.Transparent)
                        .clickable { isSignIn = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = if (isSignIn) Paper else Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isSignIn) Ink else Color.Transparent)
                        .clickable { isSignIn = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        color = if (!isSignIn) Paper else Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = "" },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GhostGreen,
                    unfocusedBorderColor = FaintBorder,
                    focusedLabelColor = GhostGreen,
                    unfocusedLabelColor = MutedText,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GhostGreen,
                    unfocusedBorderColor = FaintBorder,
                    focusedLabelColor = GhostGreen,
                    unfocusedLabelColor = MutedText,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink
                )
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = DangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                CircularProgressIndicator(color = GhostGreen, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                OutlinedButton(
                    onClick = ::signInWithGoogle,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
                ) {
                    Text("G", color = GhostGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("Continue with Google", modifier = Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { errorMessage = "Apple Sign-In requires an Apple Services ID and verified callback domain." },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
                ) {
                    Text("Apple", fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = FaintBorder)
                    Text("or use email", color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = FaintBorder)
                }
                PrimaryButton(
                    text = if (isSignIn) "Sign In" else "Sign Up",
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all fields"
                            return@PrimaryButton
                        }
                        loading = true
                        scope.launch {
                            val result = if (isSignIn) {
                                AuthRepository.signIn(email, password)
                            } else {
                                AuthRepository.signUp(email, password)
                            }
                            loading = false
                            result.onSuccess {
                                onAuthSuccess(email)
                            }.onFailure {
                                errorMessage = it.message ?: "Authentication failed"
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Continue as Guest",
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onGuest)
                        .padding(8.dp)
                )
            }
        }
    }
}
