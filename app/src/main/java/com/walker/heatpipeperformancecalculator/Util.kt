package com.walker.heatpipeperformancecalculator

import android.content.*
import android.graphics.*
import android.text.*
import android.view.*
import android.widget.*
import com.walker.heatpipeperformancecalculator.Globals.formatter
import com.walker.heatpipeperformancecalculator.Globals.mathContext
import com.walker.heatpipeperformancecalculator.Globals.whiteSpace
import java.math.*
import java.text.*
import kotlin.math.*

object Globals {
    val sigFigs = 3
    val mathContext = MathContext(sigFigs)
    val formatter = DecimalFormat("0.##E0")
    var density = 0f
    val whiteSpace = Regex("\\s")
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

fun Any.log(tag: Tags) {
    android.util.Log.i(tag.name, "\n $this")
}

fun Double.clamp(min: Double, max: Double) = min(max, max(min, this))

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

fun <T> Spinner.init(items: List<T>, onSelect: (view: View?, position: Int) -> Unit) {
    adapter = ArrayAdapter<T>(context, R.layout.text_view_wrap, items)
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(view: AdapterView<*>?) {}
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onSelect(view, position)
        }
    }
}

fun createUnitConverter(units: String, static: Boolean) =
        if (!units.isEmpty() && static) {
            if (units in TemperatureConverter.allUnits)
                TemperatureConverter(units)
            else
                MultiConverter(units)
        } else StaticConverter(units)

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

fun createTextWatcher(callback: () -> Unit): TextWatcher {
    return object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            callback()
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    }
}

fun createRelativeLayoutParams(position: Int, view: View) =
        RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(position, view.id)
        }

fun <T> createSpinner(context: Context, items: List<T>, initialSelection: T, onSelect: (oldVal: T, newVal: T) -> Unit) =
        Spinner(context).apply {
            adapter = ArrayAdapter<T>(context, R.layout.text_view_wrap, items)
            setSelection(items.indexOf(initialSelection))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                var selected: T = initialSelection
                override fun onNothingSelected(view: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val oldVal = selected
                    selected = items[position]
                    onSelect(oldVal, selected)
                }
            }
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            gravity = Gravity.RIGHT
        }

inline fun <reified T : View> ViewGroup.getAllChildren() = List(childCount) { getChildAt(it) as T }

fun String.removeWhiteSpace() = replace(whiteSpace, "")

class Range(val min: Double, val max: Double) {
    fun mapTo(num: Double, start: Double, end: Double): Double {
        return (num - min) / (max - min) * (end - start) + start
    }

    fun mapTo(num: Double, range: Range): Double {
        return mapTo(num, range.min, range.max)
    }
}


fun Double.mapTo(from: Range, to: Range): Double {
    return from.mapTo(this, to)
}

fun Int.mapTo(from: Range, to: Range): Double {
    return from.mapTo(this.toDouble(), to)
}

fun Int.dpToPx() = (this * Globals.density).toInt()



