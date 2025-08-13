package so.engage.android.sdk.views

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import so.engage.android.sdk.models.ContentType
import so.engage.android.sdk.models.InAppPayload

@Composable
fun SimpleDialog(
    inAppPayload: InAppPayload,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onDismissRequest.invoke() }
            }
        ) {
            Card(
                modifier = Modifier
                    .align(inAppPayload.alignment)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .pointerInput(Unit) { detectTapGestures {} },
                shape = RoundedCornerShape(inAppPayload.radius),
                colors = CardDefaults.cardColors(
                    containerColor = inAppPayload.backgroundColor,
                )
            ) {
                Column {
                    if (inAppPayload.closeBtn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { onDismissRequest.invoke() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close Icon"
                                )
                            }
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = if (inAppPayload.closeBtn) 0.dp else 16.dp,
                            start = 16.dp, end = 16.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (content in inAppPayload.contents.first()) {
                            item {
                                when (content.type) {
                                    ContentType.Row -> {}
                                    ContentType.Text -> {
                                        Text(
                                            text = content.content ?: "",
                                            color = inAppPayload.textColor,
                                            textAlign = TextAlign.Center,
                                        )
                                    }

                                    ContentType.Image -> {
                                        AsyncImage(
                                            model = content.url,
                                            contentDescription = "Network Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth(content.imageMaxWidth)
                                                .clip(shape = RoundedCornerShape(inAppPayload.radius))

                                        )
                                    }

                                    ContentType.Button -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Button(
                                                onClick = onDismissRequest,
                                                modifier = Modifier.height(50.dp).let {
                                                    if (content.buttonFillWidth) it.fillMaxWidth(
                                                        fraction = content.buttonMaxWidth,
                                                    ) else it
                                                },
                                                shape = RoundedCornerShape(content.buttonRadius),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = inAppPayload.buttonColor,
                                                    contentColor = inAppPayload.buttonTextColor,
                                                )
                                            ) {
                                                Text(content.content ?: "")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}