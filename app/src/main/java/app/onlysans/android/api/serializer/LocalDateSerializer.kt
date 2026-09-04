package app.onlysans.android.api.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

/**
 * `lastModified` arrives as an ISO-8601 date ("2025-09-08"), which is exactly what [LocalDate]
 * parses and prints. kotlinx.serialization ships serializers for kotlinx-datetime rather than
 * java.time, so this is the adapter for the type the model already used.
 */
object LocalDateSerializer : KSerializer<LocalDate> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

  override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}