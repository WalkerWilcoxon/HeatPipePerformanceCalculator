package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.View
import android.widget.*
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KProperty

object Globals {
    val smallSpinnerText = R.layout.spinner_text_small
    val wrapSpinnerText = R.layout.spinner_text_wrap
    val sigFigs = 3
}

fun Double.convertTo(from: UnitConverter, to: UnitConverter) = from.convertTo(this, to)

fun Int.dpToPx() = this * Resources.getSystem().displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT


fun Any.toast(context: Context) {
    Toast.makeText(context, this.toString(), Toast.LENGTH_LONG).show()
}

enum class Tags {
    Default,
    UnitConverter,
    NumberField,
    Error
}

fun Any.Log(tag: Tags) {
    android.util.Log.i(tag.name, "\n $this")
}

fun Double.clamp(min: Double, max: Double) = min(max, max(min, this))

fun String.unitType(): UnitConverter.Factors.UnitType {
    UnitConverter.Factors.units.forEach {
        if (this in it.value)
            return it.key
    }
    throw NoSuchElementException("Unit type of $this could not be determined")
}

fun String.baseUnitName(): String {
    return UnitConverter.Factors.baseUnits[UnitConverter.Factors.UnitType.valueOf(this)]!!
}

fun View.toggleVisibility() {
    this.visibility = if (this.visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

fun GridLayout.add(view: View, row: Int, column: Int) {
    this.addView(view, GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column)))
}

fun <T> Spinner.init(items: Array<T>, spinnerText: Int, itemSelected: (view: View?, position: Int) -> Unit) {
    adapter = ArrayAdapter<T>(context, spinnerText , items)
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        var isEditable = true
        override fun onNothingSelected(view: AdapterView<*>?) {}
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if(isEditable) {
                isEditable = false
                itemSelected(view, position)
                isEditable = true
            }
        }
    }
}

fun <T> createSpinner(context: Context, vararg items: T, itemSelected: (view: View?, position: Int) -> Unit): Spinner {
    val spinner = Spinner(context)
    spinner.init(items, Globals.wrapSpinnerText, itemSelected)
    return spinner
}

fun createTextView(context: Context, text: String = "", size: Float = 15f): TextView {
    val view = TextView(context)
    view.text = text
    view.textSize = size
    view.setTextColor(Color.BLACK)
    return view
}

fun createEditText(context: Context, size: Float = 15f): EditText {
    val view = EditText(context)
    view.textSize = size
    view.setTextColor(Color.BLACK)
    return view
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

fun Double.mapTo(from: Range, to: Range): Double{
    return from.mapTo(this, to)
}
fun Int.mapTo(from: Range, to: Range): Double{
    return from.mapTo(this.toDouble(), to)
}

fun createTextWatcher(callback: () -> Unit): TextWatcher {
    return object: TextWatcher {
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
    val layout = LinearLayout(context)
    layout.orientation = LinearLayout.HORIZONTAL
    layout.addView(first)
    with(second) {
        setPadding(left + 10, top, right, bottom)
    }
    layout.addView(second)
    return layout
}