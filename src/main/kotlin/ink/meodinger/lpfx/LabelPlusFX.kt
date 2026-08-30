package ink.meodinger.lpfx




import de.jangassen.MenuToolkit
import ink.meodinger.lpfx.component.dialog.showException
import ink.meodinger.lpfx.component.dialog.showInfo
import ink.meodinger.lpfx.component.properties.AbstractPropertiesDialog
import ink.meodinger.lpfx.component.properties.DialogLogs
import ink.meodinger.lpfx.component.properties.DialogSettings
import ink.meodinger.lpfx.component.tools.*
import ink.meodinger.lpfx.menu.nsmenufx.MacMenuStateSync
import ink.meodinger.lpfx.options.*
import ink.meodinger.lpfx.util.HookedApplication
import ink.meodinger.lpfx.util.component.withOwner
import ink.meodinger.lpfx.util.property.onChange
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Scene
import javafx.scene.control.Menu
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import javafx.util.Duration
import java.awt.SystemTray
import java.awt.TrayIcon
import java.io.File
import java.awt.MenuItem as AwtMenuItem
import java.awt.PopupMenu as AwtPopupMenu


/**
 * Author: Meodinger
 * Date: 2021/7/29
 * Have fun with my code!
 */

/**
 * LPFX Application
 */
class LabelPlusFX: HookedApplication() {

    companion object {
        private const val PARAM_UNNAMED_NO_CHECK_UPDATE = "--no-check-update"
    }

    private lateinit var state: State
    private val icon: TrayIcon by lazy {
        TrayIcon(SwingFXUtils.fromFXImage(ICON, null)).apply {
            fun restore() {
                Platform.runLater {
                    state.stage.show()
                    state.stage.toFront()
                    SystemTray.getSystemTray().remove(this)
                }
            }
            fun destroy() {
                Platform.runLater {
                    if (!state.isOpened || !state.isChanged) this@LabelPlusFX.stop()
                }
            }

            isImageAutoSize = true
            popupMenu = AwtPopupMenu().apply {
                // AWT has problem with unicode characters display
                add(AwtMenuItem("Show").apply {
                    addActionListener { restore() }
                })
                addSeparator()
                add(AwtMenuItem("Exit").apply {
                    addActionListener { destroy() }
                })
            }
            addActionListener { restore() }
        }
    }

    /**
     * Try to minimal the window to system tray if SystemTray supported.
     * Otherwise, iconify it.
     */
    fun iconify() {
        if (Config.supportSysTray) {
            SystemTray.getSystemTray().add(icon)
            state.stage.hide()
        } else {
            state.stage.isIconified = true
        }
    }

    /**
     * Cheat Sheet. To display some hints on how to use LPFX
     */
    val cheatSheet: CheatSheet by lazy {
        CheatSheet() withOwner state.stage
    }

    /**
     * Online Dict. To search some text quick and simple
     */
    val onlineDict: OnlineDict by lazy {
        OnlineDict() withOwner state.stage
    }

    /**
     * Search & Replace. To search and replace some text in all TransFile
     */
    val searchAndReplace: SearchReplace by lazy {
        SearchReplace(state) withOwner state.stage
    }

    /**
     * Format Checker. Check format when save
     */
    val formatChecker: FormatChecker by lazy {
        FormatChecker(state) withOwner state.stage
    }

    /**
     * Specify Dialog. Specify files of pictures
     */
    val dialogSpecify: SpecifyFiles by lazy {
        SpecifyFiles(state) withOwner state.stage
    }

    /**
     * Log-related Dialog
     */
    val dialogLogs: AbstractPropertiesDialog by lazy {
        DialogLogs() withOwner state.stage
    }

    /**
     * Settings Dialog
     */
    val dialogSettings: AbstractPropertiesDialog by lazy {
        DialogSettings() withOwner state.stage
    }

    /**
     * Initialize the Options
     */
    override fun init() {
        // Let FX Thread keep running when Stage closed by BOSS Key

        Platform.setImplicitExit(false)

        // Start Logger Timer
        Logger.tic()
        Options.load()
        state = State().apply { application = this@LabelPlusFX }
        Logger.info("App initializing...", "Application")
        Logger.info("App initialized", "Application")
    }

    /**
     * Start the Application.
     * The start order is `Stage` -> `View` -> `Controller` -> `Post-Start Operations`
     */
    override fun start(primaryStage: Stage) {
        Logger.info("App starting...", "Application")

        // FX Thread Catcher
        Thread.currentThread().setUncaughtExceptionHandler { _, e ->
            Logger.error("Exception uncaught in FX Thread", "Application")
            Logger.exception(e)
            if (state.isOpened) {
                showException(primaryStage, e, state.controller.emergency())
            } else {
                showException(primaryStage, e)
            }
        }

        state.stage = primaryStage

        // View & Controller
        val root: View
        val controller: Controller
        try {
            root = View(state)
            controller = Controller(state)
        } catch (e: Throwable) {
            Logger.exception(e)
            showException(null, e)
            stop()
            return
        }

        // Start construct scene & stage
        primaryStage.title = INFO["application.name"]
        primaryStage.icons.add(ICON)
        primaryStage.scene = Scene(root, Preference.windowWidth, Preference.windowHeight)
        primaryStage.setOnCloseRequest { if (!controller.stay()) stop() else it.consume() }

        // Window Size Listener
        val windowSizeListener: ChangeListener<Number> = onChange {
            if (primaryStage.isMaximized) return@onChange
            Preference.windowWidth = primaryStage.scene.width
            Preference.windowHeight = primaryStage.scene.height
        }
        primaryStage.scene.widthProperty().addListener(windowSizeListener)
        primaryStage.scene.heightProperty().addListener(windowSizeListener)

        // BOSS key
        var counter = 0
        primaryStage.addEventFilter(KeyEvent.KEY_PRESSED) {
            if (it.code == KeyCode.ESCAPE) {
                counter++
                if (counter == 2) {
                    iconify()
                    counter = 0
                }
            }
        }

        // 在 stage show 之前安装原生菜单：此时无窗口，NSMenuFX 走纯 JNA setMainMenu，
        // 规避 JavaFX useSystemMenuBar 在焦点切换时丢业务菜单的 bug（JDK-8380900）。
        if (Config.isMac) installNativeMenuBar(root)

        // Show
        primaryStage.show()

        // NSMenuFX 的原生 keyEquivalent 可能因 autoenablesItems 失效，这里把菜单快捷键额外注册到
        // Scene（纯 JavaFX），保证 Cmd+N/O/S/Z/F/E/D 等快捷键在 JavaFX 层直接触发菜单动作。
        if (Config.isMac) primaryStage.scene?.let { registerSceneAccelerators(root, it) }

        Logger.info("App started", "Application")

        // region Post-Start Operations

        // Check update
        if (PARAM_UNNAMED_NO_CHECK_UPDATE !in parameters.unnamed) if (Settings.autoCheckUpdate) controller.checkUpdate()
        // Open file
        if (parameters.raw.isNotEmpty() && File(parameters.raw.last()).isFile) {
            // Open the given file
            val file = File(parameters.raw.last())
            Platform.runLater { state.controller.open(file, file.parentFile) }
        } else if (Settings.autoOpenLastFile) {
            // Open last
            val file = RecentFiles.lastFile?.takeIf(File::exists)?.takeIf(File::isFile)
            if (file != null) Platform.runLater { state.controller.open(file, file.parentFile) }
        }
        // Notify user about graphics switch
        if (Config.isWin) {
            val currentMode = Settings.currentPrismMode
            val isUsingSWPrism = Config.usingSWPrism
            if (isUsingSWPrism && currentMode != PrismMode.SW) {
                showInfo(state.stage, String.format(I18N["graphic_switch.open_message"], I18N["graphic_switch.SW"]))
            } else if (!isUsingSWPrism && currentMode == PrismMode.SW) {
                showInfo(state.stage, String.format(I18N["graphic_switch.open_message"], I18N["graphic_switch.HW"]))
            }
        }
        // endregion

        Logger.toc()
    }

    /** 用 NSMenuFX 安装原生 macOS 菜单栏（show 之前调用走纯 JNA），并注册焦点重断言。 */
    private fun installNativeMenuBar(root: View) {
        try {
            val toolkit = MenuToolkit.toolkit()
            // 隐藏窗口内的 JavaFX 菜单栏（原生 NSMenu 接管）
            root.mainMenuBar.isUseSystemMenuBar = false
            root.mainMenuBar.isVisible = false
            root.mainMenuBar.isManaged = false
            toolkit.setMenuBar(root.mainMenuBar)
            MacMenuStateSync.sync(root.mainMenuBar)
            Logger.info("Menu registered (NSMenuFX global)", "MenuBar")
            System.err.println("[MenuBar] NSMenuFX global menu installed")

            // 焦点切换时重断言完整菜单（纯 JNA setMainMenu，复刻老 JNI 的 nativeReassertMenu）。
            // Glass 清业务菜单是异步的，所以重断言必须延迟到它清完之后：
            // - 切走（失活）：500ms 后重断言（用户在看别的显示器，延迟无感）。
            // - 切回（激活）：200ms 短延迟（覆盖 Glass 早清、减少闪烁）+ 500ms 兜底（保证最终恢复）。
            state.stage.focusedProperty().addListener { _, _, focused ->
                if (focused) {
                    PauseTransition(Duration.millis(200.0)).apply {
                        setOnFinished { reassertNativeMenuBar(root.mainMenuBar) }
                        play()
                    }
                    PauseTransition(Duration.millis(500.0)).apply {
                        setOnFinished { reassertNativeMenuBar(root.mainMenuBar) }
                        play()
                    }
                } else {
                    PauseTransition(Duration.millis(500.0)).apply {
                        setOnFinished { reassertNativeMenuBar(root.mainMenuBar) }
                        play()
                    }
                }
            }
        } catch (e: Throwable) {
            Logger.warning("NSMenuFX menu install failed: ${e.message}", "MenuBar")
            Logger.exception(e)
            System.err.println("[MenuBar] NSMenuFX menu install failed: ${e.message}")
        }
    }

    /** 缓存反射查找结果：`MacNativeAdapter` 单例 + `setMenuBar(List)` 方法，只解析一次。 */
    private val macNativeSetMenuBar by lazy {
        val adapterClass = Class.forName("de.jangassen.platform.mac.MacNativeAdapter")
        val adapter = adapterClass.getMethod("getInstance").invoke(null)
        val method = adapterClass.getMethod("setMenuBar", java.util.List::class.java)
        method.isAccessible = true
        adapter to method
    }

    /**
     * 纯 JNA 重设完整原生菜单栏（`NSApplication.setMainMenu`）。
     * NSMenuFX 的 `MacNativeAdapter.setMenuBar(List)` 是限定导出到 jfa 的，公开 API 在窗口存在后
     * 走不到纯 JNA 分支（会退回 useSystemMenuBar），故这里用反射直接调用，复刻老 JNI 的 nativeReassertMenu。
     * 需启动参数 `--add-opens nsmenufx/de.jangassen.platform.mac=lpfx`。
     */
    private fun reassertNativeMenuBar(menuBar: javafx.scene.control.MenuBar) {
        try {
            val (adapter, setMenuBar) = macNativeSetMenuBar
            setMenuBar.invoke(adapter, menuBar.menus)
            MacMenuStateSync.sync(menuBar)
        } catch (e: Throwable) {
            Logger.warning("NSMenuFX native reassert failed: ${e.message}", "MenuBar")
        }
    }

    /** 把菜单项的 accelerator 注册到 Scene，让快捷键在 JavaFX 层直接触发（不依赖原生 keyEquivalent）。 */
    private fun registerSceneAccelerators(root: View, scene: Scene) {
        root.mainMenuBar.menus.forEach { registerMenuAccelerators(it, scene) }
    }

    private fun registerMenuAccelerators(menu: Menu, scene: Scene) {
        for (item in menu.items) {
            if (item is Menu) registerMenuAccelerators(item, scene)
            val accel = item.accelerator
            if (accel != null) {
                scene.accelerators[accel] = Runnable { if (!item.isDisable) item.fire() }
            }
        }
    }

    /**
     * Stop the Application.
     * The close order is `Hooks` -> `Options` -> `Stage`
     */
    override fun stop() {
        Logger.info("App stopping...", "Application")
        // Register restore hook
        if (Config.isWin) {
            val currentMode = Settings.currentPrismMode
            val isUsingSWPrism = Config.usingSWPrism
            if (isUsingSWPrism && currentMode != PrismMode.SW) {
                state.application.addShutdownHook("RestorePrism", ::useHardwarePrism)
            } else if (!isUsingSWPrism && currentMode == PrismMode.SW) {
                state.application.addShutdownHook("RestorePrism", ::useSoftwarePrism)
            }
        }

        runHooks {
            Logger.error("Exception occurred during hooks run", "Application")
            Logger.exception(it)
        }
        Options.save()
        state.stage.close()

        Platform.exit()
    }

}
