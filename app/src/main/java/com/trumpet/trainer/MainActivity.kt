package com.trumpet.trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trumpet.trainer.game.GameMode
import com.trumpet.trainer.game.MicViewModel
import com.trumpet.trainer.game.RhythmViewModel
import com.trumpet.trainer.game.TrainerViewModel
import com.trumpet.trainer.ui.localization.getStrings
import com.trumpet.trainer.ui.screens.HomeScreen
import com.trumpet.trainer.ui.screens.MicScreen
import com.trumpet.trainer.ui.screens.RhythmScreen
import com.trumpet.trainer.ui.screens.StatsScreen
import com.trumpet.trainer.ui.screens.TrainerScreen
import com.trumpet.trainer.ui.theme.TrumpetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TrumpetTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = (context.applicationContext as TrumpetApp).container

    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                container = container,
                onStartSight = { navController.navigate("sight") },
                onStartRhythm = { navController.navigate("rhythm") },
                onStartEar = { navController.navigate("ear") },
                onStartMic = { navController.navigate("mic") },
                onOpenStats = { navController.navigate("stats") },
            )
        }
        composable("sight") {
            val vm: TrainerViewModel =
                viewModel { TrainerViewModel(GameMode.SIGHT_READING, container) }
            val strings = getStrings(container.settings.language)
            TrainerScreen(
                vm = vm,
                settings = container.settings,
                title = strings.modeSightTitle,
                hideNoteUntilAnswer = false,
                onBack = { navController.popBackStack() },
            )
        }
        composable("ear") {
            val vm: TrainerViewModel =
                viewModel { TrainerViewModel(GameMode.EAR_TRAINING, container) }
            val strings = getStrings(container.settings.language)
            TrainerScreen(
                vm = vm,
                settings = container.settings,
                title = strings.modeEarTitle,
                hideNoteUntilAnswer = true,
                onBack = { navController.popBackStack() },
            )
        }
        composable("rhythm") {
            val vm: RhythmViewModel = viewModel { RhythmViewModel(container) }
            RhythmScreen(
                vm = vm,
                language = container.settings.language,
                onBack = { navController.popBackStack() },
            )
        }
        composable("mic") {
            val vm: MicViewModel = viewModel { MicViewModel(container) }
            MicScreen(
                vm = vm,
                settings = container.settings,
                onBack = { navController.popBackStack() },
            )
        }
        composable("stats") {
            StatsScreen(
                stats = container.stats,
                language = container.settings.language,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
