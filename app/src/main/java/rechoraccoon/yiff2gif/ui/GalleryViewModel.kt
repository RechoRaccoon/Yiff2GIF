package rechoraccoon.yiff2gif.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rechoraccoon.yiff2gif.data.E621Api
import rechoraccoon.yiff2gif.data.Post
import rechoraccoon.yiff2gif.data.Prefs
import rechoraccoon.yiff2gif.download.MediaSaver
import rechoraccoon.yiff2gif.gif.convertPostToGif

enum class DownloadState { IDLE, DOWNLOADING, DONE, ERROR }

enum class Mode { FAVORITES, SEARCH }

data class GalleryUiState(
    val loggedIn: Boolean = false,
    val username: String = "",
    val posts: List<Post> = emptyList(),
    val mode: Mode = Mode.FAVORITES,
    val query: String = "",
    val page: Int = 1,
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val gridColumns: Int = 3,
    val downloadStates: Map<Int, DownloadState> = emptyMap(),
    val loginError: String? = null
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private var api: E621Api? = null
    private var username: String = ""

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state

    init {
        viewModelScope.launch {
            val cols = prefs.getGridColumns()
            _state.value = _state.value.copy(gridColumns = cols)
            prefs.getCredentials()?.let { (user, key) ->
                username = user
                api = E621Api.create(user, key)
                _state.value = _state.value.copy(loggedIn = true, username = user)
                loadFavorites(reset = true)
            }
        }
    }

    fun login(user: String, apiKey: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loginError = null, loading = true)
            try {
                val trialApi = E621Api.create(user, apiKey)
                trialApi.favorites(login = user, page = 1, limit = 1)
                prefs.saveCredentials(user, apiKey)
                username = user
                api = trialApi
                _state.value = _state.value.copy(loggedIn = true, username = user, loading = false)
                loadFavorites(reset = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    loginError = "Login failed. Check your username and API key."
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.clearCredentials()
            api = null
            _state.value = GalleryUiState(gridColumns = _state.value.gridColumns)
        }
    }

    fun loadFavorites(reset: Boolean = false) {
        val a = api ?: return
        viewModelScope.launch {
            if (reset) _state.value = _state.value.copy(
                mode = Mode.FAVORITES, posts = emptyList(), page = 1,
                endReached = false, query = "", downloadStates = emptyMap()
            )
            if (_state.value.loading) return@launch
            _state.value = _state.value.copy(loading = true)
            try {
                val page = if (reset) 1 else _state.value.page
                val result = a.favorites(login = username, page = page)
                val newPosts = result.posts.orEmpty()
                _state.value = _state.value.copy(
                    posts = if (reset) newPosts else _state.value.posts + newPosts,
                    page = page + 1,
                    loading = false,
                    endReached = newPosts.isEmpty()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun search(tags: String) {
        val a = api ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                mode = Mode.SEARCH, query = tags, posts = emptyList(),
                page = 1, endReached = false, loading = true, downloadStates = emptyMap()
            )
            try {
                val result = a.posts(tags = tags, page = 1)
                val newPosts = result.posts.orEmpty()
                _state.value = _state.value.copy(
                    posts = newPosts, page = 2, loading = false, endReached = newPosts.isEmpty()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun loadMore() {
        if (_state.value.loading || _state.value.endReached) return
        val a = api ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val page = _state.value.page
                val newPosts: List<Post> = if (_state.value.mode == Mode.FAVORITES) {
                    a.favorites(login = username, page = page).posts.orEmpty()
                } else {
                    a.posts(tags = _state.value.query, page = page).posts.orEmpty()
                }
                _state.value = _state.value.copy(
                    posts = _state.value.posts + newPosts,
                    page = page + 1,
                    loading = false,
                    endReached = newPosts.isEmpty()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun changeGridColumns(delta: Int) {
        val newCols = (_state.value.gridColumns + delta).coerceIn(1, 10)
        if (newCols == _state.value.gridColumns) return
        _state.value = _state.value.copy(gridColumns = newCols)
        viewModelScope.launch { prefs.saveGridColumns(newCols) }
    }

    fun downloadPost(post: Post) {
        val current = _state.value.downloadStates[post.id]
        if (current == DownloadState.DOWNLOADING || current == DownloadState.DONE) return
        setDownloadState(post.id, DownloadState.DOWNLOADING)
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    convertPostToGif(post, getApplication<Application>().cacheDir)
                }
                val ok = withContext(Dispatchers.IO) {
                    MediaSaver.saveGif(getApplication(), post.id, bytes)
                }
                setDownloadState(post.id, if (ok) DownloadState.DONE else DownloadState.ERROR)
            } catch (e: Exception) {
                setDownloadState(post.id, DownloadState.ERROR)
            }
        }
    }

    private fun setDownloadState(postId: Int, state: DownloadState) {
        _state.value = _state.value.copy(
            downloadStates = _state.value.downloadStates + (postId to state)
        )
    }
}
