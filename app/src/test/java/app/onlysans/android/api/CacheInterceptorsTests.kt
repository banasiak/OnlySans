package app.onlysans.android.api

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CacheInterceptorsTests {
  @TempDir lateinit var cacheDir: File

  private lateinit var server: MockWebServer

  @BeforeEach
  fun beforeEach() {
    server = MockWebServer()
    server.start()
  }

  @AfterEach
  fun afterEach() {
    server.close()
  }

  private fun client(): OkHttpClient =
    OkHttpClient.Builder()
      .cache(Cache(cacheDir, CACHE_BYTES))
      .addInterceptor(OfflineFallbackInterceptor())
      .addNetworkInterceptor(CacheRewriteInterceptor())
      .build()

  private fun get(client: OkHttpClient, path: String) = client.newCall(Request.Builder().url(server.url(path)).build()).execute()

  @Test
  fun `the endpoint's no-cache is rewritten into an hour of freshness`() {
    server.enqueue(
      MockResponse.Builder()
        .addHeader("Cache-Control", "no-cache, no-store, max-age=0")
        .addHeader("Pragma", "no-cache")
        .body("{}")
        .build()
    )

    get(client(), "/webfonts").use { response ->
      response.header("Cache-Control") shouldBeEqualTo "public, max-age=3600"
      // Pragma survives and outranks Cache-Control on the way into the cache
      response.header("Pragma").shouldBeNull()
    }
  }

  @Test
  fun `a rewritten response is served from the cache without a second round trip`() {
    server.enqueue(MockResponse.Builder().addHeader("Cache-Control", "no-cache").body("catalog").build())
    val client = client()

    get(client, "/webfonts").use { it.body.string() shouldBeEqualTo "catalog" }
    val second = get(client, "/webfonts")

    second.use {
      it.body.string() shouldBeEqualTo "catalog"
      it.cacheResponse.shouldNotBeNull()
    }
    server.requestCount shouldBeEqualTo 1
  }

  @Test
  fun `a stale entry is served when the network has gone`() {
    // no rewrite interceptor here: this is the state an hour after the catalog was fetched
    val client =
      OkHttpClient.Builder()
        .cache(Cache(cacheDir, CACHE_BYTES))
        .addInterceptor(OfflineFallbackInterceptor())
        .build()
    server.enqueue(MockResponse.Builder().addHeader("Cache-Control", "max-age=0").body("catalog").build())
    val url = server.url("/webfonts")
    client.newCall(Request.Builder().url(url).build()).execute().use { it.body.string() shouldBeEqualTo "catalog" }

    server.close()

    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
      response.body.string() shouldBeEqualTo "catalog"
      response.cacheResponse.shouldNotBeNull()
    }
  }

  @Test
  fun `an ordering never fetched before reports a failure rather than hanging`() {
    val client = client()
    server.close()

    // 504 is what only-if-cached raises on a miss; Retrofit turns it into an HttpException and the
    // repository reports it like any other failure
    client.newCall(Request.Builder().url(server.url("/webfonts")).build()).execute().use { response ->
      response.code shouldBeEqualTo 504
    }
  }

  private companion object {
    const val CACHE_BYTES = 1L * 1024 * 1024
  }
}