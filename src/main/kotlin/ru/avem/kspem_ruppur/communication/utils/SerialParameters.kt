package ru.avem.kspem_ruppur.communication.utils

data class SerialParameters(
    val dataBits: Int,
    val parity: Int,
    val stopBits: Int,
    val baudrate: Int
)
