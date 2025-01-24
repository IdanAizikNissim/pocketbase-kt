package io.pocketbase.dtos

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal class ExcludeIfNullSerializer : KSerializer<String?> {
    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeString()
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("ExcludeNullString", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: String?,
    ) {
        if (value != null) {
            encoder.encodeString(value)
        }
    }
}
