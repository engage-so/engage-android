package so.engage.android.sdk.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import so.engage.android.sdk.utils.toFormattedDate

val blue = Color(0xFF3264F3)

@Composable
fun HomeView(navigator: NavController, viewModel: EngageViewModel) {
    Column(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header Section
        Column(
            modifier = Modifier
                .fillMaxHeight(0.5f)
                .fillMaxWidth()
                .background(blue, shape = RoundedCornerShape(0.dp))
                .padding(40.dp)
            ,
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Hello :)",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "Questions, feedback, hi?\nSend us a message.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Message Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(10.dp))
                .padding(16.dp)

        ) {
            if (viewModel.activeThread.value != null) {
                Text(
                    text = viewModel.activeThread.value?.lastUpdated?.toFormattedDate ?: "",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                HtmlTextView(
                    html = viewModel.activeThread.value?.excerpt ?: "",
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            Button(
                onClick = { navigator.navigate("chat") },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blue
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (viewModel.activeThread.value != null) "Continue Conversation" else "Start a Conversation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "",
                    )
                }
            }
        }
    }
}

//@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
//@Composable
//fun PreviewHome() {
//    HomeView()
//}
