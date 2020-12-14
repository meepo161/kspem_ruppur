package ru.avem.kspem_ruppur.communication.adapters.cs0202.requests

import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_CRC
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_DATA_RESPONSE
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_DEVICE_ID
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_FUNCTION
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.REQUEST_SIZE
import ru.avem.kspem_ruppur.communication.adapters.cs0202.utils.CRC
import ru.avem.kspem_ruppur.communication.utils.LogicException
import java.nio.ByteBuffer
import kotlin.experimental.or

class MeasurementVoltage(override val deviceId: Byte) : Cs0202Request {
    companion object {
        const val FUNCTION_CODE: Byte = 0x04
        const val VOLTAGE_MEASUREMENT_DATA_REQUEST: Byte = 0x40
    }

    override val function: Byte = FUNCTION_CODE
    override val dataRequest: Byte = VOLTAGE_MEASUREMENT_DATA_REQUEST

    override fun getResponseSize() = BYTE_SIZE_OF_DEVICE_ID + BYTE_SIZE_OF_FUNCTION + BYTE_SIZE_OF_DATA_RESPONSE + BYTE_SIZE_OF_CRC

    override fun getRequestBytes(): ByteArray = ByteBuffer.allocate(REQUEST_SIZE).apply {
        put(deviceId)
        put(function)
        put(dataRequest)

    }.also {
        CRC.sign(it)
    }.array()

    fun parseResponse(response: ByteArray) {
        checkResponse(response)
    }

    override fun checkFunctionIsError(response: ByteArray) {
        if (function == (response[Cs0202Request.FUNCTION_POSITION] or 0x80.toByte())) {
            throw LogicException("Ошибка устройства: Неизвестная ошибка [${response[Cs0202Request.ERROR_POSITION]}]")
        }
    }
}
