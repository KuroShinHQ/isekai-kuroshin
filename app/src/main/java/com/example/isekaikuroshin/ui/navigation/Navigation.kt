
package com.example.isekaikuroshin.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val titleKey: String) {
    object Status : BottomNavItem("status", Icons.Default.Person, "nav_character")
    object Inventory : BottomNavItem("inventory", Icons.Default.Inventory, "nav_inventory")
    object Quests : BottomNavItem("quests", Icons.AutoMirrored.Filled.Assignment, "nav_quests")
    object Catalog : BottomNavItem("catalog", Icons.AutoMirrored.Filled.MenuBook, "nav_catalog")
    object Camp : BottomNavItem("camp", Icons.Default.Deck, "nav_camp")
    object Map : BottomNavItem("map", Icons.Default.Map, "nav_map")
    object Journal : BottomNavItem("journal", Icons.Default.Book, "nav_journal")
    object HealthHub : BottomNavItem("health_hub", Icons.Default.HealthAndSafety, "nav_health_hub")
    object DroneController : BottomNavItem("drone_controller", Icons.Default.FlightTakeoff, "nav_drone_controller")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "nav_settings")
}
