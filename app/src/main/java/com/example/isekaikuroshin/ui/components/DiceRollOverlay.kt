package com.example.isekaikuroshin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.engine.DiceResult
import com.example.isekaikuroshin.engine.SuccessLevel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Interactive dice state enum
enum class DiceInteractionState {
    WAITING_FOR_INPUT,  // Dice ready to be thrown
    THROWING,          // Player is dragging
    ROLLING,           // Dice are flying and rolling
    SHOWING_RESULT     // Final result displayed
}

@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
@Composable
fun DiceRollOverlay(
    modifier: Modifier = Modifier,
    diceResult: DiceResult,
    hasAdvantage: Boolean = false,
    hasDisadvantage: Boolean = false,
    @Suppress("UNUSED_PARAMETER") targetDifficulty: Int = 10,
    @Suppress("UNUSED_PARAMETER") bonusModifier: Int = 0,
    onAnimationComplete: () -> Unit
) {
    // Interactive states
    var interactionState by remember { mutableStateOf(DiceInteractionState.WAITING_FOR_INPUT) }
    var showResult by remember { mutableStateOf(false) }

    // Position and rotation states
    var dice1Position by remember { mutableStateOf(Offset.Zero) }
    var dice2Position by remember { mutableStateOf(Offset.Zero) }
    var dice1Rotation by remember { mutableFloatStateOf(0f) }
    var dice2Rotation by remember { mutableFloatStateOf(0f) }

    // Dual dice values for advantage/disadvantage
    var dice1Value by remember { mutableIntStateOf(diceResult.baseRoll) }
    var dice2Value by remember { mutableIntStateOf((1..20).random()) } // Generate second dice value

    // Calculate which dice is selected for advantage/disadvantage
    val selectedDiceValue = if (hasAdvantage) {
        maxOf(dice1Value, dice2Value)
    } else if (hasDisadvantage) {
        minOf(dice1Value, dice2Value)
    } else {
        dice1Value
    }

    // Enhanced gesture tracking with improved holding mechanics
    var throwVelocity by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartPosition by remember { mutableStateOf(Offset.Zero) }
    var dragCurrentPosition by remember { mutableStateOf(Offset.Zero) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var lastDragTime by remember { mutableLongStateOf(0L) }
    var isValidThrow by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Physics-based animations
    val dice1PositionAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val dice2PositionAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // Interactive rolling progress
    val rollProgress by animateFloatAsState(
        targetValue = when (interactionState) {
            DiceInteractionState.ROLLING -> 1f
            DiceInteractionState.SHOWING_RESULT -> 1f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 2000, easing = EaseOutBounce),
        finishedListener = {
            if (interactionState == DiceInteractionState.ROLLING) {
                interactionState = DiceInteractionState.SHOWING_RESULT
                showResult = true
            }
        }
    )

    val resultAlpha by animateFloatAsState(
        targetValue = if (showResult) 1f else 0f,
        animationSpec = tween(durationMillis = 800)
    )

    // Falling number animation from top
    val fallingNumberOffset by animateFloatAsState(
        targetValue = if (showResult) 0f else -200f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
    )

    val fallingNumberAlpha by animateFloatAsState(
        targetValue = if (showResult) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 200)
    )

    // Velocity-driven rotation animations
    val dice1RotationAnimation by animateFloatAsState(
        targetValue = dice1Rotation,
        animationSpec = when (interactionState) {
            DiceInteractionState.ROLLING -> tween(durationMillis = 150, easing = EaseInOut)
            else -> spring(dampingRatio = 0.8f, stiffness = 300f)
        }
    )

    val dice2RotationAnimation by animateFloatAsState(
        targetValue = dice2Rotation,
        animationSpec = when (interactionState) {
            DiceInteractionState.ROLLING -> tween(durationMillis = 150, easing = EaseInOut)
            else -> spring(dampingRatio = 0.8f, stiffness = 300f)
        }
    )

    // Enhanced physics-based throwing mechanics
    LaunchedEffect(throwVelocity) {
        if (throwVelocity != Offset.Zero && interactionState == DiceInteractionState.THROWING) {
            interactionState = DiceInteractionState.ROLLING

            // Calculate physics parameters from throw velocity
            val velocityMagnitude = kotlin.math.sqrt(throwVelocity.x * throwVelocity.x + throwVelocity.y * throwVelocity.y)
            val throwDuration = (velocityMagnitude * 2).coerceAtMost(1500f).coerceAtLeast(600f)

            // Enhanced boundary system with realistic physics constraints
            val diceSize = 120f // Full dice size in pixels
            val safetyMargin = diceSize / 2f
            val throwAreaWidth = 300f // Size of the throwing area

            // Calculate screen-relative bounds
            val leftBound = -(throwAreaWidth / 2f) + safetyMargin
            val rightBound = (throwAreaWidth / 2f) - safetyMargin
            val topBound = -(throwAreaWidth / 2f) + safetyMargin
            val bottomBound = (throwAreaWidth / 2f) - safetyMargin

            // Bounce dampening factors for realistic physics
            val horizontalBounce = 0.7f
            val verticalBounce = 0.6f
            val velocityThreshold = 10f

            // Enhanced dice 1 physics animation with advanced collision detection
            launch {
                var currentPosition: Offset
                var currentVelocity = throwVelocity * 1.2f
                var bounceCount = 0
                val maxBounces = 3

                while (bounceCount < maxBounces &&
                       kotlin.math.sqrt(currentVelocity.x * currentVelocity.x +
                                       currentVelocity.y * currentVelocity.y) > velocityThreshold) {

                    // Animate to next collision point or final position
                    dice1PositionAnimation.animateDecay(
                        initialVelocity = currentVelocity,
                        animationSpec = exponentialDecay(
                            frictionMultiplier = 1.8f,
                            absVelocityThreshold = velocityThreshold
                        )
                    )

                    currentPosition = dice1PositionAnimation.value

                    // Check for boundary collisions
                    var didBounce = false
                    if (currentPosition.x <= leftBound || currentPosition.x >= rightBound) {
                        currentVelocity = Offset(-currentVelocity.x * horizontalBounce, currentVelocity.y * 0.9f)
                        currentPosition = Offset(
                            currentPosition.x.coerceIn(leftBound, rightBound),
                            currentPosition.y
                        )
                        didBounce = true
                    }

                    if (currentPosition.y <= topBound || currentPosition.y >= bottomBound) {
                        currentVelocity = Offset(currentVelocity.x * 0.9f, -currentVelocity.y * verticalBounce)
                        currentPosition = Offset(
                            currentPosition.x,
                            currentPosition.y.coerceIn(topBound, bottomBound)
                        )
                        didBounce = true
                    }

                    if (didBounce) {
                        // Update position after bounce
                        dice1PositionAnimation.snapTo(currentPosition)
                        bounceCount++

                        // Small delay for realistic bounce physics
                        delay(50)
                    } else {
                        break
                    }
                }
            }

            // Enhanced dice 2 physics with advanced collision (if advantage/disadvantage)
            if (hasAdvantage || hasDisadvantage) {
                launch {
                    val dice2Velocity = throwVelocity + Offset(35f, -20f) // Slight variation for realistic dual throw
                    var currentPosition: Offset
                    var currentVelocity = dice2Velocity * 1.1f
                    var bounceCount = 0
                    val maxBounces = 3

                    while (bounceCount < maxBounces &&
                           kotlin.math.sqrt(currentVelocity.x * currentVelocity.x +
                                           currentVelocity.y * currentVelocity.y) > velocityThreshold) {

                        // Animate to next collision point or final position
                        dice2PositionAnimation.animateDecay(
                            initialVelocity = currentVelocity,
                            animationSpec = exponentialDecay(
                                frictionMultiplier = 1.9f, // Slightly different friction
                                absVelocityThreshold = velocityThreshold
                            )
                        )

                        currentPosition = dice2PositionAnimation.value

                        // Check for boundary collisions
                        var didBounce = false
                        if (currentPosition.x <= leftBound || currentPosition.x >= rightBound) {
                            currentVelocity = Offset(-currentVelocity.x * (horizontalBounce * 0.95f), currentVelocity.y * 0.9f)
                            currentPosition = Offset(
                                currentPosition.x.coerceIn(leftBound, rightBound),
                                currentPosition.y
                            )
                            didBounce = true
                        }

                        if (currentPosition.y <= topBound || currentPosition.y >= bottomBound) {
                            currentVelocity = Offset(currentVelocity.x * 0.9f, -currentVelocity.y * (verticalBounce * 0.95f))
                            currentPosition = Offset(
                                currentPosition.x,
                                currentPosition.y.coerceIn(topBound, bottomBound)
                            )
                            didBounce = true
                        }

                        if (didBounce) {
                            // Update position after bounce
                            dice2PositionAnimation.snapTo(currentPosition)
                            bounceCount++

                            // Small delay for realistic bounce physics
                            delay(60) // Slightly different timing
                        } else {
                            break
                        }
                    }
                }
            }

            // D20 Icosahedron physics-based rotation system
            launch {
                var currentRotation1 = dice1Rotation
                var currentRotation2 = dice2Rotation

                // D20 has 20 faces, each face is approximately 72 degrees from center
                val faceAngle = 18f // Each face covers ~18 degrees of rotation for visual effect
                val baseRotationSpeed = (velocityMagnitude / 12f).coerceAtMost(80f).coerceAtLeast(25f)

                // Icosahedron-specific physics: 3 main rotation axes
                val xAxisSpeed = kotlin.math.abs(throwVelocity.x) / 8f
                val yAxisSpeed = kotlin.math.abs(throwVelocity.y) / 8f
                val zAxisSpeed = (xAxisSpeed + yAxisSpeed) / 3f // Simulated Z-axis for 3D effect

                // Calculate realistic roll duration based on icosahedron physics
                val rollSteps = (throwDuration / 60).toInt().coerceAtMost(30)
                val targetFace1 = dice1Value
                val targetFace2 = dice2Value

                repeat(rollSteps) { step ->
                    val progress = step.toFloat() / rollSteps

                    // Icosahedron physics: non-linear slowdown simulating real 20-sided die
                    val physicsSlow = 1f - progress.pow(1.8f)

                    // Multi-axis rotation with icosahedron constraints
                    val xRotation = kotlin.math.sin(progress * 8.5f) * xAxisSpeed * physicsSlow
                    val yRotation = kotlin.math.cos(progress * 7.3f) * yAxisSpeed * physicsSlow
                    val zRotation = kotlin.math.sin(progress * 5.7f) * zAxisSpeed * physicsSlow

                    // Dice 1: Complex 3-axis rotation
                    val speed1 = baseRotationSpeed * physicsSlow
                    currentRotation1 += speed1 + xRotation + yRotation + zRotation

                    // Simulate face-locking as dice slows down (last 30% of animation)
                    if (progress > 0.7f) {
                        val faceLockProgress = (progress - 0.7f) / 0.3f
                        val targetRotation = targetFace1 * faceAngle
                        currentRotation1 = lerp(currentRotation1, targetRotation, faceLockProgress * 0.1f)
                    }

                    dice1Rotation = currentRotation1 % 360f

                    // Dice 2: Independent rotation with slight variation
                    if (hasAdvantage || hasDisadvantage) {
                        val speed2 = (baseRotationSpeed * 1.1f) * physicsSlow

                        // Offset rotations for realistic dual dice physics
                        val xRotation2 = kotlin.math.cos(progress * 7.8f) * xAxisSpeed * physicsSlow
                        val yRotation2 = kotlin.math.sin(progress * 6.2f) * yAxisSpeed * physicsSlow
                        val zRotation2 = kotlin.math.cos(progress * 4.9f) * zAxisSpeed * physicsSlow

                        currentRotation2 += speed2 + xRotation2 + yRotation2 + zRotation2

                        // Face-locking for dice 2
                        if (progress > 0.7f) {
                            val faceLockProgress = (progress - 0.7f) / 0.3f
                            val targetRotation = targetFace2 * faceAngle
                            currentRotation2 = lerp(currentRotation2, targetRotation, faceLockProgress * 0.1f)
                        }

                        dice2Rotation = currentRotation2 % 360f
                    }

                    // Progressive delay increase for realistic deceleration
                    val dynamicDelay = (60 + (progress * 40)).toLong()
                    delay(dynamicDelay)
                }

                // Final face-settling animation (icosahedron comes to rest on a face)
                repeat(5) { settleStep ->
                    val settleAmount = (5 - settleStep) * 0.8f

                    // Micro-adjustments to simulate dice settling on table
                    currentRotation1 += Random.nextFloat() * (settleAmount * 2) - settleAmount
                    dice1Rotation = currentRotation1 % 360f

                    if (hasAdvantage || hasDisadvantage) {
                        currentRotation2 += Random.nextFloat() * (settleAmount * 2) - settleAmount
                        dice2Rotation = currentRotation2 % 360f
                    }

                    delay(120 + settleStep * 30L) // Increasing delay for settling effect
                }

                // Final snap to exact face angle for clean result
                dice1Rotation = (targetFace1 * faceAngle) % 360f
                if (hasAdvantage || hasDisadvantage) {
                    dice2Rotation = (targetFace2 * faceAngle) % 360f
                }

                // Reset velocity after complete animation
                throwVelocity = Offset.Zero
            }
        }
    }

    // Complete animation after delay
    LaunchedEffect(showResult) {
        if (showResult) {
            delay(3000) // Show result for 3 seconds
            onAnimationComplete()
        }
    }

    // MAIN UI CONTENT - This was missing!
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Dice display area
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dice 1
                Canvas(
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(dice1Rotation)
                ) {
                    drawAnimatedD20(
                        progress = 1f,
                        rotation = dice1Rotation,
                        finalValue = diceResult.baseRoll,
                        isHighlighted = true,
                        showValue = showResult
                    )
                }

                // Dice 2 (if advantage/disadvantage)
                if (hasAdvantage || hasDisadvantage) {
                    Canvas(
                        modifier = Modifier
                            .size(120.dp)
                            .rotate(dice2Rotation)
                    ) {
                        drawAnimatedD20(
                            progress = 1f,
                            rotation = dice2Rotation,
                            finalValue = if (hasAdvantage) kotlin.random.Random.nextInt(1, 21) else kotlin.random.Random.nextInt(1, 21),
                            isHighlighted = false,
                            showValue = showResult
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Result display
            if (showResult) {
                Text(
                    text = "Roll: ${diceResult.baseRoll} + ${diceResult.modifiedRoll - diceResult.baseRoll} = ${diceResult.modifiedRoll}",
                    color = Color.White,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = diceResult.successLevel.name,
                    color = when (diceResult.successLevel) {
                        SuccessLevel.CRITICAL_SUCCESS -> Color(0xFFFFD700) // Gold
                        SuccessLevel.GREAT_SUCCESS -> Color.Green
                        SuccessLevel.SUCCESS -> Color.Cyan
                        SuccessLevel.PARTIAL_SUCCESS -> Color.Yellow
                        SuccessLevel.FAILURE -> Color.Red
                        SuccessLevel.CRITICAL_FAILURE -> Color.Red
                        else -> Color.White
                    },
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

private fun DrawScope.drawAnimatedD20(
    progress: Float,
    rotation: Float,
    finalValue: Int,
    isHighlighted: Boolean,
    showValue: Boolean
) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val cubeSize = size.minDimension / 4

    // Slower, more realistic multi-axis rotation angles
    val rotationX = rotation * progress * 0.8f // X-axis rotation - much slower
    val rotationY = rotation * progress * 1.2f // Y-axis rotation - moderate
    val rotationZ = rotation * progress * 0.6f // Z-axis rotation - slowest

    // Dice color based on highlighting
    val diceColor = if (isHighlighted) {
        Color(0xFFFFD700) // Gold for highlighted dice
    } else {
        Color(0xFFE8E8E8) // Light gray for better 3D effect
    }

    // Draw realistic D20 icosahedron with 20 faces
    draw3DIsometricD20(
        centerX = centerX,
        centerY = centerY,
        size = cubeSize,
        rotationX = rotationX,
        rotationY = rotationY,
        rotationZ = rotationZ,
        baseColor = diceColor,
        finalValue = finalValue,
        showValue = showValue,
        isHighlighted = isHighlighted
    )
}

private fun DrawScope.draw3DIsometricD20(
    centerX: Float,
    centerY: Float,
    size: Float,
    rotationX: Float,
    rotationY: Float,
    rotationZ: Float,
    baseColor: Color,
    finalValue: Int,
    showValue: Boolean,
    isHighlighted: Boolean
) {
    // Define D20 icosahedron vertices in 3D space
    val phi = (1 + kotlin.math.sqrt(5.0)) / 2.0 // Golden ratio
    val vertices = arrayOf(
        // 12 vertices of icosahedron
        floatArrayOf(-1f, phi.toFloat(), 0f),     // 0
        floatArrayOf( 1f, phi.toFloat(), 0f),     // 1
        floatArrayOf(-1f, -phi.toFloat(), 0f),    // 2
        floatArrayOf( 1f, -phi.toFloat(), 0f),    // 3
        floatArrayOf(0f, -1f, phi.toFloat()),     // 4
        floatArrayOf(0f,  1f, phi.toFloat()),     // 5
        floatArrayOf(0f, -1f, -phi.toFloat()),    // 6
        floatArrayOf(0f,  1f, -phi.toFloat()),    // 7
        floatArrayOf(phi.toFloat(), 0f, -1f),     // 8
        floatArrayOf(phi.toFloat(), 0f,  1f),     // 9
        floatArrayOf(-phi.toFloat(), 0f, -1f),    // 10
        floatArrayOf(-phi.toFloat(), 0f,  1f)     // 11
    )

    // Apply 3D rotation transformations
    val rotatedVertices = vertices.map { vertex ->
        rotate3D(vertex, rotationX, rotationY, rotationZ)
    }

    // Project 3D coordinates to 2D isometric view
    val projectedVertices = rotatedVertices.map { vertex ->
        projectToIsometric(vertex[0], vertex[1], vertex[2], centerX, centerY, size)
    }

    // Define D20 triangular faces (20 faces)
    val faces = arrayOf(
        // Top cap faces (around vertex 1)
        intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
        // Upper belt faces
        intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
        // Lower belt faces
        intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
        // Bottom cap faces (around vertex 3)
        intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1)
    )

    // Face numbers for D20 (1-20, with result face highlighted)
    val faceNumbers = arrayOf(
        finalValue, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        11, 12, 13, 14, 15, 16, 17, 18, 19, 20
    )

    // Calculate face visibility, normal vectors, and depth sorting
    val faceData = faces.mapIndexed { index, face ->
        val center = face.map { projectedVertices[it] }.let { vertices ->
            Offset(
                vertices.map { it.x }.average().toFloat(),
                vertices.map { it.y }.average().toFloat()
            )
        }
        val depth = face.map { rotatedVertices[it][2] }.average().toFloat()

        // Calculate face normal to determine visibility
        val v1 = rotatedVertices[face[1]]
        val v0 = rotatedVertices[face[0]]
        val v2 = rotatedVertices[face[2]]

        // Cross product to get normal vector
        val normal = floatArrayOf(
            (v1[1] - v0[1]) * (v2[2] - v0[2]) - (v1[2] - v0[2]) * (v2[1] - v0[1]),
            (v1[2] - v0[2]) * (v2[0] - v0[0]) - (v1[0] - v0[0]) * (v2[2] - v0[2]),
            (v1[0] - v0[0]) * (v2[1] - v0[1]) - (v1[1] - v0[1]) * (v2[0] - v0[0])
        )

        // Check if face is visible (normal.z > 0 means facing viewer)
        val isVisible = normal[2] > 0

        // Calculate lighting intensity based on normal direction
        val lightDirection = floatArrayOf(0.577f, 0.577f, 0.577f) // Normalized diagonal light
        val lightIntensity = maxOf(0.2f,
            (normal[0] * lightDirection[0] + normal[1] * lightDirection[1] + normal[2] * lightDirection[2]) /
            kotlin.math.sqrt((normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]).toDouble()).toFloat()
        )

        data class FaceData(val index: Int, val face: IntArray, val depth: Float, val isVisible: Boolean, val lightIntensity: Float)
        FaceData(index, face, depth, isVisible, lightIntensity)
    }.filter { it.isVisible }.sortedBy { it.depth } // Only render visible faces, sorted by depth

    // Draw faces from back to front
    faceData.forEach { face ->
        drawTriangleFace(
            vertices = face.face.map { projectedVertices[it] },
            faceIndex = face.index,
            baseColor = baseColor,
            faceNumber = faceNumbers[face.index],
            showValue = showValue,
            isHighlighted = isHighlighted && face.index == 0, // Only highlight the result face
            lightIntensity = face.lightIntensity
        )
    }
}

private fun rotate3D(vertex: FloatArray, rotX: Float, rotY: Float, rotZ: Float): FloatArray {
    val x = vertex[0]
    val y = vertex[1]
    val z = vertex[2]

    val cosX = kotlin.math.cos(Math.toRadians(rotX.toDouble())).toFloat()
    val sinX = kotlin.math.sin(Math.toRadians(rotX.toDouble())).toFloat()
    val cosY = kotlin.math.cos(Math.toRadians(rotY.toDouble())).toFloat()
    val sinY = kotlin.math.sin(Math.toRadians(rotY.toDouble())).toFloat()
    val cosZ = kotlin.math.cos(Math.toRadians(rotZ.toDouble())).toFloat()
    val sinZ = kotlin.math.sin(Math.toRadians(rotZ.toDouble())).toFloat()

    // Rotation around X axis
    val x1 = x
    val y1 = y * cosX - z * sinX
    val z1 = y * sinX + z * cosX

    // Rotation around Y axis
    val x2 = x1 * cosY + z1 * sinY
    val y2 = y1
    val z2 = -x1 * sinY + z1 * cosY

    // Rotation around Z axis
    val x3 = x2 * cosZ - y2 * sinZ
    val y3 = x2 * sinZ + y2 * cosZ
    val z3 = z2

    return floatArrayOf(x3, y3, z3)
}

private fun projectToIsometric(x: Float, y: Float, z: Float, centerX: Float, centerY: Float, scale: Float): Offset {
    // Isometric projection matrix
    val isoX = (x - z) * 0.866f * scale
    val isoY = (x + 2f * y + z) * 0.5f * scale

    return Offset(centerX + isoX, centerY - isoY)
}

private fun DrawScope.drawTriangleFace(
    vertices: List<Offset>,
    @Suppress("UNUSED_PARAMETER") faceIndex: Int,
    baseColor: Color,
    faceNumber: Int,
    showValue: Boolean,
    isHighlighted: Boolean,
    lightIntensity: Float = 1.0f
) {
    // Create path for face
    val path = Path().apply {
        moveTo(vertices[0].x, vertices[0].y)
        vertices.drop(1).forEach { vertex ->
            lineTo(vertex.x, vertex.y)
        }
        close()
    }

    // Dynamic lighting-based shading with enhanced 3D effect
    val ambientLight = 0.3f // Base ambient lighting
    val totalLight = minOf(1.0f, ambientLight + lightIntensity * 0.7f)

    val faceColor = Color(
        red = minOf(1.0f, baseColor.red * totalLight),
        green = minOf(1.0f, baseColor.green * totalLight),
        blue = minOf(1.0f, baseColor.blue * totalLight),
        alpha = baseColor.alpha
    )

    // Color each face based on its number (1-20) with unique colors
    val numberBasedColor = getColorForNumber(faceNumber, totalLight)

    val tintedFaceColor = when {
        // Result face gets special glowing effect
        isHighlighted -> Color(
            red = minOf(1.0f, numberBasedColor.red * 1.3f),
            green = minOf(1.0f, numberBasedColor.green * 1.3f),
            blue = minOf(1.0f, numberBasedColor.blue * 1.3f),
            alpha = numberBasedColor.alpha
        )
        // All other faces use their number-based color
        else -> numberBasedColor
    }

    // Draw face
    drawPath(
        path = path,
        color = tintedFaceColor,
        style = Fill
    )

    // Enhanced edge rendering with improved depth perception
    val baseEdgeAlpha = if (isHighlighted) 0.9f else 0.6f
    val edgeAlpha = minOf(0.9f, baseEdgeAlpha + lightIntensity * 0.3f)
    val edgeWidth = if (isHighlighted) 2.0f else 1.0f + lightIntensity * 0.5f

    // Draw face edges with enhanced visibility
    drawPath(
        path = path,
        color = Color.Black.copy(alpha = edgeAlpha),
        style = Stroke(width = edgeWidth)
    )

    // Add highlight edges for better 3D effect
    if (lightIntensity > 0.6f) {
        drawPath(
            path = path,
            color = Color.White.copy(alpha = (lightIntensity - 0.6f) * 0.4f),
            style = Stroke(width = 0.8f)
        )
    }

    // Extra emphasis for highlighted face
    if (isHighlighted) {
        drawPath(
            path = path,
            color = Color(0xFFFFD700).copy(alpha = 0.6f), // Gold outline
            style = Stroke(width = 1.5f)
        )
    }

    // Draw number on face if visible and animation is complete
    if (showValue) {
        val faceCenter = Offset(
            vertices.map { it.x }.average().toFloat(),
            vertices.map { it.y }.average().toFloat()
        )

        // Special highlighted result face gets bigger, animated text
        if (isHighlighted) {
            // Draw glowing background circle for result
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.3f),
                radius = 25f,
                center = faceCenter
            )

            // Draw pulsing outline
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.6f),
                radius = 22f,
                center = faceCenter,
                style = Stroke(width = 2f)
            )

            // Draw larger text for result with multiple effects
            drawContext.canvas.nativeCanvas.drawText(
                faceNumber.toString(),
                faceCenter.x + 1f,
                faceCenter.y + 12f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(120, 0, 0, 0) // Shadow
                    textSize = 32f // Much larger for result
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
            )

            // Main result text - bright white with gold outline
            drawContext.canvas.nativeCanvas.drawText(
                faceNumber.toString(),
                faceCenter.x,
                faceCenter.y + 11f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 32f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    strokeWidth = 2f
                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                }
            )

            // Add gold outline by drawing stroke separately
            drawContext.canvas.nativeCanvas.drawText(
                faceNumber.toString(),
                faceCenter.x,
                faceCenter.y + 11f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(255, 215, 0) // Gold color
                    textSize = 32f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    strokeWidth = 3f
                    style = android.graphics.Paint.Style.STROKE
                }
            )
        } else {
            // Normal faces get smaller text
            val textColor = if (totalLight > 0.6f) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }

            // Small shadow
            drawContext.canvas.nativeCanvas.drawText(
                faceNumber.toString(),
                faceCenter.x + 0.3f,
                faceCenter.y + 6f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(60, 0, 0, 0)
                    textSize = 14f // Smaller for regular faces
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
            )

            // Main text
            drawContext.canvas.nativeCanvas.drawText(
                faceNumber.toString(),
                faceCenter.x,
                faceCenter.y + 5f,
                android.graphics.Paint().apply {
                    color = textColor
                    textSize = 14f // Smaller for regular faces
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    strokeWidth = 0.3f
                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                }
            )
        }
    }
}

private fun createD20Path(centerX: Float, centerY: Float, radius: Float): Path {
    return Path().apply {
        // Create a simplified icosahedron (20-sided) shape
        // For simplicity, we'll use a polygon that resembles a d20
        val angleStep = 2 * PI.toFloat() / 5

        moveTo(
            centerX + radius * cos(0f),
            centerY + radius * sin(0f)
        )

        for (i in 1..5) {
            val angle = angleStep * i
            lineTo(
                centerX + radius * cos(angle),
                centerY + radius * sin(angle)
            )
        }

        close()
    }
}

private fun createD20ShadowPathSimple(centerX: Float, centerY: Float, radius: Float, offset: Float): Path {
    return Path().apply {
        // Create shadow/depth face offset
        val angleStep = 2 * PI.toFloat() / 5

        moveTo(
            centerX + radius * cos(0f) + offset,
            centerY + radius * sin(0f) + offset
        )

        for (i in 1..5) {
            val angle = angleStep * i
            lineTo(
                centerX + radius * cos(angle) + offset,
                centerY + radius * sin(angle) + offset
            )
        }

        close()
    }
}

// Get unique color for each number (1-20) with stable, non-flickering colors
private fun getColorForNumber(number: Int, lightIntensity: Float): Color {
    val baseColors = when (number) {
        1 -> Color(0xFF8B0000)   // Dark Red
        2 -> Color(0xFFFF4500)   // Orange Red
        3 -> Color(0xFFFF8C00)   // Dark Orange
        4 -> Color(0xFFFFD700)   // Gold
        5 -> Color(0xFF9ACD32)   // Yellow Green
        6 -> Color(0xFF32CD32)   // Lime Green
        7 -> Color(0xFF00CED1)   // Dark Turquoise
        8 -> Color(0xFF4169E1)   // Royal Blue
        9 -> Color(0xFF6A5ACD)   // Slate Blue
        10 -> Color(0xFF8A2BE2)  // Blue Violet
        11 -> Color(0xFFDC143C)  // Crimson
        12 -> Color(0xFFFF6347)  // Tomato
        13 -> Color(0xFFFF8C69)  // Salmon
        14 -> Color(0xFFFFB6C1)  // Light Pink
        15 -> Color(0xFFDDA0DD)  // Plum
        16 -> Color(0xFF9370DB)  // Medium Purple
        17 -> Color(0xFF7B68EE)  // Medium Slate Blue
        18 -> Color(0xFF6495ED)  // Cornflower Blue
        19 -> Color(0xFF20B2AA)  // Light Sea Green
        20 -> Color(0xFFFFD700)  // Special Gold for 20
        else -> Color(0xFF808080) // Gray fallback
    }

    // Apply stable lighting - less variation to prevent flickering
    val stableLightIntensity = maxOf(0.6f, minOf(1.0f, lightIntensity))
    return Color(
        red = minOf(1.0f, baseColors.red * stableLightIntensity),
        green = minOf(1.0f, baseColors.green * stableLightIntensity),
        blue = minOf(1.0f, baseColors.blue * stableLightIntensity),
        alpha = baseColors.alpha
    )
}

// Linear interpolation helper function
private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + fraction * (end - start)
}