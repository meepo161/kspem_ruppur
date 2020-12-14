package ru.avem.kspem_ruppur.communication.model

interface IDeviceModel {
    val registers: Map<String, DeviceRegister>

    fun getRegisterById(idRegister: String): DeviceRegister
}
