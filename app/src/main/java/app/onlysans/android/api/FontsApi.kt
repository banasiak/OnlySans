package app.onlysans.android.api

import app.onlysans.android.data.FontsResponse
import retrofit2.Response
import retrofit2.http.GET

interface FontsApi {

  @GET("/webfonts/v1/webfonts")
  suspend fun getAllFonts(): Response<FontsResponse>
}