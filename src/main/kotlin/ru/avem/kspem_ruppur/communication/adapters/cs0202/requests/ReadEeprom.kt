package ru.avem.kspem_ruppur.communication.adapters.cs0202.requests

import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_CRC
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_DEVICE_ID
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_FUNCTION
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.DATA_RESPONSE_BYTE_COUNT_IN_REGISTER
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.DATA_RESPONSE_POSITION
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.REQUEST_SIZE
import ru.avem.kspem_ruppur.communication.adapters.cs0202.utils.CRC
import ru.avem.kspem_ruppur.communication.utils.LogicException
import java.nio.ByteBuffer
import kotlin.experimental.or

class ReadEeprom(override val deviceId: Byte) : Cs0202Request {
    companion object {
        const val FUNCTION_CODE: Byte = 0x06
        const val READ_EEPROM_DATA_REQUEST: Byte = 0x60

        const val DATA_RESPONSE_BYTE_COUNT: Byte = 121

        const val DATA_RESPONSE_FLOAT_REGISTER_COUNT = (DATA_RESPONSE_BYTE_COUNT / DATA_RESPONSE_BYTE_COUNT_IN_REGISTER).toByte()
        const val DATA_RESPONSE_BYTE_REGISTER_COUNT = 1
    }

    override val function: Byte = FUNCTION_CODE
    override val dataRequest: Byte = READ_EEPROM_DATA_REQUEST
    val dataResponse: FloatArray = FloatArray((DATA_RESPONSE_FLOAT_REGISTER_COUNT + DATA_RESPONSE_BYTE_REGISTER_COUNT)) { 0F }

    override fun getResponseSize() = BYTE_SIZE_OF_DEVICE_ID + BYTE_SIZE_OF_FUNCTION + DATA_RESPONSE_BYTE_COUNT + BYTE_SIZE_OF_CRC

    override fun getRequestBytes(): ByteArray = ByteBuffer.allocate(REQUEST_SIZE).apply {
        put(deviceId)
        put(function)
        put(dataRequest)
    }.also {
        CRC.sign(it)
    }.array()

    fun parseResponse(response: ByteArray) {
        checkResponse(response)

        val dataResponseRaw = response.copyOfRange(DATA_RESPONSE_POSITION, DATA_RESPONSE_POSITION + DATA_RESPONSE_BYTE_COUNT)

        for (registerIndex in 0 until DATA_RESPONSE_FLOAT_REGISTER_COUNT) {
            val registerBuffer = ByteBuffer.allocate(4).apply {
                put(dataResponseRaw[registerIndex + 0])
                put(dataResponseRaw[registerIndex + 1])
                put(dataResponseRaw[registerIndex + 2])
                put(0.toByte())
            }.also {
                it.flip()
            }

            dataResponse[registerIndex] = registerBuffer.float
        }

        dataResponse[DATA_RESPONSE_FLOAT_REGISTER_COUNT.toInt()] =
            dataResponseRaw[DATA_RESPONSE_FLOAT_REGISTER_COUNT * DATA_RESPONSE_BYTE_COUNT_IN_REGISTER].toFloat()
    }

    override fun checkFunctionIsError(response: ByteArray) {
        if (function == (response[Cs0202Request.FUNCTION_POSITION] or 0x80.toByte())) {
            when (response[Cs0202Request.ERROR_POSITION]) {
                0x03.toByte() -> throw LogicException("Ошибка устройства: не корректное значение нового сетевого адреса, изменение сетевого адреса не произошло.")
                else -> throw LogicException("Ошибка устройства: Неизвестная ошибка [${response[Cs0202Request.ERROR_POSITION]}]")
            }
        }
    }

    override fun checkDataRequestFromResponse(response: ByteArray) {}
}
