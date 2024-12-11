package com.example.simple_ble

import java.nio.ByteBuffer
import java.nio.ByteOrder

object DataDecoder {
    /**
     * Decodes a received hex string into a byte array and extracts float values.
     *
     * @param hexString The received data as a hex string.
     * @return A list of decoded float values.
     */
    fun decodeToFloats(hexString: String): List<Float> {
        // Convert hex string to byte array
        val byteArray = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        if (byteArray.size < 3) {
            throw IllegalArgumentException("Data too short to decode.")
        }

        // Validate header and fixed byte
        if (byteArray[0] != 0xAA.toByte() || byteArray[1] != 0xAA.toByte()) {
            throw IllegalArgumentException("Invalid header: ${byteArray[0]}, ${byteArray[1]}")
        }
        if (byteArray[2] != 0xC8.toByte()) {
            throw IllegalArgumentException("Invalid fixed byte: ${byteArray[2]}")
        }

        // Extract the meaningful byte array
        val dataBytes = byteArray.drop(3)

        // Convert every 4 bytes into a float
        val floats = mutableListOf<Float>()
        val buffer = ByteBuffer.wrap(dataBytes.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)

        while (buffer.remaining() >= 4) {
            floats.add(buffer.float)
        }

        return floats

    }
}
