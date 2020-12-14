package ru.avem.kspem_ruppur.communication.model.devices.mitsubishi.fraf740

import ru.avem.kspem_ruppur.communication.model.DeviceRegister
import ru.avem.kspem_ruppur.communication.model.IDeviceModel

class FRAF740Model : IDeviceModel {
    companion object {
        const val CONTROL_REGISTER = "CONTROL_REGISTER"
        const val CURRENT_FREQUENCY_REGISTER = "CURRENT_FREQUENCY_REGISTER"
        const val MAX_FREQUENCY_REGISTER = "MAX_FREQUENCY_REGISTER"
        const val MAX_VOLTAGE_REGISTER = "MAX_VOLTAGE_REGISTER"
    }

    override val registers: Map<String, DeviceRegister> = mapOf(
        CONTROL_REGISTER to DeviceRegister(8, DeviceRegister.RegisterValueType.SHORT),
        CURRENT_FREQUENCY_REGISTER to DeviceRegister(14, DeviceRegister.RegisterValueType.SHORT),
        MAX_FREQUENCY_REGISTER to DeviceRegister(1002, DeviceRegister.RegisterValueType.SHORT),
        MAX_VOLTAGE_REGISTER to DeviceRegister(1018, DeviceRegister.RegisterValueType.SHORT)
    )

    override fun getRegisterById(idRegister: String) =
        registers[idRegister] ?: error("Такого регистра нет в описанной карте $idRegister")
}