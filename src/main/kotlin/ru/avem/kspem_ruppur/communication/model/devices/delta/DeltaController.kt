package ru.avem.kspem_ruppur.communication.model.devices.delta

import ru.avem.kspem_ruppur.communication.adapters.modbusrtu.ModbusRTUAdapter
import ru.avem.kspem_ruppur.communication.adapters.utils.ModbusRegister
import ru.avem.kspem_ruppur.communication.model.DeviceRegister
import ru.avem.kspem_ruppur.communication.model.IDeviceController
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.CONTROL_REGISTER
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.CURRENT_FREQUENCY_OUTPUT_REGISTER
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.MAX_FREQUENCY_REGISTER
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.MAX_VOLTAGE_REGISTER
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.NOM_FREQUENCY_REGISTER
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.POINT_1_FREQUENCY_REGISTER
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.POINT_1_VOLTAGE_REGISTER
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.POINT_2_FREQUENCY_REGISTER
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaModel.Companion.POINT_2_VOLTAGE_REGISTER
import ru.avem.kspem_ruppur.communication.utils.TransportException
import ru.avem.kspem_ruppur.communication.utils.TypeByteOrder
import ru.avem.kspem_ruppur.communication.utils.allocateOrderedByteBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DeltaController(
    override val name: String,
    override val protocolAdapter: ModbusRTUAdapter,
    override val id: Byte
) : IDeviceController {
    val model = DeltaModel()
    override var isResponding = false
    override var requestTotalCount = 0
    override var requestSuccessCount = 0
    override val pollingRegisters = mutableListOf<DeviceRegister>()
    override val writingMutex = Any()
    override val writingRegisters = mutableListOf<Pair<DeviceRegister, Number>>()
    override val pollingMutex = Any()

    override fun readRegister(register: DeviceRegister) {
        transactionWithAttempts {
            val modbusRegister =
                protocolAdapter.readHoldingRegisters(id, register.address, 2).map(ModbusRegister::toShort)
            register.value = allocateOrderedByteBuffer(modbusRegister, TypeByteOrder.BIG_ENDIAN, 4).float.toDouble()
        }
    }

    override fun readAllRegisters() {
        model.registers.values.forEach {
            readRegister(it)
        }
    }

    override fun <T : Number> writeRegister(register: DeviceRegister, value: T) {
        when (value) {
            is Float -> {
                val bb = ByteBuffer.allocate(4).putFloat(value).order(ByteOrder.BIG_ENDIAN)
                val registers = listOf(ModbusRegister(bb.getShort(2)), ModbusRegister(bb.getShort(0)))
                transactionWithAttempts {
                    protocolAdapter.presetMultipleRegisters(id, register.address, registers)
                }
            }
            is Int -> {
                val bb = ByteBuffer.allocate(4).putInt(value).order(ByteOrder.BIG_ENDIAN)
                val registers = listOf(ModbusRegister(bb.getShort(2)), ModbusRegister(bb.getShort(0)))
                transactionWithAttempts {
                    protocolAdapter.presetMultipleRegisters(id, register.address, registers)
                }
            }
            is Short -> {
                transactionWithAttempts {
                    protocolAdapter.presetSingleRegister(id, register.address, ModbusRegister(value))
                }
            }
            else -> {
                throw UnsupportedOperationException("Method can handle only with Float, Int and Short")
            }
        }
    }

    override fun writeRegisters(register: DeviceRegister, values: List<Short>) {
        val registers = values.map { ModbusRegister(it) }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(id, register.address, registers)
        }
    }

    override fun checkResponsibility() {
        try {
            model.registers.values.firstOrNull()?.let {
                readRegister(it)
            }
        } catch (ignored: TransportException) {
        }
    }

    override fun getRegisterById(idRegister: String) = model.getRegisterById(idRegister)


    fun startObject() {
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(CONTROL_REGISTER).address,
                listOf(ModbusRegister(0b10))
            )
        }
    }

    fun stopObject() {
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(CONTROL_REGISTER).address,
                listOf(ModbusRegister(0b1))
            )
        }
    }

    fun setObjectParams(fOut: Short, voltageP1: Short, fP1: Short) {
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(POINT_1_VOLTAGE_REGISTER).address,
                listOf(ModbusRegister(voltageP1))
            )
        }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(POINT_1_FREQUENCY_REGISTER).address,
                listOf(ModbusRegister(fP1))
            )
        }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(CURRENT_FREQUENCY_OUTPUT_REGISTER).address,
                listOf(ModbusRegister(fOut))
            )
        }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(MAX_VOLTAGE_REGISTER).address,
                listOf(ModbusRegister(52 * 10))
            )
        }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(MAX_FREQUENCY_REGISTER).address,
                listOf(ModbusRegister(50 * 100))
            )
        }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(NOM_FREQUENCY_REGISTER).address,
                listOf(ModbusRegister(50 * 100))
            )
        }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(POINT_2_VOLTAGE_REGISTER).address,
                listOf(ModbusRegister(40))
            )
        }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(
                id,
                getRegisterById(POINT_2_FREQUENCY_REGISTER).address,
                listOf(ModbusRegister(50))
            )
        }
    }
}
