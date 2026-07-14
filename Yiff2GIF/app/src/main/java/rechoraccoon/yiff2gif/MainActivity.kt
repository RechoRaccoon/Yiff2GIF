package rechoraccoon.yiff2gif

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import rechoraccoon.yiff2gif.ui.GalleryScreen
import rechoraccoon.yiff2gif.ui.GalleryViewModel
import rechoraccoon.yiff2gif.ui.LoginScreen

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            Yiff2GifApp(viewModel)
        }
    }
}

@Composable
fun Yiff2GifApp(viewModel: GalleryViewModel) {
    val state by viewModel.state.collectAsState()

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            if (!state.loggedIn) {
                LoginScreen(
                    loading = state.loading,
                    error = state.loginError,
                    onLogin = { user, key -> viewModel.login(user, key) }
                )
            } else {
                GalleryScreen(
                    state = state,
                    onSearch = { viewModel.search(it) },
                    onStarClick = { viewModel.loadFavorites(reset = true) },
                    onLoadMore = { viewModel.loadMore() },
                    onGridColumnsChange = { viewModel.changeGridColumns(it) },
                    onPostClick = { viewModel.downloadPost(it) }
                )
            }
        }
    }
}
