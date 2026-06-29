package ink.meodinger.lpfx.component.common

import ink.meodinger.lpfx.Config
import ink.meodinger.lpfx.util.property.getValue
import ink.meodinger.lpfx.util.property.setValue

import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.TextArea
import javafx.scene.control.TextFormatter
import javafx.scene.input.InputMethodEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent


/**
 * Author: Meodinger
 * Date: 2021/8/16
 * Have fun with my code!
 */

/**
 * A TextArea that can transform some text into some other text by given rules
 *
 * For examples, if we have the rule "From `foo` to `bar`" and if we type "\foo",
 * it will be transformed into "bar". Note that the ligature mark "\" can be set
 * to some other characters.
 */
class CLigatureArea: TextArea() {

    companion object {
        private const val LIGATURE_MARK: String = "\\"
        private const val LIGATURE_MAX_LENGTH: Int = 10
    }

    private val ligatureMarkProperty: StringProperty = SimpleStringProperty(LIGATURE_MARK)
    /**
     * The start mark of a ligature input, should be a char but there isn't `CharProperty`,
     * so use `StringProperty` instead. Actually use `String` as start mark is possible but
     * is hard to produce a text change adds multi chars at once (you should use copy/paste
     * or something else) so make sure this value is a single char.
     */
    fun ligatureMarkProperty(): StringProperty = ligatureMarkProperty
    /**
     * @see ligatureMarkProperty
     */
    var ligatureMark: String by ligatureMarkProperty

    private val ligatureMaxLengthProperty: IntegerProperty = SimpleIntegerProperty(LIGATURE_MAX_LENGTH)
    /**
     * The max ligature input length. If a ligature action accmulates chars more than this
     * amount, it will automatically stop its parse procedure.
     */
    fun ligatureMaxLengthProperty(): IntegerProperty = ligatureMaxLengthProperty
    /**
     * @see ligatureMaxLengthProperty
     */
    var ligatureMaxLength: Int by ligatureMaxLengthProperty

    private val ligatureRulesProperty: ListProperty<Pair<String, String>> = SimpleListProperty(FXCollections.emptyObservableList())
    /**
     * All ligature rules.
     */
    fun ligatureRulesProperty(): ListProperty<Pair<String, String>> = ligatureRulesProperty
    /**
     * @see ligatureRulesProperty
     */
    var ligatureRules: ObservableList<Pair<String, String>> by ligatureRulesProperty

    private val boundTextPropertyProperty: ObjectProperty<StringProperty> = SimpleObjectProperty(null)

    /**
     * Whether this LigatureArea is bound to a StringProperty
     */
    val isBound: Boolean by boundTextPropertyProperty.isNotNull

    // ----- IME 组合输入状态（macOS 修复用） ----- //

    /**
     * IME 是否正在组合输入（例：macOS 中文拼音输入）。
     * 通过 [InputMethodEvent] 追踪，组合期间拦截方括号和方向键。
     */
    private var imeComposing: Boolean = false

    // ----- Ligature ----- //

    private var ligaturing: Boolean = false
    private var ligatureStart: Int = 0
    private var ligatureString: String = ""
    private val defaultTextFormatter = TextFormatter<String> { change ->
        // macOS IME 修复：组合输入期间，若文本变更中携带全角方括号，
        // 直接拒掉。这是最终防线——不依赖 KEY_PRESSED 是否被消费。
        if (Config.isMac && imeComposing && change.isAdded) {
            if (change.text.contains("【") || change.text.contains("】")) {
                change.text = ""
                return@TextFormatter change
            }
        }

        if (change.isAdded) {
            if (change.text == ligatureMark) {
                ligatureStart(caretPosition)
                return@TextFormatter change
            }

            ligatureString += change.text

            if (ligatureString.length <= ligatureMaxLength) {
                if (ligaturing) for ((from, to) in ligatureRules) if (from == ligatureString) {
                    val ligatureEnd = caretPosition
                    val caretPosition = ligatureStart + to.length

                    text = text.replaceRange(ligatureStart, ligatureEnd, to)

                    change.text = ""
                    change.setRange(caretPosition, caretPosition)
                    change.caretPosition = caretPosition
                    change.anchor = caretPosition

                    ligatureEnd()
                }
            } else {
                ligatureEnd()
            }
        } else if (change.isDeleted) {
            if (ligaturing) {
                val end = ligatureString.length - 1 - change.text.length

                if (end >= 0) {
                    ligatureString = ligatureString.substring(0, end)
                } else {
                    ligatureEnd()
                }
            }
        } else {
            ligatureEnd()
        }

        change
    }
    private fun ligatureStart(caret: Int) {
        ligaturing = true
        ligatureStart = caret
        ligatureString = ""
    }
    private fun ligatureEnd() {
        ligaturing = false
        ligatureStart = 0
        ligatureString = ""
    }

    init {
        // 将 TextFormatter 设为不可变
        textFormatterProperty().bind(ReadOnlyObjectWrapper(defaultTextFormatter))

        // ==================== macOS IME 组合输入修复 ====================
        //
        // 问题：macOS 中文输入法在拼音组合状态下，[、]、方向键应被 IME
        // 用于候选词翻页/选词，但 JavaFX 在 macOS 上未能正确拦截这些按键，
        // 导致全角方括号【】被错误插入到文本区域，光标也会随方向键移动。
        //
        // 修复策略（双层防线，仅 macOS）：
        //   1. InputMethodEvent 追踪 IME 组合状态
        //   2. KEY_PRESSED 过滤器拦截方括号和方向键
        //   3. TextFormatter 拒收全角方括号文本变更（最终防线）
        if (Config.isMac) {

            addEventHandler(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) { event ->
                imeComposing = event.composed.isNotEmpty()
            }

            addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if (imeComposing) {
                    when (event.code) {
                        KeyCode.OPEN_BRACKET,
                        KeyCode.CLOSE_BRACKET,
                        KeyCode.UP,
                        KeyCode.DOWN,
                        KeyCode.LEFT,
                        KeyCode.RIGHT -> event.consume()
                        else -> { /* 其他按键放行 */ }
                    }
                }
            }
        }
    }

    /**
     * Bind the StringProperty of this TextArea to another StringProperty bidirectionally.
     */
    fun bindText(property: StringProperty) {
        textProperty().bindBidirectional(property)
        boundTextPropertyProperty.set(property)
    }

    /**
     * Unbind the bound StringProperty (if not null), and clear text.
     */
    fun unbindText() {
        val bound = boundTextPropertyProperty.get() ?: return

        textProperty().unbindBidirectional(bound)
        boundTextPropertyProperty.set(null)
        text = ""
    }

}
