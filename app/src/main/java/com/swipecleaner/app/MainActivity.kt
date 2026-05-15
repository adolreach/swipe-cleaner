package com.swipecleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swipecleaner.app.data.UserPreferences
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
                .collectAsState(initial = UserPreferences())

            SwipeCleanerTheme(themeMode = prefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionsGate {
                        val nav = rememberNavController()
                        NavHost(
                            navController = nav,
                            startDestination = "swipe",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            }
                        ) {
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
                }
            }
        }
    }
}
