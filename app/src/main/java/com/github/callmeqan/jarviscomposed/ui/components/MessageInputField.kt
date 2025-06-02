package com.github.callmeqan.jarviscomposed.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.callmeqan.jarviscomposed.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    sendBtnOnClick: () -> Unit,
    micBtnOnClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    TextField(
        value = value,
        onValueChange = onValueChange,
        maxLines = 6,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .imePadding(),
        textStyle = TextStyle(
            color = Color(0xFFCCCCCC),
            fontSize = 16.sp,
            //fontFamily = FontFamily(Font(R.font.sen))
        ),
        placeholder = {
            Text(
                text = "Type a message...",
                style = TextStyle(
                    color = Color(0xFFCCCCCC),
                    fontSize = 16.sp,
                    //fontFamily = FontFamily(Font(R.font.sen))
                )
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Send button
                IconButton(
                    onClick = sendBtnOnClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color(0xFFCCCCCC)
                    ),
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(5.dp)) // Adjust the width as needed

                // Mic button
                IconButton(
                    onClick = micBtnOnClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic),
                        contentDescription = null
                    )
                }
            }

        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF2B2B2B),
            unfocusedContainerColor = Color(0xFF2B2B2B),
            disabledContainerColor = Color(0xFF2B2B2B),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        )
    )
}