package com.example.simple_ble

import java.nio.ByteBuffer
import java.nio.ByteOrder

object DataDecoder {
    private val fullDataBuffer = mutableListOf<Byte>() // To store all received bytes
    private var totalExpectedSize: Int = 0            // Total expected size in bytes

    /**
     * Processes incoming BLE data, combining packets if necessary and decoding floats.
     *
     * @param packet The received BLE packet as a byte array.
     * @return A list of decoded floats if all packets are received, or null if incomplete.
     */
    fun processPacket(packet: ByteArray): List<Float>? {
        if (packet.size < 4) throw IllegalArgumentException("Packet too short to process.")

        // Check header
        val header = packet.slice(0..1).toByteArray()
        when {
            header.contentEquals(byteArrayOf(0xAA.toByte(), 0xAA.toByte())) -> {
                // First packet, extract total size
                totalExpectedSize = ByteBuffer.wrap(packet.slice(2..3).toByteArray())
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .short
                    .toInt()
                fullDataBuffer.clear() // Reset buffer for new data
                fullDataBuffer.addAll(packet.slice(4 until packet.size))
            }
            header.contentEquals(byteArrayOf(0xBB.toByte(), 0xBB.toByte())) -> {
                // Subsequent packet
                if (totalExpectedSize == 0) throw IllegalStateException("Received data out of sequence.")
                fullDataBuffer.addAll(packet.slice(4 until packet.size))
            }
            else -> {
                throw IllegalArgumentException("Invalid packet header: ${header.joinToString { "%02x".format(it) }}")
            }
        }

        // Check if we have received all data
        if (fullDataBuffer.size >= totalExpectedSize) {
            val fullData = fullDataBuffer.toByteArray()
            fullDataBuffer.clear() // Reset buffer after processing
            return decodeFloats(fullData)
        }

        return null // Data is incomplete
    }

    /**
     * Decodes a byte array into a list of floats.
     *
     * @param dataBytes The byte array containing the float data.
     * @return A list of decoded float values.
     */
    private fun decodeFloats(dataBytes: ByteArray): List<Float> {
        if (dataBytes.size % 4 != 0) throw IllegalArgumentException("Data size is not a multiple of 4.")

        val floats = mutableListOf<Float>()
        val buffer = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN)

        while (buffer.remaining() >= 4) {
            floats.add(buffer.float)
        }

        return floats
    }
}
