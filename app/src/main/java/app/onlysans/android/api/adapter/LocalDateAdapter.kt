package app.onlysans.android.api.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDate

class LocalDateAdapter {

  @FromJson
  fun fromJson(localDate: String): LocalDate {
    return LocalDate.parse(localDate)
  }

  @ToJson
  fun toJson(localDate: LocalDate): String {
    return localDate.toString()
  }
}