package rechoraccoon.yiff2gif.data

data class PostFile(
    val url: String?,
    val ext: String?,
    val width: Int?,
    val height: Int?
)

data class PostPreview(
    val url: String?
)

data class Post(
    val id: Int,
    val file: PostFile?,
    val preview: PostPreview?,
    val tags: Map<String, List<String>>?
)

data class PostsResponse(
    val posts: List<Post>?
)

data class FavoritesResponse(
    val posts: List<Post>?
)

/** What kind of media a post is, so we know whether to convert or just save. */
enum class MediaKind { IMAGE, GIF, VIDEO, UNKNOWN }

fun Post.mediaKind(): MediaKind {
    return when (file?.ext?.lowercase()) {
        "gif" -> MediaKind.GIF
        "jpg", "jpeg", "png", "webp" -> MediaKind.IMAGE
        "webm", "mp4" -> MediaKind.VIDEO
        else -> MediaKind.UNKNOWN
    }
}
