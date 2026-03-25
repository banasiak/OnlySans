package app.onlysans.android.data

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import java.time.LocalDate
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

@Parcelize
@JsonClass(generateAdapter = true)
data class Font(
  val family: String,
  val variants: List<String> = emptyList(),
  val subsets: List<String> = emptyList(),
  val version: String,
  val lastModified: @WriteWith<LocalDateParceler> LocalDate,
  val files: Map<String, String> = emptyMap(),
  val category: String = "",
  val kind: String = ""
) : Parcelable {
  override fun toString(): String = family
}

object LocalDateParceler : Parceler<LocalDate> {
  override fun create(parcel: Parcel): LocalDate = LocalDate.ofEpochDay(parcel.readLong())
  override fun LocalDate.write(parcel: Parcel, flags: Int) = parcel.writeLong(toEpochDay())
}
