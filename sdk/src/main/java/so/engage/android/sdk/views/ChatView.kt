package so.engage.android.sdk.views

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import so.engage.android.sdk.models.MessageModel
import so.engage.android.sdk.utils.isScrolledToEnd
import so.engage.android.sdk.utils.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(navigator: NavController, viewModel: EngageViewModel) {
    val listState = rememberLazyListState()
    val input = remember { mutableStateOf("") }

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty() && !listState.isScrolledToEnd()) {
            listState.animateScrollToItem(viewModel.messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = ""
                            )
                        }
                    },
                    windowInsets = WindowInsets()
                )
            }
        ) {
            Column(modifier = Modifier.padding(top = it.calculateTopPadding())) {
                // Offline message
                if (viewModel.agentsOnline.intValue < 1) {
                    Text(
                        text = "We are currently offline. Send us a message and we will respond soon.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    )
                }

                // Message List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    viewModel.sections.forEachIndexed { index, section ->
                        stickyHeader(key = "header_$index") {
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
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(vertical = 6.dp)

    ) {
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(18.dp)
                )
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(100.dp)
                )
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MessageBubble(message: MessageModel) {
    val config = LocalConfiguration.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    println(message.toString())
                }
            }
            .wrapContentWidth(if (message.outbound) Alignment.End else Alignment.Start),
        horizontalAlignment = if (message.outbound) Alignment.End else Alignment.Start

    ) {
        Box(
            modifier = Modifier
                .widthIn(max = (config.screenWidthDp * 0.8).dp)
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(18.dp)
                )
                .background(
                    if (message.outbound) Color(0xFFE8EAED) else Color.Transparent,
                    RoundedCornerShape(18.dp)
                )
                .padding(10.dp)


        ) {
            HtmlTextView(html = message.body, color = if (message.outbound) Color.Black else MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = message.lastUpdated.toFormattedDate,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun InputArea(input: String, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Send a message...", color = Color(0xFF9CA3AF)) },
                modifier = Modifier
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = {}) { Text("📎") }
            ElevatedButton(
                onClick = onSend,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = blue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier
                    .size(44.dp)

            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "",
                    tint = Color.White,
                )
            }
        }
    }
}
