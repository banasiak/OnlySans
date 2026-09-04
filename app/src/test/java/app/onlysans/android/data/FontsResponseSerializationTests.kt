package app.onlysans.android.data

import kotlinx.serialization.json.Json
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

/**
 * Parsing checks against a slice of a real webfonts response, kept because the app swapped JSON
 * libraries and the failure mode of the new one is different: Moshi ignored a key it had no field
 * for, kotlinx.serialization rejects the whole document.
 */
class FontsResponseSerializationTests {
  // the same configuration ApiModule provides
  private val json = Json { ignoreUnknownKeys = true }

  private fun response(): FontsResponse =
    json.decodeFromString(checkNotNull(javaClass.getResourceAsStream("/webfonts-response.json")).use { it.readBytes().decodeToString() })

  @Test
  fun `a real response parses`() {
    val response = response()

    response.kind shouldBeEqualTo "webfonts#webfontList"
    response.items.map { it.family } shouldBeEqualTo listOf("ABeeZee", "Advent Pro", "Aref Ruqaa Ink")
  }

  @Test
  fun `every modelled field survives the round trip`() {
    val font = response().items.first()

    font.family shouldBeEqualTo "ABeeZee"
    font.category shouldBeEqualTo "sans-serif"
    font.kind shouldBeEqualTo "webfonts#webfont"
    font.variants shouldBeEqualTo listOf("regular", "italic")
    font.subsets shouldContain "latin"
    font.version shouldBeEqualTo "v23"
    font.menu.shouldNotBeNull()
    font.files.keys shouldBeEqualTo setOf("regular", "italic")
  }

  @Test
  fun `the ISO date becomes a LocalDate`() {
    response().items.first().lastModified shouldBeEqualTo LocalDate.of(2025, 9, 8)
  }

  @Test
  fun `a family carrying a field the app does not model still parses`() {
    // "Aref Ruqaa Ink" has colorCapabilities; a strict parser would fail the entire list over it
    val font = response().items.last()

    font.family shouldBeEqualTo "Aref Ruqaa Ink"
    font.cuts.map { it.key } shouldBeEqualTo listOf("regular", "700")
  }

  @Test
  fun `a strict parser is what would have broken, which is why the app is not strict`() {
    val strict = Json

    assertThrows<Exception> {
      strict.decodeFromString<FontsResponse>(
        """{"kind":"webfonts#webfontList","items":[{"family":"Aref Ruqaa Ink","colorCapabilities":["COLRv1"]}]}"""
      )
    }
  }

  @Test
  fun `a family missing every optional field still parses`() {
    val font = json.decodeFromString<Font>("""{"family":"Bare"}""")

    font.family shouldBeEqualTo "Bare"
    font.variants shouldBeEqualTo emptyList()
    font.files shouldBeEqualTo emptyMap()
    font.lastModified.shouldBeNull()
  }

  @Test
  fun `cuts and the default cut are derived from what parsed`() {
    val font = response().items[1]

    font.family shouldBeEqualTo "Advent Pro"
    font.cuts.size shouldBeEqualTo 18
    font.defaultCut?.key shouldBeEqualTo "regular"
  }
}