package com.dalan.gh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dalan.gh.ui.theme.AppTheme
import com.dalan.gh.utils.appVersionCode
import com.dalan.gh.utils.appVersionName

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
        Column() {
            Text(text = "VersionedApp\nAPI: $baseUrl")
            Text(text = "VersionCode: $appVersionCode")
            Text(text = "VersionName: $appVersionName")
        }
    }
}

@Preview
@Composable
fun GreetingPreview() {
    AppTheme { Greeting("https://api.example.com") }
}
