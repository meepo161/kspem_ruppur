package ru.avem.kspem_ruppur.utils

import ru.avem.kspem_ruppur.database.entities.Protocol
import ru.avem.kspem_ruppur.database.entities.TestObjectsType


object Singleton {
    lateinit var currentProtocol: Protocol
    lateinit var currentTestItem: TestObjectsType
}
