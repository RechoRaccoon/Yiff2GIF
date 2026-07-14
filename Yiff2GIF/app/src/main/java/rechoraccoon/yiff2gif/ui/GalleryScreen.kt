package rechoraccoon.yiff2gif.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import rechoraccoon.yiff2gif.data.Post

private val NeonGreen = Color(0xFF00FF07)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onSearch: (String) -> Unit,
    onStarClick: () -> Unit,
    onLoadMore: () -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onPostClick: (Post) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()

    // Trigger load-more when user scrolls near the end of the list.
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    // Accumulated pinch scale; every time it crosses a threshold we bump columns by +-1
    // and reset, so it takes a full pinch gesture per step rather than continuous scaling.
    var pinchAccum by remember { mutableStateOf(1f) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed search bar, padded so it clears the display cutout / status bar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onStarClick) {
                StarIcon(filled = state.mode == Mode.FAVORITES)
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                singleLine = true,
                placeholder = { Text("search tags...", color = Color.Gray) },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) onSearch(query.trim())
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = NeonGreen
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        pinchAccum *= zoom
                        val step = 0.18f
                        if (pinchAccum > 1f + step) {
                            onGridColumnsChange(-1) // zoom in -> fewer, bigger columns
                            pinchAccum = 1f
                        } else if (pinchAccum < 1f - step) {
                            onGridColumnsChange(1) // zoom out -> more, smaller columns
                            pinchAccum = 1f
                        }
                    }
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(state.gridColumns),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.posts, key = { it.id }) { post ->
                    PostTile(
                        post = post,
                        downloadState = state.downloadStates[post.id] ?: DownloadState.IDLE,
                        onClick = { onPostClick(post) }
                    )
                }
            }

            if (state.loading && state.posts.isEmpty()) {
                CircularProgressIndicator(
                    color = NeonGreen,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun PostTile(
    post: Post,
    downloadState: DownloadState,
    onClick: () -> Unit
) {
    val thumbUrl = post.preview?.url ?: post.file?.url
    val isBusyOrDone = downloadState == DownloadState.DOWNLOADING || downloadState == DownloadState.DONE

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .then(
                if (downloadState == DownloadState.DONE)
                    Modifier.border(3.dp, NeonGreen)
                else Modifier
            )
    ) {
        AsyncImage(
            model = thumbUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (isBusyOrDone) Modifier.blur(14.dp) else Modifier)
        )

        if (isBusyOrDone) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        when (downloadState) {
            DownloadState.DOWNLOADING -> {
                CircularProgressIndicator(
                    color = NeonGreen,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                )
            }
            DownloadState.DONE -> {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Downloaded",
                    tint = NeonGreen,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }
            DownloadState.ERROR -> {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Red)
                )
            }
            DownloadState.IDLE -> {}
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(post.id) {
                    androidx.compose.foundation.gestures.detectTapGestures(onTap = { onClick() })
                }
        )
    }
}

/** Simple non-branded 5-point star, drawn with Canvas so there's no trademark/copyright concern. */
@Composable
private fun StarIcon(filled: Boolean) {
    val color = if (filled) NeonGreen else Color.White
    Canvas(modifier = Modifier.size(24.dp)) {
        val path = Path()
        val cx = size.width / 2
        val cy = size.height / 2
        val outerR = size.minDimension / 2
        val innerR = outerR * 0.42f
        val points = 5
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = (PI / points * i) - PI / 2
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = color)
    }
}
