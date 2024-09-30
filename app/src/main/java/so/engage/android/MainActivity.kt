package so.engage.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import so.engage.android.sdk.engage.Engage
import so.engage.android.ui.theme.EngageandroidTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.messaging.isAutoInitEnabled = true

        Engage.instance.initialise(this, "pk_bcbdcceecc80b6b83d7d8df664a98761")
        val properties = mapOf(
            "first_name" to "Ifeanyi",
            "last_name" to "Onuoha",
            "email" to "ifeonu@gmail.com",
        )
        Engage.instance.identify("beifbeiue", properties)
        Engage.instance.track("app_open")
        Engage.instance.onMessageOpened { message ->
            println("FIREBASE MESSAGE OPENED ON APP ${message.data}")
        }
        Engage.instance.onMessageReceived { message ->
            println("FIREBASE MESSAGE RECEIVED ON APP ${message.data}")
        }


        setContent {
            EngageandroidTheme {
                HomePage()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomePage() {
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    LaunchedEffect(key1 = Unit) {
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) { }
}