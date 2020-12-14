package ru.avem.kspem_ruppur.communication.adapters.cs0202.requests

import ru.avem.kspem_ruppur.communication.adapters.cs0202.utils.CRC
import ru.avem.kspem_ruppur.communication.utils.TransportException
import kotlin.experimental.or

interface Cs0202Request {
    val deviceId: Byte
    val function: Byte
    val dataRequest: Byte

    fun getRequestBytes(): ByteArray

    fun getResponseSize(): Int

    fun checkResponse(response: ByteArray) {
        checkResponseSize(response.size)
        checkCRC(response)
        checkDeviceId(response[DEVICE_ID_POSITION])

        checkFunctionSame(response)
        checkFunctionIsError(response)

        checkDataRequestFromResponse(response)
    }

    fun checkResponseSize(size: Int) {
        if (getResponseSize() != size) {
            throw TransportException("Ошибка ответа: неправильный размер")
        }
    }

    fun checkDataRequestFromResponse(response: ByteArray) {
        val dataRequestFromResponse = response[DATA_RESPONSE_POSITION]

        if (dataRequest != dataRequestFromResponse) {
            throw TransportException("Ошибка ответа: неправильная Data $dataRequestFromResponse")
        }
    }

    fun checkCRC(response: ByteArray) {
        if (!CRC.isValid(response)) {
            throw TransportException("Ошибка ответа: неправильный CRC")
        }
    }

    fun checkDeviceId(deviceIdFromResponse: Byte) {
        if (deviceId != deviceIdFromResponse) {
            throw TransportException("Ошибка ответа: неправильный id устройства $deviceIdFromResponse")
        }
    }

    fun checkFunctionIsError(response: ByteArray)

    fun checkFunctionSame(response: ByteArray) {
        if (!(function == response[FUNCTION_POSITION] || function == (response[FUNCTION_POSITION] or 0x80.toByte()))) {
            throw TransportException("Ошибка ответа: неправильная функция")
        }
    }

    companion object {
        const val BYTE_SIZE_OF_DEVICE_ID: Int = 1
        const val BYTE_SIZE_OF_FUNCTION: Int = 1
        const val BYTE_SIZE_OF_DATA_REQUEST: Int = 1
        const val BYTE_SIZE_OF_DATA_RESPONSE: Int = 1
        const val BYTE_SIZE_OF_CRC: Int = 2

        const val REQUEST_SIZE = BYTE_SIZE_OF_DEVICE_ID + BYTE_SIZE_OF_FUNCTION + BYTE_SIZE_OF_DATA_REQUEST + BYTE_SIZE_OF_CRC

        const val DEVICE_ID_POSITION = 0
        const val FUNCTION_POSITION = DEVICE_ID_POSITION + BYTE_SIZE_OF_DEVICE_ID
        const val ERROR_POSITION = FUNCTION_POSITION + BYTE_SIZE_OF_FUNCTION
        const val DATA_RESPONSE_POSITION = FUNCTION_POSITION + BYTE_SIZE_OF_FUNCTION

        const val DATA_RESPONSE_BYTE_COUNT_IN_REGISTER: Byte = 3
    }
}
