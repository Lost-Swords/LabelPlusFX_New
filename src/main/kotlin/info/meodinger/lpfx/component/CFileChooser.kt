package info.meodinger.lpfx.component

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.stage.FileChooser
import javafx.stage.Window
import java.io.File

/**
 * Author: Meodinger
 * Date: 2021/7/29
 * Location: info.meodinger.lpfx.component
 */
class CFileChooser {

    companion object {
        var lastDirectory : File?
            get() = lastDirectoryProperty.value
            set(value) {
                if (value == null) return
                if (value.isDirectory) lastDirectoryProperty.set(value)
                else lastDirectoryProperty.set(value.parentFile)
            }

        val lastDirectoryProperty: ObjectProperty<File> = SimpleObjectProperty(File(System.getProperty("user.home")))
    }

    private val chooser = FileChooser()

    init {
        chooser.initialDirectoryProperty().bind(lastDirectoryProperty)
    }

    fun showOpenDialog(owner: Window?): File? {
        val file = chooser.showOpenDialog(owner)
        lastDirectory = file
        return file
    }

    fun showSaveDialog(owner: Window?): File? {
        val file = chooser.showSaveDialog(owner)
        lastDirectory = file
        return file
    }

    fun setTitle(title: String?) {
        chooser.title = title
    }

    fun getExtensionFilters(): List<FileChooser.ExtensionFilter> {
        return chooser.extensionFilters
    }
}