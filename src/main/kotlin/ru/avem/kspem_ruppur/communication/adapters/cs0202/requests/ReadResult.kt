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

class ReadResult(override val deviceId: Byte, val dataRequestRequire: Byte) : Cs0202Request {
    companion object {
        const val FUNCTION_CODE: Byte = 0x07

        const val READ_RESULT_MEAS_0_REQUEST: Byte = 0x71
        const val READ_RESULT_MEAS_MANUAL_REQUEST: Byte = 0x72
        const val READ_RESULT_U_INPUT_REQUEST: Byte = 0x73

        const val DATA_RESPONSE_BYTE_COUNT: Byte = 12

        const val DATA_RESPONSE_FLOAT_REGISTER_COUNT = (DATA_RESPONSE_BYTE_COUNT / DATA_RESPONSE_BYTE_COUNT_IN_REGISTER).toByte()
    }

    override val function: Byte = FUNCTION_CODE
    override val dataRequest: Byte = checkDataRequest(dataRequestRequire)

    private fun checkDataRequest(dataRequestRequire: Byte) =
        when (dataRequestRequire) {
            READ_RESULT_MEAS_0_REQUEST -> READ_RESULT_MEAS_0_REQUEST
            READ_RESULT_MEAS_MANUAL_REQUEST -> READ_RESULT_MEAS_MANUAL_REQUEST
            READ_RESULT_U_INPUT_REQUEST -> READ_RESULT_U_INPUT_REQUEST
            else -> throw LogicException("Ошибка устройства: Неизвестный dataRequestRequire [${dataRequestRequire}]")
        }

    val dataResponse: FloatArray = FloatArray(DATA_RESPONSE_BYTE_COUNT / DATA_RESPONSE_BYTE_COUNT_IN_REGISTER) { 0F }

    override fun getResponseSize() =
        when (dataRequestRequire) {
            READ_RESULT_MEAS_0_REQUEST -> BYTE_SIZE_OF_DEVICE_ID + BYTE_SIZE_OF_FUNCTION + DATA_RESPONSE_BYTE_COUNT_IN_REGISTER * DATA_RESPONSE_FLOAT_REGISTER_COUNT + BYTE_SIZE_OF_CRC
            READ_RESULT_MEAS_MANUAL_REQUEST -> BYTE_SIZE_OF_DEVICE_ID + BYTE_SIZE_OF_FUNCTION + DATA_RESPONSE_BYTE_COUNT_IN_REGISTER * DATA_RESPONSE_FLOAT_REGISTER_COUNT + BYTE_SIZE_OF_CRC
            READ_RESULT_U_INPUT_REQUEST -> BYTE_SIZE_OF_DEVICE_ID + BYTE_SIZE_OF_FUNCTION + DATA_RESPONSE_BYTE_COUNT_IN_REGISTER + BYTE_SIZE_OF_CRC
            else -> throw LogicException("Ошибка устройства: Неизвестный dataRequestRequire [${dataRequestRequire}]")
        }

    override fun getRequestBytes(): ByteArray = ByteBuffer.allocate(REQUEST_SIZE).apply {
        put(deviceId)
        put(function)
        put(dataRequest)
    }.also {
        CRC.sign(it)
    }.array()

    fun parseResponse(response: ByteArray) {
        checkResponse(response)

        when (dataRequestRequire) {
            READ_RESULT_MEAS_0_REQUEST, READ_RESULT_MEAS_MANUAL_REQUEST -> {
                val dataResponseRaw = response.copyOfRange(DATA_RESPONSE_POSITION, DATA_RESPONSE_POSITION + DATA_RESPONSE_BYTE_COUNT)

                for (registerIndex in 0 until DATA_RESPONSE_FLOAT_REGISTER_COUNT) {
                    val registerBuffer = ByteBuffer.allocate(4).apply {
                        put(dataResponseRaw[registerIndex * DATA_RESPONSE_BYTE_COUNT_IN_REGISTER + 0])
                        put(dataResponseRaw[registerIndex * DATA_RESPONSE_BYTE_COUNT_IN_REGISTER + 1])
                        put(dataResponseRaw[registerIndex * DATA_RESPONSE_BYTE_COUNT_IN_REGISTER + 2])
                        put(0.toByte())
                    }.also {
                        it.flip()
                    }

                    dataResponse[registerIndex] = registerBuffer.float
                }
            }
            READ_RESULT_U_INPUT_REQUEST -> {
                val dataResponseRaw = response.copyOfRange(DATA_RESPONSE_POSITION, DATA_RESPONSE_POSITION + DATA_RESPONSE_BYTE_COUNT_IN_REGISTER)

                val registerBuffer = ByteBuffer.allocate(4).apply {
                    put(dataResponseRaw[0])
                    put(dataResponseRaw[1])
                    put(dataResponseRaw[2])
                    put(0.toByte())
                }.also {
                    it.flip()
                }

                dataResponse[0] = registerBuffer.float
            }
        }
    }

    override fun checkFunctionIsError(response: ByteArray) {
        if (function == (response[Cs0202Request.FUNCTION_POSITION] or 0x80.toByte())) {
            when (response[Cs0202Request.ERROR_POSITION]) {
                0x03.toByte() -> throw LogicException("Ошибка устройства: измерение сопротивления изоляции в ручном режиме не активировано.")
                0x04.toByte() -> throw LogicException("Ошибка устройства: пробой изоляции, измерение не производилось.")
                else -> throw LogicException("Ошибка устройства: Неизвестная ошибка [${response[Cs0202Request.ERROR_POSITION]}]")
            }
        }
    }

    override fun checkDataRequestFromResponse(response: ByteArray) {}
}
