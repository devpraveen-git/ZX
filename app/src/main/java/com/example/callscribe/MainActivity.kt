package com.example.callscribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import com.example.callscribe.theme.CallScribeTheme
import com.example.callscribe.ui.CallDetailViewModel
import com.example.callscribe.ui.HomeViewModel
import com.example.callscribe.ui.ModelManagerViewModel
import com.example.callscribe.ui.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val homeViewModel: HomeViewModel by viewModels()
  private val callDetailViewModel: CallDetailViewModel by viewModels()
  private val settingsViewModel: SettingsViewModel by viewModels()
  private val modelManagerViewModel: ModelManagerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      CallScribeTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            homeViewModel = homeViewModel,
            callDetailViewModel = callDetailViewModel,
            settingsViewModel = settingsViewModel,
            modelManagerViewModel = modelManagerViewModel
          )
        }
      }
    }
  }
}
