package ink.meodinger.lpfx.input

import ink.meodinger.lpfx.NOT_FOUND
import ink.meodinger.lpfx.State
import ink.meodinger.lpfx.action.ActionType
import ink.meodinger.lpfx.action.FunctionAction
import ink.meodinger.lpfx.action.LabelAction
import ink.meodinger.lpfx.component.CTreeLabelItem
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.options.Settings
import ink.meodinger.lpfx.type.TransLabel
import ink.meodinger.lpfx.util.collection.nextIndex
import ink.meodinger.lpfx.util.collection.nextItem
import ink.meodinger.lpfx.util.collection.prevIndex
import ink.meodinger.lpfx.util.collection.prevItem
import ink.meodinger.lpfx.util.component.expand
import ink.meodinger.lpfx.util.component.s
import javafx.event.EventHandler
import javafx.scene.input.KeyEvent
import javafx.scene.input.ScrollEvent
import kotlin.math.roundToInt

class ShortcutManager(private val state: State) {

    // 注册所有的快捷键处理逻辑
    fun registerShortcuts() {
        registerTransLabelMark()
        registerTransAreaUndoRedo()
        registerLabelPaneFontSizeChange()
        registerViewModeSwitch()
        registerWorkModeSwitch()
        registerCLabelPicNavigation()
        registerPicNavigation()
        registerLabelNavigation()
        registerEnterTransform()
        registerCopyPaste()
        registerLabelGroupMove()
        registerLabelIndexMove()
    }


    private val view = state.view
    private val bSwitchViewMode = view.bSwitchViewMode
    private val bSwitchWorkMode = view.bSwitchWorkMode
    private val cPicBox = view.cPicBox
    private val cLabelPane = view.cLabelPane
    private val cTreeView = view.cTreeView
    private val cTransArea = view.cTransArea


    // Register Alt(Win)/Command(macOS) + X to mark/unmark Label
    private fun registerTransLabelMark() {
        val markHandler = EventHandler<KeyEvent> { event ->
            if (!ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_MARK_TOGGLE)) return@EventHandler
            if (state.isOpened && state.currentLabelIndex != NOT_FOUND) {
                val transLabel = state.transFile.getTransLabel(state.currentPicName, state.currentLabelIndex)
                transLabel.isMarked = !transLabel.isMarked
            }
            event.consume()
        }
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, markHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, markHandler)
        Logger.info("Registered Ctrl/Meta + X mark/unmark TransLabel", "Controller")
    }

    // Register Alias & Global redo/undo in TransArea
    private fun registerTransAreaUndoRedo() {
        val handler = EventHandler<KeyEvent> { event ->
            when {
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.TRAN_UNDO) -> {
                    event.consume()
                    if (cTransArea.isUndoable) cTransArea.undo() else if (state.isUndoable) state.undo()
                }

                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.TRAN_REDO) -> {
                    event.consume()
                    if (cTransArea.isRedoable) cTransArea.redo() else if (state.isRedoable) state.redo()
                }
            }
        }
        cTransArea.addEventFilter(KeyEvent.KEY_PRESSED, handler)
        Logger.info("Registered CTransArea Alias & Global undo/redo", "Controller")
    }

    // 注册调整字体大小的快捷键 Ctrl/Alt/Meta + 滚轮
    private fun registerLabelPaneFontSizeChange() {
        cTransArea.addEventHandler(ScrollEvent.SCROLL) { event ->
            if (!ShortcutRegistry.matchesScroll(event, Settings.shortcuts, ShortcutAction.TRAN_FONT_SIZE_SCROLL)) return@addEventHandler
            event.consume()
            val newSize = (cTransArea.font.size + if (event.deltaY > 0) 1 else -1)
                .roundToInt().coerceAtLeast(12).coerceAtMost(64).toDouble()
            cTransArea.font = cTransArea.font.s(newSize)
            cTransArea.positionCaret(0)
        }
        Logger.info("Registered TransArea font size change", "Controller")
    }

    // 注册切换模式的快捷键（Tab）
    private fun registerViewModeSwitch() {
        cTreeView.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (!ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.MODE_TOGGLE)) return@addEventFilter
            event.consume()
            bSwitchViewMode.fire()
        }
        Logger.info("Registered Tab on CTreeView", "Controller")
    }

    // 注册切换模式的快捷键（Tab）
    private fun registerWorkModeSwitch() {
        cLabelPane.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (!ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.MODE_TOGGLE)) return@addEventFilter
            event.consume()
            bSwitchWorkMode.fire()
        }
        Logger.info("Registered Tab on CLabelPane", "Controller")
    }

    // 注册 Q/W 切换图片
    private fun registerCLabelPicNavigation() {
        val handler = EventHandler<KeyEvent> { event ->
            when {
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.PIC_PREV_Q) -> {
                    event.consume()
                    cPicBox.back()
                }

                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.PIC_NEXT_W) -> {
                    event.consume()
                    cPicBox.next()
                }

                else -> return@EventHandler
            }
            cTreeView.selectRoot(clear = true, scrollTo = false)
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, handler)
        Logger.info("Transformed Q/W pressed", "Controller")
    }


    // 注册 Ctrl+左右方向键切换图片
    private fun registerPicNavigation() {
        val arrowKeyChangePicHandler = EventHandler<KeyEvent> handler@{ event ->
            when {
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.PIC_PREV_ARROW) -> cPicBox.back()
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.PIC_NEXT_ARROW) -> cPicBox.next()
                else -> return@handler
            }
            cTreeView.selectFirst()
            cLabelPane.moveToLabel(cTreeView.selectedLabel)
            event.consume() // Consume used event
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangePicHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangePicHandler)
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangePicHandler)
        Logger.info("Transformed Ctrl + Left/Right", "Controller")
    }

    // 注册 Ctrl+上下方向键切换标签
    private fun registerLabelNavigation() {
        val arrowKeyChangeLabelHandler = EventHandler<KeyEvent> handler@{ event ->
            val forward: Boolean = when {
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_PREV) -> false
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_NEXT) -> true
                else -> return@handler
            }
            // Make sure we'll not get into endless LabelItem find loop
            if (state.transFile.getTransList(state.currentPicName).isEmpty()) return@handler
            // Mark immediately when this event will be consumed
            event.consume() // stop further propagation
            moveCurrLabelToNext(forward = forward)
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangeLabelHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangeLabelHandler)
        Logger.info("Transformed Ctrl + Up/Down", "Controller")

    }

    // 注册 Enter 键跳转下一条/上一条标签
    private fun registerEnterTransform() {
        val enterKeyTransformerHandler = EventHandler<KeyEvent> handler@{ event ->
            val forward = when {
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_PREV_CROSS) -> false
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_NEXT_CROSS) -> true
                else -> return@handler
            }
            // Mark immediately when this event will be consumed
            event.consume() // stop further propagation
            moveCurrLabelToNext(forward = forward, isBreakPage = true)
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, enterKeyTransformerHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, enterKeyTransformerHandler)
        Logger.info("Transformed Ctrl + Enter", "Controller")
    }

    // 注册 Ctrl+C/V 复制粘贴标签文本
    private fun registerCopyPaste() {
        val copyLabelHandler = EventHandler<KeyEvent> handler@{ event ->
            when {
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_COPY) -> {
                    // Copy the text of the selected label item
                    val treeItem = cTreeView.getTreeItem(cTreeView.selectionModel.selectedIndex) as CTreeLabelItem
                    cTreeView.copyLabelText(treeItem.transLabel.index)
                }

                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_PASTE) -> {
                    // Paste text to selected label items
                    val selectItems: Collection<CTreeLabelItem> =
                        cTreeView.selectionModel.selectedIndices.map { cTreeView.getTreeItem(it) }
                            .filterIsInstance<CTreeLabelItem>()

                    cTreeView.pasteLabelsText(selectItems.map { it.transLabel.index }, state)
                }

                else -> return@handler
            }

            event.consume() // Consume used event
        }
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, copyLabelHandler)
        Logger.info("Transformed Ctrl + C/V", "Controller")
    }

    // 注册 Alt+左右方向键移动标签分组
    private fun registerLabelGroupMove() {
        val labelGroupChangeHandler = EventHandler<KeyEvent> handler@{ event ->
            val newGroupId = when {
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_GROUP_PREV) -> {
                    if (cTreeView.selectionModel.selectedItem !is CTreeLabelItem) return@handler
                    val selectedItem = cTreeView.selectionModel.selectedItem as CTreeLabelItem
                    val groups = state.transFile.groupList
                    if (groups.isEmpty()) return@handler
                    groups.prevIndex(selectedItem.transLabel.groupId)
                }

                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_GROUP_NEXT) -> {
                    if (cTreeView.selectionModel.selectedItem !is CTreeLabelItem) return@handler
                    val selectedItem = cTreeView.selectionModel.selectedItem as CTreeLabelItem
                    val groups = state.transFile.groupList
                    if (groups.isEmpty()) return@handler
                    groups.nextIndex(selectedItem.transLabel.groupId)
                }

                else -> return@handler
            }

            val selectedItem = cTreeView.selectionModel.selectedItem as CTreeLabelItem
            val itemIndex = selectedItem.transLabel.index

            val action = LabelAction(
                ActionType.CHANGE, state,
                state.currentPicName,
                state.transFile.getTransLabel(state.currentPicName, itemIndex),
                newGroupId = newGroupId
            )

            val moveAction = FunctionAction(
                { action.commit(); state.controller.requestUpdateTree() },
                { action.revert(); state.controller.requestUpdateTree() }
            )
            state.doAction(moveAction)
            event.consume() // Consume used event
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, labelGroupChangeHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, labelGroupChangeHandler)
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, labelGroupChangeHandler)
        Logger.info("Transformed Alt + Left/Right", "Controller")
    }


    // 注册 Alt+上下方向键移动标签序号
    private fun registerLabelIndexMove() {
        val labelIndexChangeHandler = EventHandler<KeyEvent> handler@{ event ->
            val newIndex = when {
                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_INDEX_PREV) -> {
                    val selectedItem = cTreeView.selectionModel.selectedItem as? CTreeLabelItem ?: return@handler
                    val labels = state.transFile.getTransList(state.currentPicName).map(TransLabel::index)
                    labels.prevItem(selectedItem.transLabel.index)
                }

                ShortcutRegistry.matchesKey(event, Settings.shortcuts, ShortcutAction.LABEL_INDEX_NEXT) -> {
                    val selectedItem = cTreeView.selectionModel.selectedItem as? CTreeLabelItem ?: return@handler
                    val labels = state.transFile.getTransList(state.currentPicName).map(TransLabel::index)
                    labels.nextItem(selectedItem.transLabel.index)
                }

                else -> return@handler
            } ?: return@handler

            if (newIndex == NOT_FOUND) return@handler
            val selectedItem = cTreeView.selectionModel.selectedItem as? CTreeLabelItem ?: return@handler
            // 创建动作
            val action = LabelAction(
                ActionType.CHANGE, state,
                state.currentPicName,
                selectedItem.transLabel,
                newLabelIndex = newIndex
            )
            val moveAction = FunctionAction(
                { action.commit(); state.controller.requestUpdateTree() },
                { action.revert(); state.controller.requestUpdateTree() }
            )
            state.doAction(moveAction)
            event.consume() // Consume used event
            cLabelPane.moveToLabel(newIndex)
            cTreeView.selectLabel(newIndex, clear = true, scrollTo = true)
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, labelIndexChangeHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, labelIndexChangeHandler)
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, labelIndexChangeHandler)
        Logger.info("Transformed Alt + Up/Down for Index Move", "Controller")
    }


    /**
     * Find next LabelItem as int index.
     * @param  from  start index
     * @param  forward true for next, false for previous
     * @return NOT_FOUND when have no next
     */
    private fun getNextLabelItemIndex(from: Int, forward: Boolean = true): Int {
        // Make sure we have items to select
        cTreeView.getTreeItem(from).apply { this?.expand() }

        val direction = if (forward) 1 else -1
        var index = from + direction

        while (true) {
            val item = cTreeView.getTreeItem(index) ?: return NOT_FOUND
            if (item is CTreeLabelItem) return index

            item.expand()
            index += direction
        }
    }


    /**
     * move CurrLabel to next/previous LabelItem
     * @param  forward true for next, false for previous
     * @param  isBreakPage true if break page
     * @return  true if succeeded, false if failed
     */
    private fun moveCurrLabelToNext(forward: Boolean = true, isBreakPage: Boolean = false) {
        var itemIndex = getNextLabelItemIndex(cTreeView.selectionModel.selectedIndex, forward)
        if (itemIndex == NOT_FOUND) {
            //  if no next/previous LabelItem, try to find next/previous LabelItem
            if (isBreakPage) {
                //  if selected first and try getting previous, return last
                if (forward) {
                    cPicBox.next()
                    cTreeView.selectFirst(clear = true, scrollTo = false)
                    cLabelPane.moveToLabel(cTreeView.selectedLabel)
                    return
                } else {
                    //  if selected last and try getting next, return first
                    cPicBox.back()
                    cTreeView.selectLast(clear = true, scrollTo = false)
                    cLabelPane.moveToLabel(cTreeView.selectedLabel)
                    return
                }
            } else {
                // if selected first and try getting previous, return last;
                // if selected last and try getting next, return first;
                itemIndex = getNextLabelItemIndex(if (forward) 0 else cTreeView.expandedItemCount, forward)
            }
        }
        if (itemIndex == NOT_FOUND) {
            return
        }
        Logger.info("moveCurrLabelTo$itemIndex", "moveCurrLabelTo")
        val item = cTreeView.getTreeItem(itemIndex) as CTreeLabelItem
        cLabelPane.moveToLabel(item.transLabel.index)
        cTreeView.selectLabel(item.transLabel.index, clear = true, scrollTo = true)
    }


}
