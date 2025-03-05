package ink.meodinger.lpfx.util.file

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


/**
 * Author: Meodinger
 * Date: 2021/8/24
 * Have fun with my code!
 */

/**
 * Transfer a File to another File
 * @param ori File that transfer from
 * @param dst File that transfer to
 */
@Throws(IOException::class)
fun transfer(ori: File, dst: File, overwrite: Boolean = true) {
    if (!ori.exists()) throw IOException("Source file `$ori` not exists")
    if (!overwrite && dst.exists()) throw IOException("Destination file `$dst` already exists")
    if (ori.isDirectory || dst.isDirectory) throw IOException("Cannot transfer Directory")

    val input = FileInputStream(ori).channel
    val output = FileOutputStream(dst).channel

    output.transferFrom(input, 0, input.size())

    input.close()
    output.close()
}

/**
 * Append a line to a file if it does not already exist in the file
 * @param file The file to append to
 * @param line The line to append
 */
@Throws(IOException::class)
fun appendLine(file: File, line: String) {
    if (!file.exists()) throw IOException("Source file `$file` not exists")
    val lines = file.readLines()
    if (!lines.contains(line)) {
        file.appendText("\n$line", Charsets.UTF_8)
    }
}

/**
 * Whether this file exists. `null` is treat as not exist.
 */
@Throws(SecurityException::class)
fun File?.exists(): Boolean = this != null && exists()
