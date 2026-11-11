package com.mukeshprajapat.kharcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mukeshprajapat.kharcha.ui.navigation.KharchaNavHost
import com.mukeshprajapat.kharcha.ui.theme.KharchaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KharchaTheme {
                KharchaNavHost()
            }
        }
    }
}
