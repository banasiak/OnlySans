package app.onlysans.android.data

import app.onlysans.android.Fonts
import app.onlysans.android.api.FontsApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class FontRepositoryTests {
  private val api: FontsApi = mockk()

  private fun repository(): FontRepository = FontRepository(api)

  @Test
  fun `a successful fetch returns the catalog`() =
    runTest {
      coEvery { api.getFonts("alpha") } returns FontsResponse(items = Fonts.all)

      repository().fonts(SortOrder.ALPHA).getOrNull() shouldBeEqualTo Fonts.all
    }

  @Test
  fun `a second request for the same ordering is served from memory`() =
    runTest {
      coEvery { api.getFonts("alpha") } returns FontsResponse(items = Fonts.all)
      val repository = repository()

      repository.fonts(SortOrder.ALPHA)
      repository.fonts(SortOrder.ALPHA)

      coVerify(exactly = 1) { api.getFonts("alpha") }
    }

  @Test
  fun `each ordering is fetched separately`() =
    runTest {
      coEvery { api.getFonts("alpha") } returns FontsResponse(items = Fonts.all)
      coEvery { api.getFonts("trending") } returns FontsResponse(items = Fonts.all.reversed())
      val repository = repository()

      repository.fonts(SortOrder.ALPHA).getOrNull() shouldBeEqualTo Fonts.all
      repository.fonts(SortOrder.TRENDING).getOrNull() shouldBeEqualTo Fonts.all.reversed()
    }

  @Test
  fun `a failure is reported rather than thrown`() =
    runTest {
      coEvery { api.getFonts(any()) } throws IOException("no network")

      repository().fonts(SortOrder.ALPHA).isFailure.shouldBeTrue()
    }

  @Test
  fun `a failed fetch is not cached`() =
    runTest {
      coEvery { api.getFonts("alpha") } throws IOException("no network")
      val repository = repository()

      repository.fonts(SortOrder.ALPHA)
      repository.fonts(SortOrder.ALPHA)

      coVerify(exactly = 2) { api.getFonts("alpha") }
    }

  @Test
  fun `a family already loaded is returned without a round trip`() =
    runTest {
      coEvery { api.getFonts("trending") } returns FontsResponse(items = Fonts.all)
      val repository = repository()
      repository.fonts(SortOrder.TRENDING)

      repository.family("Roboto") shouldBeEqualTo Fonts.roboto

      coVerify(exactly = 0) { api.getFonts("alpha") }
    }

  @Test
  fun `a family requested before any catalog was loaded fetches one`() =
    runTest {
      // this is the process-death path: the specimen screen restores with an empty repository
      coEvery { api.getFonts("alpha") } returns FontsResponse(items = Fonts.all)

      repository().family("Roboto") shouldBeEqualTo Fonts.roboto
    }

  @Test
  fun `a family the catalog does not have is null`() =
    runTest {
      coEvery { api.getFonts("alpha") } returns FontsResponse(items = Fonts.all)

      repository().family("Comic Sans MS").shouldBeNull()
    }
}