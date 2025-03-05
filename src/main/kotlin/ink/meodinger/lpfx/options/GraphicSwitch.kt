package ink.meodinger.lpfx.options

import ink.meodinger.lpfx.Config
import ink.meodinger.lpfx.util.file.appendLine
import ink.meodinger.lpfx.util.file.transfer


import java.io.File

/**
 * Author: Meodinger
 * Date: 2022/5/25
 * Have fun with my code!
 */

/**
 * App config file
 */
private val CFG_File: File = Config.workingDir.resolve("app/LabelPlusFX.cfg")
private val HW_File: File = Config.workingDir.resolve("app/LabelPlusFX-HW.cfg").apply {
    // Check if the file exists, if not, copy from CFG_File and execute transfer function
    if (!exists()) {
        transfer(CFG_File, this)
        appendLine(this,"java-options=-Dprism.order=d3d" )

    }
}
private val SW_File: File = Config.workingDir.resolve("app/LabelPlusFX-SW.cfg").apply {
    // Check if the file exists, if not, copy from CFG_File and execute transfer function
    if (!exists()) {
        transfer(CFG_File, this)
        appendLine(this,"java-options=-Dprism.order=sw" )
    }
}

/**
 * Switch Prism to software, require restart the application.
 * **This function should only be used on Windows.**
 */
fun useSoftwarePrism() {
    transfer(SW_File, CFG_File)
}

/**
 * Switch Prism to hardware, require restart the application.
 * **This function should only be used on Windows.**
 */
fun useHardwarePrism() {
    transfer(HW_File, CFG_File)
}
