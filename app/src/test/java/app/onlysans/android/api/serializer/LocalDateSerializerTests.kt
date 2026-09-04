package app.onlysans.android.api.serializer

import kotlinx.serialization.json.Json
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class LocalDateSerializerTests {
  @Test
  fun `an ISO date decodes`() {
    Json.decodeFromString(LocalDateSerializer, "\"2025-09-08\"") shouldBeEqualTo LocalDate.of(2025, 9, 8)
  }

  @Test
  fun `a date encodes back to the same string`() {
    Json.encodeToString(LocalDateSerializer, LocalDate.of(2026, 1, 31)) shouldBeEqualTo "\"2026-01-31\""
  }

  @Test
  fun `a date that is not ISO is an error rather than a silent zero`() {
    assertThrows<Exception> { Json.decodeFromString(LocalDateSerializer, "\"08/09/2025\"") }
  }
}