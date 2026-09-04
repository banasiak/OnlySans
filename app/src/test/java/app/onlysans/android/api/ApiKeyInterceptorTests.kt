package app.onlysans.android.api

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApiKeyInterceptorTests {
  private lateinit var server: MockWebServer

  @BeforeEach
  fun beforeEach() {
    server = MockWebServer()
    server.start()
    server.enqueue(MockResponse.Builder().body("{}").build())
  }

  @AfterEach
  fun afterEach() {
    server.close()
  }

  private fun get(key: String, path: String = "/webfonts/v1/webfonts") {
    val client = OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor(key)).build()
    client.newCall(Request.Builder().url(server.url(path)).build()).execute().close()
  }

  @Test
  fun `the key is appended to the query`() {
    get("a-real-key")

    server.takeRequest().url.queryParameter("key") shouldBeEqualTo "a-real-key"
  }

  @Test
  fun `query parameters already on the request are kept`() {
    get("a-real-key", "/webfonts/v1/webfonts?sort=trending&prettyPrint=false")

    val url = server.takeRequest().url
    url.queryParameter("sort") shouldBeEqualTo "trending"
    url.queryParameter("prettyPrint") shouldBeEqualTo "false"
    url.queryParameter("key") shouldBeEqualTo "a-real-key"
  }

  @Test
  fun `the path is left alone`() {
    get("a-real-key")

    server.takeRequest().url.encodedPath shouldBeEqualTo "/webfonts/v1/webfonts"
  }

  @Test
  fun `a key needing escaping is encoded rather than breaking the URL`() {
    get("key with spaces&more")

    val request = server.takeRequest()
    request.url.queryParameter("key") shouldBeEqualTo "key with spaces&more"
    request.target.shouldContain("key=key%20with%20spaces%26more")
  }

  @Test
  fun `a blank key is still sent, so the API can say what is wrong`() {
    // omitting the parameter entirely gets a 400 that reads as a malformed request rather than a
    // missing key, which is the message the gallery wants to surface
    get("")

    server.takeRequest().url.queryParameter("key") shouldBeEqualTo ""
  }
}