package com.example.isekaikuroshin.ui.spellstudio

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.data.spell.SpellAction
import com.example.isekaikuroshin.data.spell.SpellStep
import com.example.isekaikuroshin.data.spell.MeteorCount
import com.example.isekaikuroshin.data.spell.FallSpeed
import com.example.isekaikuroshin.data.spell.EffectArea
import com.example.isekaikuroshin.data.spell.SnowDensity
import com.example.isekaikuroshin.data.spell.FlakeSize
import com.example.isekaikuroshin.data.spell.WindDirection
import com.example.isekaikuroshin.data.spell.ExplosionPower
import com.example.isekaikuroshin.data.spell.EffectDuration
import com.example.isekaikuroshin.data.spell.LaunchHeight
import com.example.isekaikuroshin.data.spell.BurstParticleCount
import com.example.isekaikuroshin.data.spell.AuraSize
import com.example.isekaikuroshin.data.spell.FadeSpeed

/**
 * Eylem Ayarları Ekranı
 *
 * ⚡⚡⚡ YENİ TASARIM:
 * - İki aşamalı seçim (kart seçimi → özelleştirme)
 * - VFXWorkshop stili kart sistemi
 * - Animasyonlu geçişler
 * - Seçim sayacı göstergesi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfigurationScreen(
    step: SpellStep,
    viewModel: SpellStudioViewModel
) {
    // ⚡⚡⚡ YENİ: Seçili efekt state'i
    var selectedEffect by remember { mutableStateOf<EffectType?>(null) }

    // ⚡⚡⚡ KRİTİK: Anlık action state'i (kullanıcının yeni seçimi)
    var currentAction by remember { mutableStateOf(step.action) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedEffect == null) "Eylem Seçin"
                        else "Eylem Ayarları"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedEffect != null) {
                                selectedEffect = null // Kart seçimine geri dön
                            } else {
                                viewModel.returnToRecipeEditor()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00BFFF)
                )
            )
        }
    ) { padding ->
        // ⚡⚡⚡ YENİ: Animasyonlu geçiş
        AnimatedContent(
            targetState = selectedEffect,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "effect_transition"
        ) { effect ->
            if (effect == null) {
                // ✅ ADIM 1: Efekt Kartları Göster
                EffectSelectionCards(
                    modifier = Modifier.padding(padding),
                    onEffectSelected = { effectType ->
                        // ⚡⚡⚡ KRİTİK: Seçilen efekt tipine göre yeni action oluştur
                        currentAction = createActionFromEffect(effectType)
                        selectedEffect = effectType
                        android.util.Log.e("ConfigScreen", "🎨 Effect selected: ${effectType.name}, Created action: ${currentAction::class.simpleName}")
                    }
                )
            } else {
                // ✅ ADIM 2: Seçilen Efektin Ayarları
                EffectConfigurationPanel(
                    modifier = Modifier.padding(padding),
                    effect = effect,
                    currentAction = currentAction,
                    onActionChange = { currentAction = it },
                    onSave = {
                        // ⚡⚡⚡ KRİTİK: Yeni action'ı kaydet
                        android.util.Log.e("ConfigScreen", "💾💾💾 SAVE button clicked!")
                        android.util.Log.e("ConfigScreen", "   Saving NEW action: ${currentAction::class.simpleName}")

                        viewModel.updateStep(
                            stepId = step.id,
                            newTrigger = null,  // Trigger değişmedi
                            newAction = currentAction  // YENİ action'ı kaydet
                        )

                        android.util.Log.e("ConfigScreen", "✅ updateStep() called with NEW action, returning to recipe editor")
                        viewModel.returnToRecipeEditor()
                    },
                    onBackToSelection = { selectedEffect = null },
                    viewModel = viewModel
                )
            }
        }
    }
}

// ========================================
// ⚡⚡⚡ YENİ: EFEKT TİPİ ENUM
// ========================================

enum class EffectType(
    val emoji: String,
    val displayName: String,
    val color: Color,
    val description: String
) {
    // Temel Efektler
    EMIT_PARTICLES("💧", "Parçacık Püskürtme", Color(0xFF00BFFF), "Parçacıkları havaya fışkırtır"),
    ATTRACT_PARTICLES("🧲", "Parçacık Birleştirme", Color(0xFF9D4EDD), "Parçacıkları çeker ve birleştirir"),
    REPEL_PARTICLES("💨", "Parçacık Dağıtma", Color(0xFFFFB800), "Parçacıkları dağıtır"),

    // Gelişmiş Efektler
    METEOR("☄️", "Meteor Yağmuru", Color(0xFFFF5252), "Gökten ateş topları yağdırır"),
    SNOW("❄️", "Kar Fırtınası", Color(0xFF90CAF9), "Kar taneleri oluşturur"),
    EXPLOSION("💥", "Patlama", Color(0xFFFF6D00), "Güçlü patlama efekti"),
    FIREWORK("🎆", "Havai Fişek", Color(0xFFFFD600), "Gökyüzünde havai fişek patlatır"),
    AURA("✨", "Solan Aura", Color(0xFFB388FF), "Etrafta parıldayan aura oluşturur")
}

// ========================================
// ⚡⚡⚡ YENİ: EFEKT KART SEÇİMİ
// ========================================

@Composable
private fun EffectSelectionCards(
    modifier: Modifier = Modifier,
    onEffectSelected: (EffectType) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Başlık ve açıklama
        Text(
            text = rememberLocalizedText("select_effect"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = rememberLocalizedText("spell_action_description"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ⚡⚡⚡ YENİ: Efekt Sayacı Göstergesi
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Toplam ${EffectType.entries.size} Efekt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rememberLocalizedText("basic_advanced_count"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF00BFFF)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ⚡⚡⚡ KART GRİDİ
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(EffectType.entries) { effect ->
                EffectCard(
                    effect = effect,
                    onClick = { onEffectSelected(effect) }
                )
            }
        }
    }
}

/**
 * ⚡⚡⚡ YENİ: Efekt Kartı (VFXWorkshop stilinde)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffectCard(
    effect: EffectType,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = effect.color.copy(alpha = 0.15f)
        ),
        border = BorderStroke(2.dp, effect.color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emoji
            Text(
                text = effect.emoji,
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // İsim
            Text(
                text = effect.displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Kategori Badge
            Surface(
                shape = CircleShape,
                color = effect.color.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (effect.ordinal < 3) "Temel" else "Gelişmiş",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = effect.color
                )
            }
        }
    }
}

// ========================================
// ⚡⚡⚡ YENİ: EFEKT AYAR PANELİ
// ========================================

/**
 * ⚡⚡⚡ YENİ: Efekt tipine göre default action oluştur
 */
private fun createActionFromEffect(effectType: EffectType): SpellAction {
    return when (effectType) {
        EffectType.EMIT_PARTICLES -> SpellAction.EmitParticles(particleCount = 50)
        EffectType.ATTRACT_PARTICLES -> SpellAction.AttractParticles(strength = 1.0f)
        EffectType.REPEL_PARTICLES -> SpellAction.RepelParticles(strength = 1.0f)
        EffectType.METEOR -> SpellAction.MeteorRain(
            meteorCount = MeteorCount.MEDIUM,
            fallSpeed = FallSpeed.NORMAL,
            effectArea = EffectArea.WIDE
        )
        EffectType.SNOW -> SpellAction.Snowstorm(
            snowDensity = SnowDensity.NORMAL,
            flakeSize = FlakeSize.NORMAL,
            windDirection = WindDirection.NONE
        )
        EffectType.EXPLOSION -> SpellAction.Explosion(
            explosionPower = ExplosionPower.MEDIUM,
            effectDuration = EffectDuration.SHORT,
            shockwaveEnabled = true
        )
        EffectType.FIREWORK -> SpellAction.Firework(
            launchHeight = LaunchHeight.MEDIUM,
            burstParticleCount = BurstParticleCount.MEDIUM,
            trailEnabled = true,
            cascadeCount = 1
        )
        EffectType.AURA -> SpellAction.FadingAura(
            auraSize = AuraSize.MEDIUM,
            fadeSpeed = FadeSpeed.NORMAL,
            pulseEnabled = false
        )
    }
}

@Composable
private fun EffectConfigurationPanel(
    modifier: Modifier = Modifier,
    effect: EffectType,
    currentAction: SpellAction,
    onActionChange: (SpellAction) -> Unit,
    onSave: () -> Unit,
    onBackToSelection: () -> Unit,
    viewModel: SpellStudioViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Başlık
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = effect.emoji,
                fontSize = 32.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = effect.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = effect.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ⚡⚡⚡ YENİ: VFX PREVIEW PANEL
        VFXPreviewPanel(
            effect = effect,
            currentAction = currentAction,
            particleManager = viewModel.particleSystemManager
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ⚡⚡⚡ Ayar içeriği
        when (effect) {
            EffectType.EMIT_PARTICLES -> {
                when (currentAction) {
                    is SpellAction.EmitParticles -> {
                        EmitParticlesConfig(
                            action = currentAction,
                            onSave = { onActionChange(it) }
                        )
                    }
                    else -> PlaceholderConfig("Parçacık Püskürtme")
                }
            }

            EffectType.ATTRACT_PARTICLES -> PlaceholderConfig("Parçacık Birleştirme")
            EffectType.REPEL_PARTICLES -> PlaceholderConfig("Parçacık Dağıtma")
            EffectType.METEOR -> PlaceholderConfig("Meteor Yağmuru")
            EffectType.SNOW -> PlaceholderConfig("Kar Fırtınası")
            EffectType.EXPLOSION -> PlaceholderConfig("Patlama")
            EffectType.FIREWORK -> PlaceholderConfig("Havai Fişek")
            EffectType.AURA -> PlaceholderConfig("Solan Aura")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ⚡⚡⚡ YENİ: Başka Efekt Seç Butonu
        OutlinedButton(
            onClick = onBackToSelection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.GridView, "Efektler")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Başka Efekt Seç")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Geri dön butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    android.util.Log.e("ConfigScreen", "❌ Cancel button clicked")
                    viewModel.returnToRecipeEditor()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(rememberLocalizedText("cancel_button"))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text(rememberLocalizedText("save_button"))
            }
        }
    }
}

/**
 * ⚡⚡⚡ YENİ: Placeholder Config (gelecekte implement edilecek efektler için)
 */
@Composable
private fun PlaceholderConfig(effectName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Construction,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF00BFFF).copy(alpha = 0.6f)
        )
        Text(
            text = rememberLocalizedText("effect_settings").replace("{effectName}", effectName),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = rememberLocalizedText("not_configured_yet"),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ========================================
// TEMEL EFEKT AYAR PANELLERİ
// ========================================

@Composable
private fun EmitParticlesConfig(
    action: SpellAction.EmitParticles,
    onSave: (SpellAction.EmitParticles) -> Unit
) {
    var particleCount by remember { mutableStateOf(action.particleCount.toString()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = particleCount,
            onValueChange = { particleCount = it },
            label = { Text("Parçacık Sayısı") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = rememberLocalizedText("future_config_params"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun AttractParticlesConfig(
    action: SpellAction.AttractParticles,
    onSave: (SpellAction.AttractParticles) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Çekim gücü ve yarıçap ayarları gelecek...")
    }
}

@Composable
private fun RepelParticlesConfig(
    action: SpellAction.RepelParticles,
    onSave: (SpellAction.RepelParticles) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("İtme gücü ve yarıçap ayarları gelecek...")
    }
}

// ========================================
// GELİŞMİŞ EFEKT AYAR PANELLERİ
// ========================================

/**
 * ☄️ Meteor Yağmuru Ayarları
 */
@Composable
private fun MeteorRainConfig(
    action: SpellAction.MeteorRain,
    onSave: (SpellAction.MeteorRain) -> Unit
) {
    var meteorCount by remember { mutableStateOf(action.meteorCount) }
    var fallSpeed by remember { mutableStateOf(action.fallSpeed) }
    var effectArea by remember { mutableStateOf(action.effectArea) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Meteor Sayısı
        Text(
            text = rememberLocalizedText("meteor_count"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MeteorCount.entries.forEach { count ->
                FilterChip(
                    selected = meteorCount == count,
                    onClick = { meteorCount = count },
                    label = { Text(count.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Düşme Hızı
        Text(
            text = rememberLocalizedText("fall_speed"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FallSpeed.entries.forEach { speed ->
                FilterChip(
                    selected = fallSpeed == speed,
                    onClick = { fallSpeed = speed },
                    label = { Text(speed.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Etki Alanı
        Text(
            text = rememberLocalizedText("impact_area"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EffectArea.entries.forEach { area ->
                FilterChip(
                    selected = effectArea == area,
                    onClick = { effectArea = area },
                    label = { Text(area.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = rememberLocalizedText("fire_color_future"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

/**
 * ❄️ Kar Fırtınası Ayarları
 */
@Composable
private fun SnowstormConfig(
    action: SpellAction.Snowstorm,
    onSave: (SpellAction.Snowstorm) -> Unit
) {
    var snowDensity by remember { mutableStateOf(action.snowDensity) }
    var flakeSize by remember { mutableStateOf(action.flakeSize) }
    var windDirection by remember { mutableStateOf(action.windDirection) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kar Yoğunluğu
        Text(
            text = rememberLocalizedText("snow_density"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SnowDensity.entries.forEach { density ->
                FilterChip(
                    selected = snowDensity == density,
                    onClick = { snowDensity = density },
                    label = { Text(density.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Tane Büyüklüğü
        Text(
            text = rememberLocalizedText("particle_size"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlakeSize.entries.forEach { size ->
                FilterChip(
                    selected = flakeSize == size,
                    onClick = { flakeSize = size },
                    label = { Text(size.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Rüzgar Yönü
        Text(
            text = rememberLocalizedText("wind_direction"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WindDirection.entries.forEach { direction ->
                FilterChip(
                    selected = windDirection == direction,
                    onClick = { windDirection = direction },
                    label = { Text(direction.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 💥 Patlama Ayarları
 */
@Composable
private fun ExplosionConfig(
    action: SpellAction.Explosion,
    onSave: (SpellAction.Explosion) -> Unit
) {
    var explosionPower by remember { mutableStateOf(action.explosionPower) }
    var effectDuration by remember { mutableStateOf(action.effectDuration) }
    var shockwaveEnabled by remember { mutableStateOf(action.shockwaveEnabled) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Patlama Gücü
        Text(
            text = rememberLocalizedText("explosion_power"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExplosionPower.entries.forEach { power ->
                FilterChip(
                    selected = explosionPower == power,
                    onClick = { explosionPower = power },
                    label = { Text(power.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Efekt Süresi
        Text(
            text = rememberLocalizedText("effect_duration"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EffectDuration.entries.forEach { duration ->
                FilterChip(
                    selected = effectDuration == duration,
                    onClick = { effectDuration = duration },
                    label = { Text(duration.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Şok Dalgası
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = rememberLocalizedText("shockwave"),
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = shockwaveEnabled,
                onCheckedChange = { shockwaveEnabled = it }
            )
        }

        Text(
            text = rememberLocalizedText("particle_color_future"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

/**
 * 🎆 Havai Fişek Ayarları
 */
@Composable
private fun FireworkConfig(
    action: SpellAction.Firework,
    onSave: (SpellAction.Firework) -> Unit
) {
    var launchHeight by remember { mutableStateOf(action.launchHeight) }
    var burstParticleCount by remember { mutableStateOf(action.burstParticleCount) }
    var trailEnabled by remember { mutableStateOf(action.trailEnabled) }
    var cascadeCount by remember { mutableStateOf(action.cascadeCount) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fırlatma Yüksekliği
        Text(
            text = rememberLocalizedText("launch_height"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LaunchHeight.entries.forEach { height ->
                FilterChip(
                    selected = launchHeight == height,
                    onClick = { launchHeight = height },
                    label = { Text(height.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Patlama Parçacık Sayısı
        Text(
            text = rememberLocalizedText("explosion_particle_count"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BurstParticleCount.entries.forEach { count ->
                FilterChip(
                    selected = burstParticleCount == count,
                    onClick = { burstParticleCount = count },
                    label = { Text(count.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // İz Efekti
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = rememberLocalizedText("trail_effect"),
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = trailEnabled,
                onCheckedChange = { trailEnabled = it }
            )
        }

        // Art Arda Patlama Sayısı
        Text(
            text = "Art Arda Patlama: $cascadeCount",
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = cascadeCount.toFloat(),
            onValueChange = { cascadeCount = it.toInt() },
            valueRange = 1f..5f,
            steps = 3
        )

        Text(
            text = rememberLocalizedText("explosion_color_future"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

/**
 * ✨ Solan Aura Ayarları
 */
@Composable
private fun FadingAuraConfig(
    action: SpellAction.FadingAura,
    onSave: (SpellAction.FadingAura) -> Unit
) {
    var auraSize by remember { mutableStateOf(action.auraSize) }
    var fadeSpeed by remember { mutableStateOf(action.fadeSpeed) }
    var pulseEnabled by remember { mutableStateOf(action.pulseEnabled) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Aura Büyüklüğü
        Text(
            text = rememberLocalizedText("aura_size"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuraSize.entries.forEach { size ->
                FilterChip(
                    selected = auraSize == size,
                    onClick = { auraSize = size },
                    label = { Text(size.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Solma Hızı
        Text(
            text = rememberLocalizedText("fade_speed"),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FadeSpeed.entries.forEach { speed ->
                FilterChip(
                    selected = fadeSpeed == speed,
                    onClick = { fadeSpeed = speed },
                    label = { Text(speed.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Nabız Efekti
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = rememberLocalizedText("pulse_effect"),
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = pulseEnabled,
                onCheckedChange = { pulseEnabled = it }
            )
        }

        Text(
            text = rememberLocalizedText("aura_color_future"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

// ========================================
// ⚡⚡⚡ YENİ: VFX ÖNİZLEME VE KONUMLANDIRMA PANELİ
// ========================================

/**
 * VFX Önizleme Paneli - GÜÇLENDİRİLMİŞ SÜRÜM
 *
 * Unity/Unreal Engine tarzı profesyonel VFX önizleme paneli
 * ✅ Playback kontrolleri (Play/Pause/Reset)
 * ✅ Playback speed slider
 * ✅ Particle count göstergesi
 * ✅ Intensity ve property sliders
 * ✅ Color picker
 * ✅ Performance optimizasyonu (remember, derivedStateOf)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VFXPreviewPanel(
    effect: EffectType,
    currentAction: SpellAction,
    particleManager: com.example.isekaikuroshin.engine.vfx.ParticleSystemManager
) {
    // ⚡ Efekt pozisyon offset'i (elin merkezine göre sapma)
    var effectOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // ⚡⚡⚡ YENİ: Playback kontrolü
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }

    // ⚡⚡⚡ YENİ: VFX Properties
    var intensity by remember { mutableStateOf(1f) }
    var particleSize by remember { mutableStateOf(10f) }
    var emissionRate by remember { mutableStateOf(50f) }
    var selectedColor by remember { mutableStateOf(effect.color) }

    // ⚡⚡⚡ G89: Basit parçacık sistemi - sadece emitter sayısını göster
    val activeParticleCount by remember {
        derivedStateOf {
            particleManager.activeEmitters.value.size * 5 // Her emitter ~5 parçacık üretir
        }
    }
    val maxParticles = 500

    // ⚡ Önizleme tetikleyici
    var triggerPreview by remember { mutableStateOf(0) }

    // ⚡⚡⚡ YENİ: İki Column Layout (Preview + Controls)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ========== PREVIEW VIEWPORT ==========
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E2E)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Arka plan grid deseni
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val gridSize = 40f
                    // Dikey çizgiler
                    for (i in 0..size.width.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = androidx.compose.ui.geometry.Offset(i.toFloat(), 0f),
                            end = androidx.compose.ui.geometry.Offset(i.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                    // Yatay çizgiler
                    for (i in 0..size.height.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = androidx.compose.ui.geometry.Offset(0f, i.toFloat()),
                            end = androidx.compose.ui.geometry.Offset(size.width, i.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                }

                // Merkez el ikonu (referans noktası)
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "El Referansı",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.4f)
                    )
                }

                // Efekt pozisyon göstergesi
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = (effectOffset.x / 3).dp,
                            y = (effectOffset.y / 3).dp
                        )
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = selectedColor.copy(alpha = 0.7f),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.9f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = effect.emoji,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Bilgi metni + Particle Count
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = rememberLocalizedText("vfx_preview"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = if (isPlaying) "▶️ Oynatılıyor" else "⏸️ Duraklatıldı",
                            fontSize = 9.sp,
                            color = if (isPlaying) Color(0xFF00FF88) else Color(0xFFFF6B6B)
                        )
                    }

                    // Particle Count Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🎆",
                                fontSize = 10.sp
                            )
                            Text(
                                text = "$activeParticleCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600)
                            )
                            Text(
                                text = "/ $maxParticles",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // ========== PLAYBACK CONTROLS ==========
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2332)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Playback Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play/Pause
                    OutlinedButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isPlaying) Color(0xFF00FF88) else Color.White
                        )
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying) "Duraklat" else "Oynat")
                    }

                    // Reset
                    OutlinedButton(
                        onClick = {
                            effectOffset = androidx.compose.ui.geometry.Offset.Zero
                            isPlaying = false
                            particleManager.clearAll()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sıfırla")
                    }

                    // Test
                    Button(
                        onClick = { triggerPreview++ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedColor
                        )
                    ) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(rememberLocalizedText("test_button"))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Playback Speed
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rememberLocalizedText("speed"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD600)
                        )
                    }
                    Slider(
                        value = playbackSpeed,
                        onValueChange = { playbackSpeed = it },
                        valueRange = 0.1f..3f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ========== PROPERTY CONTROLS ==========
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2332)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = rememberLocalizedText("effect_properties"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Intensity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rememberLocalizedText("intensity"),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${(intensity * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Slider(
                        value = intensity,
                        onValueChange = { intensity = it },
                        valueRange = 0f..2f
                    )
                }

                // Particle Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rememberLocalizedText("particle_size"),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${particleSize.toInt()}px",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Slider(
                        value = particleSize,
                        onValueChange = { particleSize = it },
                        valueRange = 2f..50f
                    )
                }

                // Emission Rate
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rememberLocalizedText("emission_rate"),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${emissionRate.toInt()} p/s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Slider(
                        value = emissionRate,
                        onValueChange = { emissionRate = it },
                        valueRange = 10f..200f
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Color Picker (Basit versiyon)
                Column {
                    Text(
                        text = "🎨 Renk",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Color(0xFFFF5252) to "🔴",
                            Color(0xFF00BFFF) to "🔵",
                            Color(0xFF00FF88) to "🟢",
                            Color(0xFFFFD600) to "🟡",
                            Color(0xFFB388FF) to "🟣",
                            Color(0xFFFFFFFF) to "⚪"
                        ).forEach { (color, emoji) ->
                            Surface(
                                onClick = { selectedColor = color },
                                modifier = Modifier
                                    .size(40.dp)
                                    .weight(1f),
                                shape = CircleShape,
                                color = color,
                                border = if (selectedColor == color) BorderStroke(3.dp, Color.White) else null
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColor == color) {
                                        Text(text = emoji, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Performance Warning
                if (activeParticleCount > 300) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF6B6B).copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚠️", fontSize = 16.sp)
                            Text(
                                text = rememberLocalizedText("high_particle_warning"),
                                fontSize = 10.sp,
                                color = Color(0xFFFF6B6B)
                            )
                        }
                    }
                }
            }
        }
    }
}
