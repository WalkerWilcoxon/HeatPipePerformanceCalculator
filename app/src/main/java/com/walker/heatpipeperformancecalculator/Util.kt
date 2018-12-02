package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.walker.heatpipeperformancecalculator.Globals.formatter
import com.walker.heatpipeperformancecalculator.Globals.mathContext
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KProperty

object Globals {
    val sigFigs = 3
    val mathContext = MathContext(sigFigs)
    val formatter = DecimalFormat("0.##E0")
    var density = 0f
}

fun Any.toast(context: Context) {
    Toast.makeText(context, this.toString(), Toast.LENGTH_LONG).show()
}

enum class Tags {
    Default,
    UnitConverter,
    Field,
    Error
}

fun Any.Log(tag: Tags) {
    android.util.Log.i(tag.name, "\n $this")
}

fun Double.clamp(min: Double, max: Double) = min(max, max(min, this))

fun String.baseUnitName(): String {
    return UnitConverter.Factors.baseUnits[UnitConverter.Factors.UnitType.valueOf(this)]!!
}

fun Double.toRoundedString(): String = toRoundedBigDecimal().toPlainString()

fun Double.toRoundedBigDecimal(): BigDecimal = BigDecimal(this).round(mathContext).stripTrailingZeros()

fun Double.toFormattedString(): String {
    val num = toRoundedBigDecimal()
    return if (abs(num.scale()) >= 3) {
        formatter.format(this)
    } else {
        num.toPlainString()
    }
}

fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

fun GridLayout.add(view: View, row: Int, column: Int) {
    addView(view, GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column)))
}

fun <T> Spinner.init(items: Array<T>, itemSelected: (view: View?, position: Int) -> Unit) {
    adapter = ArrayAdapter<T>(context, R.layout.text_view_wrap, items)
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        var isEditable = true
        override fun onNothingSelected(view: AdapterView<*>?) {}
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (isEditable) {
                isEditable = false
                itemSelected(view, position)
                isEditable = true
            }
        }
    }
}

fun createUnitConverter(units: String, static: Boolean) = if (!units.isEmpty() && static) UnitConverter(units) else StaticUnitConverter

fun createTextView(context: Context, text: String = "", size: Float = 15f) =
        TextView(context).apply {
            setText(text)
            textSize = size
            setTextColor(Color.BLACK)
        }

fun createEditText(context: Context, text: String, size: Float = 15f) =
        EditText(context).apply {
            setText(text)
            textSize = size
            setTextColor(Color.BLACK)
        }

class lateInit<T>(val constructor: () -> T) {
    companion object {
        val arrayList = ArrayList<lateInit<Any>>()
        fun inialize() {
            arrayList.forEach {
                it.value = it.constructor()
            }
        }
    }

    init {
        arrayList.add(this as lateInit<Any>)
    }

    var value: T? = null
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        return value!!
    }
}

class Range(val min: Double, val max: Double) {
    fun mapTo(num: Double, start: Double, end: Double): Double {
        return (num - min) / (max - min) * (end - start) + start
    }

    fun mapTo(num: Double, range: Range): Double {
        return mapTo(num, range.min, range.max)
    }
}

fun Int.dpToPx() = (this * Globals.density).toInt()

fun Double.mapTo(from: Range, to: Range): Double {
    return from.mapTo(this, to)
}

fun Int.mapTo(from: Range, to: Range): Double {
    return from.mapTo(this.toDouble(), to)
}

fun createTextWatcher(callback: () -> Unit): TextWatcher {
    return object : TextWatcher {
        var isEditable = true
        override fun afterTextChanged(p0: Editable?) {
            if (isEditable) {
                isEditable = false
                callback()
                isEditable = true
            }
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    }
}

fun createLinearLayout(context: Context, first: View, second: View): LinearLayout {
    return LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        with(second) {
            setPadding(left + 10, top, right, bottom)
        }
        addView(first)
        addView(second)
    }
}