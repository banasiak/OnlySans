package app.onlysans.android.data

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class FontVariantTests {
  @Test
  fun `regular parses as upright 400`() {
    FontVariant.parse("regular") shouldBeEqualTo FontVariant("regular", 400, italic = false)
  }

  @Test
  fun `bare italic parses as italic 400`() {
    FontVariant.parse("italic") shouldBeEqualTo FontVariant("italic", 400, italic = true)
  }

  @Test
  fun `a numeric weight parses upright`() {
    FontVariant.parse("700") shouldBeEqualTo FontVariant("700", 700, italic = false)
  }

  @Test
  fun `a numeric weight with an italic suffix parses italic`() {
    FontVariant.parse("300italic") shouldBeEqualTo FontVariant("300italic", 300, italic = true)
  }

  @Test
  fun `an unparseable key is skipped rather than thrown`() {
    FontVariant.parse("wibble").shouldBeNull()
  }

  @Test
  fun `the key is kept verbatim so it still indexes the files map`() {
    // 400 and regular name the same cut, but only one of them is a key in Font.files
    FontVariant.parse("regular")?.key shouldBeEqualTo "regular"
  }

  @Test
  fun `weights are named, and italics say so`() {
    FontVariant.parse("100")?.displayName shouldBeEqualTo "Thin"
    FontVariant.parse("regular")?.displayName shouldBeEqualTo "Regular"
    FontVariant.parse("700italic")?.displayName shouldBeEqualTo "Bold Italic"
  }

  @Test
  fun `a weight with no name falls back to the number`() {
    FontVariant.parse("450")?.displayName shouldBeEqualTo "450"
  }

  @Test
  fun `sorting runs the uprights through before the italics`() {
    val keys = listOf("700italic", "regular", "100italic", "700", "100")

    val sorted = keys.mapNotNull { FontVariant.parse(it) }.sorted().map { it.key }

    sorted shouldBeEqualTo listOf("100", "regular", "700", "100italic", "700italic")
  }
}