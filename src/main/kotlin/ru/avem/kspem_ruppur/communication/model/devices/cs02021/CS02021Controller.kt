package ru.avem.kspem_ruppur.communication.model.devices.cs02021

import org.slf4j.LoggerFactory
import ru.avem.kspem_ruppur.communication.adapters.CRC16
import ru.avem.kspem_ruppur.communication.adapters.serial.SerialAdapter
import ru.avem.kspem_ruppur.communication.model.DeviceRegister
import ru.avem.kspem_ruppur.communication.model.IDeviceController
import ru.avem.kspem_ruppur.communication.model.devices.ohmmeter.APPAResponse
import ru.avem.kspem_ruppur.utils.Constants
import ru.avem.kspem_ruppur.utils.Logger
import ru.avem.kspem_ruppur.utils.sleep
import java.nio.ByteBuffer

class CS02021Controller(
    override val name: String,
    override val protocolAdapter: SerialAdapter,
    override val id: Byte
) : IDeviceController {
    val model = CS020201Model()
    override var requestTotalCount = 0
    override var requestSuccessCount = 0
    override val pollingRegisters = mutableListOf<DeviceRegister>()
    override val writingMutex = Any()

    override val writingRegisters = mutableListOf<Pair<DeviceRegister, Number>>()
    override val pollingMutex = Any()
    lateinit var response: APPAResponse

    companion object {
        const val PRODUCT_NAME = "CP2102 USB to UART M4122"
        private val logger = LoggerFactory.getLogger(this::class.java)
        private val WATCHDOG_REQ = byteArrayOf(0x55)
        private val EMPTY_WATCHDOG = byteArrayOf(0x00)
        private val LOCKER = Any()

        val BREAK = -2
        val NOT_RESPONDING = -3
    }

    fun setVoltage(u: Int): Boolean {
        synchronized(LOCKER) {
            val byteU = (u / 10).toByte()
            val outputBuffer = ByteBuffer.allocate(5)
                .put(id)
                .put(0x01.toByte())
                .put(byteU)
            CRC16.signReversWithSlice(outputBuffer)
            protocolAdapter.write(outputBuffer.array())
            val inputArray = ByteArray(40)
            val inputBuffer = ByteBuffer.allocate(40)
            var attempt = 0
            var frameSize = 0
            do {
                sleep(2)
                frameSize = protocolAdapter.read(inputArray)
                inputBuffer.put(inputArray, 0, frameSize)
            } while (inputBuffer.position() < 5 && ++attempt < 10)
            return frameSize > 0
        }
    }

    fun readData(): FloatArray {
        synchronized(LOCKER) {
            val data = FloatArray(4)
            val outputBuffer = ByteBuffer.allocate(5)
                .put(id)
                .put(0x07.toByte())
                .put(0x71.toByte())
                .put(0x64.toByte())
                .put(0x7F.toByte())
            val inputBuffer = ByteBuffer.allocate(40)
            val finalBuffer = ByteBuffer.allocate(40)
            inputBuffer.clear()
            protocolAdapter.write(outputBuffer.array())
            val inputArray = ByteArray(40)
            var attempt = 0
            do {
                sleep(2)
                val frameSize: Int = protocolAdapter.read(inputArray)
                inputBuffer.put(inputArray, 0, frameSize)
            } while (inputBuffer.position() < 16 && ++attempt < 15)
            if (inputBuffer.position() == 16) {
                inputBuffer.flip().position(2)
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(0.toByte())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(0.toByte())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(0.toByte())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(inputBuffer.get())
                finalBuffer.put(0.toByte())
                finalBuffer.flip()
                data[0] = finalBuffer.float
                data[1] = finalBuffer.float
                data[2] = finalBuffer.float
                data[3] = finalBuffer.float
            }
            return data
        }
    }

    override var isResponding: Boolean = false
        get() {
            synchronized(LOCKER) {
                val outputBuffer = ByteBuffer.allocate(5)
                    .put(id)
                    .put(0x07.toByte())
                    .put(0x71.toByte())
                    .put(0x64.toByte())
                    .put(0x7F.toByte())
                val inputBuffer = ByteBuffer.allocate(40)
                inputBuffer.clear()
                val writtenBytes: Int = protocolAdapter.write(outputBuffer.array())
                Logger.withTag("TAG").log("writtenBytes=$writtenBytes")
                val inputArray = ByteArray(40)
                var attempt = 0
                do {
                    sleep(2)
                    val frameSize: Int = protocolAdapter.read(inputArray)
                    inputBuffer.put(inputArray, 0, frameSize)
                } while (inputBuffer.position() < 16 && ++attempt < 15)
                return inputBuffer.position() >= 16
            }
        }

    override fun getRegisterById(idRegister: String): DeviceRegister {
        TODO("Not yet implemented")
    }

    override fun checkResponsibility() {
        TODO("Not yet implemented")
    }
}