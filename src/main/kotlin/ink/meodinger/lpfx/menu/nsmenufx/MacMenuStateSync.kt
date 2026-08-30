package ink.meodinger.lpfx.menu.nsmenufx

import de.jangassen.jfa.ObjcToJava
import de.jangassen.jfa.appkit.NSApplication
import de.jangassen.jfa.appkit.NSMenu
import de.jangassen.jfa.appkit.NSMenuItem
import de.jangassen.jfa.foundation.Foundation
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem

/**
 * 手动把 JavaFX 菜单项的 enable/disable 状态同步到原生 NSMenuItem。
 *
 * NSMenuFX 完全不同步 [MenuItem.disableProperty]（其 JNA 转换里没有 setEnabled），
 * 导致原生菜单项永远黑色可点。这里用 jfa 的 [Foundation.invoke] 直接调 Obj-C：
 * - 对每个 NSMenu 调 `setAutoenablesItems:NO`（autoenablesItems 是 NSMenu 的属性，YES 时
 *   macOS 会按响应链自动覆盖菜单项的手动 enabled 状态）。
 * - 对每个 NSMenuItem 调 `setEnabled:`。
 *
 * JavaFX 菜单结构（含分隔符）与 NSMenuFX 转出的原生菜单是 1:1 的，故可按索引并行遍历。
 */
object MacMenuStateSync {

    private val nativeItems = HashMap<MenuItem, NSMenuItem>()
    private val attached = HashSet<MenuItem>()

    /** 重新遍历 JavaFX 菜单栏与原生主菜单，同步每个菜单项的 enabled 状态。 */
    fun sync(menuBar: MenuBar) {
        val mainMenu = NSApplication.sharedApplication().mainMenu() ?: return
        nativeItems.clear()
        setAutoenables(mainMenu, false)
        var i = 0
        for (menu in menuBar.menus) {
            mainMenu.itemAtIndex(i)?.let { traverse(menu, it) }
            i++
        }
    }

    private fun traverse(item: MenuItem, nsItem: NSMenuItem) {
        nativeItems[item] = nsItem
        // 每个 JavaFX 菜单项只挂一次 disable 监听；监听里查当前原生项（重断言后会重建 nativeItems）
        if (attached.add(item)) {
            item.disableProperty().addListener { _, _, disabled ->
                nativeItems[item]?.let { setEnabled(it, !disabled) }
            }
        }
        setEnabled(nsItem, !item.isDisable)
        if (item is Menu) {
            val submenu = nsItem.submenu() ?: return
            setAutoenables(submenu, false)
            var i = 0
            for (child in item.items) {
                submenu.itemAtIndex(i)?.let { traverse(child, it) }
                i++
            }
        }
    }

    private fun setEnabled(item: NSMenuItem, enabled: Boolean) {
        try {
            Foundation.invoke(ObjcToJava.toID(item), "setEnabled:", enabled)
        } catch (_: Throwable) {
        }
    }

    private fun setAutoenables(menu: NSMenu, enabled: Boolean) {
        try {
            Foundation.invoke(ObjcToJava.toID(menu), "setAutoenablesItems:", enabled)
        } catch (_: Throwable) {
        }
    }
}
