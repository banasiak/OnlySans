package app.onlysans.android.api

import app.onlysans.android.data.FontsResponse
import app.onlysans.android.data.SortOrder
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FontsApi {

  @GET("/webfonts/v1/webfonts?prettyPrint=false")
  suspend fun getAllFonts(@Query("sort") sort: SortOrder): Response<FontsResponse>
}