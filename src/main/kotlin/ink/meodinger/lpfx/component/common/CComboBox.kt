package ink.meodinger.lpfx.component.common

import ink.meodinger.lpfx.I18N
import ink.meodinger.lpfx.NOT_FOUND
import ink.meodinger.lpfx.get
import ink.meodinger.lpfx.util.property.*

import javafx.beans.property.*
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.layout.HBox


/**
 * Author: Meodinger
 * Date: 2021/7/29
 * Have fun with my code!
 */

/**
 * A ComboBox with back/next Button (wrapped in a HBox).
 * Provided some common wrapped properties and accessibility
 * to the inner ComboBox by field `innerBox`.
 * Note that we do not export property value because
 * **it can be anything as long as it is a valid value of type T.**
 * You can use bindings like `itemsProperty.valueAt(indexProperty)`
 */
class CComboBox<T> : HBox() {

    private val back = Button("<")
    private val next = Button(">")
    val innerBox = ComboBox<T>()

    private val itemsProperty: ListProperty<T> = SimpleListProperty(null)
    fun itemsProperty(): ListProperty<T> = itemsProperty
    var items: ObservableList<T> by itemsProperty

    private val indexProperty: IntegerProperty = SimpleIntegerProperty(NOT_FOUND)
    fun indexProperty(): IntegerProperty = indexProperty
    var index: Int by indexProperty

    private val wrappedProperty: BooleanProperty = SimpleBooleanProperty(false)
    fun wrappedProperty(): BooleanProperty = wrappedProperty
    var isWrapped: Boolean by wrappedProperty

    init {
        // Bind bidirectionally by listeners
        innerBox.itemsProperty().bindBidirectional(itemsProperty)
        innerBox.selectionModel.selectedIndexProperty().addListener(onNew<Number, Int>(indexProperty::set))
        indexProperty.addListener(onNew<Number, Int>(innerBox.selectionModel::select))

        back.setOnMouseClicked { back() }
        next.setOnMouseClicked { next() }

        innerBox.prefWidthProperty().bind(prefWidthProperty() - back.widthProperty() - next.widthProperty())
        back.alignment = Pos.CENTER
        next.alignment = Pos.CENTER

        children.addAll(innerBox, back, next)
    }

    fun back() {
        val size = items.size
        var newIndex = index - 1

        if (isWrapped) if (newIndex < 0) newIndex += size
        if (newIndex >= 0) innerBox.selectionModel.select(newIndex)
    }
    fun next() {
        val size = items.size
        var newIndex = index + 1

        if (isWrapped) if (newIndex >= size) newIndex -= size
        if (newIndex < size) innerBox.selectionModel.select(newIndex)
    }

    fun select(index: Int) {
        if (items.size == 0 && index == 0) return

        if (index in 0 until items.size) innerBox.selectionModel.select(index)
        else throw IllegalArgumentException(String.format(I18N["exception.combo_box.item_index_invalid.i"], index))
    }
    fun select(item: T) {
        if (items.contains(item)) innerBox.selectionModel.select(item)
        else throw IllegalArgumentException(String.format(I18N["exception.combo_box.no_such_item.s"], item.toString()))
    }

}
