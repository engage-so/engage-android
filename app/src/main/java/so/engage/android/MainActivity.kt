package so.engage.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import so.engage.android.sdk.engage.Engage
import so.engage.android.ui.theme.EngageandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Engage.instance.initialise(this, "pk_bcbdcceecc80b6b83d7d8df664a98761")
        val properties = mapOf(
            "first_name" to "Ifeanyi",
            "last_name" to "Onuoha",
            "email" to "ifeonu@gmail.com",
            "number" to "+2348060943333",
        )
        Engage.instance.identify("beifbeiue", properties)
        Engage.instance.track("app_open")
        setContent {
            EngageandroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { }
            }
        }
    }
}
