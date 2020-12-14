package ru.avem.kspem_ruppur.controllers

import ru.avem.kspem_ruppur.communication.model.CommunicationModel
import ru.avem.kspem_ruppur.communication.model.CommunicationModel.getDeviceById
import ru.avem.kspem_ruppur.communication.model.devices.avem.avem4.Avem4Controller
import ru.avem.kspem_ruppur.communication.model.devices.avem.avem7.Avem7Controller
import ru.avem.kspem_ruppur.communication.model.devices.bris.m4122.M4122Controller
import ru.avem.kspem_ruppur.communication.model.devices.delta.DeltaController
import ru.avem.kspem_ruppur.communication.model.devices.ohmmeter.APPAController
import ru.avem.kspem_ruppur.communication.model.devices.owen.pr.OwenPrController
import tornadofx.Controller

abstract class TestController : Controller() {
    protected val owenPR = getDeviceById(CommunicationModel.DeviceID.DD2) as OwenPrController
    protected val avem7 = getDeviceById(CommunicationModel.DeviceID.PV21) as Avem7Controller
    protected val avem4 = getDeviceById(CommunicationModel.DeviceID.PV24) as Avem4Controller
    protected val deltaCP = getDeviceById(CommunicationModel.DeviceID.UZ91) as DeltaController
    protected val appa = getDeviceById(CommunicationModel.DeviceID.PR61) as APPAController
    protected val bris = getDeviceById(CommunicationModel.DeviceID.PR65) as M4122Controller

}
