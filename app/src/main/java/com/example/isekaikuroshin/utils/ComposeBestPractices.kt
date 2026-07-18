package com.example.isekaikuroshin.utils

/**
 * G60d: Jetpack Compose Performance Best Practices
 * Target: 60 FPS stable on Samsung A34s (Exynos 1280, 6GB RAM)
 *
 * This file documents critical performance patterns for Compose.
 * Apply these practices across all @Composable functions.
 */

// ═══════════════════════════════════════════════════════════════════════════
// 1️⃣ STATE HOISTING & REMEMBER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: State hoisting prevents unnecessary recompositions
 */
/*
@Composable
fun GoodExample(
    value: String,                    // ✅ Hoisted state
    onValueChange: (String) -> Unit   // ✅ Event callback
) {
    TextField(
        value = value,
        onValueChange = onValueChange
    )
}
*/

/**
 * ❌ BAD: Internal state causes recomposition of entire parent
 */
/*
@Composable
fun BadExample() {
    var value by remember { mutableStateOf("") }  // ❌ State not hoisted
    TextField(
        value = value,
        onValueChange = { value = it }
    )
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 2️⃣ DERIVEDSTATEOF - Avoid Expensive Recalculations
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: derivedStateOf only recalculates when dependencies change
 */
/*
@Composable
fun GoodDerivedState(items: List<Item>) {
    // ✅ Only recalculates when items actually changes
    val sortedItems by remember(items) {
        derivedStateOf { items.sortedBy { it.priority } }
    }

    LazyColumn {
        items(sortedItems) { item ->
            ItemRow(item)
        }
    }
}
*/

/**
 * ❌ BAD: Expensive calculation runs on every recomposition
 */
/*
@Composable
fun BadDerivedState(items: List<Item>) {
    // ❌ Sorts on EVERY recomposition (even if items didn't change)
    val sortedItems = items.sortedBy { it.priority }

    LazyColumn {
        items(sortedItems) { item ->
            ItemRow(item)
        }
    }
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 3️⃣ IMMUTABLE / STABLE CLASSES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: @Immutable tells Compose to skip recomposition if reference is same
 */
/*
@Immutable
data class UserProfile(
    val name: String,
    val level: Int,
    val avatar: String
)

@Composable
fun ProfileCard(profile: UserProfile) {  // ✅ Skips recomposition if profile unchanged
    // ...
}
*/

/**
 * ❌ BAD: Mutable classes cause unnecessary recompositions
 */
/*
data class MutableProfile(
    var name: String,      // ❌ var = mutable
    var level: Int
)

@Composable
fun BadProfileCard(profile: MutableProfile) {  // ❌ Always recomposes
    // ...
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 4️⃣ LAZY LISTS - LazyColumn/LazyRow Optimization
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: Use key() for stable list items
 */
/*
@Composable
fun GoodLazyList(items: List<Quest>) {
    LazyColumn {
        items(
            items = items,
            key = { quest -> quest.id }  // ✅ Stable key prevents recomposition
        ) { quest ->
            QuestCard(quest)
        }
    }
}
*/

/**
 * ❌ BAD: No key() means full recomposition on list changes
 */
/*
@Composable
fun BadLazyList(items: List<Quest>) {
    LazyColumn {
        items(items) { quest ->  // ❌ No key - entire list recomposes
            QuestCard(quest)
        }
    }
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 5️⃣ AVOID ALLOCATIONS IN COMPOSITION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: remember {} prevents object allocation on every recomposition
 */
/*
@Composable
fun GoodAllocation() {
    val scrollState = rememberScrollState()  // ✅ Allocated once
    val listState = rememberLazyListState()  // ✅ Allocated once

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        // ...
    }
}
*/

/**
 * ❌ BAD: Creates new object on every recomposition
 */
/*
@Composable
fun BadAllocation() {
    val scrollState = ScrollState(0)  // ❌ New object every recomposition!

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        // ...
    }
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 6️⃣ MODIFIER REUSE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: Reuse modifiers to reduce allocations
 */
/*
@Composable
fun GoodModifiers() {
    val commonPadding = Modifier.padding(16.dp)  // ✅ Reusable

    Column {
        Text("Title", modifier = commonPadding)
        Text("Subtitle", modifier = commonPadding)
    }
}
*/

/**
 * ❌ BAD: Creating new modifier instances
 */
/*
@Composable
fun BadModifiers() {
    Column {
        Text("Title", modifier = Modifier.padding(16.dp))     // ❌ New allocation
        Text("Subtitle", modifier = Modifier.padding(16.dp))  // ❌ New allocation
    }
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 7️⃣ DEFER READS - Read State Late
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: Read state inside lambda to scope recomposition
 */
/*
@Composable
fun GoodStateReading(count: State<Int>) {
    Box(
        modifier = Modifier.clickable {
            // ✅ Reading count here only recomposes lambda, not entire Box
            Log.d("Click", "Count: ${count.value}")
        }
    ) {
        Text("Click me")
    }
}
*/

/**
 * ❌ BAD: Reading state early causes entire composable to recompose
 */
/*
@Composable
fun BadStateReading(count: State<Int>) {
    val currentCount = count.value  // ❌ Reads state immediately - full recomposition

    Box(
        modifier = Modifier.clickable {
            Log.d("Click", "Count: $currentCount")
        }
    ) {
        Text("Click me")
    }
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 8️⃣ SIDE EFFECTS - LaunchedEffect, DisposableEffect
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: LaunchedEffect with key to control re-execution
 */
/*
@Composable
fun GoodSideEffect(userId: String) {
    LaunchedEffect(userId) {  // ✅ Only runs when userId changes
        fetchUserData(userId)
    }
}
*/

/**
 * ❌ BAD: LaunchedEffect(true) runs on EVERY recomposition
 */
/*
@Composable
fun BadSideEffect(userId: String) {
    LaunchedEffect(true) {  // ❌ Runs on every recomposition!
        fetchUserData(userId)
    }
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 9️⃣ AVOID EXPENSIVE OPERATIONS IN COMPOSITION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: Move expensive operations to LaunchedEffect
 */
/*
@Composable
fun GoodExpensiveOp(data: List<String>) {
    var processedData by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(data) {  // ✅ Runs in coroutine, off-main-thread
        processedData = withContext(Dispatchers.Default) {
            data.map { it.processExpensiveOperation() }
        }
    }

    LazyColumn {
        items(processedData) { item ->
            Text(item)
        }
    }
}
*/

/**
 * ❌ BAD: Expensive operation blocks composition (main thread)
 */
/*
@Composable
fun BadExpensiveOp(data: List<String>) {
    // ❌ Blocks main thread during composition!
    val processedData = data.map { it.processExpensiveOperation() }

    LazyColumn {
        items(processedData) { item ->
            Text(item)
        }
    }
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 🔟 COMPOSITIONLOCAL - Avoid Unnecessary Providers
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ✅ GOOD: Provide CompositionLocal at top level once
 */
/*
val LocalTheme = compositionLocalOf<ThemeData> { error("No theme provided") }

@Composable
fun AppRoot() {
    CompositionLocalProvider(LocalTheme provides myTheme) {  // ✅ Provide once
        MainScreen()
    }
}

@Composable
fun ChildComponent() {
    val theme = LocalTheme.current  // ✅ Access anywhere
}
*/

/**
 * ❌ BAD: Providing CompositionLocal too often causes recompositions
 */
/*
@Composable
fun BadCompositionLocal() {
    CompositionLocalProvider(LocalTheme provides myTheme) {  // ❌ Provides on every recomposition
        ChildComponent()
    }
}
*/

// ═══════════════════════════════════════════════════════════════════════════
// 📊 PERFORMANCE CHECKLIST (G60d)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * BEFORE COMMITTING COMPOSE CODE, CHECK:
 *
 * ✅ 1. Are you using remember {} for object allocations?
 * ✅ 2. Are expensive calculations in derivedStateOf {}?
 * ✅ 3. Are data classes @Immutable or @Stable?
 * ✅ 4. Are LazyColumn items using key = { item.id }?
 * ✅ 5. Are side effects in LaunchedEffect with proper keys?
 * ✅ 6. Are you reading state late (inside lambdas)?
 * ✅ 7. Are modifiers reused where possible?
 * ✅ 8. Is state hoisted to parent composables?
 * ✅ 9. Are expensive operations off-main-thread?
 * ✅ 10. Are CompositionLocals provided at top level?
 *
 * TARGET METRICS (Samsung A34s):
 * - FPS: 60 stable (90Hz display)
 * - Frame render time: <16ms
 * - Recomposition count: Minimize via Layout Inspector
 * - Memory: <500MB peak during gameplay
 */

// ═══════════════════════════════════════════════════════════════════════════
// 🛠️ DEBUGGING TOOLS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * USE THESE TOOLS TO FIND PERFORMANCE ISSUES:
 *
 * 1. Layout Inspector (Android Studio)
 *    - View recomposition counts (colored highlights)
 *    - Find hot-path composables
 *
 * 2. Compose Compiler Reports
 *    kotlinOptions {
 *        freeCompilerArgs += [
 *            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=..."
 *        ]
 *    }
 *
 * 3. Profiler (CPU + Memory)
 *    - Check frame rendering time
 *    - Find memory leaks
 *
 * 4. LogCompositions() modifier (custom):
 *    fun Modifier.logCompositions(tag: String): Modifier = this.then(
 *        Modifier.drawBehind {
 *            Log.d("Recomposition", "🔄 $tag recomposed")
 *        }
 *    )
 */
