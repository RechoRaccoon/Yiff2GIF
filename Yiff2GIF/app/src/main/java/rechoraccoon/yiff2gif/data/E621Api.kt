package rechoraccoon.yiff2gif.data

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface E621Api {
    @GET("favorites.json")
    suspend fun favorites(
        @Query("login") login: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 60
    ): FavoritesResponse

    @GET("posts.json")
    suspend fun posts(
        @Query("tags") tags: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 60
    ): PostsResponse

    companion object {
        private const val BASE_URL = "https://e621.net/"
        private const val USER_AGENT = "Yiff2GIF/1.0 (by RechoRaccoon)"

        /** Build a fresh client bound to the given credentials. */
        fun create(username: String, apiKey: String): E621Api {
            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("User-Agent", USER_AGENT)
                if (username.isNotBlank() && apiKey.isNotBlank()) {
                    builder.header("Authorization", Credentials.basic(username, apiKey))
                }
                chain.proceed(builder.build())
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(E621Api::class.java)
        }
    }
}
