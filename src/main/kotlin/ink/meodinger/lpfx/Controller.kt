package ink.meodinger.lpfx

import ink.meodinger.lpfx.component.*
import ink.meodinger.lpfx.component.common.*
import ink.meodinger.lpfx.component.singleton.ADialogSpecify
import ink.meodinger.lpfx.component.singleton.AMenuBar
import ink.meodinger.lpfx.component.singleton.ATreeMenu
import ink.meodinger.lpfx.io.export
import ink.meodinger.lpfx.io.load
import ink.meodinger.lpfx.io.pack
import ink.meodinger.lpfx.options.*
import ink.meodinger.lpfx.type.TransFile
import ink.meodinger.lpfx.type.TransGroup
import ink.meodinger.lpfx.type.TransLabel
import ink.meodinger.lpfx.util.component.*
import ink.meodinger.lpfx.util.contains
import ink.meodinger.lpfx.util.dialog.*
import ink.meodinger.lpfx.util.doNothing
import ink.meodinger.lpfx.util.event.*
import ink.meodinger.lpfx.util.file.transfer
import ink.meodinger.lpfx.util.property.*
import ink.meodinger.lpfx.util.resource.*
import ink.meodinger.lpfx.util.string.emptyString
import ink.meodinger.lpfx.util.timer.TimerTaskManager

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ListProperty
import javafx.beans.property.Property
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.*
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.LinkedHashMap


/**
 * Author: Meodinger
 * Date: 2021/7/29
 * Have fun with my code!
 */

/**
 * Main controller
 */
class Controller(private val view: View) {

    companion object {
        /**
         * Auto-save
         */
        private const val AUTO_SAVE_DELAY = 5 * 60 * 1000L
        private const val AUTO_SAVE_PERIOD = 3 * 60 * 1000L
    }

    private val bSwitchViewMode: Button      = view.bSwitchViewMode does { switchViewMode() }
    private val bSwitchWorkMode: Button      = view.bSwitchWorkMode does { switchWorkMode() }
    private val lInfo: Label                 = view.lInfo
    private val pMain: SplitPane             = view.pMain
    private val pRight: SplitPane            = view.pRight
    private val cGroupBar: CGroupBar         = view.cGroupBar
    private val cLabelPane: CLabelPane       = view.cLabelPane
    private val cSlider: CTextSlider         = view.cSlider
    private val cPicBox: CComboBox<String>   = view.cPicBox
    private val cGroupBox: CComboBox<String> = view.cGroupBox
    private val cTreeView: CTreeView         = view.cTreeView
    private val cTransArea: CLigatureArea    = view.cTransArea

    private val backupManager = TimerTaskManager(AUTO_SAVE_DELAY, AUTO_SAVE_PERIOD) {
        if (State.isChanged) {
            val bak = State.getBakFolder().resolve("${Date().time}.${EXTENSION_BAK}")
            try {
                export(bak, FileType.MeoFile, State.transFile)
                Logger.info("Backed TransFile", LOGSRC_CONTROLLER)
            } catch (e: IOException) {
                Logger.error("Auto-backup failed", LOGSRC_CONTROLLER)
                Logger.exception(e)
            }
        }
    }

    private fun genPicNamesBinding(): ObjectBinding<ObservableList<String>> {
        return object : ObjectBinding<ObservableList<String>>() {
            private var lastMapObservable = State.transFile.transMapObservable

            init {
                bind(State.transFileProperty())
            }

            override fun computeValue(): ObservableList<String> {
                if (lastMapObservable !== State.transFile.transMapObservable) {
                    unbind(lastMapObservable)
                    lastMapObservable = State.transFile.transMapObservable
                    bind(lastMapObservable)
                }

                return FXCollections.observableList(State.transFile.sortedPicNames)
            }

        }
    }
    private fun genLabelsBinding(): ObjectBinding<ObservableList<TransLabel>> {
        return object : ObjectBinding<ObservableList<TransLabel>>() {

            init {
                bind(State.currentPicNameProperty())
            }

            override fun computeValue(): ObservableList<TransLabel> {
                return if (State.currentPicName.isNotEmpty())
                    State.transFile.transMapObservable[State.currentPicName]!!
                else
                    FXCollections.emptyObservableList()
            }
        }
    }
    private fun genGroupsBinding(): ObjectBinding<ListProperty<TransGroup>> {
        return object : ObjectBinding<ListProperty<TransGroup>>() {
            private var lastGroupListObservable = State.transFile.groupListObservable

            init {
                bind(State.transFileProperty())
            }

            override fun computeValue(): ListProperty<TransGroup> {
                if (lastGroupListObservable !== State.transFile.groupListObservable) {
                    unbind(lastGroupListObservable)
                    lastGroupListObservable = State.transFile.groupListObservable
                    bind(lastGroupListObservable)
                }

                return State.transFile.groupListProperty
            }
        }
    }
    private fun <T> genGroupPropertyBinding(getter: () -> ObservableList<T>,
        propertyGetter: (TransGroup) -> Property<T>
    ): ObjectBinding<ObservableList<T>> {
        return object : ObjectBinding<ObservableList<T>>() {
            private var lastGroupListObservable = State.transFile.groupListObservable
            private val boundGroupNameProperties = ArrayList<Property<T>>()

            init {
                bind(State.transFileProperty())
            }

            override fun computeValue(): ObservableList<T> {
                if (lastGroupListObservable !== State.transFile.groupListObservable) {
                    unbind(lastGroupListObservable)
                    lastGroupListObservable = State.transFile.groupListObservable
                    bind(State.transFile.groupListObservable)
                }

                for (property in boundGroupNameProperties) unbind(property)
                State.transFile.groupListObservable.mapTo(boundGroupNameProperties.apply(ArrayList<*>::clear), propertyGetter)
                for (property in boundGroupNameProperties) bind(property)

                return getter()
            }
        }
    }
    private val cLabelPaneImageBinding: ObjectBinding<Image> = object : ObjectBinding<Image>() {

        init {
            bind(State.currentPicNameProperty())
        }

        override fun computeValue(): Image {
            if (!State.isOpened) return INIT_IMAGE

            var image: Image? = null
            try {
                val picFile = State.getPicFileNow()
                if (picFile.exists()) {
                    image = Image(picFile.toURI().toURL().toString())
                } else {
                    Logger.error("Picture `${State.currentPicName}` not exists", LOGSRC_CONTROLLER)
                    showError(State.stage, String.format(I18N["error.picture_not_exists.s"], State.currentPicName))
                }
            } catch (e: IOException) {
                Logger.error("LabelPane render failed", LOGSRC_CONTROLLER)
                Logger.exception(e)
                showException(State.stage, e)
            }
            return image ?: INIT_IMAGE
        }

    }

    private fun switchViewMode() {
        State.viewMode = ViewMode.values()[(State.viewMode.ordinal + 1) % ViewMode.values().size]
        labelInfo("Switched work mode to ${State.viewMode}")
    }
    private fun switchWorkMode() {
        State.workMode = WorkMode.values()[(State.workMode.ordinal + 1) % WorkMode.values().size]
        labelInfo("Switched work mode to ${State.workMode}")
    }

    init {
        Logger.info("Controller initializing...", LOGSRC_CONTROLLER)

        init()
        bind()
        listen()
        effect()
        transform()

        Logger.info("Controller initialized", LOGSRC_CONTROLLER)

        // Display default image
        cLabelPane.isVisible = false
        Platform.runLater {
            cLabelPane.moveToCenter()
            cLabelPane.isVisible = true
        }
    }

    /**
     * Components Initialize
     */
    private fun init() {
        Logger.info("Initializing components...", LOGSRC_CONTROLLER)

        // MenuBar
        view.top = AMenuBar
        Logger.info("Added MenuBar", LOGSRC_CONTROLLER)

        // Last directory
        var lastFile = RecentFiles.lastFile
        while (lastFile != null) {
            if (lastFile.exists() && lastFile.parentFile.exists()) {
                CFileChooser.lastDirectory = lastFile.parentFile
                break
            } else {
                RecentFiles.remove(lastFile)
                lastFile = RecentFiles.lastFile
            }
        }
        Logger.info("Set CFileChooser lastDirectory: ${CFileChooser.lastDirectory}", LOGSRC_CONTROLLER)

        // Settings
        State.viewMode = Settings.viewModes[State.workMode.ordinal]
        Logger.info("Applied Settings @ ViewMode", LOGSRC_CONTROLLER)

        // Drag and Drop
        view.setOnDragOver {
            if (it.dragboard.hasFiles()) it.acceptTransferModes(TransferMode.COPY)
            it.consume() // Consume used event
        }
        view.setOnDragDropped {
            if (stay()) return@setOnDragDropped

            State.reset()

            val board = it.dragboard
            if (board.hasFiles()) {
                val file = board.files.firstOrNull { f -> EXTENSIONS_FILE.contains(f.extension) } ?: return@setOnDragDropped

                // To make sure exception can be caught
                Platform.runLater { open(file) }
                it.isDropCompleted = true
            }
            it.consume() // Consume used event
        }
        Logger.info("Enabled Drag and Drop", LOGSRC_CONTROLLER)

        // Global event catch, prevent mnemonic parsing and the beep
        view.addEventHandler(KeyEvent.KEY_PRESSED) { if (it.isAltDown) it.consume() }
        Logger.info("Prevented Alt-Key mnemonic", LOGSRC_CONTROLLER)

        // Register CGroupBar handler
        cGroupBar.setOnGroupCreate { ATreeMenu.toggleGroupCreate() }
        Logger.info("Registered CGroupBar Handler", LOGSRC_CONTROLLER)

        // Register CLabelPane handler
        cLabelPane.setOnLabelCreate {
            if (State.workMode != WorkMode.LabelMode) return@setOnLabelCreate
            if (State.transFile.groupCount == 0) return@setOnLabelCreate

            val newIndex =
                if (State.currentLabelIndex != -1) State.currentLabelIndex + 1
                else State.transFile.getTransList(State.currentPicName).size + 1
            val transLabel = TransLabel(newIndex, State.currentGroupId, it.labelX, it.labelY, "")

            // Edit data
            State.addTransLabel(State.currentPicName, transLabel)
            // Mark change
            State.isChanged = true
            // Select it
            cTreeView.selectLabel(newIndex)
            // If instant translate
            if (Settings.instantTranslate) cTransArea.requestFocus()
        }
        cLabelPane.setOnLabelRemove {
            if (State.workMode != WorkMode.LabelMode) return@setOnLabelRemove

            // Edit data
            State.removeTransLabel(State.currentPicName, it.labelIndex)
            // Change state
            if (State.currentLabelIndex == it.labelIndex) State.currentLabelIndex = NOT_FOUND
            // Mark change
            State.isChanged = true
        }
        cLabelPane.setOnLabelHover {
            val transLabel = State.transFile.getTransLabel(State.currentPicName, it.labelIndex)

            // Text display
            cLabelPane.removeText()
            when (State.workMode) {
                WorkMode.InputMode -> {
                    cLabelPane.createText(transLabel.text, Color.BLACK, it.displayX, it.displayY)
                }
                WorkMode.LabelMode -> {
                    val transGroup = State.transFile.getTransGroup(transLabel.groupId)
                    cLabelPane.createText(transGroup.name, Color.web(transGroup.colorHex), it.displayX, it.displayY)
                }
            }
        }
        cLabelPane.setOnLabelClick {
            if (State.workMode != WorkMode.InputMode) return@setOnLabelClick

            if (it.source.isDoubleClick) cLabelPane.moveToLabel(it.labelIndex)

            cTreeView.selectLabel(it.labelIndex)
        }
        cLabelPane.setOnLabelMove {
            State.isChanged = true
        }
        cLabelPane.setOnLabelOther {
            if (State.workMode != WorkMode.LabelMode) return@setOnLabelOther
            if (State.transFile.groupCount == 0) return@setOnLabelOther

            val transGroup = State.transFile.getTransGroup(State.currentGroupId)

            cLabelPane.removeText()
            cLabelPane.createText(transGroup.name, Color.web(transGroup.colorHex), it.displayX, it.displayY)
        }
        Logger.info("Registered CLabelPane Handler", LOGSRC_CONTROLLER)
    }
    /**
     * Properties' bindings
     */
    private fun bind() {
        Logger.info("Binding properties...", LOGSRC_CONTROLLER)

        // Preferences
        cTransArea.fontProperty().bindBidirectional(Preference.textAreaFontProperty())
        pMain.dividers[0].positionProperty().bindBidirectional(Preference.mainDividerPositionProperty())
        pRight.dividers[0].positionProperty().bindBidirectional(Preference.rightDividerPositionProperty())
        view.showStatsBarProperty().bind(Preference.showStatsBarProperty())
        Logger.info("Bound Preferences @ DividerPositions, TextAreaFont", LOGSRC_CONTROLLER)

        // RecentFiles
        AMenuBar.recentFilesProperty().bind(RecentFiles.recentFilesProperty())
        Logger.info("Bound recent files menu", LOGSRC_CONTROLLER)

        // Set components disabled
        bSwitchViewMode.disableProperty().bind(!State.isOpenedProperty())
        bSwitchWorkMode.disableProperty().bind(!State.isOpenedProperty())
        cTransArea.disableProperty().bind(!State.isOpenedProperty())
        cTreeView.disableProperty().bind(!State.isOpenedProperty())
        cPicBox.disableProperty().bind(!State.isOpenedProperty())
        cGroupBox.disableProperty().bind(!State.isOpenedProperty())
        cSlider.disableProperty().bind(!State.isOpenedProperty())
        cLabelPane.disableProperty().bind(!State.isOpenedProperty())
        Logger.info("Bound disabled", LOGSRC_CONTROLLER)

        // CLigatureTextArea - rules
        cTransArea.ligatureRulesProperty().bind(Settings.ligatureRulesProperty())
        Logger.info("Bound ligature rules", LOGSRC_CONTROLLER)

        // CSlider - CLabelPane#scale
        cSlider.initScaleProperty().bindBidirectional(cLabelPane.initScaleProperty())
        cSlider.minScaleProperty().bindBidirectional(cLabelPane.minScaleProperty())
        cSlider.maxScaleProperty().bindBidirectional(cLabelPane.maxScaleProperty())
        cSlider.scaleProperty().bindBidirectional(cLabelPane.scaleProperty())
        Logger.info("Bound scale", LOGSRC_CONTROLLER)

        // Switch Button text
        bSwitchWorkMode.textProperty().bind(Bindings.createStringBinding({
            when (State.workMode) {
                WorkMode.InputMode -> I18N["mode.work.input"]
                WorkMode.LabelMode -> I18N["mode.work.label"]
            }
        }, State.workModeProperty()))
        bSwitchViewMode.textProperty().bind(Bindings.createStringBinding({
            when (State.viewMode) {
                ViewMode.IndexMode -> I18N["mode.view.index"]
                ViewMode.GroupMode -> I18N["mode.view.group"]
            }
        }, State.viewModeProperty()))
        Logger.info("Bound switch button text", LOGSRC_CONTROLLER)

        // GroupBox
        cGroupBox.itemsProperty().bind(genGroupPropertyBinding({
            FXCollections.observableList(State.transFile.groupNames)
        }, {
            it.nameProperty
        }))
        cGroupBox.indexProperty().bindBidirectional(State.currentGroupIdProperty())
        Logger.info("Bound GroupBox & CurrentGroupId", LOGSRC_CONTROLLER)

        // GroupBar
        cGroupBar.groupsProperty().bind(genGroupsBinding())
        cGroupBar.indexProperty().bindBidirectional(State.currentGroupIdProperty())
        Logger.info("Bound GroupBar & CurrentGroupId", LOGSRC_CONTROLLER)

        // PictureBox
        cPicBox.itemsProperty().bind(genPicNamesBinding())
        RuledGenericBidirectionalBinding.bind(
            cPicBox.valueProperty(), rule@{ observable, _, newValue, _ ->
                // Indicate current item was removed
                // Use run later to avoid Issue#5 (Reason unclear).
                // Check opened to avoid accidentally set "Close time empty str" to "Open time pic"
                if (State.isOpened && newValue == null) Platform.runLater {
                    observable.value = State.transFile.sortedPicNames[0]
                }

                // Directly bind bi-directionally will cause NPE
                return@rule newValue ?: if (State.isOpened) State.transFile.sortedPicNames[0] else emptyString()
            },
            State.currentPicNameProperty(), { _, _, newValue, _ -> newValue!! }
        )
        Logger.info("Bound PicBox & CurrentPicName", LOGSRC_CONTROLLER)

        // TreeView
        cTreeView.rootNameProperty().bind(State.currentPicNameProperty())
        cTreeView.viewModeProperty().bind(State.viewModeProperty())
        cTreeView.groupsProperty().bind(genGroupsBinding())
        cTreeView.labelsProperty().bind(genLabelsBinding())
        Logger.info("Bound CTreeView properties", LOGSRC_CONTROLLER)

        // LabelPane
        cLabelPane.imageProperty().bind(cLabelPaneImageBinding)
        cLabelPane.labelsProperty().bind(genLabelsBinding())
        cLabelPane.colorHexListProperty().bind(genGroupPropertyBinding({
            FXCollections.observableList(State.transFile.groupColors)
        }, {
            it.colorHexProperty
        }))
        cLabelPane.labelRadiusProperty().bind(Settings.labelRadiusProperty())
        cLabelPane.labelAlphaProperty().bind(Settings.labelAlphaProperty())
        cLabelPane.newPictureScaleProperty().bind(Settings.newPictureScaleProperty())
        cLabelPane.commonCursorProperty().bind(Bindings.createObjectBinding({
            when (State.workMode) {
                WorkMode.LabelMode -> Cursor.CROSSHAIR
                WorkMode.InputMode -> Cursor.DEFAULT
            }
        }, State.workModeProperty()))
        Logger.info("Bound CLabelPane properties", LOGSRC_CONTROLLER)
    }
    /**
     * Properties' listeners (for unbindable)
     */
    private fun listen() {
        Logger.info("Attaching Listeners...", LOGSRC_CONTROLLER)

        // Default image auto-center
        cLabelPane.widthProperty().addListener(onChange {
            if (!State.isOpened || !State.getPicFileNow().exists()) cLabelPane.moveToCenter()
        })
        cLabelPane.heightProperty().addListener(onChange {
            if (!State.isOpened || !State.getPicFileNow().exists()) cLabelPane.moveToCenter()
        })
        Logger.info("Listened for default image location", LOGSRC_CONTROLLER)

        // isChanged
        cTransArea.textProperty().addListener(onChange {
            if (cTransArea.isBound) State.isChanged = true
        })
        Logger.info("Listened for isChanged", LOGSRC_CONTROLLER)

        // currentLabelIndex
        cTreeView.selectionModel.selectedItemProperty().addListener(onNew {
            if (it != null && it is CTreeLabelItem && cTreeView.selectionModel.selectedItems.size == 1)
                State.currentLabelIndex = it.index
        })
        State.currentPicNameProperty().addListener(onChange {
            // Clear selected when change pic
            State.currentLabelIndex = NOT_FOUND
        })
        Logger.info("Listened for CurrentLabelIndex", LOGSRC_CONTROLLER)
    }
    /**
     * Properties' effect on view
     */
    private fun effect() {
        Logger.info("Applying Affections...", LOGSRC_CONTROLLER)

        // Update LabelInfo
        State.currentGroupIdProperty().addListener(onNew<Number, Int> {
            if (it == NOT_FOUND) labelInfo("Cleared group selection")
            else labelInfo("Selected group $it, ${State.transFile.getTransGroup(it).name}")
        })
        State.currentPicNameProperty().addListener(onNew {
            if (it.isEmpty()) labelInfo("Cleared picture selection")
            else labelInfo("Changed picture to $it")
        })
        State.currentLabelIndexProperty().addListener(onNew<Number, Int> {
            if (it == NOT_FOUND) labelInfo("Cleared label selection")
            else labelInfo("Selected label $it")
        })
        Logger.info("Added effect: show info on InfoLabel", LOGSRC_CONTROLLER)

        // Clear text when some state change
        val clearTextListener = onChange<Any> { cLabelPane.removeText() }
        State.currentGroupIdProperty().addListener(clearTextListener)
        State.workModeProperty().addListener(clearTextListener)
        Logger.info("Added effect: clear text when some state change", LOGSRC_CONTROLLER)

        // Select TreeItem when state change
        State.currentGroupIdProperty().addListener(onNew<Number, Int> {
            if (!State.isOpened || it == NOT_FOUND || State.viewMode != ViewMode.GroupMode) return@onNew
            cTreeView.selectGroup(State.transFile.getTransGroup(it).name, false)
        })
        State.currentLabelIndexProperty().addListener(onNew<Number, Int> {
            if (!State.isOpened || it == NOT_FOUND) return@onNew
            cTreeView.selectLabel(it, false)
        })
        Logger.info("Added effect: select TreeItem on CurrentXXIndex change", LOGSRC_CONTROLLER)

        // Update text area when label change
        State.currentLabelIndexProperty().addListener(onNew<Number, Int> {
            if (!State.isOpened) return@onNew

            // unbind TextArea
            cTransArea.unbindBidirectional()

            if (it == NOT_FOUND) return@onNew

            // bind new text property
            cTransArea.bindBidirectional(State.transFile.getTransLabel(State.currentPicName, it).textProperty)
        })
        Logger.info("Added effect: bind text property on CurrentLabelIndex change", LOGSRC_CONTROLLER)

        // Bind Ctrl/Alt/Meta + Scroll with font size change
        cTransArea.addEventHandler(ScrollEvent.SCROLL) {
            if (!(it.isControlOrMetaDown || it.isAltDown)) return@addEventHandler

            val newSize = (cTransArea.font.size + if (it.deltaY > 0) 1 else -1).toInt()
                .coerceAtLeast(FONT_SIZE_MIN).coerceAtMost(FONT_SIZE_MAX)

            cTransArea.font = cTransArea.font.s(newSize.toDouble())
            cTransArea.positionCaret(0)

            labelInfo("Set text font size to $newSize")
        }
        Logger.info("Added effect: change font size on Ctrl/Alt/Meta + Scroll", LOGSRC_CONTROLLER)

        // Bind Label and Tree
        cTreeView.addEventHandler(MouseEvent.MOUSE_CLICKED) {
            if (it.button != MouseButton.PRIMARY || !it.isDoubleClick) return@addEventHandler

            val item = cTreeView.selectionModel.selectedItem
            if (item != null && item is CTreeLabelItem) cLabelPane.moveToLabel(item.index)
        }
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED) {
            val direction = when (it.code) {
                KeyCode.UP -> -1
                KeyCode.DOWN -> 1
                else -> return@addEventHandler
            }

            val item = cTreeView.getTreeItem(cTreeView.selectionModel.selectedIndex + direction)
            if (item != null && item is CTreeLabelItem) cLabelPane.moveToLabel(item.index)
        }
        Logger.info("Added effect: move to label on CTreeLabelItem select", LOGSRC_CONTROLLER)

        // Work Progress
        val workProgressListener = onChange<Any> {
            if (State.isOpened) RecentFiles.setProgressOf(State.translationFile.path,
                State.transFile.sortedPicNames.indexOf(State.currentPicName) to State.currentLabelIndex
            )
        }
        State.currentPicNameProperty().addListener(workProgressListener)
        State.currentLabelIndexProperty().addListener(workProgressListener)
        Logger.info("Added effect: update work progress on PicName/LabelIndex change", LOGSRC_CONTROLLER)
    }
    /**
     * Transformations
     */
    private fun transform() {
        Logger.info("Applying Transformations...", LOGSRC_CONTROLLER)

        // Transform CTreeView group selection to CGroupBox select
        cTreeView.selectionModel.selectedItemProperty().addListener(onNew {
            if (it != null && it is CTreeGroupItem)
                cGroupBox.select(State.transFile.getGroupIdByName(it.name))
        })
        Logger.info("Transformed CTreeGroupItem selected", LOGSRC_CONTROLLER)

        // Transform tab press in CTreeView to ViewModeBtn click
        cTreeView.addEventFilter(KeyEvent.KEY_PRESSED) {
            if (it.code != KeyCode.TAB) return@addEventFilter

            bSwitchViewMode.fire()
            it.consume() // Disable tab shift
        }
        Logger.info("Transformed Tab on CTreeView", LOGSRC_CONTROLLER)

        // Transform tab press in CLabelPane to WorkModeBtn click
        cLabelPane.addEventFilter(KeyEvent.KEY_PRESSED) {
            if (it.code != KeyCode.TAB) return@addEventFilter

            bSwitchWorkMode.fire()
            it.consume() // Disable tab shift
        }
        Logger.info("Transformed Tab on CLabelPane", LOGSRC_CONTROLLER)

        // Transform number key press to CGroupBox select
        view.addEventHandler(KeyEvent.KEY_PRESSED) {
            if (!it.code.isDigitKey) return@addEventHandler

            val index = it.text.toInt() - 1
            if (index in 0..cGroupBox.items.size) cGroupBox.select(index)
        }
        Logger.info("Transformed num-key pressed", LOGSRC_CONTROLLER)

        // Transform Ctrl + Left/Right KeyEvent to CPicBox button click
        val arrowKeyChangePicHandler = EventHandler<KeyEvent> {
            if (!it.isControlOrMetaDown) return@EventHandler

            when (it.code) {
                KeyCode.LEFT -> cPicBox.back()
                KeyCode.RIGHT -> cPicBox.next()
                else -> return@EventHandler
            }

            it.consume() // Consume used event
        }
        view.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangePicHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangePicHandler)
        Logger.info("Transformed Ctrl + Left/Right", LOGSRC_CONTROLLER)

        // Transform Ctrl + Up/Down KeyEvent to CTreeView select (and have effect: move to label)
        /**
         * Find next LabelItem as int index.
         * @return NOT_FOUND when have no next
         */
        fun getNextLabelItemIndex(from: Int, direction: Int): Int {
            // Make sure we have items to select
            cTreeView.getTreeItem(from).apply { this?.expand() }

            var index = from
            var item: TreeItem<String>?
            do {
                index += direction
                item = cTreeView.getTreeItem(index)
                item?.expand()

                if (item == null) return NOT_FOUND
            } while (item !is CTreeLabelItem)

            return index
        }
        val arrowKeyChangeLabelHandler = EventHandler<KeyEvent> {
            if (!(it.isControlOrMetaDown && it.code.isArrowKey)) return@EventHandler
            // Direction
            val labelItemShift: Int = when (it.code) {
                KeyCode.UP -> -1
                KeyCode.DOWN -> 1
                else -> return@EventHandler
            }
            // Make sure we'll not get into endless LabelItem find loop
            if (State.transFile.getTransList(State.currentPicName).isEmpty()) return@EventHandler

            var labelItemIndex: Int = cTreeView.selectionModel.selectedIndex + labelItemShift

            var item: TreeItem<String>? = cTreeView.getTreeItem(labelItemIndex)
            while (item !is CTreeLabelItem) {
                // if selected first and try getting previous, return last;
                // if selected last and try getting next, return first;
                labelItemIndex = getNextLabelItemIndex(
                    if (labelItemShift == -1)
                        if (labelItemIndex != NOT_FOUND) labelItemIndex else cTreeView.expandedItemCount
                    else
                        if (labelItemIndex != NOT_FOUND) labelItemIndex else 0
                , labelItemShift)
                item = cTreeView.getTreeItem(labelItemIndex)
            }

            cTreeView.selectionModel.clearSelection()
            cTreeView.selectionModel.select(labelItemIndex)
            cTreeView.scrollTo(labelItemIndex)
            cLabelPane.moveToLabel(State.currentLabelIndex)

            it.consume() // Consume used event
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangeLabelHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangeLabelHandler)
        Logger.info("Transformed Ctrl + Up/Down", LOGSRC_CONTROLLER)

        // Transform Ctrl + Enter to Ctrl + Down / Right (+Shift -> back)
        val enterKeyTransformerHandler = EventHandler<KeyEvent> {
            if (!(it.isControlOrMetaDown && it.code == KeyCode.ENTER)) return@EventHandler

            val backward = it.isShiftDown
            val selectedItemIndex = cTreeView.selectionModel.selectedIndex
            val nextLabelItemIndex = getNextLabelItemIndex(selectedItemIndex, if (backward) -1 else 1)

            val code = if (nextLabelItemIndex == NOT_FOUND) {
                // Met the bounds, consider change picture
                if (backward) KeyCode.LEFT else KeyCode.RIGHT
            } else {
                // Got next label, still in this picture
                if (backward) KeyCode.UP else KeyCode.DOWN
            }

            cLabelPane.fireEvent(keyEvent(it, character = "\u0000", text = "", code = code))
            when (code) {
                KeyCode.LEFT  -> cLabelPane.fireEvent(keyEvent(it, character = "\u0000", text = "", code = KeyCode.UP))
                KeyCode.RIGHT -> cLabelPane.fireEvent(keyEvent(it, character = "\u0000", text = "", code = KeyCode.DOWN))
                else -> doNothing()
            }

            it.consume() // Consume used event
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, enterKeyTransformerHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, enterKeyTransformerHandler)
        Logger.info("Transformed Ctrl + Enter", LOGSRC_CONTROLLER)
    }

    // ----- Controller Methods ----- //

    /**
     * Specify pictures of current translation file
     * @return true if completed; false if not; null if cancel
     */
    fun specifyPicFiles(): Boolean? {
        val picFiles = ADialogSpecify.specify()

        // Closed or Cancelled
        if (picFiles.isEmpty()) return null

        val picCount = State.transFile.picCount
        val picNames = State.transFile.sortedPicNames
        var completed = true
        for (i in 0 until picCount) {
            val picFile = picFiles[i]
            if (!picFile.exists()) {
                completed = false
                continue
            }
            State.transFile.setFile(picNames[i], picFile)
        }
        return completed
    }

    /**
     * Whether stay here or not
     */
    fun stay(): Boolean {
        // Not open
        if (!State.isOpened) return false
        // Opened but saved
        if (!State.isChanged) return false

        // Opened but not saved
        val result = showAlert(State.stage, null, I18N["alert.not_save.content"], I18N["common.exit"])
        // Dialog present
        if (result.isPresent) when (result.get()) {
            ButtonType.YES -> {
                save(State.translationFile, silent = true)
                return false
            }
            ButtonType.NO -> return false
            ButtonType.CANCEL -> return true
        }
        // Dialog closed
        return true
    }

    /**
     * Create a new TransFile file and its FileSystem file.
     * @param file Which file the TransFile will write to
     * @param type Which type the Translation file will be
     * @return ProjectFolder if success, null if fail
     */
    fun new(file: File, type: FileType = FileType.getFileType(file)): File? {
        Logger.info("Newing $type to ${file.path}", LOGSRC_CONTROLLER)

        // Choose Pics
        var projectFolder = file.parentFile
        val potentialPics = ArrayList<String>()
        val selectedPics  = ArrayList<String>()
        while (potentialPics.isEmpty()) {
            // Find pictures
            projectFolder.listFiles()?.forEach {
                if (it.isFile && EXTENSIONS_PIC.contains(it.extension)) {
                    potentialPics.add(it.name)
                }
            }

            if (potentialPics.isEmpty()) {
                // Find nothing, this folder isn't project folder, confirm to use another folder
                val result = showConfirm(State.stage, I18N["confirm.project_folder_invalid"])
                if (result.isPresent && result.get() == ButtonType.YES) {
                    // Specify project folder
                    val newFolder = DirectoryChooser().apply { initialDirectory = projectFolder }.showDialog(State.stage)
                    if (newFolder != null) projectFolder = newFolder
                } else {
                    // Do not specify, cancel
                    Logger.info("Cancel (project folder has no pictures)", LOGSRC_CONTROLLER)
                    showInfo(State.stage, I18N["common.cancel"])
                    return null
                }
            } else {
                // Find some pics, continue procedure
                State.projectFolder = projectFolder
                Logger.info("Project folder set to ${projectFolder.path}", LOGSRC_CONTROLLER)
            }
        }
        val result = showChoiceList(State.stage, potentialPics)
        if (result.isPresent) {
            if (result.get().isEmpty()) {
                Logger.info("Cancel (selected none)", LOGSRC_CONTROLLER)
                showInfo(State.stage, I18N["info.required_at_least_1_pic"])
                return null
            }
            selectedPics.addAll(result.get())
        } else {
            Logger.info("Cancel (didn't do the selection)", LOGSRC_CONTROLLER)
            showInfo(State.stage, I18N["common.cancel"])
            return null
        }
        Logger.info("Chose pictures", LOGSRC_CONTROLLER)

        // Prepare new TransFile
        val groupNameList = Settings.defaultGroupNameList
        val groupColorList = Settings.defaultGroupColorHexList
        val groupCreateList = Settings.isGroupCreateOnNewTransList
        val groupList = ArrayList<TransGroup>()
        for (i in groupNameList.indices)
            if (groupCreateList[i]) groupList.add(TransGroup(groupNameList[i], groupColorList[i]))
        val transMap = LinkedHashMap<String, MutableList<TransLabel>>()
        for (pic in selectedPics)
            transMap[pic] = ArrayList()
        val transFile = TransFile(TransFile.DEFAULT_VERSION, TransFile.DEFAULT_COMMENT, groupList, transMap)
        Logger.info("Built TransFile", LOGSRC_CONTROLLER)

        // Export to file
        try {
            export(file, type, transFile)
        } catch (e: IOException) {
            Logger.error("New failed", LOGSRC_CONTROLLER)
            Logger.exception(e)
            showError(State.stage, I18N["error.new_failed"])
            showException(State.stage, e)
            return null
        }
        Logger.info("Newed TransFile", LOGSRC_CONTROLLER)

        return projectFolder
    }
    /**
     * Open a translation file
     * @param file Which file will be open
     * @param type Which type the file is
     * @param projectFolder Which folder the pictures locate in; translation file's folder by default
     */
    fun open(file: File, type: FileType = FileType.getFileType(file), projectFolder: File = file.parentFile) {
        Logger.info("Opening TransFile: ${file.path}", LOGSRC_CONTROLLER)

        // Load File
        val transFile: TransFile
        try {
            transFile = load(file, type)
            // We assume that all pics are in the project folder.
            // If not, TransFile.checkLost() will find them out.
            for (picName in transFile.picNames) transFile.setFile(picName, projectFolder.resolve(picName))
        } catch (e: IOException) {
            Logger.error("Open failed", LOGSRC_CONTROLLER)
            Logger.exception(e)
            showError(State.stage, I18N["error.open_failed"])
            showException(State.stage, e)
            return
        }
        Logger.info("Loaded TransFile", LOGSRC_CONTROLLER)

        // Opened, update State
        State.transFile = transFile
        State.translationFile = file
        State.projectFolder = projectFolder
        State.isOpened = true

        // Show info if comment not in default list
        // Should do this before update RecentFiles
        if (!RecentFiles.recentFiles.contains(file)) {
            val comment = transFile.comment.trim()
            if (!TransFile.DEFAULT_COMMENT_LIST.contains(comment)) {
                Logger.info("Showed modified comment", LOGSRC_CONTROLLER)
                showInfo(State.stage, I18N["m.comment.dialog.content"], comment, I18N["common.info"])
            }
        }

        // Update recent files
        RecentFiles.add(file)

        // Auto backup
        backupManager.clear()
        val bakDir = State.getBakFolder()
        if ((bakDir.exists() && bakDir.isDirectory) || bakDir.mkdir()) {
            backupManager.schedule()
            Logger.info("Scheduled auto-backup", LOGSRC_CONTROLLER)
        } else {
            Logger.warning("Auto-backup unavailable", LOGSRC_CONTROLLER)
            showWarning(State.stage, I18N["warning.auto_backup_unavailable"])
        }

        // Check lost
        if (State.transFile.checkLost().isNotEmpty()) {
            // Specify now?
            showConfirm(State.stage, I18N["specify.confirm.lost_pictures"]).ifPresent {
                if (it == ButtonType.YES) {
                    val completed = specifyPicFiles()
                    if (completed == null) showInfo(State.stage, I18N["specify.info.cancelled"])
                    else if (!completed) showInfo(State.stage, I18N["specify.info.incomplete"])
                }
            }
        }

        // Initialize workspace
        val (picIndex, labelIndex) = RecentFiles.getProgressOf(file.path)
        State.currentGroupId = 0
        State.currentPicName = State.transFile.sortedPicNames[picIndex.takeIf { it in 0 until State.transFile.picCount } ?: 0]
        State.currentLabelIndex = labelIndex.takeIf { State.transFile.getTransList(State.currentPicName).contains { l -> l.index == it } } ?: NOT_FOUND
        if (labelIndex != NOT_FOUND) cLabelPane.moveToLabel(labelIndex)

        // Change title
        State.stage.title = INFO["application.name"] + " - " + file.name

        labelInfo("Opened TransFile: ${file.path}")
    }
    /**
     * Save a TransFile
     * @param file Which file will the TransFile write to
     * @param type Which type will the translation file be
     * @param silent Whether the save procedure is done in silence or not
     */
    fun save(file: File, type: FileType = FileType.getFileType(file), silent: Boolean = false) {
        // Whether overwriting existing file
        val overwrite = file.exists()

        Logger.info("Saving to ${file.path}, silent:$silent, overwrite:$overwrite", LOGSRC_CONTROLLER)

        // Check folder
        if (!silent) if (file.parentFile != State.projectFolder) {
            val confirm = showConfirm(State.stage, I18N["confirm.save_to_another_place"])
            if (!(confirm.isPresent && confirm.get() == ButtonType.YES)) return
        }

        // Use temp if overwrite
        val exportDest = if (overwrite) File.createTempFile(file.path, "temp").apply(File::deleteOnExit) else file

        // Export
        try {
            export(exportDest, type, State.transFile)
        } catch (e: IOException) {
            Logger.error("Export translation failed", LOGSRC_CONTROLLER)
            Logger.exception(e)
            showError(State.stage, I18N["error.save_failed"])
            showException(State.stage, e)

            Logger.info("Save failed", LOGSRC_CONTROLLER)
            return
        }
        Logger.info("Exported translation", LOGSRC_CONTROLLER)

        // Transfer to origin file if overwrite
        if (overwrite) {
            try {
                transfer(exportDest, file)
            } catch (e: Exception) {
                Logger.error("Transfer temp file failed", LOGSRC_CONTROLLER)
                Logger.exception(e)
                showError(State.stage, I18N["error.save_temp_transfer_failed"])
                showException(State.stage, e)

                Logger.info("Save failed", LOGSRC_CONTROLLER)
                return
            }
            Logger.info("Transferred temp file", LOGSRC_CONTROLLER)
        }

        // Update state
        State.translationFile = file
        State.isChanged = false

        // Change title
        State.stage.title = INFO["application.name"] + " - " + file.name

        labelInfo("Saved TransFile to ${file.path}")
        if (!silent) showInfo(State.stage, I18N["info.saved_successfully"])
    }
    /**
     * Recover from backup file
     * @param from The backup file
     * @param to Which file will the backup recover to
     */
    fun recovery(from: File, to: File, type: FileType = FileType.getFileType(to)) {
        Logger.info("Recovering from ${from.path}", LOGSRC_CONTROLLER)

        try {
            val tempFile = File.createTempFile("temp", type.name).apply(File::deleteOnExit)
            val transFile = load(from, FileType.MeoFile)

            export(tempFile, type, transFile)
            transfer(tempFile, to)
        } catch (e: Exception) {
            Logger.error("Recover failed", LOGSRC_CONTROLLER)
            Logger.exception(e)
            showError(State.stage, I18N["error.recovery_failed"])
            showException(State.stage, e)
        }
        Logger.info("Recovered to ${to.path}", LOGSRC_CONTROLLER)

        open(to, type)
    }
    /**
     * Export a TransFile in specific type
     * @param file Which file will the TransFile write to
     * @param type Which type will the translation file be
     */
    fun export(file: File, type: FileType = FileType.getFileType(file)) {
        Logger.info("Exporting to ${file.path}", LOGSRC_CONTROLLER)

        try {
            export(file, type, State.transFile)
        } catch (e: IOException) {
            Logger.error("Export failed", LOGSRC_CONTROLLER)
            Logger.exception(e)
            showError(State.stage, I18N["error.export_failed"])
            showException(State.stage, e)
        }

        labelInfo("Exported to ${file.path}")
        showInfo(State.stage, I18N["info.exported_successful"])
    }
    /**
     * Generate a zip file with translation file and picture files
     * @param file Which file will the zip file write to
     */
    fun pack(file: File) {
        Logger.info("Packing to ${file.path}", LOGSRC_CONTROLLER)

        try {
            pack(file, State.transFile)
        } catch (e : IOException) {
            Logger.error("Pack failed", LOGSRC_CONTROLLER)
            Logger.exception(e)
            showError(State.stage, I18N["error.export_failed"])
            showException(State.stage, e)
        }

        labelInfo("Packed to ${file.path}")
        showInfo(State.stage, I18N["info.exported_successful"])
    }

    fun reset() {
        backupManager.clear()

        cTransArea.unbindBidirectional()

        State.stage.title = INFO["application.name"]
    }

    // ----- Component Methods ----- //

    fun moveLabelTreeItem(labelIndex: Int, oriGroupId: Int, dstGroupId: Int) {
        cTreeView.moveLabelItem(labelIndex, oriGroupId, dstGroupId)

        Logger.info("Moved label item @ $labelIndex @ ori=$oriGroupId, dst=$dstGroupId", LOGSRC_CONTROLLER)
    }
    fun labelInfo(info: String, source: String = LOGSRC_CONTROLLER) {
        lInfo.text = info
        Logger.info(info, source)
    }

    // ----- Global Methods ----- //

    fun requestRepaint() {
        cLabelPaneImageBinding.invalidate()
        cLabelPane.requestShowImage()
        cLabelPane.requestCreateLabels()
    }

}
