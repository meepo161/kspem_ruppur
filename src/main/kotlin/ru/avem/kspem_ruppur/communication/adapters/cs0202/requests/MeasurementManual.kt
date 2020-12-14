package ru.avem.kspem_ruppur.communication.adapters.cs0202.requests

import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_CRC
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_DATA_RESPONSE
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_DEVICE_ID
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.BYTE_SIZE_OF_FUNCTION
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.Cs0202Request.Companion.REQUEST_SIZE
import ru.avem.kspem_ruppur.communication.adapters.cs0202.utils.CRC
import ru.avem.kspem_ruppur.communication.utils.InvalidCommandException
import ru.avem.kspem_ruppur.communication.utils.LogicException
import java.nio.ByteBuffer
import kotlin.experimental.or

class MeasurementManual(override val deviceId: Byte, measurementVoltageRequire: Int) : Cs0202Request {
    companion object {
        const val FUNCTION_CODE: Byte = 0x02
    }

    override val function: Byte = FUNCTION_CODE
    override val dataRequest: Byte = checkDataRequest(measurementVoltageRequire)

    private fun checkDataRequest(dataRequestRequire: Int) =
        when (dataRequestRequire) {
            in 10..250 -> ((dataRequestRequire / 5).toByte() * 5).toByte()
            else -> throw InvalidCommandException("Ошибка команды: предпринята попытка установить некорректное напряжение измерения [${dataRequestRequire}], который должен быть  (10..250)x10 В")
        }

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
            when (response[Cs0202Request.ERROR_POSITION]) {
                0x01.toByte() -> throw LogicException("Ошибка устройства: Не корректное значение Utest, процесс измерения сопротивления изоляции не активизирован.")
                0x03.toByte() -> throw LogicException("Ошибка устройства: Напряжение в цепи измерения, процесс измерения сопротивления изоляции не активизирован.")

                else -> throw LogicException("Ошибка устройства: Неизвестная ошибка [${response[Cs0202Request.ERROR_POSITION]}]")
            }
        }
    }
}
