package info.meodinger.lpfx.io

import com.fasterxml.jackson.databind.ObjectMapper
import info.meodinger.lpfx.type.TransFile
import info.meodinger.lpfx.type.TransFile.Companion.LPTransFile
import info.meodinger.lpfx.type.TransFile.Companion.MeoTransFile
import info.meodinger.lpfx.type.TransGroup
import info.meodinger.lpfx.type.TransLabel
import info.meodinger.lpfx.util.resource.I18N
import info.meodinger.lpfx.util.resource.get

import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Author: Meodinger
 * Date: 2021/7/29
 * Location: info.meodinger.lpfx.io
 */

/**
 * Load LP File
 */
@Throws(IOException::class)
fun loadLP(file: File): TransFile {

    val transFile = TransFile()
    val reader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))

    // Remove BOM (EF BB BF)
    val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    val buf = ByteArray(3)
    val fis = FileInputStream(file)
    val len = fis.read(buf, 0, 3)
    if (len != 3) throw IOException(I18N["exception.unexpected_eof"])
    if (bom.contentEquals(buf)) reader.read(CharArray(3), 0, 1)

    val lines = reader.readLines()
    val size = lines.size

    var pointer = 0

    /**
     * Parse from current line, leave pointer on mark
     */
    fun parseText(vararg marks: String): String {
        val builder = StringBuilder()

        while (pointer < size) {
            for (mark in marks) {
                if (lines[pointer].startsWith(mark)) {
                    // return when read stop mark
                    return builder.toString().replace(Regex("\n+"), "\n").trim()
                }
            }
            builder.append(lines[pointer]).append("\n")
            pointer++
        }

        // return when eof
        return builder.toString().replace(Regex("\n+"), "\n").trim()
    }

    /**
     * Parse from current line, leave pointer on next pic/label
     */
    fun parseTranLabel(): TransLabel {
        val s = lines[pointer].split(LPTransFile.LABEL_END)
        val props = s[1].replace(LPTransFile.PROP_START, "").replace(LPTransFile.PROP_END, "").split(LPTransFile.SPLIT)

        val index = s[0].replace(LPTransFile.LABEL_START, "").trim().toInt()
        val x = props[0].trim().toDouble()
        val y = props[1].trim().toDouble()
        val groupId = props[2].trim().toInt() - 1

        if (index < 0) throw IOException(String.format(I18N["exception.invalid_index.format"], index))

        pointer++
        return TransLabel(index, x, y, groupId, parseText(LPTransFile.PIC_START, LPTransFile.LABEL_START))
    }

    /**
     * Parse from current line, leave pointer on label/empty
     */
    fun parsePicHead(): String {
        val picName = lines[pointer].replace(LPTransFile.PIC_START, "").replace(LPTransFile.PIC_END, "")
        pointer++
        return picName
    }

    /**
     * Parse from current line, leave pointer on next pic
     */
    fun parsePicBody(): ArrayList<TransLabel> {
        val transLabels = ArrayList<TransLabel>()

        while (pointer < size && lines[pointer].startsWith(LPTransFile.LABEL_START)) {
            val label = parseTranLabel()

            for (l in transLabels) {
                if (l.index == label.index) {
                    throw IOException(String.format(I18N["exception.repeated_index.format"], label.index))
                }
            }
            transLabels.add(label)
        }

        // move to next pic
        while (pointer < size && !lines[pointer].startsWith(LPTransFile.PIC_START)) pointer++

        return transLabels
    }

    // Version
    val v = lines[pointer].split(LPTransFile.SPLIT)
    transFile.version = intArrayOf(v[0].trim().toInt(), v[1].trim().toInt())
    pointer++

    // Separator
    pointer++

    // Group Info and Separator
    var groupCount = 1
    val groupList = ArrayList<TransGroup>()
    while (lines[pointer] != LPTransFile.SEPARATOR && groupCount < 10) {
        if (lines[pointer].isBlank()) throw IOException(I18N["exception.empty_group_name"])

        val group = TransGroup(lines[pointer], MeoTransFile.DEFAULT_COLOR_LIST[groupCount - 1])

        groupList.forEach {
            if (it.name == group.name) throw IOException(String.format(I18N["exception.repeated_group_name.format"], group.name))
        }
        groupList.add(group)

        groupCount++
        pointer++
    }
    if (lines[pointer] != LPTransFile.SEPARATOR) throw IOException(I18N["exception.too_many_groups"])
    transFile.groupList = groupList
    pointer++

    // Comment
    transFile.comment = parseText(LPTransFile.PIC_START)

    // Content
    val transMap = HashMap<String, MutableList<TransLabel>>()
    while (pointer < size && lines[pointer].startsWith(LPTransFile.PIC_START)) {
        transMap[parsePicHead()] = parsePicBody()
    }
    transFile.transMap = transMap

    return transFile
}

/**
 * Load MEO File
 */
@Throws(IOException::class)
fun loadMeo(file: File): TransFile {
    return ObjectMapper().readValue(
        BufferedInputStream(FileInputStream(file)),
        TransFile::class.java
    )
}