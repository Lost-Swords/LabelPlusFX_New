package ink.meodinger.lpfx.component

import ink.meodinger.lpfx.*
import ink.meodinger.lpfx.action.Action
import ink.meodinger.lpfx.action.ActionType
import ink.meodinger.lpfx.action.FunctionAction
import ink.meodinger.lpfx.action.LabelAction
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.type.TransGroup
import ink.meodinger.lpfx.type.TransLabel
import ink.meodinger.lpfx.util.component.expandAll
import ink.meodinger.lpfx.util.doNothing
import ink.meodinger.lpfx.util.property.*
import ink.meodinger.lpfx.util.string.emptyString

import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.input.*
import javafx.scene.paint.Color





/**
 * Author: Meodinger
 * Date: 2021/8/16
 * Have fun with my code!
 */

/**
 * A TreeView for tree-style label display
 */
class CTreeView: TreeView<String>() {

    // region Properties:Layout

    private val rootNameProperty: StringProperty = SimpleStringProperty(emptyString())
    fun rootNameProperty(): StringProperty = rootNameProperty
    var rootName: String by rootNameProperty

    private val viewModeProperty: ObjectProperty<ViewMode> = SimpleObjectProperty(ViewMode.IndexMode)
    fun viewModeProperty(): ObjectProperty<ViewMode> = viewModeProperty
    var viewMode: ViewMode by viewModeProperty

    private val groupsProperty: ListProperty<TransGroup> = SimpleListProperty(FXCollections.emptyObservableList())
    fun groupsProperty(): ListProperty<TransGroup> = groupsProperty
    val groups: ObservableList<TransGroup> by groupsProperty

    private val labelsProperty: ListProperty<TransLabel> = SimpleListProperty(FXCollections.emptyObservableList())
    fun labelsProperty(): ListProperty<TransLabel> = labelsProperty
    val labels: ObservableList<TransLabel> by labelsProperty

    // endregion

    // region Properties:Selection

    private val selectedGroupProperty: IntegerProperty = SimpleIntegerProperty(NOT_FOUND)
    fun selectedGroupProperty(): ReadOnlyIntegerProperty = selectedGroupProperty
    /**
     * Selected GroupItem's GroupId
     */
    var selectedGroup: Int by selectedGroupProperty
        private set

    private val selectedLabelProperty: IntegerProperty = SimpleIntegerProperty(NOT_FOUND)
    fun selectedLabelProperty(): ReadOnlyIntegerProperty = selectedLabelProperty
    /**
     * Selected LabelItem's index
     */
    var selectedLabel: Int by selectedLabelProperty
        private set

    // endregion

    private val groupItems: MutableList<CTreeGroupItem> = ArrayList()
    private val labelItems: MutableList<CTreeLabelItem> = ArrayList()
//    private val copyTextProperty:  StringProperty = SimpleStringProperty(emptyString())
//
//    /**
//     * Selected copyText
//     */
//    var copyText: String by copyTextProperty
//        private set



    init {
        // Init
        root = TreeItem<String?>().apply {
            valueProperty().bind(rootNameProperty())
            valueProperty().addListener(onNew { expandAll() })
        }
        selectionModel.selectionMode = SelectionMode.MULTIPLE

        // Layout
        viewModeProperty.addListener(onNew { update() })
        groupsProperty.addListener(ListChangeListener {
            while (it.next()) {
                if (it.wasPermutated()) {
                    // will not happen
                    throw IllegalStateException("Permuted: $it")
                } else if (it.wasUpdated()) {
                    // Ignore, TransGroup's Property changed
                } else {
                    if (it.wasRemoved()) it.removed.forEach(this::removeGroupItem)
                    if (it.wasAdded()) {
                        it.addedSubList.forEachIndexed { index, group ->
                            createGroupItem(group, groupId = it.from + index)
                        }
                    }
                }
            }
        })
        labelsProperty.addListener(ListChangeListener {
            while (it.next()) {
                if (it.wasPermutated()) {
                    // will not happen
                    throw IllegalStateException("Permuted: $it")
                } else if (it.wasUpdated()) {
                    // will not happen
                    throw IllegalStateException("Updated: $it")
                } else {
                    if (it.wasRemoved()) it.removed.forEach(this::removeLabelItem)
                    if (it.wasAdded()) it.addedSubList.forEach(this::createLabelItem)
                }
            }
        })

        // Selection
        selectionModel.selectedItemProperty().addListener(onNew {
            when (it) {
                // These set will be ignored if select by set selected properties. (old == new)
                is CTreeGroupItem -> selectedGroup = groups.indexOf(it.transGroup)
                is CTreeLabelItem -> selectedLabel = it.transLabel.index
            }
        })
    }

    private fun update() {
        // Save expanded state of group items before clearing
        val expandedGroups = groupItems
            .filter { it.isExpanded }
            .map { it.transGroup.name }
            .toSet()

        root.children.clear()
        groupItems.clear()
        labelItems.clear()

        for ((groupId, transGroup) in groups.withIndex()) createGroupItem(transGroup, groupId)
        for (transLabel in labels) createLabelItem(transLabel)

        // Restore expanded state
        for (groupItem in groupItems) {
            if (groupItem.transGroup.name in expandedGroups) {
                groupItem.isExpanded = true
            }
        }

        selectLabel(selectedLabel, clear = true, scrollTo = true)
    }
    private fun createGroupItem(transGroup: TransGroup, groupId: Int) {
        val groupItem = CTreeGroupItem(transGroup)
        // Add view
        when (viewMode) {
            ViewMode.IndexMode -> doNothing()
            ViewMode.GroupMode -> root.children.add(groupId, groupItem)
        }
        // Add data
        groupItems.add(groupId, groupItem)
    }
    private fun removeGroupItem(transGroup: TransGroup) {
        val groupItem = groupItems.first { it.transGroup === transGroup }
        // Clear selection
        selectionModel.clearSelection(getRow(groupItem))
        // Remove view
        when (viewMode) {
            ViewMode.IndexMode -> doNothing()
            ViewMode.GroupMode -> root.children.remove(groupItem)
        }
        // Remove data
        groupItems.remove(groupItem)
    }
    private fun createLabelItem(transLabel: TransLabel) {
        val labelItem = CTreeLabelItem(transLabel, viewMode == ViewMode.IndexMode)
        // Add view
        val parent = when (viewMode) {
            ViewMode.IndexMode -> root
            ViewMode.GroupMode -> groupItems[transLabel.groupId]
        }
        val index = parent.children.indexOfLast { (it as CTreeLabelItem).transLabel.index < transLabel.index }
        if (index == parent.children.size - 1) parent.children.add(labelItem) else parent.children.add(index + 1, labelItem)
        // Add data
        labelItems.add(labelItem)
    }
    private fun removeLabelItem(transLabel: TransLabel) {
        val labelItem = labelItems.first { it.transLabel === transLabel }
        // Clear selection
        selectionModel.clearSelection(getRow(labelItem))
        // Remove view
        when (viewMode) {
            ViewMode.IndexMode -> root.children.remove(labelItem)
            ViewMode.GroupMode -> groupItems[transLabel.groupId].children.remove(labelItem)
        }
        // Remove data
        labelItems.remove(labelItem)
    }

    /**
     * Select root item.
     * @param clear Whether clear selection before select.
     * @param scrollTo Whether scroll to selected item.
     */
    fun selectRoot(clear: Boolean, scrollTo: Boolean) {
        if (clear) clearSelection()
        selectionModel.select(root)
        if (scrollTo) scrollTo(getRow(root))
    }
    /**
     * Select first label in the tree.
     * If no label exists, select root.
     * @param clear Whether clear selection before select.
     * @param scrollTo Whether scroll to selected item.
     * @return The index of first label.
     */
    fun selectFirst(clear: Boolean = true, scrollTo: Boolean = true) : Int {
        if(labelItems.isEmpty()) {
            selectRoot(clear, scrollTo)
            return NOT_FOUND
        }
        selectLabel(labelItems[0].transLabel.index, clear, scrollTo)
        return labelItems[0].transLabel.index
    }

    /**
     * Select last label in the tree.
     * If no label exists, select root.
     * @param clear Whether clear selection before select.
     * @param scrollTo Whether scroll to selected item.
     * @return The index of last label.
     */
    fun selectLast(clear: Boolean = true, scrollTo: Boolean = true) : Int {
        if(labelItems.isEmpty()) {
            selectRoot(clear, scrollTo)
            return NOT_FOUND
        }
        selectLabel(labelItems.last().transLabel.index, clear, scrollTo)
        return labelItems.last().transLabel.index
    }

    /**
     * Select group item.
     * @param groupName The name of group.
     * @param clear Whether clear selection before select.
     * @param scrollTo Whether scroll to selected item.
     */
    fun selectGroup(groupName: String, clear: Boolean, scrollTo: Boolean) {
        // In IndexMode this is not available
        if (viewMode == ViewMode.IndexMode) return

        if (clear) clearSelection()
        val item = groupItems.first { it.transGroup.name == groupName }

        selectionModel.select(item)
        if (scrollTo) scrollTo(getRow(item))
    }

    /**
     * Select label item.
     * @param labelIndex The index of label.
     * @param clear Whether clear selection before select.
     * @param scrollTo Whether scroll to selected item.
     */
    fun selectLabel(labelIndex: Int, clear: Boolean, scrollTo: Boolean) {
        val item = labelItems.firstOrNull{ it.transLabel.index == labelIndex } ?:return
        if (clear) clearSelection()

        selectionModel.select(item)
        if (scrollTo) scrollTo(getRow(item))
    }

    /**
     * Select label items.
     * @param labelIndices The indices of labels.
     * @param clear Whether clear selection before select.
     * @param scrollTo Whether scroll to selected item.
     */
    fun selectLabels(labelIndices: Collection<Int>, clear: Boolean, scrollTo: Boolean) {
        if (clear) clearSelection()
        val items = labelItems.filter { it.transLabel.index in labelIndices }

        items.forEach(selectionModel::select)
        if (scrollTo && items.isNotEmpty()) scrollTo(getRow(items.first()))
    }

    /**
     * Copy label text.
     * @param labelIndex The index of label.
     */
    fun copyLabelText(labelIndex: Int) {
        val item = labelItems.firstOrNull { it.transLabel.index == labelIndex } ?:return
        val clipboard = Clipboard.getSystemClipboard()
        val clipboardContent = ClipboardContent()
        clipboardContent.putString(item.transLabel.text)
        Logger.info("Copy text from the label of $labelIndex", "CTreeView")
        clipboard.setContent(clipboardContent)
    }

    /**
     * Paste labels text.
     * @param labelIndexes The indices of labels.
     * @param state The state of the program.
     */
    fun pasteLabelsText(labelIndexes: Collection<Int>, state: State) {
        val indexes = labelItems.map { it.transLabel.index }.filter { labelIndexes.any { i -> i == it } }
        val clipboard = Clipboard.getSystemClipboard()
        if (!clipboard.hasString()||indexes.isEmpty()) return
        Logger.info("Paste text into the labels of ${indexes.joinToString(separator = ",")}", "CTreeView")
        val labelActions = indexes.map {
            LabelAction(
                ActionType.CHANGE, state,
                state.currentPicName,
                state.transFile.getTransLabel(state.currentPicName, it),
                newText = clipboard.string
            )
        }
        val pasteAction = FunctionAction(
            { labelActions.forEach(Action::commit) },
            { labelActions.forEach(Action::revert) }
        )
        state.doAction(pasteAction)
    }


    /**
     * This will also clear the selected-index
     */
    fun clearSelection() {
        selectionModel.clearSelection()
        selectedGroup = NOT_FOUND
        selectedLabel = NOT_FOUND
    }

    /**
     * Request the TreeView to re-render. This function is useful
     * when some labels' group change in IndexMode.
     */
    fun requestUpdate() {
        update()
    }

    // region Drag and Drop

    companion object {
        /** Drag data format: comma-separated label indices as String, e.g. "1,3,5" */
        private val LABEL_DRAG_FORMAT: DataFormat =
            DataFormat.lookupMimeType("application/x-lpfx-label-drag") ?: DataFormat("application/x-lpfx-label-drag")
    }

    /**
     * Install cell factory with drag-and-drop support (multi-select).
     *
     * Drag-and-drop rules:
     * - IndexMode: always reorder (change index)
     * - GroupMode + same group label: reorder (change index)
     * - GroupMode + different group label / group item: switch group only
     *
     * @param state application state
     * @param labelPane the CLabelPane, used for moveToLabel after reorder
     */
    fun installCellFactory(state: State, labelPane: CLabelPane) {
        setCellFactory {
            object : TreeCell<String>() {

                private val markListener: ChangeListener<Boolean> = onNew {
                    textFill = if (it) Color.RED else Color.BLACK
                }

                init {
                    treeItemProperty().addListener { _, oldV, newV ->
                        if (oldV is CTreeLabelItem) oldV.transLabel.markedProperty().removeListener(markListener)
                        if (newV is CTreeLabelItem) newV.transLabel.markedProperty().addListener(markListener)
                    }

                    // region Drag and Drop

                    setOnDragDetected { event ->
                        val labelItem = treeItem as? CTreeLabelItem ?: return@setOnDragDetected
                        // 快照所有选中的 label index
                        val selectedLabelIndices = mutableListOf<Int>()
                        for (item in selectionModel.selectedItems) {
                            if (item is CTreeLabelItem) selectedLabelIndices.add(item.transLabel.index)
                        }
                        val dragIndices = if (labelItem.transLabel.index in selectedLabelIndices) {
                            selectedLabelIndices
                        } else {
                            mutableListOf(labelItem.transLabel.index)
                        }

                        val dragboard = startDragAndDrop(TransferMode.MOVE)
                        val content = ClipboardContent()
                        content[LABEL_DRAG_FORMAT] = dragIndices.joinToString(",")
                        dragboard.setContent(content)
                        dragboard.dragView = snapshot(null, null)
                        event.consume()
                    }

                    setOnDragOver { event ->
                        if (event.gestureSource !== this
                            && event.dragboard.hasContent(LABEL_DRAG_FORMAT)
                            && (treeItem is CTreeLabelItem || treeItem is CTreeGroupItem)
                        ) {
                            event.acceptTransferModes(TransferMode.MOVE)
                        }
                        event.consume()
                    }

                    setOnDragEntered { event ->
                        if (event.gestureSource !== this
                            && event.dragboard.hasContent(LABEL_DRAG_FORMAT)
                            && (treeItem is CTreeLabelItem || treeItem is CTreeGroupItem)
                        ) {
                            opacity = 0.7
                        }
                    }

                    setOnDragExited { _ ->
                        opacity = 1.0
                    }

                    setOnDragDropped { event ->
                        val dragboard = event.dragboard
                        var success = false
                        if (dragboard.hasContent(LABEL_DRAG_FORMAT) && state.isOpened) {
                            val indicesStr = dragboard.getContent(LABEL_DRAG_FORMAT) as String
                            val fromIndices = indicesStr.split(",").mapNotNull { it.toIntOrNull() }
                            if (fromIndices.isNotEmpty()) when (treeItem) {
                                is CTreeLabelItem -> {
                                    val targetLabel = (treeItem as CTreeLabelItem).transLabel
                                    val toIndex = targetLabel.index
                                    val insertAfter = event.y >= height / 2.0
                                    val shouldReorder = viewMode == ViewMode.IndexMode
                                            || fromIndices.all {
                                        state.transFile.getTransLabel(state.currentPicName, it).groupId == targetLabel.groupId
                                    }
                                    if (shouldReorder) {
                                        success = doBatchReorder(state, labelPane, fromIndices, toIndex, insertAfter)
                                    } else {
                                        success = doBatchGroupChange(state, fromIndices, targetLabel.groupId)
                                    }
                                }
                                is CTreeGroupItem -> {
                                    if (viewMode == ViewMode.GroupMode) {
                                        val targetGroupId = (treeItem as CTreeGroupItem).transGroup.index
                                        success = doBatchGroupChange(state, fromIndices, targetGroupId)
                                    }
                                }
                            }
                        }
                        event.isDropCompleted = success
                        event.consume()
                    }

                    setOnDragDone { event ->
                        event.consume()
                    }

                    // endregion
                }

                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    textFill = Color.BLACK

                    val actualItem = treeItem
                    if (item != null && !empty) {
                        text = item
                        graphic = actualItem.graphic
                        if (actualItem is CTreeLabelItem && actualItem.transLabel.isMarked) {
                            textFill = Color.RED
                        }
                    } else {
                        text = emptyString()
                        graphic = null
                    }
                }
            }
        }
    }

    /**
     * 批量改变序号：直接操作底层列表做一次性重排，避免多次 remove/add 触发连锁 listener 导致卡死。
     * 用 FunctionAction 包装，支持撤销。
     */
    private fun doBatchReorder(
        state: State,
        labelPane: CLabelPane,
        fromIndices: List<Int>,
        toIndex: Int,
        insertAfter: Boolean
    ): Boolean {
        val picName = state.currentPicName
        val list = state.transFile.transMapObservable[picName] ?: return false
        val indicesToMove = fromIndices.filter { it != toIndex }
        if (indicesToMove.isEmpty()) return false
        val movedLabels = list.filter { it.index in indicesToMove }

        // 快照原始列表顺序（保存每个 TransLabel 对象的引用和原始 index）
        val originalSnapshot = list.map { Pair(it, it.index) }

        val action = FunctionAction(
            {
                val labelsToMove = indicesToMove.mapNotNull { idx -> list.firstOrNull { it.index == idx } }
                val targetLabel = list.firstOrNull { it.index == toIndex } ?: return@FunctionAction

                // 从列表中移除要移动的标签
                list.removeAll(labelsToMove.toSet())
                // 找到目标标签的新位置并插入
                val insertPos = (list.indexOf(targetLabel) + if (insertAfter) 1 else 0).coerceAtMost(list.size)
                list.addAll(insertPos, labelsToMove)
                // 重新编号（1-based）
                for ((i, label) in list.withIndex()) label.index = i + 1

                state.controller.requestUpdateTree()
            },
            {
                // 按原始快照恢复顺序
                val currentLabels = list.toMutableList()
                list.clear()
                for ((origLabel, _) in originalSnapshot) {
                    val label = currentLabels.firstOrNull { it === origLabel }
                    if (label != null) {
                        list.add(label)
                        currentLabels.remove(label)
                    }
                }
                // 补回可能遗漏的（不应发生）
                list.addAll(currentLabels)
                // 恢复原始编号
                for ((i, pair) in originalSnapshot.withIndex()) {
                    if (i < list.size) list[i].index = pair.second
                }

                state.controller.requestUpdateTree()
            }
        )
        state.doAction(action)
        val movedLabelIndex = movedLabels.firstOrNull()?.index ?: toIndex
        selectLabel(movedLabelIndex, clear = true, scrollTo = true)
        labelPane.moveToLabel(movedLabelIndex)
        return true
    }

    /**
     * 批量切换分组：只修改 groupId，不涉及列表 remove/add，安全无卡死风险。
     */
    private fun doBatchGroupChange(state: State, fromIndices: List<Int>, targetGroupId: Int): Boolean {
        val picName = state.currentPicName
        val labelsAndOldGroupId = fromIndices.mapNotNull { idx ->
            val label = state.transFile.getTransLabel(picName, idx)
            if (label.groupId != targetGroupId) label to label.groupId else null
        }
        if (labelsAndOldGroupId.isEmpty()) return false

        val action = FunctionAction(
            {
                for ((label, _) in labelsAndOldGroupId) label.groupId = targetGroupId
                state.controller.requestUpdateTree()
            },
            {
                for ((label, oldGroupId) in labelsAndOldGroupId) label.groupId = oldGroupId
                state.controller.requestUpdateTree()
            }
        )
        state.doAction(action)
        selectLabel(fromIndices.first(), clear = true, scrollTo = true)
        return true
    }

    // endregion

}
