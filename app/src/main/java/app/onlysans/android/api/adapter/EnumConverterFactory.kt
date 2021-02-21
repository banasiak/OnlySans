package app.onlysans.android.api.adapter

import com.squareup.moshi.JsonClass
import java.lang.reflect.Type
import retrofit2.Converter
import retrofit2.Retrofit

class EnumConverterFactory : Converter.Factory() {
  override fun stringConverter(
    type: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<*, String>? {
    return if (type is Class<*> && type.isEnum) {
      Converter<Enum<*>, String> { enum ->
        try {
          enum.javaClass.getField(enum.name).getAnnotation(JsonClass::class.java)?.generator
        } catch (e: Exception) {
          null
        } ?: enum.toString()
      }
    } else {
      null
    }
  }
}