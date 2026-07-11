package ink.meodinger.lpfx.component.properties

import ink.meodinger.lpfx.*
import ink.meodinger.lpfx.component.CLabel
import ink.meodinger.lpfx.component.CLabelPane
import ink.meodinger.lpfx.component.common.CColorPicker
import ink.meodinger.lpfx.component.common.CComboBox
import ink.meodinger.lpfx.component.common.CInputLabel
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.options.Preference
import ink.meodinger.lpfx.options.Settings
import ink.meodinger.lpfx.input.*
import ink.meodinger.lpfx.type.TransFile
import ink.meodinger.lpfx.util.color.isColorHex
import ink.meodinger.lpfx.util.color.toHexRGB
import ink.meodinger.lpfx.util.component.*
import ink.meodinger.lpfx.util.doNothing
import ink.meodinger.lpfx.util.property.minus
import ink.meodinger.lpfx.util.property.onChange
import ink.meodinger.lpfx.util.property.onNew
import ink.meodinger.lpfx.util.string.emptyString
import ink.meodinger.lpfx.util.string.isMathematicalDecimal

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextFlow
import javafx.util.Duration
import javafx.util.StringConverter
import java.awt.Desktop
import java.net.URI
import kotlin.math.ceil


/**
 * Author: Meodinger
 * Date: 2021/8/25
 * Have fun with my code!
 */

/**
 * A Dialog Singleton for settings
 */
class DialogSettings : AbstractPropertiesDialog() {

    companion object {
        private const val gRowShift = 1
        private const val rRowShift = 1
        private const val qRowShift = 1
        private const val rIsFrom = "C_Is_From"
        private const val rRuleIndex = "C_Rule_Index"

        /**
         * Supported languages enumeration
         */
        private enum class SupportedLanguage(val code: String, val displayName: String) {
            ENGLISH("en", "English"),
            SIMPLIFIED_CHINESE("zh_CN", "简体中文"),
            TRADITIONAL_CHINESE("zh_TW", "繁體中文");
        }

        /**
         * 根据语言代码获取在languageCombo中的索引位置
         */
        private fun getLanguageIndex(languageCode: String): Int {
            return SupportedLanguage.entries.indexOfFirst { it.code == languageCode }.takeIf { it >= 0 } ?: 0
        }

        /**
         * 根据languageCombo中的索引位置获取语言代码
         */
        private fun getLanguageCode(index: Int): String {
            return SupportedLanguage.entries.getOrNull(index)?.code ?: SupportedLanguage.ENGLISH.code
        }
    }

    private val gGridPane = GridPane().apply {
        alignment = Pos.TOP_CENTER
        padding = Insets(16.0)
        vgap = 16.0
        hgap = 16.0
    }
    private val gLabelHint = Label(I18N["settings.group.hint"])
    private val gLabelName = Label(I18N["settings.group.name"])
    private val gLabelColor = Label(I18N["settings.group.color"])
    private val gLabelCreate = Label(I18N["settings.group.is_create_on_new"])

    private val rGridPane = GridPane().apply {
        alignment = Pos.TOP_CENTER
        padding = Insets(16.0)
        vgap = 16.0
        hgap = 16.0
    }
    private val rLabelHint = Label(I18N["settings.ligature.hint"])
    private val rLabelFrom = Label(I18N["settings.ligature.from"])
    private val rLabelTo = Label(I18N["settings.ligature.to"])

    private val qGridPane = GridPane().apply {
        alignment = Pos.TOP_CENTER
        padding = Insets(16.0)
        vgap = 16.0
        hgap = 32.0
    }
    private val qLabelHint = Label(I18N["settings.quick_input.hint"])

    private val sGridPane = GridPane().apply {
        alignment = Pos.TOP_CENTER
        padding = Insets(16.0)
        vgap = 12.0
        hgap = 16.0
    }
    private val sConflictLabel = Label().apply {
        textFill = Color.FIREBRICK
        isWrapText = true
    }
    private val shortcutEdits = mutableMapOf<String, ShortcutGesture>()
    private val shortcutConflictProperty = SimpleBooleanProperty(false)

    private val mComboInput = CComboBox<ViewMode>()
    private val mComboLabel = CComboBox<ViewMode>()
    private val mComboScale = CComboBox<CLabelPane.NewPictureScale>()
    private val mCheckWheel = CheckBox(I18N["settings.mode.use_wheel"])

    private val lCLabel = CLabel(labelIndex = 8, labelColor = Color.RED)
    private val lLabelPane = AnchorPane()
    private val lSliderRadius = Slider()
    private val lSliderAlpha = Slider()
    private val lLabelRadius = CInputLabel()
    private val lLabelAlpha = CInputLabel()
    private val lCheckTextOpaque = CheckBox(I18N["settings.label.text_opaque"])
    private val lCheckSelectedStroke = CheckBox(I18N["settings.label.selected_stroke"])

    private val xPrismMode = CComboBox<PrismMode>()
    private val xCheckUpdate = CheckBox(I18N["settings.other.auto_check_upd"])
    private val xCheckAutoOp = CheckBox(I18N["settings.other.auto_open_last"])
    private val xCheckBackup = CheckBox(I18N["settings.other.auto_check_backup"])
    private val xCheckInstTr = CheckBox(I18N["settings.other.inst_trans"])
    private val xCheckFormat = CheckBox(I18N["settings.other.check_format"])
    private val xCheckUseMeo = CheckBox(I18N["settings.other.meo_default"])
    private val xCheckUseTmp = CheckBox(I18N["settings.other.template.enable"])
    private val xFieldTemplate = TextField()

    // Tool
    private val tComboTranslationAPI = CComboBox<TranslationAPI>()
    private val tCheckUseCustomBaiduKey = CheckBox(I18N["settings.tool.translate_keys.enable"])
    private val tFieldBaiduTranslateKey = TextField()
    private val tFieldBaiduTranslateAppId = TextField()
    private val tTranslateTextFlow = TextFlow().apply {
        children += Text(I18N["settings.tool.fan_hua_ji.description.prefix"])
        children += Hyperlink(I18N["settings.tool.fan_hua_ji.description.link_text"]).apply {
            setOnAction {
                val desktop = Desktop.getDesktop()
                if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI(INFO["fanHuaJi.url"]))
                }
            }
        }
        children += Text(I18N["settings.tool.fan_hua_ji.description.suffix"])
    }

    // OTHER
    private val languageCombo = CComboBox<String>()
    private val languageLabel = Label(I18N["settings.other.language"])


    init {
        title = I18N["settings.title"]
        dialogPane.prefWidth = 600.0
        dialogPane.prefHeight = 480.0
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        (dialogPane.lookupButton(ButtonType.OK) as Button).disableProperty().bind(shortcutConflictProperty)
        dialogPane.withContent(TabPane()) {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            add(I18N["settings.group.title"]) {
                withContent(BorderPane()) {
                    val stackPane = StackPane(gGridPane)
                    val scrollPane = ScrollPane(stackPane)
                    stackPane.prefWidthProperty().bind(scrollPane.widthProperty() - 16.0)

                    center(scrollPane) { style = "-fx-background-color:transparent;" }
                    bottom(HBox()) {
                        alignment = Pos.CENTER_RIGHT
                        padding = Insets(16.0, 8.0, 8.0, 16.0)
                        add(Button(I18N["settings.group.add"])) { does { createGroupRow() } }
                    }
                }
            }
            add(I18N["settings.ligature.title"]) {
                withContent(BorderPane()) {
                    val stackPane = StackPane(rGridPane)
                    val scrollPane = ScrollPane(stackPane)
                    stackPane.prefWidthProperty().bind(scrollPane.widthProperty() - 16.0)

                    center(scrollPane) { style = "-fx-background-color:transparent;" }
                    bottom(HBox()) {
                        alignment = Pos.CENTER_RIGHT
                        padding = Insets(16.0, 8.0, 8.0, 16.0)
                        add(Label(I18N["settings.ligature.sample"]))
                        add(HBox()) { hgrow = Priority.ALWAYS }
                        add(Button(I18N["settings.ligature.add"])) { does { createLigatureRow() } }
                    }
                }
            }
            add(I18N["settings.quick_input.title"]) {
                withContent(BorderPane()) {
                    val stackPane = StackPane(qGridPane)
                    val scrollPane = ScrollPane(stackPane)
                    stackPane.prefWidthProperty().bind(scrollPane.widthProperty() - 16.0)

                    center(scrollPane) {
                        style = "-fx-background-color:transparent;"
                    }
                    bottom(HBox()) {
                        alignment = Pos.CENTER_RIGHT
                        padding = Insets(16.0, 8.0, 8.0, 16.0)
                        add(Label(I18N["settings.quick_input.sample"]))
                        add(HBox()) { hgrow = Priority.ALWAYS }
                        add(Button(I18N["settings.quick_input.add"])) { does { createQuickInputRow() } }
                    }
                }
            }
            add(I18N["settings.shortcut.title"]) {
                withContent(BorderPane()) {
                    val stackPane = StackPane(sGridPane)
                    val scrollPane = ScrollPane(stackPane)
                    stackPane.prefWidthProperty().bind(scrollPane.widthProperty() - 16.0)

                    center(scrollPane) { style = "-fx-background-color:transparent;" }
                    bottom(HBox()) {
                        alignment = Pos.CENTER_LEFT
                        padding = Insets(8.0, 16.0, 8.0, 16.0)
                        add(sConflictLabel)
                    }
                }
            }
            add(I18N["settings.mode.title"]) {
                withContent(GridPane()) {
                    alignment = Pos.TOP_CENTER
                    padding = Insets(16.0)
                    vgap = 16.0
                    hgap = 16.0

                    //   0         1
                    // 0 Input     | input | < > (ViewMode)
                    // 1 Label     | label | < > (ViewMode)
                    // 2
                    // 3 Scale on new picture
                    // 4 | selection | < >       (NewPicScale)
                    // 5
                    // 6 Use wheel to scale      (UseWheelToScale)

                    add(Label(I18N["mode.work.input"]), 0, 0)
                    add(mComboInput, 1, 0) {
                        prefWidth = 160.0
                        items = FXCollections.observableList(ViewMode.entries)
                        isWrapped = true
                    }
                    add(Label(I18N["mode.work.label"]), 0, 1)
                    add(mComboLabel, 1, 1) {
                        prefWidth = 160.0
                        items = FXCollections.observableList(ViewMode.entries)
                        isWrapped = true
                    }
                    add(HBox(), 0, 2)
                    add(Label(I18N["settings.mode.scale.label"]), 0, 3, 2, 1)
                    add(mComboScale, 0, 4, 2, 1) {
                        prefWidth = 224.0
                        items = FXCollections.observableList(CLabelPane.NewPictureScale.entries)
                        isWrapped = true
                    }
                    add(HBox(), 0, 5)
                    add(mCheckWheel, 0, 6, 2, 1)
                    add(xCheckInstTr, 0, 7, 2, 1)
                }
            }
            add(I18N["settings.label.title"]) {
                withContent(GridPane()) {
                    alignment = Pos.CENTER
                    padding = Insets(16.0, 16.0, 0.0, 16.0)
                    vgap = 16.0
                    hgap = 16.0

                    // lGridPane.isGridLinesVisible = true
                    //   0           1           2
                    // 0 ----------  Radius
                    // 1 |        |  ----O------ 24.0
                    // 2 |        |  Alpha
                    // 3 |        |  ------O---- 0x80
                    // 4 |        |  O TextOpaque
                    // 5 |        |  0 selectedColorPicker
                    // 6 ----------    *HTLP TEXT*
                    add(lLabelPane, 0, 0, 1, 6) {
                        val lLabelPaneBorderWidth = 2.0

                        border = Border(
                            BorderStroke(
                                Color.DARKGRAY,
                                BorderStrokeStyle.SOLID,
                                CornerRadii(0.0),
                                BorderWidths(lLabelPaneBorderWidth)
                            )
                        )
                        background = Background(
                            BackgroundImage(
                                SAMPLE_IMAGE,
                                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                BackgroundPosition.CENTER,
                                BackgroundSize.DEFAULT
                            )
                        )

                        setPrefSize(320.0, 320.0)
                        add(lCLabel) {
                            radiusProperty().bind(lSliderRadius.valueProperty())
                            colorOpacityProperty().bind(lSliderAlpha.valueProperty())
                            textOpaqueProperty().bind(lCheckTextOpaque.selectedProperty())
                            selectedStrokeProperty().bind(lCheckSelectedStroke.selectedProperty())


                            // Draggable & drag-limitation
                            var shiftX = 0.0
                            var shiftY = 0.0
                            isSelected = true
                            addEventHandler(MouseEvent.MOUSE_PRESSED) {
                                // Mark immediately when this event will be consumed
                                it.consume() // disable further propagation

                                cursor = Cursor.MOVE

                                shiftX = anchorX - it.sceneX
                                shiftY = anchorY - it.sceneY
                            }
                            addEventHandler(MouseEvent.MOUSE_DRAGGED) {
                                // Mark immediately when this event will be consumed
                                it.consume() // disable further propagation

                                val newAnchorX = shiftX + it.sceneX
                                val newAnchorY = shiftY + it.sceneY

                                //  0--L-----    0 LR LR |
                                //  |  R         LR      |
                                //  |LR|-----    LR      |
                                //  |  |         --------|
                                val limitX = SAMPLE_IMAGE.width - prefWidth - 2 * lLabelPaneBorderWidth
                                val limitY = SAMPLE_IMAGE.height - prefHeight - 2 * lLabelPaneBorderWidth
                                if (newAnchorX < 0 || newAnchorX > limitX) return@addEventHandler
                                if (newAnchorY < 0 || newAnchorY > limitY) return@addEventHandler

                                anchorX = newAnchorX
                                anchorY = newAnchorY
                            }
                            addEventHandler(MouseEvent.MOUSE_RELEASED) {
                                cursor = Cursor.HAND
                            }
                            radiusProperty().addListener(onChange {
                                val limitX = SAMPLE_IMAGE.width - prefWidth - 2 * lLabelPaneBorderWidth
                                val limitY = SAMPLE_IMAGE.height - prefHeight - 2 * lLabelPaneBorderWidth
                                if (anchorX > limitX) anchorX = limitX
                                if (anchorY > limitY) anchorY = limitY
                            })
                        }
                    }
                    add(Label(I18N["settings.label.radius"]), 1, 0)
                    add(lSliderRadius, 1, 1) {
                        prefWidth = 160.0

                        min = 8.0
                        max = 48.0
                        majorTickUnit = 8.0
                        minorTickCount = 3
                        blockIncrement = 2.0
                        isSnapToTicks = true
                        isShowTickMarks = true
                        isShowTickLabels = true
                        valueProperty().addListener(onNew<Number, Double> {
                            lLabelRadius.text = String.format("%05.2f", it)
                        })
                    }
                    add(lLabelRadius, 2, 1) {
                        textFormatter = genTextFormatter<String> {
                            if (!it.text.isMathematicalDecimal() ||
                                !it.controlNewText.isMathematicalDecimal()
                            ) emptyString() else it.text
                        }
                        setOnChangeToLabel {
                            lSliderRadius.value = fieldText.padStart(5, '0').toDouble()

                            labelText = String.format("%05.2f", lSliderRadius.value)
                        }
                    }
                    add(Label(I18N["settings.label.alpha"]), 1, 2)
                    add(lSliderAlpha, 1, 3) {
                        prefWidth = 160.0

                        min = 0.0
                        max = 1.0
                        majorTickUnit = max / 4
                        minorTickCount = 3
                        blockIncrement = majorTickUnit / (minorTickCount + 1)
                        isShowTickMarks = true
                        isShowTickLabels = true
                        labelFormatter = object : StringConverter<Double>() {
                            override fun toString(double: Double): String =
                                ceil(double * 255.0).toInt().toString(16).padStart(2, '0')

                            override fun fromString(string: String): Double = string.toInt(16).toDouble() / 255.0
                        }
                        valueProperty().addListener(onNew<Number, Double> {
                            val alphaPart = ceil(it * 255.0).toInt().toString(16).padStart(2, '0')
                            lLabelAlpha.text = (if (lLabelAlpha.isEditing) "" else "0x") + alphaPart
                        })
                    }
                    add(lLabelAlpha, 2, 3) {
                        textFormatter = genTextFormatter<String> {
                            if (it.text.uppercase().contains(Regex("[^\\dA-F]")) ||
                                it.controlNewText.length > 2
                            ) emptyString() else it.text
                        }
                        setOnChangeToField {
                            fieldText = labelText.substring(2)
                        }
                        setOnChangeToLabel {
                            val alphaStr = fieldText.padStart(2, '0').uppercase()
                            lSliderAlpha.value = alphaStr.toInt(16).toDouble() / 255.0

                            labelText = "0x$alphaStr"
                        }
                    }
                    add(lCheckTextOpaque, 1, 4, 2, 1)
                    add(lCheckSelectedStroke, 1, 5, 2, 1)
                    GridPane.setValignment(lCheckSelectedStroke, VPos.TOP)
                    add(Label(I18N["settings.label.helpText"]), 1, 6, 2, 1) {
                        isWrapText = true
                        textAlignment = TextAlignment.CENTER
                    }
                }
            }
            add(I18N["settings.tool.title"]) {
                withContent(GridPane()) {
                    alignment = Pos.TOP_CENTER
                    padding = Insets(16.0)
                    vgap = 16.0
                    hgap = 16.0

                    //   0        1
                    // 1 tComboTranslationAPI <>
                    // 2 xCheckUseCustomBaiduKey
                    // 3 Translate_keys.key
                    // 4 Translate_keys.app_id
                    // 4

                    add(Label(I18N["settings.tool.translate.api"]), 0, 0)
                    add(tComboTranslationAPI, 1, 0) {
                        prefWidth = 160.0
                        items = FXCollections.observableList(TranslationAPI.entries)
                        selectionModel.select(Settings.selectedTranslationAPI)
                    }
                    add(tCheckUseCustomBaiduKey, 0, 1, 2, 1)
                    add(Label(I18N["settings.tool.translate_keys.key"]), 0, 2)
                    add(tFieldBaiduTranslateKey, 1, 2) {
                        disableProperty().bind(!tCheckUseCustomBaiduKey.selectedProperty())

                    }
                    add(Label(I18N["settings.tool.translate_keys.app_id"]), 0, 3)
                    add(tFieldBaiduTranslateAppId, 1, 3) {
                        disableProperty().bind(!tCheckUseCustomBaiduKey.selectedProperty())
                    }
                    add(Separator(), 0, 5, 2, 1)
                    add(tTranslateTextFlow, 0, 6, 2, 1)
                }
            }
            add(I18N["settings.general.title"]) {
                withContent(GridPane()) {
                    alignment = Pos.TOP_CENTER
                    padding = Insets(16.0)
                    vgap = 16.0
                    hgap = 16.0

                    add(languageLabel, 0, 0)
                    add(languageCombo, 1, 0).apply {
                        prefWidth = 160.0
                    }
                    add(xCheckUpdate, 0, 1, 2, 1)
                    add(xCheckAutoOp, 0, 2, 2, 1)
                    add(xCheckBackup, 0, 3, 2, 1)
                    add(xCheckFormat, 0, 4, 2, 1)
                    add(xCheckUseMeo, 0, 5, 2, 1)
                    add(Label(I18N["settings.other.pic_prism_mode"]), 0, 6)
                    add(xPrismMode, 0, 7, 2, 1) {
                        prefWidth = 280.0
                        items = FXCollections.observableList(PrismMode.entries)
                        isWrapped = true
//                        tooltip = Tooltip(I18N["settings.other.pic_prism_mode.description"]).apply {
//                            showDelay = Duration(500.0)
//                        }
                    }
//                    add(xUseSWPrism, 0, 7, 2, 1)

                }
            }
            add(I18N["settings.export.title"]) {
                withContent(GridPane()) {
                    alignment = Pos.TOP_CENTER
                    padding = Insets(16.0)
                    vgap = 16.0
                    hgap = 16.0

                    add(xCheckUseTmp, 0, 0, 2, 1)
                    add(xFieldTemplate, 0, 1, 2, 1) {
                        disableProperty().bind(!xCheckUseTmp.selectedProperty())
                        textFormatter = genTextFormatter<String> { it.text.replace(Regex("[:*?<>|/\"\\\\]"), "") }
                        tooltip = Tooltip(I18N["settings.other.template.hint"]).apply {
                            showDelay = Duration(500.0)
                        }
                    }
                }
            }

            val generalTab = tabs.first { it.text == I18N["settings.general.title"] }
            tabs.remove(generalTab)
            tabs.add(0, generalTab)

            val exportTab = tabs.first { it.text == I18N["settings.export.title"] }
            tabs.remove(exportTab)
            val modeIndex = tabs.indexOfFirst { it.text == I18N["settings.mode.title"] }
            tabs.add(modeIndex, exportTab)
        }

        initProperties()
    }

    // ----- Group ----- //
    private fun initGroupTab() {
        gGridPane.children.clear()

        val nameList = Settings.defaultGroupNameList
        val colorList = Settings.defaultGroupColorHexList
        val createList = Settings.isGroupCreateOnNewTransList

        if (nameList.isEmpty()) {
            gGridPane.add(gLabelHint, 0, 0)
        } else {
            for (i in nameList.indices) createGroupRow(createList[i], nameList[i], colorList[i])
        }
    }

    private fun createGroupRow(createOnNew: Boolean = false, name: String = "", color: String = "") {
        val newRowIndex = if (gGridPane.rowCount == 0) 1 else gGridPane.rowCount

        if (gGridPane.children.size == 1 || gGridPane.rowCount == 0) { // Only hint or nothing
            gGridPane.children.clear()
            gGridPane.add(gLabelName, 0, 0)
            gGridPane.add(gLabelColor, 1, 0)
            gGridPane.add(gLabelCreate, 2, 0)
        }

        val groupId = newRowIndex - gRowShift
        val colorHex = if (color.isColorHex()) color else TransFile.DEFAULT_COLOR_HEX_LIST[groupId % 9]

        val checkBox = CheckBox().apply { isSelected = createOnNew }
        val textField = TextField(name).apply { textFormatter = genGeneralFormatter() }
        val colorPicker = CColorPicker(Color.web(colorHex))
        val button = Button(I18N["common.delete"]) does { removeGroupRow(GridPane.getRowIndex(this)) }

        checkBox.disableProperty().bind(textField.textProperty().isEmpty)
        textField.textProperty().addListener(onNew { if (it.isEmpty()) checkBox.isSelected = false })

        //   0       1             2        3
        // 0 Name    Color         isCreate
        // 1 _______ | 66CCFF  V |  X       Delete
        gGridPane.add(textField, 0, newRowIndex)
        gGridPane.add(colorPicker, 1, newRowIndex)
        gGridPane.add(checkBox, 2, newRowIndex)
        gGridPane.add(button, 3, newRowIndex)
    }

    private fun removeGroupRow(index: Int) {
        val toRemoveSet = HashSet<Node>()
        for (node in gGridPane.children) {
            val row = GridPane.getRowIndex(node) ?: 0
            if (row == index) toRemoveSet.add(node)
            if (row > index) GridPane.setRowIndex(node, row - 1)
        }
        gGridPane.children.removeAll(toRemoveSet)

        if (gGridPane.rowCount == gRowShift) {
            gGridPane.children.removeAll(gLabelCreate, gLabelName, gLabelColor)
            gGridPane.add(gLabelHint, 0, 0)
        }
    }

    // ----- Ligature ----- //
    private fun initLigatureTab() {
        rGridPane.children.clear()

        val ruleList = Settings.ligatureRules

        if (ruleList.isEmpty()) {
            rGridPane.add(rLabelHint, 0, 0)
        } else {
            for ((from, to) in ruleList) createLigatureRow(from, to)
        }
    }

    private fun createLigatureRow(from: String = "", to: String = "") {
        val newRowIndex = if (rGridPane.rowCount == 0) 1 else rGridPane.rowCount

        if (rGridPane.children.size == 1 || rGridPane.rowCount == 0) { // Only hint || nothing
            rGridPane.children.clear()
            rGridPane.add(rLabelFrom, 0, 0)
            rGridPane.add(rLabelTo, 1, 0)
        }

        val fromField = TextField(from).apply {
            textFormatter = genGeneralFormatter()
            properties[rRuleIndex] = newRowIndex - rRowShift
            properties[rIsFrom] = true
        }
        val toField = TextField(to).apply {
            textFormatter = genGeneralFormatter()
            properties[rRuleIndex] = newRowIndex - rRowShift
            properties[rIsFrom] = false
        }
        val button = Button(I18N["common.delete"]) does { removeLigatureRow(GridPane.getRowIndex(this)) }

        //   0         1         2
        // 0 From      To
        // 1 ________  ________  Delete
        rGridPane.add(fromField, 0, newRowIndex)
        rGridPane.add(toField, 1, newRowIndex)
        rGridPane.add(button, 2, newRowIndex)
    }

    private fun removeLigatureRow(index: Int) {
        val toRemoveSet = HashSet<Node>()
        for (node in rGridPane.children) {
            val row = GridPane.getRowIndex(node) ?: 0
            if (row == index) toRemoveSet.add(node)
            if (row > index) {
                GridPane.setRowIndex(node, row - 1)
                node.properties[rRuleIndex] = row - 1 - rRowShift
            }
        }
        rGridPane.children.removeAll(toRemoveSet)

        if (rGridPane.rowCount == rRowShift) {
            rGridPane.children.removeAll(rLabelFrom, rLabelTo)
            rGridPane.add(rLabelHint, 0, 0)
        }
    }

    // ----- Quick Input  ----- //
    private fun initQuickInputTab() {
        qGridPane.children.clear()

        val quickInputTextsList = Settings.quickInputTexts

        if (quickInputTextsList.isEmpty()) {
            qGridPane.add(qLabelHint, 0, 0)
        } else {
            for (text in quickInputTextsList) createQuickInputRow(text)
        }
    }

    private fun createQuickInputRow(text: String = "") {
        val newRowIndex = if (qGridPane.rowCount == 0) 1 else qGridPane.rowCount
        if (qGridPane.children.size == 1 || qGridPane.rowCount == 0) { // Only hint || nothing
            qGridPane.children.clear()
        }
        val textField = TextField(text).apply {
            textFormatter = genGeneralFormatter()
        }
        val button = Button(I18N["common.delete"]) does { removeQuickInputRow(GridPane.getRowIndex(this)) }
        //   0         1         2
        // 1 textField  ________  Delete
        qGridPane.add(textField, 0, newRowIndex)
        qGridPane.add(button, 1, newRowIndex)
    }

    private fun removeQuickInputRow(index: Int) {
        val toRemoveSet = HashSet<Node>()
        for (node in qGridPane.children) {
            val row = GridPane.getRowIndex(node) ?: 0
            if (row == index) toRemoveSet.add(node)
            if (row > index) {
                GridPane.setRowIndex(node, row - 1)
            }
        }
        qGridPane.children.removeAll(toRemoveSet)

        if (qGridPane.rowCount == qRowShift) {
            qGridPane.children.removeAll()
            qGridPane.add(qLabelHint, 0, 0)
        }
    }

    // ----- Shortcut ----- //
    private fun initShortcutTab() {
        sGridPane.children.clear()
        shortcutEdits.clear()

        var row = 0
        sGridPane.add(Label(I18N["settings.shortcut.action"]), 0, row)
        sGridPane.add(Label(I18N["settings.shortcut.key"]), 1, row)
        row += 1

        // 先显示通用快捷键（有分组标题）
        val generalShortcuts = ShortcutRegistry.definitions.filter { it.groupKey == "settings.shortcut.group.general" }
        if (generalShortcuts.isNotEmpty()) {
            val groupLabel = Label(I18N["settings.shortcut.group.general"])
            groupLabel.style = "-fx-font-weight: bold;"
            sGridPane.add(groupLabel, 0, row, 2, 1)
            row += 1

            for (definition in generalShortcuts) {
                val gesture = ShortcutRegistry.resolveGesture(Settings.shortcuts, definition.id)
                shortcutEdits[definition.id] = gesture

                val actionLabel = Label(I18N[definition.labelKey])
                val editor = TextField(gesture.toDisplayString()).apply {
                    prefWidth = 260.0
                    isEditable = false
                    if (!definition.editable) isDisable = true
                }

                if (definition.editable) {
                    bindShortcutEditor(definition, editor)
                }

                sGridPane.add(actionLabel, 0, row)
                sGridPane.add(editor, 1, row)
                row += 1
            }
        }

        // 再显示图片查看区快捷键（有分组标题）
        val imageViewShortcuts = ShortcutRegistry.definitions.filter { it.groupKey == "settings.shortcut.group.image_view" }
        if (imageViewShortcuts.isNotEmpty()) {
            val groupLabel = Label(I18N["settings.shortcut.group.image_view"])
            groupLabel.style = "-fx-font-weight: bold;"
            sGridPane.add(groupLabel, 0, row, 2, 1)
            row += 1

            for (definition in imageViewShortcuts) {
                val gesture = ShortcutRegistry.resolveGesture(Settings.shortcuts, definition.id)
                shortcutEdits[definition.id] = gesture

                val actionLabel = Label(I18N[definition.labelKey])
                val editor = TextField(gesture.toDisplayString()).apply {
                    prefWidth = 260.0
                    isEditable = false
                    if (!definition.editable) isDisable = true
                }

                if (definition.editable) {
                    bindShortcutEditor(definition, editor)
                }

                sGridPane.add(actionLabel, 0, row)
                sGridPane.add(editor, 1, row)
                row += 1
            }
        }

        updateShortcutConflicts()
    }

    private fun bindShortcutEditor(definition: ShortcutDefinition, field: TextField) {
        field.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (!field.isFocused) return@addEventFilter
            when (event.code) {
                KeyCode.ESCAPE, KeyCode.BACK_SPACE, KeyCode.DELETE -> {
                    applyShortcutGesture(definition, field, definition.defaultGesture)
                    event.consume()
                    return@addEventFilter
                }
                KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.ALT, KeyCode.META -> return@addEventFilter
                else -> {
                    val modifiers = ModifierSpec(
                        shift = event.isShiftDown,
                        alt = event.isAltDown,
                        shortcut = event.isControlDown || event.isMetaDown
                    )
                    val gesture = KeyGesture(event.code, modifiers)
                    applyShortcutGesture(definition, field, gesture)
                    event.consume()
                }
            }
        }
        if (definition.defaultGesture is MouseGesture) {
            field.addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
                if (!field.isFocused) return@addEventFilter
                val modifiers = ModifierSpec(
                    shift = event.isShiftDown,
                    alt = event.isAltDown,
                    shortcut = event.isControlDown || event.isMetaDown
                )
                val gesture = MouseGesture(event.button, modifiers)
                applyShortcutGesture(definition, field, gesture)
                event.consume()
            }
        }
        if (definition.defaultGesture is ScrollGesture) {
            field.addEventFilter(ScrollEvent.SCROLL) { event ->
                if (!field.isFocused) return@addEventFilter
                val modifiers = ModifierSpec(
                    shift = event.isShiftDown,
                    alt = event.isAltDown,
                    shortcut = event.isControlDown || event.isMetaDown
                )
                val gesture = ScrollGesture(modifiers)
                applyShortcutGesture(definition, field, gesture)
                event.consume()
            }
        }
    }

    private fun applyShortcutGesture(definition: ShortcutDefinition, field: TextField, gesture: ShortcutGesture) {
        shortcutEdits[definition.id] = gesture
        field.text = gesture.toDisplayString()
        updateShortcutConflicts()
    }

    private fun updateShortcutConflicts() {
        val conflicts = mutableListOf<String>()
        val definitions = ShortcutRegistry.definitions
        for (i in definitions.indices) {
            val left = definitions[i]
            val leftGesture = shortcutEdits[left.id] ?: left.defaultGesture
            val leftTokens = leftGesture.conflictTokens()
            if (leftTokens.isEmpty()) continue
            for (j in i + 1 until definitions.size) {
                val right = definitions[j]
                if (left.scopes.intersect(right.scopes).isEmpty()) continue
                val rightGesture = shortcutEdits[right.id] ?: right.defaultGesture
                val rightTokens = rightGesture.conflictTokens()
                if (leftTokens.intersect(rightTokens).isNotEmpty()) {
                    conflicts += "${I18N[left.labelKey]} ↔ ${I18N[right.labelKey]}"
                }
            }
        }

        if (conflicts.isEmpty()) {
            sConflictLabel.text = ""
            shortcutConflictProperty.set(false)
        } else {
            sConflictLabel.text = I18N["settings.shortcut.conflict"] + "\n" + conflicts.joinToString("\n")
            shortcutConflictProperty.set(true)
        }
    }


    // ----- Initialize Properties ----- //
    override fun initProperties() {
        // Group
        initGroupTab()

        // Ligature Rule
        initLigatureTab()

        // quick Input
        initQuickInputTab()

        // Shortcut
        initShortcutTab()

        // Mode
        mComboInput.select(Settings.viewModes[0])
        mComboLabel.select(Settings.viewModes[1])
        mComboScale.select(Settings.newPictureScalePicture)
        mCheckWheel.isSelected = Settings.useWheelToScale
        xCheckInstTr.isSelected = Settings.instantTranslate

        // Label
        lCLabel.anchorX = (lLabelPane.prefWidth - lCLabel.prefWidth) / 2
        lCLabel.anchorY = (lLabelPane.prefHeight - lCLabel.prefHeight) / 2

        lLabelRadius.isEditing = false
        lSliderRadius.value = Settings.labelRadius

        lLabelAlpha.isEditing = false
        lSliderAlpha.value = Settings.labelColorOpacity

        lCheckTextOpaque.isSelected = Settings.labelTextOpaque
        lCheckSelectedStroke.isSelected = Settings.labelSelectedStroke

        // Tool
        tComboTranslationAPI.select(Settings.selectedTranslationAPI)
        tCheckUseCustomBaiduKey.isSelected = Settings.useCustomBaiduKey
        tFieldBaiduTranslateKey.text = Settings.baiduTransLateKey
        tFieldBaiduTranslateAppId.text = Settings.baiduTransLateAppId

        // General
        xCheckUpdate.isSelected = Settings.autoCheckUpdate
        xCheckAutoOp.isSelected = Settings.autoOpenLastFile
        xCheckBackup.isSelected = Settings.autoCheckBackup
        xCheckFormat.isSelected = Settings.checkFormatWhenSave
        xCheckUseMeo.isSelected = Settings.useMeoFileAsDefault
        xPrismMode.select(Settings.currentPrismMode)

        // Populate language combo box with display names from the enum
        languageCombo.items = FXCollections.observableArrayList(
            SupportedLanguage.entries.map { it.displayName }
        )
        // Select the current language based on Preference
        languageCombo.selectionModel.select(getLanguageIndex(Preference.currentLanguage))

        // Export
        xCheckUseTmp.isSelected = Settings.useExportNameTemplate
        xFieldTemplate.text = Settings.exportNameTemplate


    }

    // ----- Result convert ---- //
    private fun convertGroup(): Map<String, Any> {
        val size = gGridPane.rowCount - gRowShift
        if (size < 0) return emptyMap()

        val map = HashMap<String, Any>()

        val nameList = MutableList(size) { "" }
        val colorList = MutableList(size) { "" }
        val isCreateList = MutableList(size) { false }
        for (node in gGridPane.children) {
            val groupId = GridPane.getRowIndex(node) - gRowShift
            if (groupId < 0) continue
            when (node) {
                is CheckBox -> isCreateList[groupId] = node.isSelected
                is TextField -> nameList[groupId] = node.text
                is ColorPicker -> colorList[groupId] = node.value.toHexRGB()
            }
        }

        map[Settings.DefaultGroupNameList] = nameList
        map[Settings.DefaultGroupColorHexList] = colorList
        map[Settings.IsGroupCreateOnNewTrans] = isCreateList

        return map
    }

    private fun convertLigatureRule(): Map<String, Any> {
        val size = rGridPane.rowCount - rRowShift
        if (size < 0) return emptyMap()

        val map = HashMap<String, List<Pair<String, String>>>()

        val fromList = MutableList(size) { "" }
        val toList = MutableList(size) { "" }
        for (node in rGridPane.children) {
            if (node is TextField) {
                val ruleIndex = node.properties[rRuleIndex] as Int
                val isFrom = node.properties[rIsFrom] as Boolean

                if (isFrom) fromList[ruleIndex] = node.text
                else toList[ruleIndex] = node.text
            }
        }
        // Abandon repeated rule-from by toMap
        val rules = List(size) { fromList[it] to toList[it] }.toMap().toList()

        map[Settings.LigatureRules] = rules

        return map
    }

    private fun convertQuickInput(): Map<String, Any> {
        val map = HashMap<String, Any>()
        val size = qGridPane.rowCount - qRowShift
        if (size < 0) return emptyMap()

        val textList = MutableList(size) { "" }
        for (node in qGridPane.children) {
            val groupId = GridPane.getRowIndex(node) - qRowShift
            if (groupId < 0) continue
            when (node) {
                is TextField -> textList[groupId] = node.text
            }
        }
        map[Settings.QuickInputTexts] = textList
        return map
    }

    private fun convertShortcut(): Map<String, Any> {
        val map = HashMap<String, Any>()
        val storageMap = shortcutEdits.mapValues { it.value.toStorageString() }
        map[Settings.Shortcuts] = ShortcutRegistry.toStorageList(storageMap)
        return map
    }

    private fun convertMode(): Map<String, Any> {
        val map = HashMap<String, Any>()

        map[Settings.ViewModes] = listOf(mComboInput.index, mComboLabel.index).map(ViewMode.entries.toTypedArray()::get)
        map[Settings.NewPictureScale] = mComboScale.index.let(CLabelPane.NewPictureScale.entries.toTypedArray()::get)
        map[Settings.UseWheelToScale] = mCheckWheel.isSelected
        map[Settings.InstantTranslate] = xCheckInstTr.isSelected

        return map
    }

    private fun convertLabel(): Map<String, Any> {
        val map = HashMap<String, Any>()

        map[Settings.LabelRadius] = lSliderRadius.value
        map[Settings.LabelColorOpacity] = lSliderAlpha.value
        map[Settings.LabelTextOpaque] = lCheckTextOpaque.isSelected
        map[Settings.LabelSelectedStroke] = lCheckSelectedStroke.isSelected

        return map
    }

    private fun convertTool(): Map<String, Any> {
        val map = HashMap<String, Any>()

        map[Settings.SelectedTranslationAPI] = tComboTranslationAPI.selectionModel.selectedItem.name
        map[Settings.UseCustomBaiduKey] = tCheckUseCustomBaiduKey.isSelected
        map[Settings.BaiduTransLateKey] = tFieldBaiduTranslateKey.text
        map[Settings.BaiduTransLateAppId] = tFieldBaiduTranslateAppId.text
        return map
    }

    private fun convertGeneral(): Map<String, Any> {
        val map = HashMap<String, Any>()

        map[Settings.AutoCheckUpdate] = xCheckUpdate.isSelected
        map[Settings.AutoOpenLastFile] = xCheckAutoOp.isSelected
        map[Settings.AutoCheckBackup] = xCheckBackup.isSelected
        map[Settings.CheckFormatWhenSave] = xCheckFormat.isSelected
        map[Settings.UseMeoFileAsDefault] = xCheckUseMeo.isSelected
        map[Settings.CurrentPrismMode] = xPrismMode.index.let(PrismMode.entries.toTypedArray()::get)
        val selectedLanguage = getLanguageCode(languageCombo.selectionModel.selectedIndex)
        map[Preference.CurrentLanguage] = selectedLanguage
        return map
    }

    private fun convertExport(): Map<String, Any> {
        val map = HashMap<String, Any>()

        map[Settings.UseExportNameTemplate] = xCheckUseTmp.isSelected
        map[Settings.ExportNameTemplate] = xFieldTemplate.text
        return map
    }

    override fun convertResult(): Map<String, Any> {
        val map =  HashMap<String, Any>().apply {
            putAll(convertGroup())
            putAll(convertLigatureRule())
            putAll(convertQuickInput())
            putAll(convertShortcut())
            putAll(convertMode())
            putAll(convertLabel())
            putAll(convertTool())
            putAll(convertGeneral())
            putAll(convertExport())
        }
        // 直接在返回前保存一次设置
        saveSettings(map)
        return map
    }

    private fun saveSettings(map: Map<String,Any> ){
        Logger.info("Generated common settings", "DialogSetting")
        Logger.debug("got $map", "DialogSetting")

        @Suppress("UNCHECKED_CAST")
        for ((key, value) in map) when (key) {
            Settings.DefaultGroupNameList     -> Settings.defaultGroupNameList        .setAll(value as List<String>)
            Settings.DefaultGroupColorHexList -> Settings.defaultGroupColorHexList    .setAll(value as List<String>)
            Settings.IsGroupCreateOnNewTrans  -> Settings.isGroupCreateOnNewTransList .setAll(value as List<Boolean>)
            Settings.LigatureRules            -> Settings.ligatureRules               .setAll(value as List<Pair<String, String>>)
            Settings.QuickInputTexts          -> Settings.quickInputTexts             .setAll(value as List<String>)
            Settings.ViewModes                -> Settings.viewModes                   .setAll(value as List<ViewMode>)
            Settings.NewPictureScale          -> Settings.newPictureScalePicture      = value as CLabelPane.NewPictureScale
            Settings.UseWheelToScale          -> Settings.useWheelToScale             = value as Boolean
            Settings.LabelRadius              -> Settings.labelRadius                 = value as Double
            Settings.LabelColorOpacity        -> Settings.labelColorOpacity           = value as Double
            Settings.LabelTextOpaque          -> Settings.labelTextOpaque             = value as Boolean
            Settings.LabelSelectedStroke      -> Settings.labelSelectedStroke         = value as Boolean
            Settings.CurrentPrismMode         -> Settings.currentPrismMode            = value as PrismMode
            Settings.AutoCheckUpdate          -> Settings.autoCheckUpdate             = value as Boolean
            Settings.AutoOpenLastFile         -> Settings.autoOpenLastFile            = value as Boolean
            Settings.AutoCheckBackup          -> Settings.autoCheckBackup             = value as Boolean
            Settings.InstantTranslate         -> Settings.instantTranslate            = value as Boolean
            Settings.CheckFormatWhenSave      -> Settings.checkFormatWhenSave         = value as Boolean
            Settings.UseMeoFileAsDefault      -> Settings.useMeoFileAsDefault         = value as Boolean
            Settings.UseExportNameTemplate    -> Settings.useExportNameTemplate       = value as Boolean
            Settings.ExportNameTemplate       -> Settings.exportNameTemplate          = value as String
            Settings.UseCustomBaiduKey        -> Settings.useCustomBaiduKey           = value as Boolean
            Settings.BaiduTransLateKey        -> Settings.baiduTransLateKey           = value as String
            Settings.BaiduTransLateAppId      -> Settings.baiduTransLateAppId         = value as String
            Settings.SelectedTranslationAPI   -> Settings.selectedTranslationAPI      = TranslationAPI.fromString(value as String)
            Settings.Shortcuts                -> Settings.shortcuts.apply {
                clear()
                putAll(ShortcutRegistry.normalizeShortcutMap(ShortcutRegistry.parseShortcutMap(value as List<String>)))
            }
            Preference.CurrentLanguage        -> Preference.currentLanguage           = value as String
            else -> doNothing()
        }
    }

}
