package ink.meodinger.lpfx.component.singleton

import ink.meodinger.lpfx.*
import ink.meodinger.lpfx.component.common.CFileChooser
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.options.RecentFiles
import ink.meodinger.lpfx.options.Settings
import ink.meodinger.lpfx.util.component.disableMnemonicParsingForAll
import ink.meodinger.lpfx.util.dialog.*
import ink.meodinger.lpfx.util.platform.isMac
import ink.meodinger.lpfx.util.resource.I18N
import ink.meodinger.lpfx.util.resource.INFO
import ink.meodinger.lpfx.util.resource.get

import javafx.event.ActionEvent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.stage.FileChooser
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
import kotlin.io.path.extension
import kotlin.io.path.name


/**
 * Author: Meodinger
 * Date: 2021/8/17
 * Have fun with my code!
 */

/**
 * A MenuBar for main scene, did not make it singleton for fxml loader
 */
object AMenuBar : MenuBar() {

    private val mmFile = Menu(I18N["mm.file"])
    private val mNew = MenuItem(I18N["m.new"])
    private val mOpen = MenuItem(I18N["m.open"])
    private val mOpenRecent = Menu(I18N["m.recent"])
    private val mSave = MenuItem(I18N["m.save"])
    private val mSaveAs = MenuItem(I18N["m.save_as"])
    private val mClose = MenuItem(I18N["m.close"])
    private val mBakRecover = MenuItem(I18N["m.bak_recovery"])
    private val mExit = MenuItem(I18N["m.exit"])
    private val mmExport = Menu(I18N["mm.export"])
    private val mExportAsLp = MenuItem(I18N["m.lp"])
    private val mExportAsMeo = MenuItem(I18N["m.meo"])
    private val mExportAsTransPack = MenuItem(I18N["m.pack"])
    private val mEditComment = MenuItem(I18N["m.comment"])
    private val mEditPictures = MenuItem(I18N["m.pictures"])
    private val mmAbout = Menu(I18N["mm.about"])
    private val mSettings = MenuItem(I18N["m.settings"])
    private val mLogs = MenuItem(I18N["m.logs"])
    private val mAbout = MenuItem(I18N["m.about"])
    private val mCrash = MenuItem("Crash")

    private val fileFilter = FileChooser.ExtensionFilter(I18N["filetype.translation"], "*.${EXTENSION_MEO}", "*.${EXTENSION_LP}")
    private val lpFilter = FileChooser.ExtensionFilter(I18N["filetype.translation_lp"], "*.${EXTENSION_LP}")
    private val meoFilter = FileChooser.ExtensionFilter(I18N["filetype.translation_meo"], "*.${EXTENSION_MEO}")
    private val bakFilter = FileChooser.ExtensionFilter(I18N["filetype.bak"], "*.${EXTENSION_BAK}")
    private val packFilter = FileChooser.ExtensionFilter(I18N["filetype.pack"], "*.${EXTENSION_PACK}")
    private val fileChooser = CFileChooser()
    private val bakChooser = CFileChooser()
    private val exportChooser = CFileChooser()
    private val exportPackChooser = CFileChooser()

    init {
        fileChooser.extensionFilters.addAll(fileFilter, meoFilter, lpFilter)
        bakChooser.title = I18N["chooser.bak"]
        bakChooser.extensionFilters.add(bakFilter)
        exportChooser.title = I18N["chooser.export"]
        exportPackChooser.title = I18N["chooser.pack"]
        exportPackChooser.extensionFilters.add(packFilter)
        exportPackChooser.initialFileName = "$PACKAGE_FILE_NAME.$EXTENSION_PACK"

        this.disableMnemonicParsingForAll()

        mNew.setOnAction { newTranslation() }
        mOpen.setOnAction { openTranslation() }
        mSave.setOnAction { saveTranslation() }
        mSaveAs.setOnAction { saveAsTranslation() }
        mClose.setOnAction { closeTranslation() }
        mBakRecover.setOnAction { bakRecovery() }
        mExit.setOnAction { State.controller.exit() }
        mExportAsLp.setOnAction { exportTransFile(it) }
        mExportAsMeo.setOnAction { exportTransFile(it) }
        mExportAsTransPack.setOnAction { exportTransPack() }
        mEditComment.setOnAction { editComment() }
        mEditPictures.setOnAction { editPictures() }
        mSettings.setOnAction { settings() }
        mLogs.setOnAction { logs() }
        mAbout.setOnAction { about() }
        mCrash.setOnAction { crash() }

        mSave.disableProperty().bind(!State.isOpenedProperty)
        mSaveAs.disableProperty().bind(!State.isOpenedProperty)
        mClose.disableProperty().bind(!State.isOpenedProperty)
        mExportAsLp.disableProperty().bind(!State.isOpenedProperty)
        mExportAsMeo.disableProperty().bind(!State.isOpenedProperty)
        mExportAsTransPack.disableProperty().bind(!State.isOpenedProperty)
        mEditComment.disableProperty().bind(!State.isOpenedProperty)
        mEditPictures.disableProperty().bind(!State.isOpenedProperty)

        // Set accelerators
        if (isMac) {
            mSave.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.META_DOWN)
            mSaveAs.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN)
        } else {
            mSave.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
            mSaveAs.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
        }

        mmFile.items.addAll(mNew, mOpen, mOpenRecent, mClose, SeparatorMenuItem(), mSave, mSaveAs, SeparatorMenuItem(), mBakRecover, SeparatorMenuItem(), mExit)
        mmExport.items.addAll(mExportAsLp, mExportAsMeo, mExportAsTransPack, SeparatorMenuItem(), mEditComment, mEditPictures)
        mmAbout.items.addAll(mSettings, mLogs, SeparatorMenuItem(), mAbout, SeparatorMenuItem(), mCrash)
        this.menus.addAll(mmFile, mmExport, mmAbout)
    }

    fun updateOpenRecent() {
        mOpenRecent.items.clear()
        for (path in RecentFiles.getAll()) {
            mOpenRecent.items.add(MenuItem(path).also { it.setOnAction { _ -> openRecentTranslation(it) } })
        }
    }
    private fun openRecentTranslation(item: MenuItem) {
        val path = item.text
        val file = File(path)

        if (!file.exists()) {
            showError(String.format(I18N["error.file_not_exist.s"], path), State.stage)
            RecentFiles.remove(path)
            mOpenRecent.items.remove(item)
            return
        }

        if (State.controller.stay()) return

        State.reset()

        State.controller.open(file, FileType.getType(file))
    }

    private fun newTranslation() {
        // new & open

        if (State.controller.stay()) return

        State.reset()

        fileChooser.title = I18N["chooser.new"]
        fileChooser.initialFileName = "$INITIAL_FILE_NAME.$EXTENSION_MEO"
        val file = fileChooser.showSaveDialog(State.stage) ?: return
        val type = FileType.getType(file)

        val newed = State.controller.new(file, type)
        if (newed) State.controller.open(file, type)
    }
    private fun openTranslation() {
        // open

        if (State.controller.stay()) return

        State.reset()

        fileChooser.title = I18N["chooser.open"]
        fileChooser.initialFileName = ""
        val file = fileChooser.showOpenDialog(State.stage) ?: return

        State.controller.open(file, FileType.getType(file))
    }
    private fun saveTranslation() {
        // save

        State.controller.save(State.translationFile, FileType.getType(State.translationFile), true)
    }
    private fun saveAsTranslation() {
        // save

        fileChooser.title = I18N["chooser.save"]
        fileChooser.initialFileName = State.translationFile.name
        val file = fileChooser.showSaveDialog(State.stage) ?: return

        State.controller.save(file, FileType.getType(file), false)
    }
    private fun closeTranslation() {
        if (State.controller.stay()) return

        State.reset()
    }
    private fun bakRecovery() {
        // transfer & open

        if (State.controller.stay()) return

        State.reset()

        val bak = bakChooser.showOpenDialog(State.stage) ?: return
        CFileChooser.lastDirectory = bak.parentFile.parentFile

        fileChooser.title = I18N["chooser.rec"]
        fileChooser.initialFileName = "$RECOVERY_FILE_NAME.$EXTENSION_MEO"
        val rec = fileChooser.showSaveDialog(State.stage) ?: return

        State.controller.recovery(bak, rec)
    }

    private fun exportTransFile(event: ActionEvent) {
        exportChooser.extensionFilters.clear()

        val file: File
        if (event.source == mExportAsMeo) {
            exportChooser.extensionFilters.add(meoFilter)
            exportChooser.initialFileName = "$EXPORT_FILE_NAME.$EXTENSION_MEO"
            file = exportChooser.showSaveDialog(State.stage) ?: return
            State.controller.export(file, FileType.MeoFile)
        } else {
            exportChooser.extensionFilters.add(lpFilter)
            exportChooser.initialFileName = "$EXPORT_FILE_NAME. $EXTENSION_LP"
            file = exportChooser.showSaveDialog(State.stage) ?: return
            State.controller.export(file, FileType.LPFile)
        }
    }
    private fun exportTransPack() {
        val file = exportPackChooser.showSaveDialog(State.stage) ?: return

        State.controller.pack(file)
    }

    private fun editComment() {
        showInputArea(State.stage, I18N["dialog.edit_comment.title"], State.transFile.comment).ifPresent {
            State.setComment(it)
            State.isChanged = true
        }
    }
    private fun editPictures() {
        // Choose Pics
        val selected = State.transFile.sortedPicNames
        val unselected = Files.walk(State.getFileFolder().toPath(), 1).filter {
            if (selected.contains(it.name)) return@filter false
            for (extension in EXTENSIONS_PIC) if (it.extension == extension) return@filter true
            false
        }.map { it.name }.collect(Collectors.toList())

        // todo: use checkPic()
        showChoiceList(State.stage, unselected, selected).ifPresent {
            if (it.isEmpty()) {
                showInfo(I18N["info.required_at_least_1_pic"], State.stage)
                return@ifPresent
            }

            val picNames = State.transFile.sortedPicNames

            // Edit date
            val toRemove = ArrayList<String>()
            for (picName in picNames) if (!it.contains(picName)) toRemove.add(picName)
            val toAdd = ArrayList<String>()
            for (picName in it) if (!picNames.contains(picName)) toAdd.add(picName)

            if (toRemove.size == 0 && toAdd.size == 0) return@ifPresent
            if (toRemove.size != 0) {
                val confirm = showConfirm(I18N["confirm.removing_pic"], State.stage)
                if (!confirm.isPresent || confirm.get() != ButtonType.YES) return@ifPresent
            }

            for (picName in toRemove) State.removePicture(picName)
            for (picName in toAdd) State.addPicture(picName)
            // Mark change
            State.isChanged = true
        }
    }

    private fun settings() {
        val list = ASettingsDialog.generateProperties()

        var updatePane = false
        var updateRules = false

        for (property in list) {
            if (Settings[property.key].asString() == property.value) continue

            if (property.key == Settings.LabelAlpha || property.key == Settings.LabelRadius) updatePane = true
            if (property.key == Settings.LigatureRules) updateRules = true

            Settings[property.key] = property
        }

        if (updatePane && State.isOpened) State.controller.renderLabelPane()
        if (updateRules) State.controller.updateLigatureRules()
    }
    private fun logs() {
        val list = ALogsDialog.generateProperties()

        for (property in list) {
            if (Settings[property.key].asString() == property.value) continue

            when (property.key) {
                Settings.LogLevelPreference -> Logger.level = Logger.LogType.getType(property.value)
            }

            Settings[property.key] = property
        }
    }
    private fun about() {
        showLink(
            State.stage,
            I18N["dialog.about.title"],
            null,
            StringBuilder()
                .append(INFO["application.name"]).append(" - ").append(INFO["application.version"]).append("\n")
                .append("Developed By ").append(INFO["application.vendor"]).append("\n")
                .toString(),
            INFO["application.link"]
        ) {
            State.application.hostServices.showDocument(INFO["application.url"])
        }
    }
    private fun crash() {
        val key = "C_Crash_Count"
        if (this.properties[key] == null) this.properties[key] = 0

        this.properties[key] = (this.properties[key] as Int) + 1
        if (this.properties[key] as Int >= 5) {
            this.properties[key] = 0
            val confirm = showConfirm(I18N["confirm.extra"], State.stage)
            if (confirm.isPresent && confirm.get() == ButtonType.YES) {
                State.controller.justMonika()
            } else return
        }

        throw RuntimeException("Crash")
    }
}