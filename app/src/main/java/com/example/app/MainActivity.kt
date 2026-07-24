package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                // BASE_URL is injected per-flavor via buildConfigField.
                Greeting(BuildConfig.BASE_URL)
            }
        }
    }
}

@Composable
fun Greeting(baseUrl: String) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Text(text = "VersionedApp\nAPI: $baseUrl")
    }
}

@Preview
@Composable
fun GreetingPreview() {
    AppTheme { Greeting("https://api.example.com") }
}
