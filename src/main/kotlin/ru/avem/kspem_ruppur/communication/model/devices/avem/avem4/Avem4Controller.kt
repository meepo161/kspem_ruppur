package ru.avem.kspem_ruppur.communication.model.devices.avem.avem4

import ru.avem.kspem_ruppur.communication.adapters.modbusrtu.ModbusRTUAdapter
import ru.avem.kspem_ruppur.communication.adapters.utils.ModbusRegister
import ru.avem.kspem_ruppur.communication.model.DeviceRegister
import ru.avem.kspem_ruppur.communication.model.IDeviceController
import ru.avem.kspem_ruppur.communication.utils.TransportException
import ru.avem.kspem_ruppur.communication.utils.TypeByteOrder
import ru.avem.kspem_ruppur.communication.utils.allocateOrderedByteBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Avem4Controller(
    override val name: String,
    override val protocolAdapter: ModbusRTUAdapter,
    override val id: Byte
) : IDeviceController {
    private val model = Avem4Model()
    override var isResponding = false
    override var requestTotalCount = 0
    override var requestSuccessCount = 0
    override val pollingRegisters = mutableListOf<DeviceRegister>()
    override val pollingMutex = Any()
    override val writingMutex = Any()
    override val writingRegisters = mutableListOf<Pair<DeviceRegister, Number>>()

    override fun readRegister(register: DeviceRegister) {
        transactionWithAttempts {
            when (register.valueType) {
                DeviceRegister.RegisterValueType.SHORT -> {
                    val value =
                        protocolAdapter.readInputRegisters(id, register.address, 1).first().toShort()
                    register.value = value
                }
                DeviceRegister.RegisterValueType.FLOAT -> {
                    val modbusRegister =
                        protocolAdapter.readInputRegisters(id, register.address, 4).map(ModbusRegister::toShort)
                    register.value =
                        allocateOrderedByteBuffer(modbusRegister, TypeByteOrder.BIG_ENDIAN, 4).float
                }
                DeviceRegister.RegisterValueType.INT32 -> {
                    val modbusRegister =
                        protocolAdapter.readInputRegisters(id, register.address, 4).map(ModbusRegister::toShort)
                    register.value =
                        allocateOrderedByteBuffer(modbusRegister, TypeByteOrder.BIG_ENDIAN, 4).int
                }
            }
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

    override fun readAllRegisters() {
        model.registers.values.forEach {
            readRegister(it)
        }
    }

    override fun writeRegisters(register: DeviceRegister, values: List<Short>) {
        val registers = values.map { ModbusRegister(it) }
        transactionWithAttempts {
            protocolAdapter.presetMultipleRegisters(id, register.address, registers)
        }
    }

    override fun getRegisterById(idRegister: String) = model.getRegisterById(idRegister)

    override fun checkResponsibility() {
        try {
            model.registers.values.firstOrNull()?.let {
                readRegister(it)
            }
        } catch (ignored: TransportException) {
        }
    }
}
