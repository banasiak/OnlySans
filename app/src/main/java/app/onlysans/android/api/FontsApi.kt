package app.onlysans.android.api

import app.onlysans.android.data.FontsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FontsApi {
  /**
   * `prettyPrint=false` is worth keeping: the response is ~1.4 MB of JSON either way, and the
   * indented spelling is meaningfully larger over a cellular connection.
   */
  @GET("/webfonts/v1/webfonts?prettyPrint=false")
  suspend fun getFonts(@Query("sort") sort: String): FontsResponse
}