package ink.meodinger.lpfx.component.tools

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import ink.meodinger.lpfx.*
import ink.meodinger.lpfx.component.common.CTextFlow
import ink.meodinger.lpfx.component.dialog.showException
import ink.meodinger.lpfx.ime.*
import ink.meodinger.lpfx.io.translateJP
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.type.LPFXTask
import ink.meodinger.lpfx.util.component.*
import ink.meodinger.lpfx.util.event.isDoubleClick
import ink.meodinger.lpfx.util.property.*
import ink.meodinger.lpfx.util.string.emptyString
import ink.meodinger.lpfx.util.string.remove
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection


/**
 * Author: Meodinger
 * Date: 2022/1/17
 * Have fun with my code!
 */

/**
 * Simple online dictionary, better than none, anyway.
 */
class OnlineDict : Stage() {

    companion object {
        // Maybe: Take back Neko-Dict
        // private const val NEKO_SITE = "https://nekodict.com"
        // private const val NEKO_API  = "https://nekodict.com/words?q="
        private const val WEBLIO_API = "https://www.weblio.jp/content/"
        private const val MOJI_CLICK_SEARCH_API = "https://api.mojidict.com/parse/functions/word-clickSearchV2"
        private const val MOJI_UNION_API = "https://api.mojidict.com/parse/functions/union-api"
        private const val FONT_SIZE = 16.0
    }

    private enum class DictTab { MOJI, WEBLIO, SENTENCE; }

    private val tabProperty: ObjectProperty<DictTab> = SimpleObjectProperty(DictTab.MOJI)
    private var currentTab: DictTab by tabProperty

    private val objectMapper = ObjectMapper()
    private val mojiOutputFlow: CTextFlow = CTextFlow()
    private val weblioOutputFlow: CTextFlow = CTextFlow()
    private val sentenceOutputFlow: CTextFlow = CTextFlow()

    private var oriLang: String = emptyString()

    init {
        icons.add(ICON)
        title = "${INFO["application.name"]} - Dict"
        width = 300.0
        height = 200.0
        scene = Scene(BorderPane().apply {
            top(HBox()) {
                alignment = Pos.CENTER
                backgroundProperty().bind(tabProperty.transform {
                    Background(
                        BackgroundFill(
                            when (it!!) {
                                DictTab.MOJI -> Color.LIGHTGREEN
                                DictTab.WEBLIO -> Color.LIGHTGREEN
                                DictTab.SENTENCE -> Color.LIGHTBLUE
                            },
                            CornerRadii(0.0),
                            Insets(0.0)
                        )
                    )
                })
                add(Label()) {
                    minWidth = 75.0
                    alignment = Pos.CENTER
                    textProperty().bind(tabProperty.transform {
                        when (it!!) {
                            DictTab.MOJI -> "moji"
                            DictTab.WEBLIO -> "Weblio"
                            DictTab.SENTENCE -> "长句日译中"
                        }
                    })
                }
                add(TextField()) {
                    hgrow = Priority.ALWAYS
                    setOnAction {
                        selectedOutputFlow().setText(I18N["dict.fetching"], FONT_SIZE)
                        selectedOutputFlow().flow()
                        when (currentTab) {
                            DictTab.MOJI -> searchMoji(text)
                            DictTab.WEBLIO -> searchWeblio(text)
                            DictTab.SENTENCE -> translate(text)
                        }
                    }
                    addEventFilter(KeyEvent.KEY_PRESSED) {
                        if (it.code != KeyCode.TAB) return@addEventFilter
                        // Mark immediately when this event will be consumed
                        it.consume() // disable further propagation

                        currentTab = DictTab.entries.toTypedArray()[(currentTab.ordinal + 1) % DictTab.entries.size]
                    }

                    if (Config.enableIMEAssistance) {
                        focusedProperty().addListener(onNew {
                            // We do not set the IMEConversion mode here because switch language need time.
                            if (it) {
                                oriLang = getCurrentLanguage()
                                // Focus gain will take place after the rendering, so it's safe to set by sync.
                                AvailableLanguages.firstOrNull { lang -> lang.startsWith(JA) }
                                    ?.apply(::setCurrentLanguage)
                            } else {
                                // If set immediately after lose focus will cause focus on other stages fail.
                                // Use runLater to set language after the rendering.
                                Platform.runLater { setCurrentLanguage(oriLang) }
                            }
                        })
                        addEventHandler(MouseEvent.MOUSE_CLICKED) {
                            if (it.isDoubleClick && getCurrentLanguage().startsWith(JA)) {
                                setImeConversionMode(
                                    getCurrentWindow(),
                                    ImeSentenceMode.AUTOMATIC,
                                    ImeConversionMode.JA_HIRAGANA
                                )
                            }
                        }
                    }
                }
            }
            center(TabPane().apply {
                tabs.addAll(
                    createResultTab("moji", mojiOutputFlow),
                    createResultTab("Weblio", weblioOutputFlow),
                    createResultTab("长句日译中", sentenceOutputFlow)
                )
                selectionModel.selectedIndexProperty().addListener(onNew<Number, Int> {
                    currentTab = DictTab.entries.toTypedArray()[it]
                })
                tabProperty.addListener(onNew {
                    selectionModel.select(it.ordinal)
                })
                selectionModel.select(DictTab.MOJI.ordinal)
            }) {}
        })

        closeOnEscape()
    }

    private fun selectedOutputFlow(): CTextFlow = when (currentTab) {
        DictTab.MOJI -> mojiOutputFlow
        DictTab.WEBLIO -> weblioOutputFlow
        DictTab.SENTENCE -> sentenceOutputFlow
    }

    private fun createResultTab(title: String, outputFlow: CTextFlow): Tab {
        return Tab(title).apply {
            isClosable = false
            content = ScrollPane().apply scroll@{
                content = outputFlow.apply {
                    isInstant = false
                    fontSize = FONT_SIZE
                    padding = Insets(16.0, 0.0, 16.0, 16.0)
                    prefWidthProperty().bind(this@scroll.widthProperty() - 16.0)
                }
            }
        }
    }

    private fun searchWeblioSync(word: String) {
        // URL-encode the word to make sure we search the correct thing
        val weblioURL = WEBLIO_API + URLEncoder.encode(word, StandardCharsets.UTF_8)
        Logger.debug("Dictionary: Fetching URL $weblioURL", "Dictionary")
        val weblioConnection = URI(weblioURL).toURL().openConnection().apply { connect() } as HttpsURLConnection
        if (weblioConnection.responseCode != 200) {
            Logger.debug(weblioConnection.errorStream.reader(StandardCharsets.UTF_8).readText(), "Dictionary")
            weblioOutputFlow.setText(String.format(I18N["dict.search_error.i"], weblioConnection.responseCode))
            return
        }
        val weblioHTML = weblioConnection.inputStream.reader(StandardCharsets.UTF_8).readText()
        val weblioPage = Jsoup.parse(weblioHTML)

        val notfound = weblioPage.selectXpath("//div[@id=\"nrCntTH\"]/p/text()")
        if (notfound.isNotEmpty()) {
            weblioOutputFlow.clear()
            weblioOutputFlow.appendLine(notfound[0].text())
            return
        }

        // to remove
        val regexDocumentWrite = Regex("(document.write\\()(.*)(\\);)") // In-dom js
        val regexUselessSource = Regex("(\u51fa\u5178)(.*)(\\))") // Useless Souce
        val regexKanjiCafeSource = Regex("(\u203b\u3054\u5229\u7528)(.*)(Cafe.)") // Kanji Cafe Source
        // to replace
        val regexNumber = Regex("[\uFF10-\uFF19]+ ") // Full-Width 0-9
        val regexEnglish = Regex(" [a-zA-Z]+ ") // English with trailing whitespace
        val regexEnglishDot = Regex("[a-zA-Z]+, ") // English with trailing dot
        val regexWhitespace = Regex("( )+") // Multi whitespace
        val regexMultiNewLine = Regex("(\n)+") // Multi new line

        val sourceList = weblioPage.selectXpath("//div[@class=\"pbarTL\"]").map { it.wholeText().trim() }
        val definitionList = weblioPage.selectXpath("//div[@class=\"kiji\"]").map {
            // Example sentences
            if (it.children()[1].hasClass("Wnryj")) {
                return@map it.children()[1].children()[0].children().mapIndexed { index, element ->
                    "${index + 1}.${element.wholeText()}"
                }.joinToString("\n\u3000")
            }

            it.selectXpath("//br").forEach { element -> element.replaceWith(TextNode(" ")) }
            var text = it.wholeText()
                .remove(regexDocumentWrite)
                .remove(regexUselessSource)
                .remove(regexKanjiCafeSource)

            regexNumber.findAll(text).toList().reversed().forEach { result ->
                text = text.replaceRange(result.range, result.value.dropLast(1).plus("."))
            }
            regexEnglish.findAll(text).toList().reversed().forEach { result ->
                text = text.replaceRange(result.range, result.value.drop(1).dropLast(1))
            }
            regexEnglishDot.findAll(text).toList().reversed().forEach { result ->
                text = text.replaceRange(result.range, result.value.dropLast(1))
            }

            text.replace(regexWhitespace, "\n").replace(regexMultiNewLine, "\n\u3000").trim()
        }

        weblioOutputFlow.clear()
        for ((source, definition) in sourceList.zip(definitionList)) {
            weblioOutputFlow.appendLine(source, bold = true)
            weblioOutputFlow.appendText("\u3000$definition\n\n")
        }
    }

    private fun searchWeblio(word: String) {
        LPFXTask.createTask<Unit> { searchWeblioSync(word) }.apply {
            setOnSucceeded {
                Logger.info("Dictionary: Fetched weblio info: $word", "Dictionary")
                Platform.runLater { weblioOutputFlow.flow() }
            }
            setOnFailed {
                Logger.error("Dictionary: Fetch weblio info failed", "Dictionary")
                Logger.exception(it)
                Platform.runLater { weblioOutputFlow.flow() }
                showException(this@OnlineDict, it)
            }
        }() // Remember to invoke
    }

    private fun requestMojiJson(api: String, data: Map<String, Any>, contentType: String): JsonNode {
        val connection = URI(api).toURL().openConnection().apply {
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 10_000
        } as HttpsURLConnection
        connection.requestMethod = "POST"
        mapOf(
            "accept" to "*/*",
            "accept-language" to "zh-CN,zh;q=0.9,ar;q=0.8,sq;q=0.7,ru;q=0.6",
            "content-type" to contentType,
            "origin" to "https://www.mojidict.com",
            "referer" to "https://www.mojidict.com/",
            "sec-ch-ua" to "\"Chromium\";v=\"127\", \"Google Chrome\";v=\"127\", \"Not-A.Brand\";v=\"99\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Windows\"",
            "sec-fetch-dest" to "empty",
            "sec-fetch-mode" to "cors",
            "sec-fetch-site" to "same-site"
        ).forEach { (key, value) -> connection.setRequestProperty(key, value) }

        connection.outputStream.use { it.write(objectMapper.writeValueAsBytes(data)) }
        if (connection.responseCode != 200) {
            Logger.debug(connection.errorStream?.reader(StandardCharsets.UTF_8)?.readText().orEmpty(), "Dictionary")
            throw java.net.ConnectException("Response code ${connection.responseCode}")
        }
        return objectMapper.readTree(connection.inputStream)
    }

    private fun mojiBasePayload(): Map<String, Any> {
        return mapOf(
            "_ClientVersion" to "js3.4.1",
            "_ApplicationId" to "E62VyFVLMiW7kvbtVq3p",
            "g_os" to "PCWeb",
            "g_ver" to "v4.8.8.20240829",
            "_InstallationId" to "563b6bd3-e514-46fb-8557-d6138311a06c"
        )
    }

    private fun searchMojiClick(word: String): Boolean {
        val data = mojiBasePayload().plus(
            mapOf(
                "searchText" to word,
                "langEnv" to "zh-CN_ja"
            )
        )
        val result = requestMojiJson(MOJI_CLICK_SEARCH_API, data, "text/plain")["result"]["result"]
        val wordInfo = result["word"]?.firstOrNull() ?: return false

        val spell = wordInfo["spell"]?.asText().orEmpty()
        val pron = wordInfo["pron"]?.asText().orEmpty()
        val accent = wordInfo["accent"]?.asText().orEmpty()
        val excerpt = wordInfo["excerpt"]?.asText().orEmpty()
        val type = Regex("\\[(.*?)]").find(excerpt)?.groupValues?.getOrNull(1).orEmpty()

        val collect = mutableListOf<String>()
        val relaIds = mutableListOf<String>()
        result["subdetails"]?.forEach { subdetail ->
            val title = subdetail["title"]?.asText().orEmpty()
            val relaId = subdetail["relaId"]?.asText().orEmpty()
            val lang = subdetail["lang"]?.asText().orEmpty()
            val index = relaIds.indexOf(relaId)
            if (index >= 0) {
                when (lang) {
                    "zh-CN" -> collect[index] = title + collect[index]
                    "ja" -> collect[index] = collect[index] + "($title)"
                }
            } else {
                collect.add(title)
                relaIds.add(relaId)
            }
        }

        mojiOutputFlow.appendLine("$spell | $pron$accent", 18.0, bold = true)
        if (type.isNotBlank()) mojiOutputFlow.appendLine(type)
        collect.forEachIndexed { index, detail ->
            mojiOutputFlow.appendLine("${index + 1}. $detail")
        }
        return true
    }

    private fun searchMojiUnion(word: String): Boolean {
        val data = mojiBasePayload().plus(
            "functions" to listOf(
                mapOf(
                    "name" to "search-all",
                    "params" to mapOf(
                        "text" to word,
                        "types" to listOf(102, 106, 103)
                    )
                )
            )
        )
        val searchResult = requestMojiJson(
            MOJI_UNION_API,
            data,
            "text/plain"
        )["result"]["results"]["search-all"]["result"]["word"]["searchResult"]

        var hasResult = false
        searchResult?.forEach {
            val title = it["title"]?.asText().orEmpty()
            val excerpt = it["excerpt"]?.asText().orEmpty()
            if (title.isNotBlank() || excerpt.isNotBlank()) {
                hasResult = true
                mojiOutputFlow.appendLine(title, bold = true)
                mojiOutputFlow.appendLine(excerpt)
                mojiOutputFlow.appendLine()
            }
        }
        return hasResult
    }

    private fun searchMojiSync(word: String) {
        mojiOutputFlow.clear()
        var hasResult = false
        try {
            hasResult = searchMojiClick(word)
            if (hasResult) mojiOutputFlow.appendLine("\n-----\n")
        } catch (e: Exception) {
            Logger.warning("Dictionary: MOJi click search failed: ${e.message}", "Dictionary")
        }
        try {
            hasResult = searchMojiUnion(word) || hasResult
        } catch (e: Exception) {
            Logger.warning("Dictionary: MOJi union search failed: ${e.message}", "Dictionary")
        }
        if (!hasResult) mojiOutputFlow.setText(I18N["dict.not_found"])
    }

    private fun searchMoji(word: String) {
        LPFXTask.createTask<Unit> { searchMojiSync(word) }.apply {
            setOnSucceeded {
                Logger.info("Dictionary: Fetched moji info: $word", "Dictionary")
                Platform.runLater { mojiOutputFlow.flow() }
            }
            setOnFailed {
                Logger.error("Dictionary: Fetch moji info failed", "Dictionary")
                Logger.exception(it)
                Platform.runLater { mojiOutputFlow.flow() }
                showException(this@OnlineDict, it)
            }
        }()
    }

    private fun translateSync(text: String) {
        sentenceOutputFlow.setText(translateJP(text))
    }

    private fun translate(text: String) {
        LPFXTask.createTask<Unit> { translateSync(text) }.apply {
            setOnSucceeded {
                Logger.info("Dictionary: Fetched translation", "Dictionary")
                Platform.runLater { sentenceOutputFlow.flow() }
            }
            setOnFailed {
                Logger.error("Dictionary: Fetch translation failed", "Dictionary")
                Logger.exception(it)
                Platform.runLater { sentenceOutputFlow.flow() }
                showException(this@OnlineDict, it)
            }
        }() // Remember to invoke
    }

}
