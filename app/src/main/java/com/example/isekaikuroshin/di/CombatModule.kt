package com.example.isekaikuroshin.di

import com.example.isekaikuroshin.combat.CombatController
import com.example.isekaikuroshin.combat.CombatStateMachine
import com.example.isekaikuroshin.engine.SpellCombatHelper
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.game.GameStateManager as GameStateManagerZ7
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * G93: Combat System Dependency Injection Module
 *
 * Provides:
 * - CombatStateMachine (singleton)
 * - CombatController (singleton)
 */
@Module
@InstallIn(SingletonComponent::class)
object CombatModule {

    @Provides
    @Singleton
    fun provideCombatStateMachine(): CombatStateMachine {
        return CombatStateMachine()
    }

    @Provides
    @Singleton
    fun provideCombatController(
        stateMachine: CombatStateMachine,
        gameStateManager: GameStateManager,
        gameStateManagerZ7: GameStateManagerZ7 // TODO-G96: For immediate save on critical events
    ): CombatController {
        return CombatController(
            stateMachine = stateMachine,
            gameStateManager = gameStateManager,
            spellCombatHelper = SpellCombatHelper, // object, directly accessible
            gameStateManagerZ7 = gameStateManagerZ7
        )
    }
}
