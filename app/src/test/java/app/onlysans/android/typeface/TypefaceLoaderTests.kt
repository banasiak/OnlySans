package app.onlysans.android.typeface

import android.graphics.Typeface
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TypefaceLoaderTests {
  @TempDir lateinit var directory: File

  private lateinit var server: MockWebServer

  private val parser: TypefaceParser = mockk()
  private val typeface: Typeface = mockk()

  @BeforeEach
  fun beforeEach() {
    server = MockWebServer()
    server.start()
    every { parser.parse(any()) } returns typeface
  }

  @AfterEach
  fun afterEach() {
    server.close()
  }

  /**
   * The loader refuses anything that is not HTTPS on the font CDN, so a test cannot simply point it
   * at the mock server. The URL it is given stays a real `fonts.gstatic.com` one and the client
   * reroutes the call — which leaves the trust check under test rather than worked around.
   */
  private fun client(): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor { chain ->
        val rerouted = chain.request().newBuilder().url(server.url(chain.request().url.encodedPath)).build()
        chain.proceed(rerouted)
      }
      .build()

  private fun loader(): TypefaceLoader = TypefaceLoader(client(), directory, parser)

  private fun cachedFiles(): List<String> = directory.listFiles()?.map { it.name }.orEmpty()

  private fun enqueueFont(body: String = FONT_BYTES) {
    server.enqueue(MockResponse.Builder().body(body).build())
  }

  @Test
  fun `a font is downloaded, parsed and returned`() =
    runTest {
      enqueueFont()

      loader().load(URL) shouldBeEqualTo typeface

      server.requestCount shouldBeEqualTo 1
    }

  @Test
  fun `the downloaded bytes are what gets parsed`() =
    runTest {
      enqueueFont()

      loader().load(URL)

      val cached = directory.listFiles().orEmpty().single()
      cached.readText() shouldBeEqualTo FONT_BYTES
      verify { parser.parse(cached) }
    }

  @Test
  fun `a URL outside the font CDN never reaches the network`() =
    runTest {
      loader().load("https://example.com/evil.ttf").shouldBeNull()

      server.requestCount shouldBeEqualTo 0
      cachedFiles() shouldBeEqualTo emptyList()
      verify(exactly = 0) { parser.parse(any()) }
    }

  @Test
  fun `a second request for the same face is served from memory`() =
    runTest {
      enqueueFont()
      val loader = loader()

      loader.load(URL)
      loader.load(URL) shouldBeEqualTo typeface

      server.requestCount shouldBeEqualTo 1
      // the point of the memory cache: the file is not re-parsed either
      verify(exactly = 1) { parser.parse(any()) }
    }

  @Test
  fun `a face already on disk is parsed without downloading it again`() =
    runTest {
      enqueueFont()
      loader().load(URL)

      // a fresh loader over the same directory is what a relaunch looks like
      loader().load(URL) shouldBeEqualTo typeface

      server.requestCount shouldBeEqualTo 1
      verify(exactly = 2) { parser.parse(any()) }
    }

  @Test
  fun `an HTTP error is a miss and leaves nothing behind`() =
    runTest {
      server.enqueue(MockResponse.Builder().code(404).build())

      loader().load(URL).shouldBeNull()

      cachedFiles() shouldBeEqualTo emptyList()
      verify(exactly = 0) { parser.parse(any()) }
    }

  @Test
  fun `a file that cannot be parsed is deleted rather than left to poison the URL`() =
    runTest {
      every { parser.parse(any()) } throws RuntimeException("not a font")
      enqueueFont()

      loader().load(URL).shouldBeNull()

      cachedFiles() shouldBeEqualTo emptyList()
    }

  @Test
  fun `a URL whose file was discarded can be fetched again`() =
    runTest {
      every { parser.parse(any()) } throws RuntimeException("not a font")
      enqueueFont()
      val loader = loader()
      loader.load(URL).shouldBeNull()

      every { parser.parse(any()) } returns typeface
      enqueueFont()
      loader.load(URL) shouldBeEqualTo typeface

      server.requestCount shouldBeEqualTo 2
    }

  @Test
  fun `a successful download leaves no partial file behind`() =
    runTest {
      enqueueFont()

      loader().load(URL)

      cachedFiles().none { it.endsWith(TypefaceLoader.PARTIAL_SUFFIX) }.shouldBeTrue()
      cachedFiles().single().endsWith(TypefaceLoader.FONT_SUFFIX).shouldBeTrue()
    }

  @Test
  fun `a download that dies mid-body leaves nothing behind at all`() =
    runTest {
      // a truncated file sitting at the final name is exactly what the write-aside-and-rename
      // exists to prevent: a later launch would trust it and hand the parser half a font.
      // Chunked, so closing the socket part-way through is a genuine truncation the client raises.
      server.enqueue(
        MockResponse.Builder()
          .chunkedBody(FONT_BYTES.repeat(CHUNK_REPEATS), CHUNK_SIZE)
          .onResponseBody(SocketEffect.CloseSocket())
          .build()
      )

      loader().load(URL).shouldBeNull()

      cachedFiles() shouldBeEqualTo emptyList()
    }

  @Test
  fun `two rows asking for the same face at once download it once`() =
    runTest {
      val held = CountDownLatch(1)
      server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            held.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            return MockResponse.Builder().body(FONT_BYTES).build()
          }
        }
      val loader = loader()

      // UNDISPATCHED so each runs up to its first suspension in order: the first registers the
      // in-flight entry before the second looks for one
      val first = async(start = CoroutineStart.UNDISPATCHED) { loader.load(URL) }
      val second = async(start = CoroutineStart.UNDISPATCHED) { loader.load(URL) }
      held.countDown()

      first.await() shouldBeEqualTo typeface
      second.await() shouldBeEqualTo typeface
      server.requestCount shouldBeEqualTo 1
    }

  @Test
  fun `different faces are fetched separately`() =
    runTest {
      enqueueFont()
      enqueueFont()
      val loader = loader()

      loader.load(URL)
      loader.load(OTHER_URL)

      server.requestCount shouldBeEqualTo 2
      cachedFiles().size shouldBeEqualTo 2
    }

  @Test
  fun `the cache file is named for the URL, not from it`() =
    runTest {
      enqueueFont()

      loader().load(URL)

      // a name derived from the URL's own path would let the catalog choose where bytes land
      val name = cachedFiles().single()
      name shouldBeEqualTo "%s%s".format(SHA256_OF_URL, TypefaceLoader.FONT_SUFFIX)
      name.shouldContain(SHA256_OF_URL)
    }

  @Test
  fun `the directory is created when it is not there yet`() =
    runTest {
      val missing = File(directory, "not/created/yet")
      enqueueFont()

      TypefaceLoader(client(), missing, parser).load(URL) shouldBeEqualTo typeface

      missing.exists().shouldBeTrue()
    }

  @Test
  fun `a trusted URL over cleartext is refused`() =
    runTest {
      loader().load("http://fonts.gstatic.com/s/roboto/v48/abc.ttf").shouldBeNull()

      server.requestCount shouldBeEqualTo 0
    }

  @Test
  fun `the CDN the catalog actually points at is trusted`() {
    TypefaceLoader.isTrustedFontUrl("https://fonts.gstatic.com/s/roboto/v48/abc.ttf").shouldBeTrue()
  }

  @Test
  fun `other hosts on the same CDN are trusted`() {
    TypefaceLoader.isTrustedFontUrl("https://themes.gstatic.com/x.ttf").shouldBeTrue()
  }

  @Test
  fun `an unrelated host is refused`() {
    TypefaceLoader.isTrustedFontUrl("https://example.com/evil.ttf").shouldBeFalse()
  }

  @Test
  fun `a host that merely ends in the CDN name is refused`() {
    // the check is on a label boundary, so neither of these may pass by suffix alone
    TypefaceLoader.isTrustedFontUrl("https://notgstatic.com/evil.ttf").shouldBeFalse()
    TypefaceLoader.isTrustedFontUrl("https://gstatic.com.evil.test/evil.ttf").shouldBeFalse()
  }

  @Test
  fun `cleartext is refused even on the right host`() {
    TypefaceLoader.isTrustedFontUrl("http://fonts.gstatic.com/s/roboto/v48/abc.ttf").shouldBeFalse()
  }

  @Test
  fun `something that is not a URL at all is refused`() {
    TypefaceLoader.isTrustedFontUrl("not a url").shouldBeFalse()
    TypefaceLoader.isTrustedFontUrl("").shouldBeFalse()
  }

  private companion object {
    const val URL = "https://fonts.gstatic.com/s/roboto/v48/roboto-regular.ttf"
    const val OTHER_URL = "https://fonts.gstatic.com/s/lato/v24/lato-regular.ttf"
    const val FONT_BYTES = "pretend this is a font"
    const val TIMEOUT_SECONDS = 5L
    const val CHUNK_REPEATS = 200
    const val CHUNK_SIZE = 16

    /** `echo -n "$URL" | sha256sum` — pinned so the naming scheme cannot drift unnoticed. */
    const val SHA256_OF_URL = "4fd89a341737358be8b507c2fe9fa9affa8b0825efc00ad935f1ede797fdaf55"
  }
}