package ru.avem.kspem_ruppur.communication.adapters.modbusascii.requests

import ru.avem.kspem_ruppur.communication.utils.LogicException
import ru.avem.kspem_ruppur.communication.adapters.modbusascii.requests.ModbusAsciiRequest.Companion.BYTE_SIZE_OF_COIL_DATA_WORD
import ru.avem.kspem_ruppur.communication.adapters.modbusascii.requests.ModbusAsciiRequest.Companion.BYTE_SIZE_OF_CRC
import ru.avem.kspem_ruppur.communication.adapters.modbusascii.requests.ModbusAsciiRequest.Companion.BYTE_SIZE_OF_DEVICE_ID
import ru.avem.kspem_ruppur.communication.adapters.modbusascii.requests.ModbusAsciiRequest.Companion.BYTE_SIZE_OF_FUNCTION
import ru.avem.kspem_ruppur.communication.adapters.modbusascii.requests.ModbusAsciiRequest.Companion.BYTE_SIZE_OF_REGISTER_ID
import ru.avem.kspem_ruppur.communication.adapters.modbusascii.requests.ModbusAsciiRequest.Companion.REGISTER_ID_POSITION
import java.nio.ByteBuffer

class ForceSingleCoil(override val deviceId: Byte, override val registerId: Short, coilValue: Boolean) :
    ModbusAsciiRequest {
    companion object {
        const val FUNCTION_CODE: Byte = 0x05
    }

    override val function: Byte = FUNCTION_CODE
    private val refDataPosition = REGISTER_ID_POSITION + BYTE_SIZE_OF_REGISTER_ID
    private val coilDataWord = (if (coilValue) 0xFF00 else 0x0000).toShort()

    override fun getRequestBytes(): ByteArray = ByteBuffer.allocate(getRequestSize()).apply {
        put(deviceId)
        put(function)
        putShort(registerId)
        putShort(coilDataWord)
    }.also {
        // CRC.sign(it)
    }.array()

    override fun getRequestSize() =
        BYTE_SIZE_OF_DEVICE_ID +
                BYTE_SIZE_OF_FUNCTION +
                BYTE_SIZE_OF_REGISTER_ID +
                BYTE_SIZE_OF_COIL_DATA_WORD +
                BYTE_SIZE_OF_CRC

    override fun getResponseSize() = getRequestSize()

    fun parseResponse(response: ByteArray) {
//        checkResponse(response)
//        checkRegisterId((response[REGISTER_ID_POSITION + 0] to response[REGISTER_ID_POSITION + 1]).toShort())
//        checkOfCoilWords((response[refDataPosition + 0] to response[refDataPosition + 1]).toShort())
    }

    private fun checkOfCoilWords(countOfCoilWordsFromResponse: Short) {
        if (coilDataWord != countOfCoilWordsFromResponse) {
            throw LogicException("Ошибка ответа: неправильный countOfCoilWords[$countOfCoilWordsFromResponse] вместо [$coilDataWord]")
        }
    }
}
