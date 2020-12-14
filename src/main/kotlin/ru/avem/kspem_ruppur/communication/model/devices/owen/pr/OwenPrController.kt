package ru.avem.kspem_ruppur.communication.model.devices.owen.pr

import ru.avem.kspem_ruppur.communication.adapters.modbusrtu.ModbusRTUAdapter
import ru.avem.kspem_ruppur.communication.adapters.utils.ModbusRegister
import ru.avem.kspem_ruppur.communication.model.DeviceRegister
import ru.avem.kspem_ruppur.communication.model.IDeviceController
import ru.avem.kspem_ruppur.communication.utils.TransportException
import ru.avem.kspem_ruppur.utils.sleep
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.pow

class OwenPrController(
    override val name: String,
    override val protocolAdapter: ModbusRTUAdapter,
    override val id: Byte
) : IDeviceController {
    val model = OwenPrModel()
    override var isResponding = false
    override var requestTotalCount = 0
    override var requestSuccessCount = 0
    override val pollingRegisters = mutableListOf<DeviceRegister>()
    override val writingMutex = Any()
    override val writingRegisters = mutableListOf<Pair<DeviceRegister, Number>>()
    override val pollingMutex = Any()

    var outMask: Short = 0

    override fun readRegister(register: DeviceRegister) {
        isResponding = try {
            transactionWithAttempts {
                val modbusRegister =
                    protocolAdapter.readHoldingRegisters(id, register.address, 1).map(ModbusRegister::toShort)
                register.value = modbusRegister.first()
            }
            true
        } catch (e: TransportException) {
            false
        }
    }


    override fun readAllRegisters() {
        model.registers.values.forEach {
            readRegister(it)
        }
    }

    override fun <T : Number> writeRegister(register: DeviceRegister, value: T) {
        isResponding = try {
            when (value) {
                is Float -> {
                    val bb = ByteBuffer.allocate(4).putFloat(value).order(ByteOrder.LITTLE_ENDIAN)
                    val registers = listOf(ModbusRegister(bb.getShort(2)), ModbusRegister(bb.getShort(0)))
                    transactionWithAttempts {
                        protocolAdapter.presetMultipleRegisters(id, register.address, registers)
                    }
                }
                is Int -> {
                    val bb = ByteBuffer.allocate(4).putInt(value).order(ByteOrder.LITTLE_ENDIAN)
                    val registers = listOf(ModbusRegister(bb.getShort(2)), ModbusRegister(bb.getShort(0)))
                    transactionWithAttempts {
                        protocolAdapter.presetMultipleRegisters(id, register.address, registers)
                    }
                }
                is Short -> {
                    transactionWithAttempts {
                        protocolAdapter.presetMultipleRegisters(id, register.address, listOf(ModbusRegister(value)))
                    }
                }
                else -> {
                    throw UnsupportedOperationException("Method can handle only with Float, Int and Short")
                }
            }
            true
        } catch (e: TransportException) {
            false
        }
    }

    override fun writeRegisters(register: DeviceRegister, values: List<Short>) {
        val registers = values.map { ModbusRegister(it) }
        isResponding = try {
            transactionWithAttempts {
                protocolAdapter.presetMultipleRegisters(id, register.address, registers)
            }
            true
        } catch (e: TransportException) {
            false
        }
    }

    override fun checkResponsibility() {
        model.registers.values.firstOrNull()?.let {
            readRegister(it)
        }
    }

    override fun getRegisterById(idRegister: String) = model.getRegisterById(idRegister)

    private fun onBitInRegister(register: DeviceRegister, bitPosition: Short) {
        val nor = bitPosition - 1
        outMask = outMask or 2.0.pow(nor).toInt().toShort()
        writeRegister(register, outMask)
        sleep(300)
    }

    private fun offBitInRegister(register: DeviceRegister, bitPosition: Short) {
        val nor = bitPosition - 1
        outMask = outMask and 2.0.pow(nor).toInt().inv().toShort()
        writeRegister(register, outMask)
        sleep(300)
    }


    fun initOwenPR() {
        writeRegister(getRegisterById(OwenPrModel.RES_REGISTER), 1)
    }

    fun resetKMS() {
        writeRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 0)
    }

    fun onAPPA() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
        sleep(100)
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
    }

    fun rebootAPPA() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
        sleep(100)
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
        sleep(100)
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
    }

    fun onPuskovoi() {
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 2)
    }

    fun onAppaPhaseA() {
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 3)
    }

    fun changeModeAPPA() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 4)
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 4)
        sleep(100)
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 4)
    }

    fun onAppaPhaseBIntoC() {
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 5)
    }

    fun onSoundAndLight() {
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 6)
        sleep(3000)
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 6)
    }

    fun onAppaPhaseBIntoA() {
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 7)
    }

    fun onAppaPhaseC() {
        onBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 8)
    }

    fun offDO1() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 1)
    }

    fun offDO2() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 2)
    }

    fun offDO3() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 3)
    }

    fun offDO4() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 4)
    }

    fun offDO5() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 5)
    }

    fun offDO6() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 6)
    }
    fun offDO7() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 6)
    }

    fun offDO8() {
        offBitInRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 8)
    }

    fun offAllKMs() {
        writeRegister(getRegisterById(OwenPrModel.KMS1_REGISTER), 0)
        outMask = 0
    }
}
