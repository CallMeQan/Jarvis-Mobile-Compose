package com.github.callmeqan.jarviscomposed.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.callmeqan.jarviscomposed.data.ChatMessage

@Composable
fun MessageBox(message: ChatMessage) {

    if (message.image == null) {
        val modifier = if (message.role == "user") {
            Modifier
                .padding(start = 16.dp, end = 8.dp)
                .defaultMinSize(minHeight = 40.dp)
                .clip(RoundedCornerShape(topEnd = 15.dp, topStart = 15.dp, bottomStart = 15.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF007EF4),
                            Color(0xFF2A75BC),
                        )
                    )
                )
        } else {
            Modifier
                .padding(start = 8.dp, end = 16.dp)
                .defaultMinSize(minHeight = 60.dp)
                .clip(RoundedCornerShape(topEnd = 20.dp, topStart = 20.dp, bottomEnd = 20.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF454545),
                            Color(0xFF2B2B2B),
                        )
                    )
                )
        }

        val boxArrangement =
            if (message.role == "user") Alignment.CenterEnd else Alignment.CenterStart

        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth(),
            contentAlignment = boxArrangement
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                if (message.role != "user")
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(start = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )

                Box(
                    modifier = modifier
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = message.message,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                //fontFamily = FontFamily(Font(R.font.sen)),
                            )
                        )
                    }
                }
            }
        }
    }

    // If message.image is not null
    // (do not care about message.message and message.role)
    else {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.End
            ) {
                Image(
                    bitmap = message.image.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .aspectRatio(3f / 4f)
                        .clip(
                            RoundedCornerShape(
                                topEnd = 15.dp,
                                topStart = 15.dp,
                                bottomStart = 15.dp
                            )
                        )
                        .padding(end = 8.dp),

                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}