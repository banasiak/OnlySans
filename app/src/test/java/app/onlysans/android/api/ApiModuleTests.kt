package app.onlysans.android.api

import app.onlysans.android.data.FontsResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotThrow
import org.junit.jupiter.api.Test

class ApiModuleTests {
  private val module = ApiModule

  @Test
  fun `the Json the app actually uses tolerates fields it does not model`() {
    // FontsResponseSerializationTests proves the behaviour against its own Json; this proves the
    // one Hilt hands to Retrofit is configured the same way, which is the part that ships
    val json = module.provideJson()

    json.configuration.ignoreUnknownKeys shouldBeEqualTo true
  }

  @Test
  fun `a response carrying an unmapped key parses through the provided Json`() {
    val json = module.provideJson()

    val parse = {
      json.decodeFromString<FontsResponse>(
        """{"kind":"webfonts#webfontList","items":[{"family":"Aref Ruqaa Ink","colorCapabilities":["COLRv1"]}]}"""
      )
    }

    parse shouldNotThrow Exception::class
    parse().items.single().family shouldBeEqualTo "Aref Ruqaa Ink"
  }
}