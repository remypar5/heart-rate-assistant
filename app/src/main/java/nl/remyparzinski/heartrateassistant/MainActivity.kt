package nl.remyparzinski.heartrateassistant

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import nl.remyparzinski.heartrateassistant.ui.theme.HeartRateAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println(1)
        enableEdgeToEdge()
        println(2)
        setupLockScreenFlags()

        println(3)
        val scanner = BleHeartRateScanner(this)

        println(4)
        scanner.startScan(
            scanPeriodMs = 10000,
            onDeviceFound = { name, macAddress ->
                println("Found Heart Rate Monitor: $name -> MAC: $macAddress")
                val intent = Intent(this, HeartRateService::class.java).apply {
                    putExtra(HeartRateService.EXTRA_DEVICE_ADDRESS, macAddress)
                }

                startService(intent)
            },
            onScanFailed = { errorCode ->
                println("Scan failed with error code: $errorCode")
            }
        )

        setContent {
            HeartRateAssistantTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun setupLockScreenFlags() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // TODO Set using a checkbox/toggle
        // Prevent the screen from turning off when active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HeartRateAssistantTheme {
        Greeting("Android")
    }
}
