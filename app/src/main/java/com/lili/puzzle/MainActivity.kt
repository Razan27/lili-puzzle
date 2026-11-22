package com.lili.puzzle

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LiliPuzzleTheme { PuzzleApp() } }
    }
}

@Composable
fun LiliPuzzleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF6C63FF),
        surface = Color(0xFFFFFBFE)
    ), content = content)
}

@Composable
fun PuzzleApp() {
    var gridSize by remember { mutableIntStateOf(4) }
    var restartKey by remember { mutableIntStateOf(0) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Lili Puzzle", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 6).forEach { size ->
                FilterChip(selected = gridSize == size, onClick = { gridSize = size; restartKey++ }, label = { Text("${size}×$size") })
            }
            OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Image") }
        }
        Spacer(Modifier.height(12.dp))
        JigsawBoard(gridSize = gridSize, imageUri = imageUri, restartKey = restartKey)
    }
}

data class Piece(val correctIndex: Int, var currentIndex: Int)

@Composable
fun JigsawBoard(gridSize: Int, imageUri: Uri?, restartKey: Int) {
    var pieces by remember(gridSize, restartKey) {
        mutableStateOf(List(gridSize * gridSize) { i -> Piece(i, i) }.shuffled(Random(System.currentTimeMillis())))
    }
    var moves by remember(gridSize, restartKey) { mutableIntStateOf(0) }
    var elapsed by remember(gridSize, restartKey) { mutableLongStateOf(0) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(gridSize, restartKey) {
        elapsed = 0
        while (pieces.any { it.correctIndex != it.currentIndex }) {
            kotlinx.coroutines.delay(1000)
            elapsed++
        }
    }
    val won = pieces.indices.all { pieces[it].correctIndex == pieces[it].currentIndex }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Text("Moves: $moves")
        Text("Time: ${elapsed}s")
    }
    Spacer(Modifier.height(8.dp))

    Canvas(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .onSizeChanged { boardSize = it }
            .pointerInput(gridSize, restartKey) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (boardSize.width == 0) return@detectDragGestures
                    val fromCol = (change.position.x / (boardSize.width / gridSize.toFloat())).toInt().coerceIn(0, gridSize - 1)
                    val fromRow = (change.position.y / (boardSize.height / gridSize.toFloat())).toInt().coerceIn(0, gridSize - 1)
                    val from = (fromRow * gridSize + fromCol).coerceIn(pieces.indices)
                    val toCol = ((change.position.x + dragAmount.x) / (boardSize.width / gridSize.toFloat())).toInt().coerceIn(0, gridSize - 1)
                    val toRow = ((change.position.y + dragAmount.y) / (boardSize.height / gridSize.toFloat())).toInt().coerceIn(0, gridSize - 1)
                    val to = (toRow * gridSize + toCol).coerceIn(pieces.indices)
                    if (from != to) {
                        val mutable = pieces.toMutableList()
                        val removed = mutable.removeAt(from)
                        mutable.add(to, removed)
                        pieces = mutable
                        pieces = pieces.mapIndexed { index, piece -> piece.copy(currentIndex = index) }
                        moves++
                    }
                }
            }
    ) {
        val cellW = size.width / gridSize
        val cellH = size.height / gridSize
        pieces.forEachIndexed { position, piece ->
            val row = position / gridSize
            val col = position % gridSize
            val correctRow = piece.correctIndex / gridSize
            val correctCol = piece.correctIndex % gridSize
            val left = correctCol * cellW
            val top = correctRow * cellH
            val isCorrect = position == piece.correctIndex

            clipRect(left = col * cellW, top = row * cellH, right = (col + 1) * cellW, bottom = (row + 1) * cellH) {
                drawRect(
                    color = if (isCorrect) Color(0xFF80CBC4) else Color(0xFFB39DDB),
                    topLeft = Offset(col * cellW - left + col * 0f, row * cellH - top + row * 0f),
                    size = Size(cellW, cellH)
                )
                drawRect(color = Color(0x55000000), topLeft = Offset(col * cellW, row * cellH), size = Size(cellW, cellH))
            }
        }
        for (i in 1 until gridSize) {
            drawLine(Color.White, Offset(i * cellW, 0f), Offset(i * cellW, size.height), 2f)
            drawLine(Color.White, Offset(0f, i * cellH), Offset(size.width, i * cellH), 2f)
        }
    }

    if (won) {
        Spacer(Modifier.height(12.dp))
        Text("Puzzle complete!", color = Color(0xFF2E7D32), style = MaterialTheme.typography.titleLarge)
    }
}
