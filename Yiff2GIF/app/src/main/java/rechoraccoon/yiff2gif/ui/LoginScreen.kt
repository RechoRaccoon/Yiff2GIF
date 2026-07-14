package rechoraccoon.yiff2gif.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.12f))

        Text(
            text = "Yiff2GIF",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = darkFieldColors()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            colors = darkFieldColors()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onLogin(username.trim(), apiKey.trim()) },
            enabled = username.isNotBlank() && apiKey.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00FF07),
                contentColor = Color.Black
            )
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
            } else {
                Text("Log In")
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, color = Color(0xFFFF5555), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = buildAnnotatedString {
                append("Made by ")
                withStyle(SpanStyle(color = Color(0xFF00FF07))) {
                    append("Recho Raccoon")
                }
            },
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF00FF07),
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = Color(0xFF00FF07),
    unfocusedLabelColor = Color.Gray,
    cursorColor = Color(0xFF00FF07)
)
