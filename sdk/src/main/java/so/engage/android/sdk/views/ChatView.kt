package so.engage.android.sdk.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import so.engage.android.sdk.models.MessageModel
import so.engage.android.sdk.utils.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(viewModel: EngageViewModel) {
    val input = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
    ) {
        // Offline message
        if (viewModel.agentsOnline.intValue < 1) {
            Text(
                text = "We are currently offline. Send us a message and we will respond soon.",
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp,
                color = Color(0xFF374151)
            )
        }

        // Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            viewModel.sections.forEach { section ->
                item {
                    SectionHeader(title = section["title"] as String)
                }
                items(section["data"] as List<*>) { message ->
                    MessageBubble(message = message as MessageModel)
                }
            }
        }

        // Typing indicator
        if (viewModel.agentTyping.value) {
            Text(
                text = "Typing...",
                modifier = Modifier.padding(start = 12.dp),
                fontSize = 14.sp,
                color = Color(0xFF777777)
            )
        }

        // Input Area
        InputArea(
            input = input.value,
            onInputChange = { text ->
                input.value = text
                viewModel.handleTypingChange()
            },
            onSend = { viewModel.handleSend(input) }
        )
    }

}

@Composable
fun SectionHeader(title: String) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
fun MessageBubble(message: MessageModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    println(message.toString())
                }
            }
            .wrapContentWidth(if (message.outbound) Alignment.End else Alignment.Start)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (message.outbound) Color.Green else Color.Cyan,
                    RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
        ) {
            HtmlTextView(html = message.body)
            Text(
                text = message.lastUpdated.toFormattedDate,
                fontSize = 12.sp,
                color = Color(0xFF777777),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun InputArea(input: String, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .border(1.dp, Color(0xFFE5E7EB)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Type a message...", color = Color(0xFF9CA3AF)) },
            modifier = Modifier
                .weight(1f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        IconButton(onClick = {}) { Text("😊") }
        IconButton(onClick = {}) { Text("📎") }
        Button(
            onClick = onSend,
            modifier = Modifier
                .padding(start = 8.dp)
                .background(Color(0xFF0B93F6), RoundedCornerShape(6.dp))
        ) {
            Text("Send", color = Color.White)
        }
    }
}
