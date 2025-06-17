package com.github.callmeqan.jarviscomposed.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onGloballyPositioned
import com.github.callmeqan.jarviscomposed.utils.SharedViewModel

@Composable
fun LlmModeDropUp(
    viewModel: SharedViewModel,
    onModeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "vanilla" to "chatbot/vanilla",
        "func call" to "chatbot/function_call_chatbot",
        "bluetooth" to "chatbot/bluetooth_processor"
    )
    val initial = options.find { it.second == viewModel.apiMode } ?: options.first()
    var selected by remember(viewModel.apiMode) { mutableStateOf(initial) }
    val buttonWidth = remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .onGloballyPositioned { coords ->
                buttonWidth.value = coords.size.width
            }
    ) {
        Button(onClick = { expanded = !expanded }) {
            Text("Mode: ${selected.first}")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { buttonWidth.value.toDp() })
                .align(Alignment.TopStart),
            offset = DpOffset(x = 0.dp, y = 0.dp)
        ) {
            options.forEach { (label, code) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        selected = label to code
                        expanded = false
                        onModeChange(code)
                    }
                )
            }
        }
    }
}
