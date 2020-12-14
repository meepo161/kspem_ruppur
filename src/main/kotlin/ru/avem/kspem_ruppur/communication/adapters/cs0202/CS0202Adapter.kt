package ru.avem.kspem_ruppur.communication.adapters.cs0202

import ru.avem.kspem_ruppur.communication.Connection
import ru.avem.kspem_ruppur.communication.adapters.Adapter
import ru.avem.kspem_ruppur.communication.adapters.cs0202.requests.*
import ru.avem.kspem_ruppur.communication.utils.ConnectionException
import ru.avem.kspem_ruppur.communication.utils.InvalidCommandException

class CS0202Adapter(override val connection: Connection) : Adapter {
    fun readRegister(
        address: Int,
        dataRequestRequire: Byte,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        customBaudrate: Int?,
        functionCode: Byte?
    ): FloatArray {
        try {
            when (functionCode) {
                ReadEeprom.FUNCTION_CODE -> {
                    return readEeprom(
                        deviceId = address,
                        frameBetweenTimeout = frameBetweenTimeout,
                        frameAfterTimeout = frameAfterTimeout,
                        customBaudrate = customBaudrate
                    )
                }
                ReadResult.FUNCTION_CODE -> {
                    return readResult(
                        deviceId = address,
                        dataRequestRequire = dataRequestRequire,
                        customBaudrate = customBaudrate,
                        frameBetweenTimeout = frameBetweenTimeout,
                        frameAfterTimeout = frameAfterTimeout
                    )
                }
                else -> {
                    throw InvalidCommandException()
                }
            }
        } catch (ex: InvalidCommandException) {
            throw InvalidCommandException(if (ex.message.isNullOrEmpty()) "Ошибка модели: ${ex.message}невозможно выполнить команду $dataRequestRequire" else ex.message)
        } catch (ex: ConnectionException) {
            throw ex
        }
    }

    private fun doRequestForResponse(
        request: Cs0202Request,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        customBaudrate: Int?
    ): ByteArray {
        val response = ByteArray(request.getResponseSize())
        connection.request(
            writeBuffer = request.getRequestBytes(),
            readBuffer = response,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout,
            customBaudrate = customBaudrate
        )

        return response
    }

    private fun readEeprom(deviceId: Int, frameBetweenTimeout: Long, frameAfterTimeout: Long, customBaudrate: Int?
    ): FloatArray {
        val request = ReadEeprom(deviceId = deviceId.toByte())

        val response = doRequestForResponse(
            request = request,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout,
            customBaudrate = customBaudrate
        )
        request.parseResponse(response)

        return request.dataResponse
    }

    private fun readResult(
        deviceId: Int,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        dataRequestRequire: Byte,
        customBaudrate: Int?
    ): FloatArray {
        val request = ReadResult(
            deviceId = deviceId.toByte(),
            dataRequestRequire = dataRequestRequire
        )

        val response = doRequestForResponse(
            request = request,
            customBaudrate = customBaudrate,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout
        )
        request.parseResponse(response)

        return request.dataResponse
    }

    fun writeRegister(
        address: Int,
        functionCode: Byte?,
        registerData: Int,
        dataRequestRequire: Byte,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        newAddressRequire: Int,
        customBaudrate: Int?
    ) {
        try {
            when (functionCode) {
                MeasurementAutomatic.FUNCTION_CODE -> {
                    measurementAutomatic(
                        deviceId = address,
                        voltageRequire = registerData / 10,
                        customBaudrate = customBaudrate,
                        frameBetweenTimeout = frameBetweenTimeout,
                        frameAfterTimeout = frameAfterTimeout
                    )
                }
                MeasurementManual.FUNCTION_CODE -> {
                    measurementManual(
                        deviceId = address,
                        voltageRequire = registerData / 10,
                        customBaudrate = customBaudrate,
                        frameBetweenTimeout = frameBetweenTimeout,
                        frameAfterTimeout = frameAfterTimeout
                    )
                }
                MeasurementStop.FUNCTION_CODE -> {
                    measurementStop(
                        deviceId = address,
                        dataRequestRequire = dataRequestRequire,
                        customBaudrate = customBaudrate,
                        frameBetweenTimeout = frameBetweenTimeout,
                        frameAfterTimeout = frameAfterTimeout
                    )
                }
                MeasurementVoltage.FUNCTION_CODE -> {
                    measurementVoltage(
                        deviceId = address,
                        customBaudrate = customBaudrate,
                        frameBetweenTimeout = frameBetweenTimeout,
                        frameAfterTimeout = frameAfterTimeout
                    )
                }
                ImitationPressedButtons.FUNCTION_CODE -> {
                    imitationPressedButtons(
                        deviceId = address,
                        dataRequestRequire = dataRequestRequire,
                        customBaudrate = customBaudrate,
                        frameBetweenTimeout = frameBetweenTimeout,
                        frameAfterTimeout = frameAfterTimeout
                    )
                }
                ChangeAddress.FUNCTION_CODE -> {
                    changeAddress(
                        deviceId = address,
                        newAddressRequire = newAddressRequire,
                        customBaudrate = customBaudrate,
                        frameBetweenTimeout = frameBetweenTimeout,
                        frameAfterTimeout = frameAfterTimeout
                    )
                }
                else -> {
                    throw InvalidCommandException()
                }
            }
        } catch (ex: InvalidCommandException) {
            throw InvalidCommandException(if (ex.message.isNullOrEmpty()) "Ошибка модели: ${ex.message}невозможно выполнить команду $dataRequestRequire" else ex.message)
        } catch (ex: ConnectionException) {
            throw ex
        }
    }

    private fun measurementAutomatic(
        deviceId: Int,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        voltageRequire: Int,
        customBaudrate: Int?
    ) {
        val request = MeasurementAutomatic(
            deviceId = deviceId.toByte(),
            measurementVoltageRequire = voltageRequire
        )

        val response = doRequestForResponse(
            request = request,
            customBaudrate = customBaudrate,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout
        )
        request.parseResponse(response)
    }

    private fun measurementManual(
        deviceId: Int,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        voltageRequire: Int,
        customBaudrate: Int?
    ) {
        val request = MeasurementManual(
            deviceId = deviceId.toByte(),
            measurementVoltageRequire = voltageRequire
        )

        val response = doRequestForResponse(
            request = request,
            customBaudrate = customBaudrate,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout
        )
        request.parseResponse(response)
    }

    private fun measurementStop(
        deviceId: Int,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        dataRequestRequire: Byte,
        customBaudrate: Int?
    ) {
        val request = MeasurementStop(deviceId.toByte(), dataRequestRequire)

        val response = doRequestForResponse(
            request = request,
            customBaudrate = customBaudrate,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout
        )
        request.parseResponse(response)
    }

    private fun measurementVoltage(
        deviceId: Int,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        customBaudrate: Int?
    ) {
        val request = MeasurementVoltage(deviceId = deviceId.toByte())

        val response = doRequestForResponse(
            request = request,
            customBaudrate = customBaudrate,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout
        )
        request.parseResponse(response)
    }

    private fun imitationPressedButtons(
        deviceId: Int,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        dataRequestRequire: Byte,
        customBaudrate: Int?
    ) {
        val request = ImitationPressedButtons(
            deviceId = deviceId.toByte(),
            dataRequestRequire = dataRequestRequire
        )

        val response = doRequestForResponse(
            request = request,
            customBaudrate = customBaudrate,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout
        )
        request.parseResponse(response)
    }

    private fun changeAddress(
        deviceId: Int,
        frameBetweenTimeout: Long,
        frameAfterTimeout: Long,
        newAddressRequire: Int,
        customBaudrate: Int?
    ) {
        val request = ChangeAddress(
            deviceId = deviceId.toByte(),
            newAddressRequire = newAddressRequire
        )

        val response = doRequestForResponse(
            request = request,
            customBaudrate = customBaudrate,
            frameBetweenTimeout = frameBetweenTimeout,
            frameAfterTimeout = frameAfterTimeout
        )
        request.parseResponse(response)
    }
}
