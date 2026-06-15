package com.lakescorp.twitchchattts.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakescorp.twitchchattts.ChatViewModel
import com.lakescorp.twitchchattts.R
import com.lakescorp.twitchchattts.data.auth.AuthManager
import com.lakescorp.twitchchattts.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val oauthToken by viewModel.oauthToken.collectAsState()
    val clientId by viewModel.clientId.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    var inputToken by remember { mutableStateOf(oauthToken) }
    var inputClientId by remember { mutableStateOf(clientId) }

    var showAdvancedSettings by remember { mutableStateOf(ChatViewModel.DEFAULT_CLIENT_ID.isEmpty()) }

    LaunchedEffect(oauthToken) { inputToken = oauthToken }
    LaunchedEffect(clientId) { inputClientId = clientId }

    val appIcon = remember(context) {
        try {
            context.packageManager.getApplicationIcon(context.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF140D26),
                        DarkBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface.copy(alpha = 0.9f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(TwitchPurple, TwitchPurpleLight)
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Safe app icon drawing preserving 1:1 aspect ratio
                    if (appIcon != null) {
                        Canvas(
                            modifier = Modifier.size(80.dp)
                        ) {
                            drawIntoCanvas { canvas ->
                                appIcon.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                                appIcon.draw(canvas.nativeCanvas)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Spacer(modifier = Modifier.height(92.dp))
                    }

                    Text(
                        text = stringResource(id = R.string.welcome_to),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    )

                    Text(
                        text = stringResource(id = R.string.chat_tts),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 13.sp,
                            color = TextMuted
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // SECTION 1: AUTOMATIC LOGIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.option_a),
                            fontWeight = FontWeight.Bold,
                            color = TwitchPurpleLight,
                            fontSize = 14.sp
                        )

                        if (ChatViewModel.DEFAULT_CLIENT_ID.isNotEmpty()) {
                            Text(
                                text = if (showAdvancedSettings) stringResource(id = R.string.hide_id) else stringResource(id = R.string.use_custom_id),
                                fontSize = 11.sp,
                                color = TwitchPurpleLight,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { showAdvancedSettings = !showAdvancedSettings }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedVisibility(visible = showAdvancedSettings) {
                        OutlinedTextField(
                            value = inputClientId,
                            onValueChange = {
                                inputClientId = it
                                viewModel.setClientId(it)
                            },
                            label = { Text(stringResource(id = R.string.client_id_label), color = TextMuted) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "Client ID Icon",
                                    tint = TwitchPurpleLight
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = TwitchPurple,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard
                            )
                        )
                    }

                    Button(
                        onClick = {
                            val authUrl = viewModel.getAuthorizeUrl()
                            if (authUrl != null) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                context.startActivity(intent)
                            }
                        },
                        enabled = inputClientId.trim().isNotEmpty() && loginState !is AuthManager.LoginState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TwitchPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.authorize),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.authorize_desc),
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // DIVIDER
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                        Text(
                            text = " " + stringResource(id = R.string.or) + " ",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                    }

                    // SECTION 2: MANUAL TOKEN CONNECTION
                    Text(
                        text = stringResource(id = R.string.option_b),
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = {
                            inputToken = it
                            viewModel.setOauthToken(it)
                        },
                        label = { Text(stringResource(id = R.string.oauth_label), color = TextMuted) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Token Icon",
                                tint = TextMuted
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = TwitchPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitchtokengenerator.com/"))
                                context.startActivity(browserIntent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkCard,
                                contentColor = TextLight
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.get_token),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.validateAndLogin()
                            },
                            enabled = inputToken.trim().isNotEmpty() && loginState !is AuthManager.LoginState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TwitchPurple,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (loginState is AuthManager.LoginState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.connect_manually),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Error States
                    AnimatedVisibility(
                        visible = loginState is AuthManager.LoginState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val errorMsg = (loginState as? AuthManager.LoginState.Error)?.message ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(AlertRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = AlertRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                color = AlertRed,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Security Warning (when EncryptedSharedPreferences fallback occurred)
                    if (!viewModel.isStorageEncrypted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(AlertOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Warning",
                                tint = AlertOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.security_warning),
                                color = AlertOrange,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
