package ru.avem.kspem_ruppur.app

import javafx.scene.image.Image
import javafx.scene.input.KeyCombination
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.LoggerFactory
import ru.avem.kspem_ruppur.database.validateDB
import ru.avem.kspem_ruppur.view.MainView
import ru.avem.kspem_ruppur.view.Styles
import tornadofx.App
import tornadofx.FX

class Ruppur : App(MainView::class, Styles::class) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        var isAppRunning = true
        var forOneInit = true
        var forOneInitComboBox = true
    }

    override fun init() {
        logger.debug("init")
        validateDB()
    }

    override fun start(stage: Stage) {
        stage.isFullScreen = true
        stage.isResizable = false
        stage.initStyle(StageStyle.TRANSPARENT)
        stage.fullScreenExitKeyCombination = KeyCombination.NO_MATCH
        super.start(stage)
        FX.primaryStage.icons += Image("icon.png")
    }


    override fun stop() {
        isAppRunning = false
    }
}
