package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.abs

/**
 * Created by walker on 3/16/18.
 */

abstract class Field(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    companion object {
        var updateOutputs = {}
    }

    val name: String

    val nameText: TextView

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Field)
        name = a.getString(R.styleable.Field_name) ?: "No Name"
        nameText = createTextView(context, name)
        addView(nameText)
        a.recycle()
    }

    inner class Menu<T>(context: Context, val items: Array<out T>, initialSelection: T = items[0], onSelect: (oldVal: T, newVale: T) -> Unit) : Spinner(context) {
        init {
            init(items) { _, position: Int ->
                val oldVal = selected
                selected = items[position]
                onSelect(oldVal, selected)
            }
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            gravity = Gravity.RIGHT
        }

        var selected = initialSelection
        fun setSelection(selection: T) {
            setSelection(items.indexOf(selection))
        }

        fun setLayoutParams(row: Int) {
            layoutParams = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(1, 2, GridLayout.LEFT, 2f))
        }
    }
    inner class UnitText(context: Context, units: String) : TextView(context) {
        init {
            addView(this)
        }
    }
    inner class NumberText(context: Context, number: String) : TextView(context) {
        init{
            addView(this)
        }
    }
    inner class EditNumber(context: Context, number: String): EditText(context) {
        init {
            addView(this)
        }
    }
}

abstract class NumberField(context: Context, attrs: AttributeSet) : Field(context, attrs) {
    abstract val number: Double

    lateinit var numberUnits: String
        private set
    var unitConverter: UnitConverter? = null
        private set

    init {
        attrs.setValues(context, R.styleable.NumberField) {
            numberUnits = getString(R.styleable.NumberField_units) ?: ""
            val staticConverter = getBoolean(R.styleable.NumberField_staticUnits, false)
            unitConverter = createUnitConverter(numberUnits, staticConverter)
        }
    }

    val convertedUnits get() = unitConverter?.convertedUnits ?: numberUnits

    val convertedNumber get() = unitConverter.convertTo(number)

    fun changeUnit(oldUnit: String, newUnit: String) = unitConverter?.changeUnit(oldUnit, newUnit)

    override fun toString() = name

    operator fun invoke() = number
}

abstract class NumberText(context: Context, attrs: AttributeSet) : NumberField(context, attrs) {
    val unitText: TextView = createTextView(context, numberUnits)
    init {

    }
    abstract val numberText: TextView
}

interface Input

class InputNumberText(context: Context, attrs: AttributeSet) : NumberText(context, attrs), Input {
    override var number: Double = 0.0
    var maxValue: Double = 0.0
        private set
    var minValue: Double = 0.0
        private set

    init {
        attrs.setValues(context, R.styleable.InputNumberText) {
            number = getFloat(R.styleable.InputNumberText_initialValue, 0f).toDouble()
            minValue = getFloat(R.styleable.InputNumberText_minValue, 0f).toDouble()
            maxValue = getFloat(R.styleable.InputNumberText_maxValue, 0f).toDouble()
        }
    }

    val convertedMaxValue get() = unitConverter.convertTo(maxValue)
    val convertedMinValue get() = unitConverter.convertTo(minValue)

    override val numberText: EditText = createEditText(context, number.toRoundedString())

    override fun changeUnit(oldUnit: String, newUnit: String) {
        unitConverter?.changeUnit(oldUnit, newUnit)
        unitText.text = convertedUnits
        numberText.setText(convertedNumber.toRoundedString())
    }

    init {
        addView(numberText)
        numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or if (minValue < 0.0) InputType.TYPE_NUMBER_FLAG_SIGNED else 0
        numberText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                var convertedNumber = numberText.text.toString().toDoubleOrNull()
                if (convertedNumber != null) {
                    if (convertedNumber < convertedMinValue || convertedMaxValue < convertedNumber) {
                        convertedNumber = convertedNumber.clamp(convertedMinValue, convertedMaxValue)
                        numberText.setText(convertedNumber.toString())
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

class OutputNumberText(context: Context, attrs: AttributeSet) : NumberText(context, attrs), Output {
    fun formula(): Double = TODO()
    override val number get() = formula()
    override val numberText = createTextView(context)
    var isImportant = false
        private set

    init {
        attrs.setValues(context, R.styleable.OutputNumberText) {
            isImportant = getBoolean(R.styleable.OutputNumberText_important, false)
        }
        unitConverter?.setOnChangeListener {
            unitText.text = convertedUnits
            numberText.text = convertedNumber.toFormattedString()
        }
    }

    override fun changeUnit(oldUnit: String, newUnit: String) {
        if (!unitConverter.isStatic) {
            unitConverter.changeUnit(oldUnit, newUnit)
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

class InputEnumeration(context: Context, attrs: AttributeSet) : Field(context, attrs), Input {

    var items: Array<Double> = arrayOf()
        private set

    init {
        attrs.setValues(context, R.styleable.InputEnumeration) {
            items = getString(R.styleable.InputEnumeration_items).split(",").map { it.toDouble() }.toTypedArray()
        }
    }

    val menu = Menu(context, items) { _, _ ->
        updateOutputs()
    }

    operator fun invoke() = menu.selected
}

class InputNumberMenu(context: Context, attrs: AttributeSet) : NumberField(context, attrs), Input {
    override var number = 0.0
    var items: Array<Double> = arrayOf()
        private set

    init {
        attrs.setValues(context, R.styleable.InputEnumeration) {
            number = getFloat(R.styleable.InputEnumeration_items, 0f).toDouble()
            items = getString(R.styleable.InputEnumeration_items).split(",").map { it.toDouble() }.toTypedArray()
        }
    }

    val menu = Menu(context, items) { _, _ ->
        Field.updateOutputs()
    }

    init {
        menu.setSelection(number)
        unitConverter?.setOnChangeListener {
            menu.adapter = ArrayAdapter<String>(context, R.layout.text_view_wrap, items.map { "${unitConverter.convertTo(it).toRoundedString()} ${convertedUnits}" })
        }
    }
}

//class UnitMenu(name: String, val baseUnit: String, items: Array<String>) : Field(name) {
//    val menu = Menu(context, items) { oldVal, newVal ->
//        MainActivity.numbers.forEach { field ->
//            field.changeUnit(oldVal, newVal)
//        }
//    }
//}