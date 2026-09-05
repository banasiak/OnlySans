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

  @Test
  fun `the names already written to settings still read back`() {
    // SettingsStore stores these by `name`, which makes the constant names a storage format rather
    // than an implementation detail: renaming one leaves the categories a reader chose unreadable,
    // and the compiler is no help because it fixes up every reference in the source and none on
    // disk. Spelled out rather than derived from `entries`, because a set built from the enum gets
    // renamed alongside it and agrees with itself forever.
    val alreadyOnDisk = setOf("SANS_SERIF", "SERIF", "DISPLAY", "HANDWRITING", "MONOSPACE")

    FontCategory.fromNames(alreadyOnDisk) shouldBeEqualTo FontCategory.entries.toSet()
  }
}