package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.abs

/**
 * Created by walker on 3/16/18.
 */

abstract class Field(val name: String) {
    companion object {
        var updateOutputs = {}
        lateinit var getContext: () -> Context
        val fields = ArrayList<Field>()
        val context get() = getContext()
    }

    val nameText: TextView = createTextView(context, name)

    val views = arrayListOf<View>(nameText)

    fun addToGrid(grid: GridLayout) {
        val row = grid.rowCount
        nameText.layoutParams = GridLayout.LayoutParams(GridLayout.spec(row, GridLayout.CENTER), GridLayout.spec(0,1, 2f))

        setLayoutParams(row)
        views.forEach { grid.addView(it) }
    }

    abstract fun setLayoutParams(row: Int)

    fun toggleVisibility() {
        views.forEach { it.toggleVisibility() }
    }

    init {
        fields.add(this)
    }

    inner class Menu<T>(context: Context, initialSelection: T, val items: Array<out T>, onSelect: (oldVal: T, newVale: T) -> Unit) : Spinner(context) {
        init {
            init(items) { _, position: Int ->
                val oldVal = selected
                selected = items[position]
                onSelect(oldVal, selected)
            }
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            gravity = Gravity.RIGHT
            views += this
        }

        var selected = initialSelection
        fun setSelection(selection: T) {
            setSelection(items.indexOf(selection))
        }

        fun setLayoutParams(row: Int) {
            layoutParams = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(1, 2, GridLayout.LEFT, 2f))
        }
    }
}

abstract class Number(name: String, numberUnits: String, typeFlags: Int) : Field(name) {
    companion object {
        const val STATIC_UNITS = 1
        const val IS_IMPORTANT = 2
    }

    abstract val number: Double

    val unitConverter = createUnitConverter(numberUnits, typeFlags and STATIC_UNITS == 0)

    val convertedUnits get() = unitConverter.convertedUnits

    val convertedNumber get() = unitConverter.convertTo(number)

    abstract fun changeUnit(oldUnit: String, newUnit: String)

    override fun toString() = name

    operator fun invoke() = number
}

abstract class NumberText(name: String, numberUnits: String, typeFlags: Int) : Number(name, numberUnits, typeFlags) {
    val unitText = createTextView(context, numberUnits).also {
        views += it
    }

    abstract val numberText: TextView

    final override fun setLayoutParams(row: Int) {
        numberText.layoutParams = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(1, 1, GridLayout.RIGHT, 1f))
        unitText.layoutParams = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(2, 1, GridLayout.LEFT, 1f))
    }
}

interface Input

class InputNumberText(name: String, numberUnits: String, initialValue: Double, val maxValue: Double, val minValue: Double, typeFlags: Int = 0) : NumberText(name, numberUnits, typeFlags), Input {
    override var number: Double = initialValue

    val convertedMaxValue get() = unitConverter.convertTo(maxValue)
    val convertedMinValue get() = unitConverter.convertTo(minValue)

    override val numberText: EditText = createEditText(context, initialValue.toRoundedString()).also {
        views += it
    }

    override fun changeUnit(oldUnit: String, newUnit: String) {
        if (!unitConverter.isStatic) {
            unitConverter.changeUnit(oldUnit, newUnit)
            unitText.text = convertedUnits
            numberText.setText(convertedNumber.toRoundedString())
        }
    }

    init {
        numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or if (minValue < 0.0) InputType.TYPE_NUMBER_FLAG_SIGNED else 0
        numberText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                var convertedNumber = numberText.text.toString().toDoubleOrNull()
                if (convertedNumber != null) {
                    if (convertedNumber < convertedMinValue || convertedMaxValue < convertedNumber) {
                        convertedNumber = convertedNumber.clamp(convertedMinValue, convertedMaxValue)
                        numberText.setText(convertedNumber.toRoundedString())
                    }
                    number = unitConverter.convertFrom(convertedNumber)
                    updateOutputs()
                } else {
                    "Please enter a number".toast(context)
                }
            }
        })
    }
}

interface Output {
    fun updateNumberText()
}

class OutputNumber(name: String, units: String, typeFlags: Int = 0, val formula: () -> Double) : NumberText(name, units, typeFlags), Output {
    override val number get() = formula()
    override val numberText = createTextView(context).also {
        views += it
    }
    val isImportant = typeFlags and Number.IS_IMPORTANT != 0

    override fun changeUnit(oldUnit: String, newUnit: String) {
        if (!unitConverter.isStatic) {
            unitConverter.changeUnit(oldUnit, newUnit)
            unitText.text = convertedUnits
            numberText.text = convertedNumber.toFormattedString()
        }
    }

    override fun updateNumberText() {
        numberText.text = number.toFormattedString()
    }

    fun isDependantOn(inputField: InputNumberText): Boolean {
        val initial = inputField.number
        inputField.number = inputField.minValue
        val min = formula()
        inputField.number = (inputField.minValue + inputField.maxValue) / 2
        val mid = formula()
        inputField.number = inputField.maxValue
        val max = formula()
        inputField.number = initial
        return abs(max - min) < 0.01 && abs(max - mid) < 0.01 && abs(mid - min) < 0.01
    }
}

class InputEnumeration(name: String, initialSelection: String, items: Array<String>) : Field(name), Input {
    val menu = Menu(context, initialSelection, items) { _, _ ->
        updateOutputs()
    }

    override fun setLayoutParams(row: Int) = menu.setLayoutParams(row)

    operator fun invoke() = menu.selected
}

class InputNumberMenu(name: String, units: String, initial: Double, val items: Array<Double>, typeFlags: Int = 0) : Number(name, units, typeFlags), Input {
    override var number = initial

    val menu = Menu(context, initial, items) { _, _ ->
        Field.updateOutputs()
    }

    init {
        menu.setSelection(initial)
    }

    override fun changeUnit(oldUnit: String, newUnit: String) {
        unitConverter.changeUnit(oldUnit, newUnit)
        menu.adapter = ArrayAdapter<String>(context, R.layout.text_view_wrap, items.map { "${unitConverter.convertTo(it).toRoundedString()} ${convertedUnits}" })
    }

    override fun setLayoutParams(row: Int) = menu.setLayoutParams(row)
}

class UnitMenu(name: String, baseUnit: String, units: Array<String>) : Field(name) {
    val menu = Menu(context, baseUnit, units) { oldVal, newVal ->
        MainActivity.numbers.forEach { field ->
            field.changeUnit(oldVal, newVal)
        }
    }

    override fun setLayoutParams(row: Int) = menu.setLayoutParams(row)
}