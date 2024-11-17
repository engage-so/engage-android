package so.engage.android.sdk.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import so.engage.android.sdk.models.ContentType
import so.engage.android.sdk.models.InAppPayload


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarouselDialog(inAppPayload: InAppPayload, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { inAppPayload.contents.size })
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                onDismissRequest.invoke()
            }
        },
        shape = RoundedCornerShape(inAppPayload.radius),
        containerColor = inAppPayload.backgroundColor
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState,
            ) { page ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    for (content in inAppPayload.contents[page]) {
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

                                else -> Box {}
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 0 until pagerState.pageCount) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)

                            .size(8.dp)

                            .background(
                                color = if (i == pagerState.currentPage) inAppPayload.buttonColor
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            for (content in inAppPayload.contents[pagerState.currentPage]) {
                when (content.type) {
                    ContentType.Button -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                onClick = {
                                    if (pagerState.currentPage == inAppPayload.contents.lastIndex) {
                                        scope.launch {
                                            sheetState.hide()
                                            onDismissRequest.invoke()
                                        }
                                    } else {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
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

                    else -> Box {}
                }
            }
        }
    }
}