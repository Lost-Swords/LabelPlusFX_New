package ink.meodinger.lpfx.input

import ink.meodinger.lpfx.Config
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent


object ShortcutAction {
    const val GROUP_SELECT_DIGITS = "group_select_digits"
    const val PIC_PREV_Q = "pic_prev_q"
    const val PIC_NEXT_W = "pic_next_w"
    const val MODE_TOGGLE = "mode_toggle"
    const val PIC_PREV_ARROW = "pic_prev_arrow"
    const val PIC_NEXT_ARROW = "pic_next_arrow"
    const val LABEL_PREV = "label_prev"
    const val LABEL_NEXT = "label_next"
    const val LABEL_NEXT_CROSS = "label_next_cross"
    const val LABEL_PREV_CROSS = "label_prev_cross"
    const val LABEL_GROUP_PREV = "label_group_prev"
    const val LABEL_GROUP_NEXT = "label_group_next"
    const val LABEL_INDEX_PREV = "label_index_prev"
    const val LABEL_INDEX_NEXT = "label_index_next"
    const val LABEL_MARK_TOGGLE = "label_mark_toggle"
    const val TRAN_UNDO = "trans_undo"
    const val TRAN_REDO = "trans_redo"
    const val LABEL_COPY = "label_copy"
    const val LABEL_PASTE = "label_paste"
    const val TRAN_FONT_SIZE_SCROLL = "trans_font_size_scroll"
    const val LABEL_ADD = "label_add"
    const val LABEL_REMOVE = "label_remove"
    const val EXPORT_CURRENT_PAGE_LP = "export_current_page_lp"
}

enum class ShortcutScope {
    IMAGE_VIEW,
    GLOBAL,
}

data class ShortcutDefinition(
    val id: String,
    val labelKey: String,
    val scopes: Set<ShortcutScope>,
    val defaultGesture: ShortcutGesture,
    val editable: Boolean = true,
    val groupKey: String,
)

data class ModifierSpec(
    val shift: Boolean = false,
    val alt: Boolean = false,
    val ctrl: Boolean = false,
    val meta: Boolean = false,
    val shortcut: Boolean = false,
) {
    fun matches(isShiftDown: Boolean, isAltDown: Boolean, isControlDown: Boolean, isMetaDown: Boolean): Boolean {
        if (shortcut && isControlDown && isMetaDown) return false
        if (shortcut && !(isControlDown || isMetaDown)) return false
        if (ctrl && !isControlDown) return false
        if (meta && !isMetaDown) return false
        if (alt && !isAltDown) return false
        if (shift && !isShiftDown) return false

        if (!shift && isShiftDown) return false
        if (!alt && isAltDown) return false
        if (!ctrl && isControlDown && !shortcut) return false
        if (!meta && isMetaDown && !shortcut) return false

        return true
    }

    fun toDisplayParts(): List<String> {
        val parts = mutableListOf<String>()
        if (shortcut) parts += "Ctrl/Meta"
        if (ctrl) parts += "Ctrl"
        if (meta) parts += "Meta"
        if (alt) parts += "Alt/Option"
        if (shift) parts += "Shift"
        return parts
    }

    fun toStorageParts(): List<String> {
        val parts = mutableListOf<String>()
        if (shortcut) parts += "Shortcut"
        if (ctrl) parts += "Ctrl"
        if (meta) parts += "Meta"
        if (alt) parts += "Alt"
        if (shift) parts += "Shift"
        return parts
    }

    fun tokenVariants(): Set<String> {
        var variants: Set<Set<String>> = setOf(emptySet())
        if (shortcut) {
            val next = mutableSetOf<Set<String>>()
            for (variant in variants) {
                next += variant + "CTRL"
                next += variant + "META"
            }
            variants = next
        }
        if (ctrl) variants = variants.map { it + "CTRL" }.toSet()
        if (meta) variants = variants.map { it + "META" }.toSet()
        if (alt) variants = variants.map { it + "ALT" }.toSet()
        if (shift) variants = variants.map { it + "SHIFT" }.toSet()
        return variants.map { it.joinToString("+") }.toSet()
    }
}

sealed class ShortcutGesture {
    abstract fun toDisplayString(): String
    abstract fun toStorageString(): String
    abstract fun conflictTokens(): Set<String>
    open fun matchesKey(event: KeyEvent): Boolean = false
    open fun matchesMouse(event: MouseEvent): Boolean = false
    open fun matchesScroll(event: ScrollEvent): Boolean = false

    companion object {
        fun parse(raw: String?): ShortcutGesture? {
            if (raw.isNullOrBlank()) return null
            val (type, body) = raw.split(":", limit = 2).let {
                if (it.size == 2) it[0] to it[1] else return null
            }
            return when (type) {
                "KEY" -> parseKey(body)
                "MOUSE" -> parseMouse(body)
                "SCROLL" -> parseScroll(body)
                "DIGITS" -> DigitRangeGesture(1, 9)
                "FIXED" -> FixedGesture(body)
                else -> null
            }
        }

        private fun parseKey(body: String): ShortcutGesture? {
            val parts = body.split("+").filter(String::isNotBlank)
            val modifiers = parseModifiers(parts.dropLast(1))
            val keyName = parts.lastOrNull() ?: return null
            val key = KeyCode.entries.firstOrNull { it.name == keyName } ?: return null
            return KeyGesture(key, modifiers)
        }

        private fun parseMouse(body: String): ShortcutGesture? {
            val parts = body.split("+").filter(String::isNotBlank)
            val modifiers = parseModifiers(parts.dropLast(1))
            val buttonName = parts.lastOrNull() ?: return null
            val button = when (buttonName) {
                "PRIMARY" -> MouseButton.PRIMARY
                "SECONDARY" -> MouseButton.SECONDARY
                "MIDDLE" -> MouseButton.MIDDLE
                else -> return null
            }
            return MouseGesture(button, modifiers)
        }

        private fun parseScroll(body: String): ShortcutGesture {
            val modifiers = parseModifiers(body.split("+").filter(String::isNotBlank))
            return ScrollGesture(modifiers)
        }

        private fun parseModifiers(parts: List<String>): ModifierSpec {
            var shift = false
            var alt = false
            var ctrl = false
            var meta = false
            var shortcut = false
            for (part in parts) {
                when (part) {
                    "Shift" -> shift = true
                    "Alt" -> alt = true
                    "Ctrl" -> ctrl = true
                    "Meta" -> meta = true
                    "Shortcut" -> shortcut = true
                }
            }
            return ModifierSpec(shift = shift, alt = alt, ctrl = ctrl, meta = meta, shortcut = shortcut)
        }
    }
}

data class KeyGesture(val code: KeyCode, val modifiers: ModifierSpec) : ShortcutGesture() {
    override fun toDisplayString(): String = (modifiers.toDisplayParts() + code.displayName()).joinToString("+")

    override fun toStorageString(): String = "KEY:" + (modifiers.toStorageParts() + code.name).joinToString("+")

    override fun conflictTokens(): Set<String> {
        val modifierTokens = modifiers.tokenVariants().ifEmpty { setOf("") }
        return modifierTokens.map { "KEY:${it}+${code.name}".trimEnd('+') }.toSet()
    }

    override fun matchesKey(event: KeyEvent): Boolean {
        if (event.code != code) return false
        return modifiers.matches(event.isShiftDown, event.isAltDown, event.isControlDown, event.isMetaDown)
    }
}

data class MouseGesture(val button: MouseButton, val modifiers: ModifierSpec) : ShortcutGesture() {
    override fun toDisplayString(): String =
        (modifiers.toDisplayParts() + button.displayName()).joinToString("+")

    override fun toStorageString(): String = "MOUSE:" + (modifiers.toStorageParts() + button.storageName()).joinToString("+")

    override fun conflictTokens(): Set<String> {
        val modifierTokens = modifiers.tokenVariants().ifEmpty { setOf("") }
        return modifierTokens.map { "MOUSE:${it}+${button.storageName()}".trimEnd('+') }.toSet()
    }

    override fun matchesMouse(event: MouseEvent): Boolean {
        if (event.button != button) return false
        return modifiers.matches(event.isShiftDown, event.isAltDown, event.isControlDown, event.isMetaDown)
    }
}

data class ScrollGesture(val modifiers: ModifierSpec) : ShortcutGesture() {
    override fun toDisplayString(): String =
        (modifiers.toDisplayParts() + "Wheel").joinToString("+")

    override fun toStorageString(): String = "SCROLL:" + modifiers.toStorageParts().joinToString("+")

    override fun conflictTokens(): Set<String> {
        val modifierTokens = modifiers.tokenVariants().ifEmpty { setOf("") }
        return modifierTokens.map { "SCROLL:${it}" }.toSet()
    }

    override fun matchesScroll(event: ScrollEvent): Boolean {
        return modifiers.matches(event.isShiftDown, event.isAltDown, event.isControlDown, event.isMetaDown)
    }
}

data class DigitRangeGesture(val from: Int, val to: Int) : ShortcutGesture() {
    override fun toDisplayString(): String = "Digit $from-$to"

    override fun toStorageString(): String = "DIGITS:$from-$to"

    override fun conflictTokens(): Set<String> {
        return (from..to).mapNotNull { digit ->
            KeyCode.getKeyCode("DIGIT$digit")?.name?.let { "KEY:$it" }
        }.toSet()
    }

    override fun matchesKey(event: KeyEvent): Boolean {
        if (!event.code.isDigitKey) return false
        if (event.isAltDown || event.isControlDown || event.isMetaDown || event.isShiftDown) return false
        val number = event.text.toIntOrNull() ?: return false
        return number in from..to
    }
}

data class FixedGesture(val text: String) : ShortcutGesture() {
    override fun toDisplayString(): String = text
    override fun toStorageString(): String = "FIXED:$text"
    override fun conflictTokens(): Set<String> = emptySet()
}

object ShortcutRegistry {
    val definitions: List<ShortcutDefinition> = listOf(
        ShortcutDefinition(
            ShortcutAction.GROUP_SELECT_DIGITS,
            "shortcut.action.group_select_digits",
            setOf(ShortcutScope.IMAGE_VIEW),
            DigitRangeGesture(1, 9),
            editable = false,
            groupKey = "settings.shortcut.group.image_view",
        ),
        ShortcutDefinition(
            ShortcutAction.PIC_PREV_Q,
            "shortcut.action.pic_prev_q",
            setOf(ShortcutScope.IMAGE_VIEW),
            KeyGesture(KeyCode.Q, ModifierSpec()),
            groupKey = "settings.shortcut.group.image_view",
        ),
        ShortcutDefinition(
            ShortcutAction.PIC_NEXT_W,
            "shortcut.action.pic_next_w",
            setOf(ShortcutScope.IMAGE_VIEW),
            KeyGesture(KeyCode.W, ModifierSpec()),
            groupKey = "settings.shortcut.group.image_view",
        ),
        ShortcutDefinition(
            ShortcutAction.MODE_TOGGLE,
            "shortcut.action.mode_toggle",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.TAB, ModifierSpec()),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.EXPORT_CURRENT_PAGE_LP,
            "shortcut.action.export_current_page_lp",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.E, ModifierSpec(shortcut = true, shift = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_ADD,
            "shortcut.action.label_add",
            setOf(ShortcutScope.IMAGE_VIEW),
            MouseGesture(MouseButton.PRIMARY, ModifierSpec(shortcut = true)),
            groupKey = "settings.shortcut.group.image_view",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_REMOVE,
            "shortcut.action.label_remove",
            setOf(ShortcutScope.IMAGE_VIEW),
            MouseGesture(MouseButton.SECONDARY, ModifierSpec(shortcut = true)),
            groupKey = "settings.shortcut.group.image_view",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_MARK_TOGGLE,
            "shortcut.action.label_mark_toggle",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(
                KeyCode.X,
                ModifierSpec(ctrl = Config.isMac, alt = !Config.isMac)
            ),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_PREV,
            "shortcut.action.label_prev",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.UP, ModifierSpec(shortcut = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_NEXT,
            "shortcut.action.label_next",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.DOWN, ModifierSpec(shortcut = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.PIC_PREV_ARROW,
            "shortcut.action.pic_prev",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.LEFT, ModifierSpec(shortcut = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.PIC_NEXT_ARROW,
            "shortcut.action.pic_next",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.RIGHT, ModifierSpec(shortcut = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_NEXT_CROSS,
            "shortcut.action.label_next_cross",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.ENTER, ModifierSpec(shortcut = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_PREV_CROSS,
            "shortcut.action.label_prev_cross",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.ENTER, ModifierSpec(shortcut = true, shift = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_INDEX_PREV,
            "shortcut.action.label_index_prev",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.UP, ModifierSpec(alt = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_INDEX_NEXT,
            "shortcut.action.label_index_next",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.DOWN, ModifierSpec(alt = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_GROUP_PREV,
            "shortcut.action.label_group_prev",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.LEFT, ModifierSpec(alt = true)),
            groupKey = "settings.shortcut.group.general",
        ),
        ShortcutDefinition(
            ShortcutAction.LABEL_GROUP_NEXT,
            "shortcut.action.label_group_next",
            setOf(ShortcutScope.GLOBAL),
            KeyGesture(KeyCode.RIGHT, ModifierSpec(alt = true)),
            groupKey = "settings.shortcut.group.general",
        ),
    )

    fun defaultShortcutStrings(): List<String> = definitions.map { "${it.id}=${it.defaultGesture.toStorageString()}" }

    fun parseShortcutMap(entries: List<String>): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        for (entry in entries) {
            val parts = entry.split("=", limit = 2)
            if (parts.size != 2) continue
            map[parts[0]] = parts[1]
        }
        return map
    }

    fun normalizeShortcutMap(raw: Map<String, String>): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        for (definition in definitions) {
            val value = raw[definition.id]
            val gesture = ShortcutGesture.parse(value) ?: definition.defaultGesture
            result[definition.id] = gesture.toStorageString()
        }
        return result
    }

    fun toStorageList(map: Map<String, String>): List<String> {
        return definitions.map { definition ->
            val value = map[definition.id] ?: definition.defaultGesture.toStorageString()
            "${definition.id}=$value"
        }
    }

    fun resolveGesture(shortcuts: Map<String, String>, id: String): ShortcutGesture {
        val definition = definitions.firstOrNull { it.id == id } ?: return FixedGesture(id)
        val raw = shortcuts[id]
        return ShortcutGesture.parse(raw) ?: definition.defaultGesture
    }

    fun matchesKey(event: KeyEvent, shortcuts: Map<String, String>, id: String): Boolean {
        return resolveGesture(shortcuts, id).matchesKey(event)
    }

    fun matchesMouse(event: MouseEvent, shortcuts: Map<String, String>, id: String): Boolean {
        return resolveGesture(shortcuts, id).matchesMouse(event)
    }

    fun matchesScroll(event: ScrollEvent, shortcuts: Map<String, String>, id: String): Boolean {
        return resolveGesture(shortcuts, id).matchesScroll(event)
    }
}

private fun KeyCode.displayName(): String {
    return when (this) {
        KeyCode.ENTER -> "Enter"
        KeyCode.TAB -> "Tab"
        KeyCode.UP -> "Up"
        KeyCode.DOWN -> "Down"
        KeyCode.LEFT -> "Left"
        KeyCode.RIGHT -> "Right"
        else -> name.replace('_', ' ')
    }
}

private fun MouseButton.displayName(): String = when (this) {
    MouseButton.PRIMARY -> "Mouse Left"
    MouseButton.SECONDARY -> "Mouse Right"
    MouseButton.MIDDLE -> "Mouse Middle"
    else -> "Mouse"
}

private fun MouseButton.storageName(): String = when (this) {
    MouseButton.PRIMARY -> "PRIMARY"
    MouseButton.SECONDARY -> "SECONDARY"
    MouseButton.MIDDLE -> "MIDDLE"
    else -> "OTHER"
}
