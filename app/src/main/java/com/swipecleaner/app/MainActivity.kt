package com.swipecleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swipecleaner.app.ui.screens.PermissionsGate
import com.swipecleaner.app.ui.screens.SettingsScreen
import com.swipecleaner.app.ui.screens.SwipeScreen
import com.swipecleaner.app.ui.screens.TrashScreen
import com.swipecleaner.app.ui.theme.SwipeCleanerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SwipeCleanerApplication

        setContent {
            val prefs by app.container.preferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = com.swipecleaner.app.data.UserPreferences())

            SwipeCleanerTheme(themeMode = prefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionsGate {
                        AppNav()
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "swipe") {
        composable("swipe") {
            SwipeScreen(
                onOpenSettings = { nav.navigate("settings") },
                onOpenTrash = { nav.navigate("trash") }
            )
        }
        composable("trash") {
            TrashScreen(onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
