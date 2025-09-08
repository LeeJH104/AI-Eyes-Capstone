package com.example.capstone_map.feature.navigation

// SafeNumberAdapters.kt

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

private fun String?.cleanNum(): String? {
    val s = this?.trim()
    if (s.isNullOrEmpty()) return null
    if (s.equals("NaN", true) || s.equals("N/A", true) || s.equals("-", true)) return null
    return s
}

/** Int: null 허용 버전 (필드 타입이 Int? 인 경우) */
class SafeIntAdapterNullable : TypeAdapter<Int?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Int?) {
        if (value == null) out.nullValue() else out.value(value)
    }
    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Int? {
        return when (`in`.peek()) {
            JsonToken.NULL -> { `in`.nextNull(); null }
            JsonToken.NUMBER -> `in`.nextInt()
            JsonToken.STRING -> `in`.nextString().cleanNum()?.toIntOrNull()
            else -> { `in`.skipValue(); null }
        }
    }
}

/** Int: primitive(널 불가) → 잘못되면 0으로 */
class SafeIntAdapterPrimitive : TypeAdapter<Int>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Int) { out.value(value) }
    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Int {
        return when (`in`.peek()) {
            JsonToken.NULL -> { `in`.nextNull(); 0 }
            JsonToken.NUMBER -> `in`.nextInt()
            JsonToken.STRING -> `in`.nextString().cleanNum()?.toIntOrNull() ?: 0
            else -> { `in`.skipValue(); 0 }
        }
    }
}

/** Long */
class SafeLongAdapterNullable : TypeAdapter<Long?>() {
    override fun write(out: JsonWriter, value: Long?) { if (value == null) out.nullValue() else out.value(value) }
    override fun read(`in`: JsonReader): Long? = when (`in`.peek()) {
        JsonToken.NULL -> { `in`.nextNull(); null }
        JsonToken.NUMBER -> `in`.nextLong()
        JsonToken.STRING -> `in`.nextString().cleanNum()?.toLongOrNull()
        else -> { `in`.skipValue(); null }
    }
}
class SafeLongAdapterPrimitive : TypeAdapter<Long>() {
    override fun write(out: JsonWriter, value: Long) { out.value(value) }
    override fun read(`in`: JsonReader): Long = when (`in`.peek()) {
        JsonToken.NULL -> { `in`.nextNull(); 0L }
        JsonToken.NUMBER -> `in`.nextLong()
        JsonToken.STRING -> `in`.nextString().cleanNum()?.toLongOrNull() ?: 0L
        else -> { `in`.skipValue(); 0L }
    }
}

/** Double */
class SafeDoubleAdapterNullable : TypeAdapter<Double?>() {
    override fun write(out: JsonWriter, value: Double?) { if (value == null) out.nullValue() else out.value(value) }
    override fun read(`in`: JsonReader): Double? = when (`in`.peek()) {
        JsonToken.NULL -> { `in`.nextNull(); null }
        JsonToken.NUMBER -> `in`.nextDouble()
        JsonToken.STRING -> `in`.nextString().cleanNum()?.toDoubleOrNull()
        else -> { `in`.skipValue(); null }
    }
}
class SafeDoubleAdapterPrimitive : TypeAdapter<Double>() {
    override fun write(out: JsonWriter, value: Double) { out.value(value) }
    override fun read(`in`: JsonReader): Double = when (`in`.peek()) {
        JsonToken.NULL -> { `in`.nextNull(); 0.0 }
        JsonToken.NUMBER -> `in`.nextDouble()
        JsonToken.STRING -> `in`.nextString().cleanNum()?.toDoubleOrNull() ?: 0.0
        else -> { `in`.skipValue(); 0.0 }
    }
}
