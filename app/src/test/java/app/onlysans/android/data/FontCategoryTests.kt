package app.onlysans.android.data

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class FontCategoryTests {
  @Test
  fun `every category the catalog uses is covered`() {
    // the five the webfonts API has shipped since 2010; a sixth would show up as an unfiltered null
    listOf("sans-serif", "serif", "display", "handwriting", "monospace")
      .map { FontCategory.from(it) }
      .shouldBeEqualTo(FontCategory.entries.toList())
  }

  @Test
  fun `an unknown category is null rather than an error`() {
    FontCategory.from("variable").shouldBeNull()
  }

  @Test
  fun `stored names that no longer exist are dropped`() {
    FontCategory.fromNames(setOf("SERIF", "BLACKLETTER")) shouldBeEqualTo setOf(FontCategory.SERIF)
  }
}