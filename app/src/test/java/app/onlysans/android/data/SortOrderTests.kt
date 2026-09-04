package app.onlysans.android.data

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SortOrderTests {
  @Test
  fun `api values are the lowercase spellings the endpoint documents`() {
    SortOrder.entries.map { it.apiValue } shouldBeEqualTo listOf("alpha", "trending", "popularity", "date", "style")
  }

  @Test
  fun `a stored name round-trips`() {
    SortOrder.entries.forEach { SortOrder.from(it.name) shouldBeEqualTo it }
  }

  @Test
  fun `an absent or unrecognised preference falls back to alphabetical`() {
    SortOrder.from(null) shouldBeEqualTo SortOrder.ALPHA
    SortOrder.from("SOMETHING_A_LATER_BUILD_ADDED") shouldBeEqualTo SortOrder.ALPHA
  }
}