package ru.avem.kspem_ruppur.controllers

import javafx.application.Platform
import javafx.scene.text.Text
import ru.avem.kspem_ruppur.communication.model.CommunicationModel
import ru.avem.kspem_ruppur.communication.model.devices.ohmmeter.APPAController.Companion.R_MODE
import ru.avem.kspem_ruppur.communication.model.devices.owen.pr.OwenPrModel
import ru.avem.kspem_ruppur.utils.LogTag
import ru.avem.kspem_ruppur.utils.Singleton.currentTestItem
import ru.avem.kspem_ruppur.utils.Toast
import ru.avem.kspem_ruppur.utils.formatRealNumber
import ru.avem.kspem_ruppur.utils.sleep
import ru.avem.kspem_ruppur.view.MainView
import tornadofx.add
import tornadofx.runLater
import tornadofx.style
import java.text.SimpleDateFormat
import kotlin.experimental.and

class Test1Controller : TestController() {
    val controller: MainViewController by inject()
    val mainView: MainView by inject()

    private var logBuffer: String? = null

    @Volatile
    var isExperimentEnded: Boolean = true

    @Volatile
    private var measuringR: Float = 0f

    @Volatile
    private var testItemR: Double = 0.0

    @Volatile
    private var measuringR1: Double = 0.0

    @Volatile
    private var measuringR2: Double = 0.0

    @Volatile
    private var measuringR3: Double = 0.0

    @Volatile
    private var currentVIU: Boolean = false

    @Volatile
    private var startButton: Boolean = false

    @Volatile
    private var stopButton: Boolean = false

    @Volatile
    private var doorSHSO: Boolean = false

    @Volatile
    private var doorZone: Boolean = false

    @Volatile
    private var platform1: Boolean = false

    @Volatile
    private var platform2: Boolean = false

    private fun appendOneMessageToLog(tag: LogTag, message: String) {
        if (logBuffer == null || logBuffer != message) {
            logBuffer = message
            appendMessageToLog(tag, message)
        }
    }

    fun appendMessageToLog(tag: LogTag, _msg: String) {
        val msg = Text("${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())} | $_msg")
        msg.style {
            fill = when (tag) {
                LogTag.MESSAGE -> tag.c
                LogTag.ERROR -> tag.c
                LogTag.DEBUG -> tag.c
            }
        }

        Platform.runLater {
            mainView.vBoxLog.add(msg)
        }
    }

    private fun startPollDevices() {
        CommunicationModel.startPoll(CommunicationModel.DeviceID.DD2, OwenPrModel.FIXED_STATES_REGISTER_1) { value ->
            startButton = value.toShort() and 1 > 0
            stopButton = value.toShort() and 2 > 0
            currentVIU = value.toShort() and 16 > 0
            doorSHSO = value.toShort() and 64 > 0
            doorZone = value.toShort() and 128 > 0
            if (currentVIU) {
                controller.cause = "Сработала токовая защита"
            }
            if (stopButton) {
                controller.cause = "Нажали кнопку СТОП"
            }
            if (doorSHSO) {
                controller.cause = "Открыта дверь ШСО"
            }
            if (doorZone) {
                controller.cause = "Открыта дверь зоны"
            }
        }
    }

    fun startTest() {
        controller.cause = ""
        testItemR = currentTestItem.xR.toDouble()

        isExperimentEnded = false

        if (controller.isExperimentRunning) {
            appendMessageToLog(LogTag.DEBUG, "Инициализация устройств")
        }

        while (!controller.isDevicesResponding() && controller.isExperimentRunning) {
            CommunicationModel.checkDevices()
            sleep(100)
        }

        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
            CommunicationModel.addWritingRegister(
                CommunicationModel.DeviceID.DD2,
                OwenPrModel.RESET_DOG,
                1.toShort()
            )
            owenPR.initOwenPR()
            startPollDevices()
            sleep(1000)
        }

        if (!startButton && controller.isExperimentRunning && controller.isDevicesResponding()) {
            runLater {
                Toast.makeText("Нажмите кнопку ПУСК").show(Toast.ToastType.WARNING)
            }
        }
        var timeToStart = 300
        while (!startButton && controller.isExperimentRunning && controller.isDevicesResponding() && timeToStart-- > 0) {
            appendOneMessageToLog(LogTag.DEBUG, "Нажмите кнопку ПУСК")
            sleep(100)
        }

        if (!startButton) {
            controller.cause = "Не нажата кнопка ПУСК"
        }

        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
            appendMessageToLog(LogTag.DEBUG, "Подготовка стенда")
            appendMessageToLog(LogTag.DEBUG, "Сбор схемы")
            owenPR.onSoundAndLight()
        }

        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
            appendMessageToLog(LogTag.DEBUG, "Подключение контакторов для измерения R AB")
            owenPR.onPuskovoi()
            sleepWhile(6)
        }

        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
            prepareAPPAForMeasureR()
            measuringR1 = formatRealNumber(appa.getR().toDouble())
            if (measuringR1 == -2.0) {
                controller.tableValuesTest1[1].resistanceAB.value = "Обрыв"
            } else {
                controller.tableValuesTest1[1].resistanceAB.value = measuringR1.toString()
            }
        }
        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
            appendMessageToLog(LogTag.DEBUG, "Подключение контакторов для измерения R BC")
            owenPR.offKM51()
            owenPR.offKM53()
            owenPR.onKM52()
            owenPR.onAppaPhaseA()
            sleepWhile(6)
        }

        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
            prepareAPPAForMeasureR()
            measuringR2 = formatRealNumber(appa.getR().toDouble())
            if (measuringR2 == -2.0) {
                controller.tableValuesTest1[1].resistanceBC.value = "Обрыв"
            } else {
                controller.tableValuesTest1[1].resistanceBC.value = measuringR2.toString()
            }
        }
        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
            appendMessageToLog(LogTag.DEBUG, "Подключение контакторов для измерения R CA")
            owenPR.offKM52()
            owenPR.offKM54()
            owenPR.onOffAppa()
            owenPR.onAppaPhaseA()
            sleepWhile(6)
        }
        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
            prepareAPPAForMeasureR()
            measuringR3 = formatRealNumber(appa.getR().toDouble())
            if (measuringR3 == -2.0) {
                controller.tableValuesTest1[1].resistanceCA.value = "Обрыв"
            } else {
                controller.tableValuesTest1[1].resistanceCA.value = measuringR3.toString()
            }
        }

//        appa.protocolAdapter.connection.disconnect()
//        sleep(5000)
//        if (controller.isExperimentRunning && controller.isDevicesResponding()) {
//            owenPR.onAPPA()
//        }

        setResult()

        finalizeExperiment()
    }

    private fun prepareAPPAForMeasureR() {
        var attempts = 10
        while (--attempts > 0 && controller.isExperimentRunning && (!appa.isResponding || appa.getMode() != R_MODE)) {
            while (!appa.isResponding && controller.isExperimentRunning) {
                owenPR.onAPPA()
                sleepWhile(10)
                appa.getMode()
                sleepWhile(1)
            }
            while (appa.getMode() != R_MODE && appa.isResponding && controller.isExperimentRunning) {
                owenPR.changeModeAPPA()
                sleepWhile(1)
            }
            sleepWhile(1)
        }
    }

    private fun sleepWhile(timeSecond: Int) {
        var timer = timeSecond * 10
        while (controller.isExperimentRunning && timer-- > 0) {
            sleep(100)
        }
    }

    private fun setResult() {
        if (!controller.isDevicesResponding()) {
            controller.tableValuesTest1[1].result.value = "Прервано"
            appendMessageToLog(LogTag.ERROR, "Испытание прервано по причине: \nпотеряна связь с устройствами")
        } else if (controller.cause.isNotEmpty()) {
            controller.tableValuesTest1[1].result.value = "Прервано"
            appendMessageToLog(LogTag.ERROR, "Испытание прервано по причине: ${controller.cause}")
        } else if ((measuringR1 < testItemR * 0.8 || measuringR1 > testItemR * 1.2)
            && (measuringR2 < testItemR * 0.8 || measuringR2 > testItemR * 1.2)
            && (measuringR3 < testItemR * 0.8 || measuringR3 > testItemR * 1.2)
        ) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nСопротивления отличаются \nболее, чем на 20%"
            )
        } else if ((measuringR1 < testItemR * 0.8 || measuringR1 > testItemR * 1.2)
            && (measuringR2 < testItemR * 0.8 || measuringR2 > testItemR * 1.2)
        ) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nСопротивления AB и BC отличаются \nболее, чем на 20%"
            )
        } else if ((measuringR1 < testItemR * 0.8 || measuringR1 > testItemR * 1.2)
            && (measuringR3 < testItemR * 0.8 || measuringR3 > testItemR * 1.2)
        ) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nСопротивления AB и BC отличаются \nболее, чем на 20%"
            )
        } else if ((measuringR1 < testItemR * 0.8 || measuringR1 > testItemR * 1.2)
            && (measuringR2 < testItemR * 0.8 || measuringR2 > testItemR * 1.2)
        ) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nСопротивления AB и CA отличаются \nболее, чем на 20%"
            )
        } else if (measuringR1 < testItemR * 0.8 || measuringR1 > testItemR * 1.2) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nСопротивление обмотки AB отличается \nболее, чем на 20%"
            )
        } else if (measuringR2 < testItemR * 0.8 || measuringR2 > testItemR * 1.2) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nСопротивление обмотки BC отличается \nболее, чем на 20%"
            )
        } else if (measuringR3 < testItemR * 0.8 || measuringR3 > testItemR * 1.2) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nСопротивление обмотки CA отличается \nболее, чем на 20%"
            )
        } else if (measuringR1 == -2.0) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nОбрыв обмотки AB"
            )
        } else if (measuringR2 == -2.0) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nОбрыв обмотки BC"
            )
        } else if (measuringR3 == -2.0) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(
                LogTag.ERROR, "Результат: \nОбрыв обмотки CA"
            )
        } else {
            controller.tableValuesTest1[1].result.value = "Годен"
            appendMessageToLog(LogTag.MESSAGE, "Испытание завершено успешно")
        }
    }

    private fun finalizeExperiment() {
        isExperimentEnded = true
        owenPR.offAllKMs()
        CommunicationModel.clearPollingRegisters()

    }
}
