package ink.meodinger.lpfx




import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import de.jangassen.MenuToolkit
import de.jangassen.jfa.FoundationCallbackRegistry
import de.jangassen.jfa.ObjcToJava
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
import javafx.scene.input.MouseEvent
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
            registerActivationObserver(root.mainMenuBar)
            Logger.info("Menu registered (NSMenuFX global)", "MenuBar")

            // 焦点切换时重断言完整菜单（纯 JNA setMainMenu，复刻老 JNI 的 nativeReassertMenu）。
            // Glass 会在两种情况下重设 NSApp.mainMenu（清掉业务菜单）：
            // 1) 应用激活（切回，focused=true）；2) 应用内打开新窗口成为 key（如小词典，主窗口 focused=false）。
            // 应用失活（切到别的 app）时 macOS 只是切走菜单栏、本 app 菜单不动。
            // 为覆盖这两种情况，焦点获得/失去都重断言（对"切到别的 app"是冗余但无害）。
            state.stage.focusedProperty().addListener { _, _, focused ->
                if (focused) {
                    reassertSoon(root.mainMenuBar, intArrayOf(0, 20, 40, 60, 80, 100, 120, 140))
                } else {
                    reassertSoon(root.mainMenuBar, intArrayOf(40, 80, 120, 160))
                }
            }

            // 兜底：Glass 还会在「场景内鼠标交互」后重设 NSApp.mainMenu（清掉业务菜单），
            // 例如点击图片开始标号（不伴随主窗口焦点变化，JDK-8351094）。在场景级
            // MOUSE_CLICKED 后延迟重断言，覆盖这类触发。
            state.stage.scene?.addEventFilter(MouseEvent.MOUSE_CLICKED) {
                reassertSoon(root.mainMenuBar, intArrayOf(30, 80, 150))
            }
        } catch (e: Throwable) {
            Logger.warning("NSMenuFX menu install failed: ${e.message}", "MenuBar")
            Logger.exception(e)
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

    /** 按给定毫秒序列安排多次重断言（一次性突发，到点各触发一次即结束）。 */
    private fun reassertSoon(menuBar: javafx.scene.control.MenuBar, delaysMs: IntArray) {
        for (delay in delaysMs) {
            PauseTransition(Duration.millis(delay.toDouble())).apply {
                setOnFinished { reassertNativeMenuBar(menuBar) }
                play()
            }
        }
    }

    /**
     * 用 raw JNA 注册 `NSApplicationDidBecomeActiveNotification` 观察者：应用激活（切回）时同步重设菜单，
     * 目标是把"消失→恢复"的可见间隙压到零（在 Glass 清菜单之后、同一 runloop 内同步设回）。
     *
     * 注意：不能用 jfa 的 Foundation.invoke（它把参数统一转成 NativeLong，4 参数 + 回调时挂起），
     * 这里用 raw JNA 的 objc_msgSend、以正确的 Pointer 类型传参；回调复用 jfa 的 registerCallback。
     */
    private fun registerActivationObserver(menuBar: javafx.scene.control.MenuBar) {
        try {
            val lib = NativeLibrary.getInstance("/usr/lib/libobjc.dylib")
            val msgSend = lib.getFunction("objc_msgSend")
            val selReg = lib.getFunction("sel_registerName")
            val getClass = lib.getFunction("objc_getClass")

            // [NSNotificationCenter defaultCenter]
            val notifClass = getClass.invokePointer(arrayOf("NSNotificationCenter"))
            val defaultCenterSel = selReg.invokePointer(arrayOf("defaultCenter"))
            val center = msgSend.invokePointer(arrayOf(notifClass, defaultCenterSel))

            // 回调：通知触发时重设菜单（jfa 创建）
            val callback = FoundationCallbackRegistry.registerCallback { _ ->
                reassertNativeMenuBar(menuBar)
            }

            // 通知名 NSString
            val namePtr = ObjcToJava.toID("NSApplicationDidBecomeActiveNotification").toPointer()
            // 观察者对象（callback.target 是 ID，转 Pointer）
            val observerPtr = callback.target.toPointer()

            // [center addObserver:observerPtr selector:callback.selector name:namePtr object:nil]
            val addSel = selReg.invokePointer(arrayOf("addObserver:selector:name:object:"))
            msgSend.invokePointer(
                arrayOf(
                    center,              // 接收者 NSNotificationCenter
                    addSel,              // SEL
                    observerPtr,         // 观察者
                    callback.selector,   // 回调选择器（已是 Pointer）
                    namePtr,             // 通知名 NSString
                    Pointer.NULL,        // object = nil
                )
            )
            Logger.info("Activation observer registered (raw JNA)", "MenuBar")
        } catch (e: Throwable) {
            Logger.warning("activation observer register failed: ${e.message}", "MenuBar")
            Logger.exception(e)
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
