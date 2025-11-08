package ink.meodinger.lpfx.options

import ink.meodinger.lpfx.Config.TextFont
import ink.meodinger.lpfx.util.property.getValue
import ink.meodinger.lpfx.util.property.setValue

import javafx.beans.property.*
import javafx.scene.text.Font
import java.io.IOException


/**
 * Author: Meodinger
 * Date: 2021/7/29
 * Have fun with my code!
 */

/**
 * The preferences that user set while using
 */
object Preference : AbstractProperties("Preference", Options.preference) {

    const val LANGUAGE_EN = "en"
    const val LANGUAGE_ZH_CN = "zh_CN"
    const val LANGUAGE_ZH_TW = "zh_TW"


    const val WINDOW_SIZE = "WindowSize"
    const val MAIN_DIVIDER = "MainDivider"
    const val RIGHT_DIVIDER = "RightDivider"
    const val TEXTAREA_FONT_SIZE = "TextAreaFontSize"
    const val SHOW_STATS_BAR = "ShowStatsBar"
    const val LAST_UPDATE_NOTICE = "LastUpdateNotice"
    const val CurrentLanguage = "CurrentLanguage"


    override val default = listOf(
        CProperty(WINDOW_SIZE, 900, 600),
        CProperty(MAIN_DIVIDER, 0.618),
        CProperty(RIGHT_DIVIDER, 0.618),
        CProperty(TEXTAREA_FONT_SIZE, 24),
        CProperty(SHOW_STATS_BAR, true),
        CProperty(LAST_UPDATE_NOTICE, 0),
        // 系统语言
        CProperty(CurrentLanguage, System.getProperty("user.language").let {
            when (it) {
                "zh" -> {
                    // 根据系统进一步判断是简体还是繁体
                    val country = System.getProperty("user.country") ?: ""
                    if (country == "TW" || country == "HK" || country == "MO") LANGUAGE_ZH_TW else LANGUAGE_ZH_CN
                }

                else -> LANGUAGE_EN
            }
        }),
    )

    private val windowWidthProperty: DoubleProperty = SimpleDoubleProperty()
    fun windowWidthProperty(): DoubleProperty = windowWidthProperty
    var windowWidth: Double by windowWidthProperty

    private val windowHeightProperty: DoubleProperty = SimpleDoubleProperty()
    fun windowHeightProperty(): DoubleProperty = windowHeightProperty
    var windowHeight: Double by windowHeightProperty

    private val mainDividerPositionProperty: DoubleProperty = SimpleDoubleProperty()
    fun mainDividerPositionProperty(): DoubleProperty = mainDividerPositionProperty
    var mainDividerPosition: Double by mainDividerPositionProperty

    private val rightDividerPositionProperty: DoubleProperty = SimpleDoubleProperty()
    fun rightDividerPositionProperty(): DoubleProperty = rightDividerPositionProperty
    var rightDividerPosition: Double by rightDividerPositionProperty

    private val textAreaFontProperty: ObjectProperty<Font> = SimpleObjectProperty()
    fun textAreaFontProperty(): ObjectProperty<Font> = textAreaFontProperty
    var textAreaFont: Font by textAreaFontProperty

    private val showStatsBarProperty: BooleanProperty = SimpleBooleanProperty()
    fun showStatsBarProperty(): BooleanProperty = showStatsBarProperty
    var isShowStatsBar: Boolean by showStatsBarProperty

    private val lastUpdateNoticeProperty: LongProperty = SimpleLongProperty()
    fun lastUpdateNoticeProperty(): LongProperty = lastUpdateNoticeProperty
    var lastUpdateNotice: Long by lastUpdateNoticeProperty


    // 添加语言属性
    private val currentLanguageProperty: StringProperty = SimpleStringProperty()
    fun currentLanguageProperty(): StringProperty = currentLanguageProperty
    var currentLanguage: String by currentLanguageProperty

    init {
        useDefault()
    }

    @Throws(IOException::class, NumberFormatException::class)
    override fun load() {
        load(this)

        val windowSizes = this[WINDOW_SIZE].asDoubleList().takeIf { it.size >= 2 } ?: default[0].asDoubleList()

        windowWidth = windowSizes[0]
        windowHeight = windowSizes[1]
        mainDividerPosition = this[MAIN_DIVIDER].asDouble()
        rightDividerPosition = this[RIGHT_DIVIDER].asDouble()
        textAreaFont = Font(TextFont, this[TEXTAREA_FONT_SIZE].asDouble())
        isShowStatsBar = this[SHOW_STATS_BAR].asBoolean()
        lastUpdateNotice = this[LAST_UPDATE_NOTICE].asLong()
        currentLanguage = this[CurrentLanguage].asString()
    }

    @Throws(IOException::class)
    override fun save() {
        this[WINDOW_SIZE].set(windowWidth, windowHeight)
        this[MAIN_DIVIDER].set(mainDividerPosition)
        this[RIGHT_DIVIDER].set(rightDividerPosition)
        this[TEXTAREA_FONT_SIZE].set(textAreaFont.size)
        this[SHOW_STATS_BAR].set(isShowStatsBar)
        this[LAST_UPDATE_NOTICE].set(lastUpdateNotice)
        this[CurrentLanguage].set(currentLanguage)

        save(this)
    }

}
