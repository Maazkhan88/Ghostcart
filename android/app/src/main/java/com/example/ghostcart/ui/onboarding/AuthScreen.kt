package com.example.ghostcart.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.ghostcart.data.AuthRepository
import com.example.ghostcart.theme.DangerRed
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GhostPaletteState
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.common.PrimaryButton
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ghostcart.app.BuildConfig
import com.ghostcart.app.R
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
    val darkMode = GhostPaletteState.darkMode

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
                require(
                    credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                )
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GhostMascotPose(poseName = "wallet", modifier = Modifier.size(72.dp))

        Text(
            text = if (isSignIn) "Welcome Back" else "Create Real Account",
            color = Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 14.dp)
        )

        Text(
            text = if (isSignIn) {
                "Sign in to keep tracking your craved saves."
            } else {
                "Sign up now. Even though it is a fake shop, your account will be real."
            },
            color = MutedText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(FaintBorder)
                .padding(4.dp)
        ) {
            AuthTab(
                text = "Sign In",
                selected = isSignIn,
                onClick = {
                    isSignIn = true
                    errorMessage = ""
                },
                modifier = Modifier.weight(1f)
            )
            AuthTab(
                text = "Sign Up",
                selected = !isSignIn,
                onClick = {
                    isSignIn = false
                    errorMessage = ""
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (!loading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (darkMode) Color(0xFF131314) else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (darkMode) Color(0xFF8E918F) else Color(0xFF747775),
                        shape = RoundedCornerShape(25.dp)
                    )
                    .clickable(onClick = ::signInWithGoogle)
            ) {
                Image(
                    painter = painterResource(R.drawable.google_g_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Continue with Google",
                    color = if (darkMode) Color(0xFFE3E3E3) else Color(0xFF1F1F1F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Image(
                painter = painterResource(
                    if (darkMode) R.drawable.sign_in_with_apple_white
                    else R.drawable.sign_in_with_apple_black
                ),
                contentDescription = "Continue with Apple",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(680f / 100f)
                    .clickable {
                        errorMessage =
                            "Apple Sign-In requires an Apple Services ID and verified callback domain."
                    }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = FaintBorder)
                Text(
                    text = "or use email",
                    color = MutedText,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = FaintBorder)
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = ""
            },
            label = { Text("Email Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = authFieldColors()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = authFieldColors()
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = DangerRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (loading) {
            CircularProgressIndicator(color = GhostGreen, modifier = Modifier.size(36.dp))
        } else {
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !loading, onClick = onGuest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Continue as Guest",
                color = if (loading) MutedText else Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AuthTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Ink else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Paper else Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GhostGreen,
    unfocusedBorderColor = FaintBorder,
    focusedLabelColor = GhostGreen,
    unfocusedLabelColor = MutedText,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    disabledTextColor = MutedText,
    disabledBorderColor = FaintBorder,
    disabledLabelColor = MutedText
)
