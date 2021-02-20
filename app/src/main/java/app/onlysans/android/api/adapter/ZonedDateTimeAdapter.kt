package app.onlysans.android.api.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.ZonedDateTime

class ZonedDateTimeAdapter {

  @FromJson
  fun fromJson(dateTime: String): ZonedDateTime {
    return ZonedDateTime.parse(dateTime)
  }

  @ToJson
  fun toJson(zonedDateTime: ZonedDateTime): String {
    return zonedDateTime.toString()
  }
}