package ru.avem.kspem_ruppur.controllers

import javafx.application.Platform
import javafx.scene.text.Text
import ru.avem.kspem_ruppur.communication.model.CommunicationModel
import ru.avem.kspem_ruppur.communication.model.devices.bris.m4122.M4122Controller
import ru.avem.kspem_ruppur.communication.model.devices.bris.m4122.M4122Controller.Companion.BREAK
import ru.avem.kspem_ruppur.communication.model.devices.bris.m4122.M4122Controller.Companion.NOT_RESPONDING
import ru.avem.kspem_ruppur.communication.model.devices.owen.pr.OwenPrModel
import ru.avem.kspem_ruppur.utils.LogTag
import ru.avem.kspem_ruppur.utils.Singleton.currentTestItem
import ru.avem.kspem_ruppur.utils.Toast
import ru.avem.kspem_ruppur.utils.sleep
import ru.avem.kspem_ruppur.view.MainView
import tornadofx.add
import tornadofx.runLater
import tornadofx.style
import java.text.SimpleDateFormat
import kotlin.experimental.and

class Test2Controller : TestController() {
    val controller: MainViewController by inject()
    val mainView: MainView by inject()

    private var logBuffer: String? = null

    @Volatile
    var isExperimentEnded: Boolean = true

    @Volatile
    private var measuringR: Double = 0.0

    @Volatile
    private var testItemR: Double = 0.0

    @Volatile
    private var currentVIU: Boolean = false

    @Volatile
    private var startButton: Boolean = false

    @Volatile
    private var stopButton: Boolean = false

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

    private fun appendMessageToLog(tag: LogTag, _msg: String) {
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
            currentVIU = value.toShort() and 16 > 0
            startButton = value.toShort() and 64 > 0
            stopButton = value.toShort() and 128 > 0
            if (currentVIU) {
                controller.cause = "Сработала токовая защита"
            }
            if (stopButton) {
                controller.cause = "Нажали кнопку СТОП"
            }
        }
        CommunicationModel.startPoll(CommunicationModel.DeviceID.DD2, OwenPrModel.INSTANT_STATES_REGISTER_2) { value ->
            platform1 = value.toShort() and 4 > 0
            platform2 = value.toShort() and 2 > 0

            if (mainView.textFieldPlatform.text == "Платформа 1" && !platform1) {
                controller.cause = "Не закрыта крышка платформы 1"
            }
            if (mainView.textFieldPlatform.text == "Платформа 2" && !platform2) {
                controller.cause = "Не закрыта крышка платформы 2"
            }
            if (platform1 && platform2) {
                controller.cause = "Ошибка в работе концевиков"
            }
        }
    }

    fun startTest() {
        controller.cause = ""
        testItemR = currentTestItem.rIsolation.toDouble()
        Platform.runLater {
            controller.tableValuesTest2[1].result.value = ""
        }

        isExperimentEnded = false

        if (controller.isExperimentRunning) {
            appendMessageToLog(LogTag.DEBUG, "Инициализация устройств")
        }

        while (!controller.isDevicesResponding() && controller.isExperimentRunning) {
            CommunicationModel.checkDevices()
            sleep(100)
        }

        if (controller.isExperimentRunning) {
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

        if (controller.isExperimentRunning) {
            appendMessageToLog(LogTag.DEBUG, "Подготовка стенда")
            appendMessageToLog(LogTag.DEBUG, "Сбор схемы")
            owenPR.onSoundAndLight()
            appendMessageToLog(LogTag.DEBUG, "Подключение контакторов для измерения R")

            if (mainView.textFieldPlatform.text == "Платформа 1") {
                owenPR.onKM41()
            } else if (mainView.textFieldPlatform.text == "Платформа 2") {
                owenPR.onKM42()
            }
        }

        if (controller.isExperimentRunning) {
            appendMessageToLog(LogTag.DEBUG, "Измерение R")
            appendMessageToLog(LogTag.DEBUG, "Дождитесь завершения...")
            var attempts = 10
            while (!bris.isResponding && attempts-- > 0) {
                bris.resetWatchdog()
                sleep(2000)
            }

            if (bris.isResponding) {
                measuringR =
                    bris.setVoltageAndStartMeasuring(1000, M4122Controller.MeasuringType.RESISTANCE).toDouble()
                when (measuringR) {
                    BREAK.toDouble() -> controller.tableValuesTest2[1].resistanceR.value = "Обрыв"
                    NOT_RESPONDING.toDouble() -> controller.tableValuesTest2[1].resistanceR.value = "Не отвечает"
                    else -> controller.tableValuesTest2[1].resistanceR.value = (measuringR / 1000).toString()
                }
            } else {
                controller.cause = "БРИС не отвечает"
            }
        }

        setResult()

        finalizeExperiment()
    }

    private fun setResult() {
        if (!controller.isDevicesResponding()) {
            controller.tableValuesTest2[1].result.value = "Прервано"
            appendMessageToLog(LogTag.ERROR, "Испытание прервано по причине: \nпотеряна связь с устройствами RS-485")
        } else if (controller.cause.isNotEmpty()) {
            controller.tableValuesTest2[1].result.value = "Прервано"
            appendMessageToLog(LogTag.ERROR, "Испытание прервано по причине: \n${controller.cause}")
        } else if (!bris.isResponding) {
            controller.tableValuesTest2[1].result.value = "Прервано"
            appendMessageToLog(LogTag.ERROR, "Испытание прервано по причине: \nпотеряна связь с устройством БРИС")
        } else if (measuringR == BREAK.toDouble()) {
            controller.tableValuesTest2[1].result.value = "Не годен"
            appendMessageToLog(LogTag.ERROR, "Испытание неуспешно по причине: Обрыв")
        } else if (measuringR < testItemR) {
            controller.tableValuesTest1[1].result.value = "Не годен"
            appendMessageToLog(LogTag.ERROR, "Результат: \nСопротивление меньше, чем заданное")
        } else {
            controller.tableValuesTest2[1].result.value = "Годен"
            appendMessageToLog(LogTag.MESSAGE, "Испытание завершено успешно")
        }
    }

    private fun finalizeExperiment() {
        owenPR.offAllKMs()
        CommunicationModel.clearPollingRegisters()
        isExperimentEnded = true
    }
}
