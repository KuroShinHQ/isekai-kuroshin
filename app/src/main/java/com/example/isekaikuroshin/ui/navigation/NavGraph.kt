
package com.example.isekaikuroshin.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.isekaikuroshin.ui.character.CharacterCatalogScreen
import com.example.isekaikuroshin.ui.character.CharacterStatusScreen
import com.example.isekaikuroshin.ui.inventory.InventoryScreen
import com.example.isekaikuroshin.ui.screens.CampScreen
import com.example.isekaikuroshin.ui.screens.CraftingScreen
import com.example.isekaikuroshin.ui.map.MapScreen
import com.example.isekaikuroshin.ui.quest.QuestScreen
import com.example.isekaikuroshin.ui.journal.JournalScreen
import com.example.isekaikuroshin.ui.training.TrainingScreen
import com.example.isekaikuroshin.ui.screens.training.ManaCoreScreen
import com.example.isekaikuroshin.ui.screens.training.QiCultivationScreen
import com.example.isekaikuroshin.ui.settings.SettingsScreen
import com.example.isekaikuroshin.ui.skilltree.SkillTreeScreen
import com.example.isekaikuroshin.ui.healthhub.HealthHubScreen
import com.example.isekaikuroshin.ui.exercise.ExerciseScreen
import com.example.isekaikuroshin.ui.exercise.ExerciseResultScreen
import com.example.isekaikuroshin.ui.sealpractice.SealPracticeScreen
import com.example.isekaikuroshin.ui.calibration.CalibrationCenterScreen
import com.example.isekaikuroshin.ui.calibration.CalibrationCameraScreen
import com.example.isekaikuroshin.ui.physicaltraining.PhysicalTrainingScreen
import com.example.isekaikuroshin.ui.vfx.VFXWorkshopScreen
import com.example.isekaikuroshin.ui.vfx.camera.VFXTestCameraScreen
import com.example.isekaikuroshin.ui.vfx.camera.VFXGestureCalibrationCameraScreen
import com.example.isekaikuroshin.ui.archive.DeathArchiveDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Status.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
    ) {
        composable(BottomNavItem.Status.route) {
            CharacterStatusScreen()
        }
        composable(BottomNavItem.Inventory.route) {
            InventoryScreen(onBackPressed = { navController.popBackStack() })
        }
        composable(BottomNavItem.Quests.route) {
            QuestScreen(navController = navController)
        }
        composable(BottomNavItem.Catalog.route) {
            CharacterCatalogScreen(navController = navController)
        }
        composable(BottomNavItem.Camp.route) {
            CampScreen(navController = navController)
        }
        composable(BottomNavItem.Map.route) {
            MapScreen(onBackPressed = { navController.popBackStack() })
        }
        composable(BottomNavItem.Journal.route) {
            JournalScreen()
        }
        composable(BottomNavItem.HealthHub.route) {
            HealthHubScreen(navController = navController)
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen(navController = navController)
        }

        // Training routes
        composable("training") {
            TrainingScreen(navController = navController)
        }
        composable("mana_core") {
            ManaCoreScreen(navController = navController)
        }
        composable("qi_cultivation") {
            QiCultivationScreen(navController = navController)
        }

        composable("stat_allocation") {
            com.example.isekaikuroshin.ui.statallocation.StatAllocationScreen(navController = navController)
        }

        // Crafting route
        composable("crafting") {
            CraftingScreen(navController = navController)
        }

        // Skill Tree route
        composable("skill_tree") {
            SkillTreeScreen(navController = navController)
        }

        // Umbros Transition route
        composable("umbros_transition") {
            com.example.isekaikuroshin.ui.dashboard.UmbrosTransitionScreen(
                navController = navController,
                onTransitionComplete = { navController.popBackStack() }
            )
        }

        // Death Statistics Screen route (Ölümü reddettikten sonra AI raporu)
        composable("death_statistics") {
            // Default parametrelerle çağrılıyor - background medya fallback kullanır
            com.example.isekaikuroshin.ui.screens.DeathStatisticsScreen(
                onClose = { /* Uygulama kapanacak - Process.killProcess */ }
            )
        }

        // Exercise Screen route (Pose Detection - ESKİ)
        composable("exercise") {
            ExerciseScreen(navController = navController)
        }

        // Physical Training Screen route (YENİ - Gelişmiş Fiziksel Antrenman)
        composable("physical_training") {
            PhysicalTrainingScreen(navController = navController)
        }

        // Seal Practice Screen route (Hand Detection - Kadim Mühür Teknikleri)
        composable("seal_practice") {
            SealPracticeScreen(navController = navController)
        }

        // Calibration Center Screen route (Kalibrasyon Merkezi)
        composable("calibration_center") {
            CalibrationCenterScreen(navController = navController)
        }

        // Calibration Camera Screen route (Belirli bir mührü kalibre etmek için)
        composable(
            route = "calibration_camera/{sealId}",
            arguments = listOf(navArgument("sealId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sealId = backStackEntry.arguments?.getString("sealId")
            if (sealId != null) {
                CalibrationCameraScreen(navController = navController, sealId = sealId)
            }
        }

        // VFX Workshop Screen route (VFX Test Atölyesi)
        // ⚡⚡⚡ BÖLÜM A: Shared ViewModel - Parent scope kullan
        composable("vfx_workshop") { backStackEntry ->
            // VFX Workshop için parent scope (vfx_workshop ekranı kendisi parent)
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("vfx_workshop")
            }
            VFXWorkshopScreen(
                navController = navController,
                viewModel = hiltViewModel(parentEntry)
            )
        }

        // VFX Camera Screen route (VFX Test Kamera Ekranı)
        composable("vfx_camera") { backStackEntry ->
            // Parent scope'u kullan (vfx_workshop)
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("vfx_workshop")
            }
            VFXTestCameraScreen(
                navController = navController,
                viewModel = hiltViewModel(parentEntry)
            )
        }

        // VFX Gesture Calibration Camera Screen (Gelişmiş Kalibrasyon)
        // ⚡⚡⚡ BÖLÜM A: Shared ViewModel - Parent scope kullan
        composable(
            route = "vfx_gesture_calibration/{gestureType}",
            arguments = listOf(navArgument("gestureType") { type = NavType.StringType })
        ) { backStackEntry ->
            val gestureTypeStr = backStackEntry.arguments?.getString("gestureType")
            if (gestureTypeStr != null) {
                // Parent scope'u kullan (vfx_workshop)
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("vfx_workshop")
                }
                VFXGestureCalibrationCameraScreen(
                    navController = navController,
                    gestureTypeStr = gestureTypeStr,
                    viewModel = hiltViewModel(parentEntry)
                )
            }
        }

        // ⚡⚡⚡ YENİ: VFX Offset Calibration Screen (3D Konumlandırma Editörü)
        // İnteraktif sürükle-bırak ile VFX offset ayarlama
        composable(
            route = "vfx_offset_calibration/{gestureType}",
            arguments = listOf(navArgument("gestureType") { type = NavType.StringType })
        ) { backStackEntry ->
            val gestureTypeStr = backStackEntry.arguments?.getString("gestureType")
            if (gestureTypeStr != null) {
                val gestureType = try {
                    com.example.isekaikuroshin.data.GestureType.valueOf(gestureTypeStr)
                } catch (e: IllegalArgumentException) {
                    com.example.isekaikuroshin.data.GestureType.CREATION
                }

                // Parent scope'u kullan (vfx_workshop)
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("vfx_workshop")
                }

                com.example.isekaikuroshin.ui.vfx.VFXOffsetCalibrationScreen(
                    navController = navController,
                    viewModel = hiltViewModel(parentEntry),
                    gestureType = gestureType
                )
            }
        }

        // Exercise Result Screen route
        composable(
            "exercise_result?exerciseType={exerciseType}&repCount={repCount}&totalXP={totalXP}&stats={stats}&calories={calories}&gold={gold}"
        ) { backStackEntry ->
            val exerciseType = backStackEntry.arguments?.getString("exerciseType") ?: "PUSH_UP"
            val repCount = backStackEntry.arguments?.getString("repCount")?.toIntOrNull() ?: 0
            val totalXP = backStackEntry.arguments?.getString("totalXP")?.toIntOrNull() ?: 0
            val statsString = backStackEntry.arguments?.getString("stats") ?: ""
            val calories = backStackEntry.arguments?.getString("calories")?.toFloatOrNull() ?: 0f
            val gold = backStackEntry.arguments?.getString("gold")?.toIntOrNull() ?: 0

            // Stats string'ini Map'e dönüştür
            val statGains = if (statsString.isNotEmpty()) {
                statsString.split(",").associate {
                    val parts = it.split(":")
                    parts[0] to (parts.getOrNull(1)?.toFloatOrNull() ?: 0f)
                }
            } else {
                emptyMap()
            }

            ExerciseResultScreen(
                navController = navController,
                exerciseType = exerciseType,
                repCount = repCount,
                totalXP = totalXP,
                statGains = statGains,
                calories = calories,
                gold = gold
            )
        }

        // Death Archive Detail Screen route
        composable(
            route = "death_archive_detail/{deathId}",
            arguments = listOf(navArgument("deathId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deathId = backStackEntry.arguments?.getString("deathId") ?: ""
            DeathArchiveDetailScreen(
                navController = navController,
                deathId = deathId
            )
        }
    }
}
