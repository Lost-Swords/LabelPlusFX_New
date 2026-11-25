package ink.meodinger.lpfx.component.tools

import ink.meodinger.lpfx.*
import ink.meodinger.lpfx.component.common.CRollerLabel
import ink.meodinger.lpfx.type.TransFile
import ink.meodinger.lpfx.util.component.*
import ink.meodinger.lpfx.component.dialog.showConfirm
import ink.meodinger.lpfx.component.dialog.showError
import ink.meodinger.lpfx.util.file.exists
import ink.meodinger.lpfx.util.property.minus
import ink.meodinger.lpfx.util.property.transform

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.util.Duration
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.stream.Collectors
import kotlin.io.path.*


/**
 * Author: Meodinger
 * Date: 2021/11/22
 * Have fun with my code!
 */

class SpecifyFiles(private val state: State) : Dialog<List<File?>>() {

    private enum class FilenameMode(private val textKey: String) {
        KEEP("specify.filename_mode.keep"),
        RENAME_LP("specify.filename_mode.rename_lp"),
        RENAME_FILE("specify.filename_mode.rename_file");

        override fun toString(): String = I18N[textKey]
    }

    private data class FileRename(val source: File, val target: File)

    companion object {
        private const val RowShift = 1
        private val Unspecified = I18N["specify.unspecified"]
    }

    private val thisWindow = dialogPane.scene.window
    private val contentGridPane = GridPane().apply {
        hgap = 16.0
        vgap = 16.0
        padding = Insets(16.0)
        alignment = Pos.CENTER
    }
    private val fileChooser = FileChooser().apply {
        extensionFilters.addAll(
            FileChooser.ExtensionFilter(I18N["file_type.pictures"], List(EXTENSIONS_PIC.size) { index -> "*.${EXTENSIONS_PIC[index]}" }),
            FileChooser.ExtensionFilter(I18N["file_type.picture_bmp"], "*.${EXTENSION_PIC_BMP}"),
            FileChooser.ExtensionFilter(I18N["file_type.picture_gif"], "*.${EXTENSION_PIC_GIF}"),
            FileChooser.ExtensionFilter(I18N["file_type.picture_png"], "*.${EXTENSION_PIC_PNG}"),
            FileChooser.ExtensionFilter(I18N["file_type.picture_jpeg"], "*.${EXTENSION_PIC_JPG}", "*.${EXTENSION_PIC_JPEG}","*.${EXTENSION_PIC_JFIF}"),
            FileChooser.ExtensionFilter(I18N["file_type.picture_tiff"], "*.${EXTENSION_PIC_TIF}", "*.${EXTENSION_PIC_TIFF}"),
            FileChooser.ExtensionFilter(I18N["file_type.picture_webp"], "*.${EXTENSION_PIC_WEBP}"),
        )
    }
    private val dirChooser = DirectoryChooser()
    private val filenameModeBox = ComboBox<FilenameMode>(FXCollections.observableArrayList(FilenameMode.entries)).apply {
        selectionModel.select(FilenameMode.KEEP)
    }

    private lateinit var workingTransFile: TransFile
    private var picCount: Int = 0
    private var picNames: List<String> = ArrayList()
    private var files: MutableList<File?> = ArrayList()
    private var labels: MutableList<CRollerLabel> = ArrayList()

    init {
        title = I18N["specify.title"]
        dialogPane.prefWidth = 600.0
        dialogPane.prefHeight = 400.0
        dialogPane.buttonTypes.addAll(ButtonType.APPLY, ButtonType.CANCEL)
        // UI hierarchy: the picture mapping table is primary; filename handling and
        // folder selection are secondary controls; Apply remains the only primary action.
        dialogPane.withContent(BorderPane()) {
            val stackPane = StackPane(contentGridPane)
            val scrollPane = ScrollPane(stackPane)
            stackPane.prefWidthProperty().bind(scrollPane.widthProperty() - 16.0)

            center(scrollPane) { style = "-fx-background-color:transparent;" }
            bottom(HBox()) {
                alignment = Pos.CENTER_RIGHT
                spacing = 8.0
                padding = Insets(16.0, 8.0, 8.0, 16.0)

                children.add(Label(I18N["specify.dialog.filename_mode"]))
                children.add(filenameModeBox)
                children.add(Region().apply { HBox.setHgrow(this, Priority.ALWAYS) })
                add(Button(I18N["specify.dialog.choose_folder"])) {
                    does {
                        // need show confirm?
                        var show = false
                        for (label in labels) if (label.text != Unspecified) {
                            show = true
                            break
                        }

                        // preserve already-set path?
                        var preserve = false
                        if (show) {
                            val confirmPre = showConfirm(thisWindow, I18N["specify.confirm.preserve"])
                            preserve = confirmPre.isPresent && confirmPre.get() == ButtonType.YES
                        }

                        // get project folder
                        val directory = dirChooser.showDialog(thisWindow) ?: return@does
                        if (!preserve) state.transFile.projectFolder = directory

                        // auto-fill
                        val newPicPaths = Files.walk(directory.toPath(), 1)
                            .filter { path -> path.extension.lowercase() in EXTENSIONS_PIC }
                            .collect(Collectors.toList())
                        for (i in 0 until picCount) {
                            if (preserve && files[i].exists()) continue

                            val lastIndex = newPicPaths.size - 1
                            for (j in newPicPaths.indices) {
                                val oldPicFile = workingTransFile.getFile(picNames[i])
                                // check full filename & simple filename
                                val fit =
                                    newPicPaths[j].name == oldPicFile.name || newPicPaths[j].nameWithoutExtension == oldPicFile.nameWithoutExtension
                                if (fit) {
                                    labels[i].text = newPicPaths[j].pathString
                                    files[i] = newPicPaths[j].toFile()

                                    // swap
                                    val temp = newPicPaths.last()
                                    newPicPaths[lastIndex] = newPicPaths[j]
                                    newPicPaths[j] = temp
                                }
                            }
                        }
                    }
                }
            }
        }

        setResultConverter {
            when (it) {
                ButtonType.APPLY -> files
                else -> emptyList()
            }
        }
    }

    /**
     * Specify pictures of current translation file
     * @return true if completed; false if not; null if cancel
     */
    fun specify(): Boolean? {
        // clear & re-init
        contentGridPane.children.clear()
        contentGridPane.add(Label(I18N["specify.dialog.pic_name"]), 0, 0)
        contentGridPane.add(Label(I18N["specify.dialog.pic_path"]), 1, 0)

        // update variables
        workingTransFile = state.transFile
        picCount = state.transFile.picCount
        picNames = state.transFile.sortedPicNames

        // prepare
        fileChooser.initialDirectory = state.transFile.projectFolder
        dirChooser.initialDirectory = state.transFile.projectFolder

        files = MutableList(picCount) { workingTransFile.getFile(picNames[it]).takeIf(File::exists) }
        labels = MutableList(picCount) { CRollerLabel().apply {
            prefWidth = 300.0
            text = files[it]?.takeIf(File::exists)?.let(File::getPath) ?: Unspecified
            tooltipProperty().bind(textProperty().transform {
                Tooltip(it).apply {
                    showDelay = Duration.ZERO
                    hideDelay = Duration.ZERO

                    showDuration = Duration.INDEFINITE
                }
            })
            textFillProperty().bind(textProperty().transform {
                if (it == Unspecified) Color.RED else Color.BLACK
            })
        } }

        // add
        for (i in 0 until picCount) {
            val button = Button(I18N["specify.dialog.choose_file"]) does {
                // Manually specify pic file
                val picFile = fileChooser.showOpenDialog(thisWindow) ?: return@does
                labels[i].text = picFile.path
                files[i] = picFile
            }

            contentGridPane.add(Label(picNames[i]), 0, i + RowShift)
            contentGridPane.add(labels[i], 1, i + RowShift)
            contentGridPane.add(button, 2, i + RowShift)
        }

        // manually control label roll to save resource
        for (label in labels) label.startRoll()
        val result = showAndWait()
        for (label in labels) label.stopRoll()

        // Result list has the same order as TransFile.sortedPicNames.
        // Empty list if Closed or Cancelled;
        // List of specified files (not-null) or didn't specified files (null)
        val picFiles = if (result.isPresent) result.get() else emptyList()

        // Closed or Cancelled
        if (picFiles.isEmpty()) return null

        return applySelectedFiles(picFiles)
    }

    private fun applySelectedFiles(picFiles: List<File?>): Boolean {
        val completed = picFiles.all { it.exists() }
        return when (filenameModeBox.selectionModel.selectedItem ?: FilenameMode.KEEP) {
            FilenameMode.KEEP -> {
                for (i in picNames.indices) {
                    val picFile = picFiles[i]
                    if (picFile.exists()) workingTransFile.setFile(picNames[i], picFile)
                }
                completed
            }

            FilenameMode.RENAME_LP -> renameLpPictures(picFiles, completed)
            FilenameMode.RENAME_FILE -> renameActualFiles(picFiles, completed)
        }
    }

    private fun renameLpPictures(picFiles: List<File?>, completed: Boolean): Boolean {
        val renamedNames = LinkedHashMap<String, String>()
        val resolvedFiles = LinkedHashMap<String, File>()
        for (i in picNames.indices) {
            val picFile = picFiles[i]?.takeIf(File::exists) ?: continue

            renamedNames[picNames[i]] = picFile.name
            resolvedFiles[picNames[i]] = picFile
        }

        val finalNames = picNames.map { renamedNames[it] ?: it }
        val duplicateName = finalNames.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.key
        if (duplicateName != null) {
            showError(thisWindow, String.format(I18N["specify.error.duplicate_picture_name.s"], duplicateName))
            return false
        }

        val oldCurrentPicName = state.currentPicName
        workingTransFile.renamePictures(renamedNames, resolvedFiles)
        renamedNames[oldCurrentPicName]?.let { state.currentPicName = it }
        if (renamedNames.any { (oldName, newName) -> oldName != newName }) state.isChanged = true
        return completed
    }

    private fun renameActualFiles(picFiles: List<File?>, completed: Boolean): Boolean {
        val selectedFiles = picNames.indices.mapNotNull { index ->
            picFiles[index]?.takeIf(File::exists)?.let { picNames[index] to it }
        }
        val duplicateSource = selectedFiles
            .groupingBy { (_, file) -> file.toPath().toAbsolutePath().normalize() }
            .eachCount().entries.firstOrNull { it.value > 1 }?.key
        if (duplicateSource != null) {
            showError(thisWindow, String.format(I18N["specify.error.picture_reused.s"], duplicateSource))
            return false
        }

        val renames = selectedFiles.mapNotNull { (picName, source) ->
            val target = source.parentFile.resolve(picName)
            if (source.toPath().toAbsolutePath().normalize() == target.toPath().toAbsolutePath().normalize()) null
            else FileRename(source, target)
        }
        for ((source, target) in renames) {
            if (target.exists() && !Files.isSameFile(source.toPath(), target.toPath())) {
                showError(thisWindow, String.format(I18N["specify.error.rename_target_exists.s"], target.path))
                return false
            }
        }

        val moved = ArrayList<FileRename>()
        try {
            for (rename in renames) {
                Files.move(rename.source.toPath(), rename.target.toPath())
                moved.add(rename)
            }
        } catch (e: IOException) {
            for (rename in moved.asReversed()) {
                try {
                    Files.move(rename.target.toPath(), rename.source.toPath())
                } catch (_: IOException) {
                    // Best-effort rollback; the original failure is reported below.
                }
            }
            showError(thisWindow, String.format(I18N["specify.error.rename_failed.s"], e.message ?: e.javaClass.simpleName))
            return false
        }

        val renamedFiles = renames.associate { it.source to it.target }
        for ((picName, source) in selectedFiles) {
            workingTransFile.setFile(picName, renamedFiles[source] ?: source)
        }
        return completed
    }

}
