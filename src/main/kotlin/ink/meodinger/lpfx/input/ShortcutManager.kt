package ink.meodinger.lpfx.input

import ink.meodinger.lpfx.Config
import ink.meodinger.lpfx.NOT_FOUND
import ink.meodinger.lpfx.State
import ink.meodinger.lpfx.action.ActionType
import ink.meodinger.lpfx.action.FunctionAction
import ink.meodinger.lpfx.action.LabelAction
import ink.meodinger.lpfx.component.CTreeLabelItem
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.type.TransLabel
import ink.meodinger.lpfx.util.collection.nextIndex
import ink.meodinger.lpfx.util.collection.nextItem
import ink.meodinger.lpfx.util.collection.prevIndex
import ink.meodinger.lpfx.util.collection.prevItem
import ink.meodinger.lpfx.util.component.expand
import ink.meodinger.lpfx.util.component.s
import javafx.event.EventHandler
import javafx.scene.input.KeyCode
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
        val markHandler = EventHandler<KeyEvent> {
            if ((it.isAltDown || (Config.isMac && it.isControlDown)) && it.code == KeyCode.X) {
                if (state.isOpened && state.currentLabelIndex != NOT_FOUND) {
                    val transLabel = state.transFile.getTransLabel(state.currentPicName, state.currentLabelIndex)
                    transLabel.isMarked = !transLabel.isMarked
                }
            }
        }
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, markHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, markHandler)
        Logger.info("Registered Ctrl/Meta + X mark/unmark TransLabel", "Controller")
    }

    // Register Alias & Global redo/undo in TransArea
    private fun registerTransAreaUndoRedo() {
        val handler = EventHandler<KeyEvent> { event ->
            if ((event.isControlDown || event.isMetaDown) && event.code == KeyCode.Z) {
                event.consume()
                if (!event.isShiftDown) {
                    if (cTransArea.isUndoable) cTransArea.undo()
                    else if (state.isUndoable) state.undo()
                } else {
                    if (cTransArea.isRedoable) cTransArea.redo()
                    else if (state.isRedoable) state.redo()
                }
            }
        }
        cTransArea.addEventFilter(KeyEvent.KEY_PRESSED, handler)
        Logger.info("Registered CTransArea Alias & Global undo/redo", "Controller")
    }

    // 注册调整字体大小的快捷键 Ctrl/Alt/Meta + 滚轮
    private fun registerLabelPaneFontSizeChange() {
        cTransArea.addEventHandler(ScrollEvent.SCROLL) { event ->
            if (event.isControlDown || event.isAltDown || event.isMetaDown) {
                event.consume()
                val newSize = (cTransArea.font.size + if (event.deltaY > 0) 1 else -1)
                    .roundToInt().coerceAtLeast(12).coerceAtMost(64).toDouble()
                cTransArea.font = cTransArea.font.s(newSize)
                cTransArea.positionCaret(0)
            }
        }
        Logger.info("Registered TransArea font size change", "Controller")
    }

    // 注册切换 ViewMode 的快捷键（Tab）
    private fun registerViewModeSwitch() {
        cTreeView.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.TAB) {
                event.consume()
                bSwitchViewMode.fire()
            }
        }
        Logger.info("Transformed Tab on CTreeView", "Controller")
    }

    // 注册切换 WorkMode 的快捷键（Tab）
    private fun registerWorkModeSwitch() {
        cLabelPane.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.TAB) {
                event.consume()
                bSwitchWorkMode.fire()
            }
        }
        Logger.info("Transformed Tab on CLabelPane", "Controller")
    }

    // 注册 Q/W 切换图片
    private fun registerCLabelPicNavigation() {
        val handler = EventHandler<KeyEvent> { event ->
            if (event.isControlDown || event.isMetaDown || event.isShiftDown || event.isAltDown || event.code.isDigitKey) return@EventHandler
            event.consume()
            when (event.code) {
                KeyCode.Q -> cPicBox.back()
                KeyCode.W -> cPicBox.next()
                else -> return@EventHandler
            }
            cTreeView.selectRoot(clear = true, scrollTo = false)
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, handler)
        Logger.info("Transformed Q/W pressed", "Controller")
    }


    // 注册 Ctrl+左右方向键切换图片
    private fun registerPicNavigation() {
        val arrowKeyChangePicHandler = EventHandler<KeyEvent> handler@{
            if (!(it.isControlDown || it.isMetaDown)) return@handler

            when (it.code) {
                KeyCode.LEFT -> cPicBox.back()
                KeyCode.RIGHT -> cPicBox.next()
                else -> return@handler
            }
            cTreeView.selectFirst()
            cLabelPane.moveToLabel(cTreeView.selectedLabel)
            it.consume() // Consume used event
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangePicHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangePicHandler)
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangePicHandler)
        Logger.info("Transformed Ctrl + Left/Right", "Controller")
    }

    // 注册 Ctrl+上下方向键切换标签
    private fun registerLabelNavigation() {
        val arrowKeyChangeLabelHandler = EventHandler<KeyEvent> handler@{
            if (!((it.isControlDown || it.isMetaDown) && it.code.isArrowKey)) return@handler
            // Make sure we'll not get into endless LabelItem find loop
            if (state.transFile.getTransList(state.currentPicName).isEmpty()) return@handler
            // Direction
            val forward: Boolean = when (it.code) {
                KeyCode.UP -> false
                KeyCode.DOWN -> true
                else -> return@handler
            }
            // Mark immediately when this event will be consumed
            it.consume() // stop further propagation
            moveCurrLabelToNext(forward = forward)
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangeLabelHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, arrowKeyChangeLabelHandler)
        Logger.info("Transformed Ctrl + Up/Down", "Controller")

    }

    // 注册 Enter 键跳转下一条/上一条标签
    private fun registerEnterTransform() {
        val enterKeyTransformerHandler = EventHandler<KeyEvent> handler@{
            if (!(it.isControlDown || it.isMetaDown) || it.code != KeyCode.ENTER) return@handler
            // Mark immediately when this event will be consumed
            it.consume() // stop further propagation
            // transform
            if (it.isShiftDown) {
                // Go to previous label
                moveCurrLabelToNext(forward = false, isBreakPage = true)
            } else {
                // Go to next label
                moveCurrLabelToNext(forward = true, isBreakPage = true)
            }
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, enterKeyTransformerHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, enterKeyTransformerHandler)
        Logger.info("Transformed Ctrl + Enter", "Controller")
    }

    // 注册 Ctrl+C/V 复制粘贴标签文本
    private fun registerCopyPaste() {
        val copyLabelHandler = EventHandler<KeyEvent> handler@{
            // Only respond to key events with Ctrl (or Meta on macOS) modifier
            if (!(it.isControlDown || it.isMetaDown)) return@handler

            when (it.code) {
                KeyCode.C -> {
                    // Copy the text of the selected label item
                    val treeItem = cTreeView.getTreeItem(cTreeView.selectionModel.selectedIndex) as CTreeLabelItem
                    cTreeView.copyLabelText(treeItem.transLabel.index)
                }

                KeyCode.V -> {
                    // Paste text to selected label items
                    val selectItems: Collection<CTreeLabelItem> =
                        cTreeView.selectionModel.selectedIndices.map { cTreeView.getTreeItem(it) }
                            .filterIsInstance<CTreeLabelItem>()

                    cTreeView.pasteLabelsText(selectItems.map { it.transLabel.index }, state)
                }

                else -> return@handler
            }

            it.consume() // Consume used event
        }
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, copyLabelHandler)
        Logger.info("Transformed Ctrl + C/V", "Controller")
    }

    // 注册 Alt+左右方向键移动标签分组
    private fun registerLabelGroupMove() {
        val labelGroupChangeHandler = EventHandler<KeyEvent> handler@{
            if (!(it.isAltDown  && it.code.isArrowKey)) return@handler
            // 确保选中的是标签项


            val selectedItem = cTreeView.selectionModel.selectedItem as CTreeLabelItem
            val itemIndex = selectedItem.transLabel.index


            // 获取当前组ID和组列表
            val groupId = selectedItem.transLabel.groupId
            val groups = state.transFile.groupList
            if (groups.isEmpty()) return@handler


            // 根据左右键判断新的分组
            val newGroupId = when (it.code) {
                KeyCode.LEFT -> groups.prevIndex(groupId)
                KeyCode.RIGHT -> groups.nextIndex(groupId)
                else -> return@handler
            }


            // 创建动作
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
            it.consume() // Consume used event
        }
        cLabelPane.addEventHandler(KeyEvent.KEY_PRESSED, labelGroupChangeHandler)
        cTransArea.addEventHandler(KeyEvent.KEY_PRESSED, labelGroupChangeHandler)
        cTreeView.addEventHandler(KeyEvent.KEY_PRESSED, labelGroupChangeHandler)
        Logger.info("Transformed Alt + Left/Right", "Controller")
    }


    // 注册 Alt+上下方向键移动标签序号
    private fun registerLabelIndexMove() {
        val labelIndexChangeHandler = EventHandler<KeyEvent> handler@{
            if (!(it.isAltDown)  && it.code.isArrowKey) return@handler
            // 确保选中的是标签项


            val selectedItem = cTreeView.selectionModel.selectedItem as? CTreeLabelItem ?: return@handler
            val transLabel = selectedItem.transLabel
            val labels = state.transFile.getTransList(state.currentPicName).map(TransLabel::index)


            // 根据上下键判断新的序号
            val newIndex = when (it.code) {
                KeyCode.UP -> labels.prevItem(transLabel.index)
                KeyCode.DOWN -> labels.nextItem(transLabel.index)
                else -> return@handler
            }
            if (newIndex == NOT_FOUND || newIndex == null) return@handler
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
            it.consume() // Consume used event
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
