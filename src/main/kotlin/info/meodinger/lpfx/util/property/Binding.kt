package info.meodinger.lpfx.util.property

import javafx.beans.binding.*
import javafx.beans.property.*
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableNumberValue

/**
 * Author: Meodinger
 * Date: 2021/9/30
 * Location: info.meodinger.lpfx.util.property
 */

/**
 * Shadow .not()
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
operator fun BooleanProperty.not(): BooleanBinding = not()

operator fun IntegerExpression.unaryMinus(): IntegerBinding = negate()
operator fun IntegerProperty.plus(other: ObservableNumberValue): NumberBinding = add(other)
operator fun IntegerProperty.plus(other: Int): IntegerBinding = add(other)
operator fun IntegerProperty.plus(other: Long): LongBinding = add(other)
operator fun IntegerProperty.plus(other: Float): FloatBinding = add(other)
operator fun IntegerProperty.plus(other: Double): DoubleBinding = add(other)
operator fun IntegerProperty.minus(other: ObservableNumberValue): NumberBinding = subtract(other)
operator fun IntegerProperty.minus(other: Int): IntegerBinding = subtract(other)
operator fun IntegerProperty.minus(other: Long): LongBinding = subtract(other)
operator fun IntegerProperty.minus(other: Float): FloatBinding = subtract(other)
operator fun IntegerProperty.minus(other: Double): DoubleBinding = subtract(other)
operator fun IntegerProperty.times(other: ObservableNumberValue): NumberBinding = multiply(other)
operator fun IntegerProperty.times(other: Int): IntegerBinding = multiply(other)
operator fun IntegerProperty.times(other: Long): LongBinding = multiply(other)
operator fun IntegerProperty.times(other: Float): FloatBinding = multiply(other)
operator fun IntegerProperty.times(other: Double): DoubleBinding = multiply(other)
operator fun IntegerProperty.div(other: ObservableNumberValue): NumberBinding = divide(other)
operator fun IntegerProperty.div(other: Int): IntegerBinding = divide(other)
operator fun IntegerProperty.div(other: Long): LongBinding = divide(other)
operator fun IntegerProperty.div(other: Float): FloatBinding = divide(other)
operator fun IntegerProperty.div(other: Double): DoubleBinding = divide(other)

operator fun LongProperty.unaryMinus(): LongBinding = negate()
operator fun LongProperty.plus(other: ObservableNumberValue): NumberBinding = add(other)
operator fun LongProperty.plus(other: Number): LongBinding = add(other.toLong())
operator fun LongProperty.plus(other: Float): FloatBinding = add(other)
operator fun LongProperty.plus(other: Double): DoubleBinding = add(other)
operator fun LongProperty.minus(other: ObservableNumberValue): NumberBinding = subtract(other)
operator fun LongProperty.minus(other: Number): LongBinding = subtract(other.toLong())
operator fun LongProperty.minus(other: Float): FloatBinding = subtract(other)
operator fun LongProperty.minus(other: Double): DoubleBinding = subtract(other)
operator fun LongProperty.times(other: ObservableNumberValue): NumberBinding = multiply(other)
operator fun LongProperty.times(other: Number): LongBinding = multiply(other.toLong())
operator fun LongProperty.times(other: Float): FloatBinding = multiply(other)
operator fun LongProperty.times(other: Double): DoubleBinding = multiply(other)
operator fun LongProperty.div(other: ObservableNumberValue): NumberBinding = divide(other)
operator fun LongProperty.div(other: Number): LongBinding = divide(other.toLong())
operator fun LongProperty.div(other: Float): FloatBinding = divide(other)
operator fun LongProperty.div(other: Double): DoubleBinding = divide(other)

operator fun FloatProperty.unaryMinus(): FloatBinding = negate()
operator fun FloatProperty.plus(other: ObservableNumberValue): NumberBinding = add(other)
operator fun FloatProperty.plus(other: Number): FloatBinding = add(other.toFloat())
operator fun FloatProperty.plus(other: Double): DoubleBinding = add(other)
operator fun FloatProperty.minus(other: ObservableNumberValue): NumberBinding = subtract(other)
operator fun FloatProperty.minus(other: Number): FloatBinding = subtract(other.toFloat())
operator fun FloatProperty.minus(other: Double): DoubleBinding = subtract(other)
operator fun FloatProperty.times(other: ObservableNumberValue): NumberBinding = multiply(other)
operator fun FloatProperty.times(other: Number): FloatBinding = multiply(other.toFloat())
operator fun FloatProperty.times(other: Double): DoubleBinding = multiply(other)
operator fun FloatProperty.div(other: ObservableNumberValue): NumberBinding = divide(other)
operator fun FloatProperty.div(other: Number): FloatBinding = divide(other.toFloat())
operator fun FloatProperty.div(other: Double): DoubleBinding = divide(other)

operator fun DoubleProperty.unaryMinus(): DoubleBinding = negate()
operator fun DoubleProperty.plus(other: ObservableNumberValue): NumberBinding = add(other)
operator fun DoubleProperty.plus(other: Number): DoubleBinding = add(other.toDouble())
operator fun DoubleProperty.minus(other: ObservableNumberValue): NumberBinding = subtract(other)
operator fun DoubleProperty.minus(other: Number): DoubleBinding = subtract(other.toDouble())
operator fun DoubleProperty.times(other: ObservableNumberValue): NumberBinding = multiply(other)
operator fun DoubleProperty.times(other: Number): DoubleBinding = multiply(other.toDouble())
operator fun DoubleProperty.div(other: ObservableNumberValue): NumberBinding = divide(other)
operator fun DoubleProperty.div(other: Number): DoubleBinding = divide(other.toDouble())

operator fun NumberBinding.unaryMinus(): NumberBinding = negate()
operator fun NumberBinding.plus(other: ObservableNumberValue): NumberBinding = add(other)
operator fun NumberBinding.plus(other: Int): NumberBinding = add(other)
operator fun NumberBinding.plus(other: Long): NumberBinding = add(other)
operator fun NumberBinding.plus(other: Float): NumberBinding = add(other)
operator fun NumberBinding.plus(other: Double): NumberBinding = add(other)
operator fun NumberBinding.minus(other: ObservableNumberValue): NumberBinding = subtract(other)
operator fun NumberBinding.minus(other: Int): NumberBinding = subtract(other)
operator fun NumberBinding.minus(other: Long): NumberBinding = subtract(other)
operator fun NumberBinding.minus(other: Float): NumberBinding = subtract(other)
operator fun NumberBinding.minus(other: Double): NumberBinding = subtract(other)
operator fun NumberBinding.times(other: ObservableNumberValue): NumberBinding = multiply(other)
operator fun NumberBinding.times(other: Int): NumberBinding = multiply(other)
operator fun NumberBinding.times(other: Long): NumberBinding = multiply(other)
operator fun NumberBinding.times(other: Float): NumberBinding = multiply(other)
operator fun NumberBinding.times(other: Double): NumberBinding = multiply(other)
operator fun NumberBinding.div(other: ObservableNumberValue): NumberBinding = divide(other)
operator fun NumberBinding.div(other: Int): NumberBinding = divide(other)
operator fun NumberBinding.div(other: Long): NumberBinding = divide(other)
operator fun NumberBinding.div(other: Float): NumberBinding = divide(other)
operator fun NumberBinding.div(other: Double): NumberBinding = divide(other)

infix fun BooleanExpression.and(other: Boolean): BooleanBinding = and(SimpleBooleanProperty(other))
infix fun BooleanExpression.and(other: ObservableBooleanValue): BooleanBinding = and(other)
infix fun BooleanExpression.or(other: Boolean): BooleanBinding = or(SimpleBooleanProperty(other))
infix fun BooleanExpression.or(other: ObservableBooleanValue): BooleanBinding = or(other)
infix fun BooleanExpression.xor(other: Boolean): BooleanBinding = Bindings.createBooleanBinding( { get() xor other }, this )
infix fun BooleanExpression.xor(other: ObservableBooleanValue): BooleanBinding = Bindings.createBooleanBinding( { get() xor other.get() }, this )
infix fun BooleanExpression.eq(other: Boolean): BooleanBinding = isEqualTo(SimpleBooleanProperty(other))
infix fun BooleanExpression.eq(other: ObservableBooleanValue): BooleanBinding = isEqualTo(other)

infix fun NumberExpression.gt(other: Int): BooleanBinding = greaterThan(other)
infix fun NumberExpression.gt(other: Long): BooleanBinding = greaterThan(other)
infix fun NumberExpression.gt(other: Float): BooleanBinding = greaterThan(other)
infix fun NumberExpression.gt(other: Double): BooleanBinding = greaterThan(other)
infix fun NumberExpression.gt(other: ObservableNumberValue): BooleanBinding = greaterThan(other)
infix fun NumberExpression.ge(other: Int): BooleanBinding = greaterThanOrEqualTo(other)
infix fun NumberExpression.ge(other: Long): BooleanBinding = greaterThanOrEqualTo(other)
infix fun NumberExpression.ge(other: Float): BooleanBinding = greaterThanOrEqualTo(other)
infix fun NumberExpression.ge(other: Double): BooleanBinding = greaterThanOrEqualTo(other)
infix fun NumberExpression.ge(other: ObservableNumberValue): BooleanBinding = greaterThanOrEqualTo(other)
infix fun NumberExpression.eq(other: Int): BooleanBinding = isEqualTo(other)
infix fun NumberExpression.eq(other: Long): BooleanBinding = isEqualTo(other)
infix fun NumberExpression.eq(other: ObservableNumberValue): BooleanBinding = isEqualTo(other)
infix fun NumberExpression.le(other: Int): BooleanBinding = lessThanOrEqualTo(other)
infix fun NumberExpression.le(other: Long): BooleanBinding = lessThanOrEqualTo(other)
infix fun NumberExpression.le(other: Float): BooleanBinding = lessThanOrEqualTo(other)
infix fun NumberExpression.le(other: Double): BooleanBinding = lessThanOrEqualTo(other)
infix fun NumberExpression.le(other: ObservableNumberValue): BooleanBinding = lessThanOrEqualTo(other)
infix fun NumberExpression.lt(other: Int): BooleanBinding = lessThan(other)
infix fun NumberExpression.lt(other: Long): BooleanBinding = lessThan(other)
infix fun NumberExpression.lt(other: Float): BooleanBinding = lessThan(other)
infix fun NumberExpression.lt(other: Double): BooleanBinding = lessThan(other)
infix fun NumberExpression.lt(other: ObservableNumberValue): BooleanBinding = lessThan(other)