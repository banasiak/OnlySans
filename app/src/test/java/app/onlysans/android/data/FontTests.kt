package app.onlysans.android.data

import app.onlysans.android.Fonts
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class FontTests {
  @Test
  fun `cuts drop variants with no file behind them`() {
    val font = Fonts.font("Partial", variants = listOf("regular", "700"), files = mapOf("regular" to "url"))

    font.cuts.map { it.key } shouldBeEqualTo listOf("regular")
  }

  @Test
  fun `cuts drop keys that cannot be parsed`() {
    val font = Fonts.font("Odd", variants = listOf("regular", "wibble"), files = mapOf("regular" to "a", "wibble" to "b"))

    font.cuts.map { it.key } shouldBeEqualTo listOf("regular")
  }

  @Test
  fun `the default cut is upright regular when there is one`() {
    Fonts.roboto.defaultCut?.key shouldBeEqualTo "regular"
  }

  @Test
  fun `a family with no regular falls back to its lightest upright`() {
    val font = Fonts.font("NoRegular", variants = listOf("700", "300", "300italic"))

    font.defaultCut?.key shouldBeEqualTo "300"
  }

  @Test
  fun `a family with only italics still has a default`() {
    val font = Fonts.font("ItalicOnly", variants = listOf("700italic", "300italic"))

    font.defaultCut?.key shouldBeEqualTo "300italic"
  }

  @Test
  fun `a family with nothing downloadable has no default`() {
    Fonts.font("Empty", variants = emptyList(), files = emptyMap()).defaultCut.shouldBeNull()
  }

  @Test
  fun `urlFor resolves through the variant key`() {
    val font = Fonts.font("Keyed", variants = listOf("regular"), files = mapOf("regular" to "https://example/x.ttf"))

    font.urlFor(font.defaultCut) shouldBeEqualTo "https://example/x.ttf"
  }

  @Test
  fun `urlFor tolerates a null variant`() {
    Fonts.roboto.urlFor(null).shouldBeNull()
  }

  @Test
  fun `the category is typed when it is one this build knows`() {
    Fonts.merriweather.categoryType shouldBeEqualTo FontCategory.SERIF
    Fonts.font("Future", category = "variable").categoryType.shouldBeNull()
  }
}