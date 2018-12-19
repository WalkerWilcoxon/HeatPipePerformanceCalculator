package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import android.widget.*
import org.mariuszgromada.math.mxparser.Argument

/**
 * Created by walker on 3/16/18.
 */

abstract class NamedView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    companion object {
        var updateOutputs = {}
    }

    val name: String
    val nameText: TextView

    final override fun addView(child: View?) = super.addView(child)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.NamedView)
        name = a.getString(R.styleable.NamedView_name) ?: "No Name"
        nameText = createTextView(context, name)
        addView(nameText)
        a.recycle()
    }
}

abstract class NumberView(context: Context, attrs: AttributeSet) : NamedView(context, attrs) {
    abstract var number: Double
    var numberUnits: String
    var unitConverter: UnitConverter

    init {
        val a = context.obtainStyledAttributes(R.styleable.NumberView)
        numberUnits = a.getString(R.styleable.NumberView_units) ?: ""
        val staticConverter = a.getBoolean(R.styleable.NumberView_staticUnits, false)
        unitConverter = createUnitConverter(numberUnits, staticConverter)
        a.recycle()
    }

    val convertedUnits get() = unitConverter.toUnits
    val convertedNumber get() = unitConverter.convertTo(number)

    fun changeUnit(oldUnit: String, newUnit: String) = unitConverter.changeUnit(oldUnit, newUnit)

    val dependencies = mutableListOf<NumberView>()

    override fun toString() = name

    operator fun invoke() = number
}

interface Input

class InputNumberText(context: Context, attrs: AttributeSet) : NumberView(context, attrs), Input {
    final override var number: Double
    val maxValue: Double
    val minValue: Double
    val convertedMaxValue get() = unitConverter.convertTo(maxValue)
    val convertedMinValue get() = unitConverter.convertTo(minValue)

    init {
        val a = context.obtainStyledAttributes(R.styleable.InputNumberText)
        number = a.getFloat(R.styleable.InputNumberText_initial, 0f).toDouble()
        minValue = a.getFloat(R.styleable.InputNumberText_min, 0f).toDouble()
        maxValue = a.getFloat(R.styleable.InputNumberText_max, 0f).toDouble()
        a.recycle()
        unitConverter.onChangeListener = {
            unitText.text = convertedUnits
            numberText.setText(convertedNumber.toRoundedString())
        }
    }

    val unitText: TextView = createTextView(context, numberUnits)

    val numberText: EditText = createEditText(context, number.toRoundedString())

    init {
        addView(numberText)
        numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or if (minValue < 0.0) InputType.TYPE_NUMBER_FLAG_SIGNED else 0
        numberText.addTextChangedListener(createTextWatcher {
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
        )
    }
}

interface Output {
    fun updateNumberText()
}

class OutputNumberText(context: Context, attrs: AttributeSet) : NumberView(context, attrs), Output {
    val formula: Double = 0.0
    override val number
        get() = formula

    val numberText = createTextView(context)

    val argument = Argument(name, number)

    val equation: String
    val isImportant: Boolean

    val unitText: TextView = createTextView(context, numberUnits)

    init {
        val a = context.obtainStyledAttributes(R.styleable.OutputNumberText)
        isImportant = a.getBoolean(R.styleable.OutputNumberText_important, false)
        equation = a.getString(R.styleable.OutputNumberText_equation)
        a.recycle()
        unitConverter.onChangeListener = {
            unitText.text = convertedUnits
            numberText.text = convertedNumber.toFormattedString()
        }
    }

    private lateinit var arguments: List<NumberView>

    fun setArguments(numberViews: List<NumberView>) {
        arguments = numberViews.filter { Regex("\\b${it.name}\\b") in equation }
        arguments.forEach {
            it.dependencies += this
        }
    }

    override fun updateNumberText() {
        numberText.text = number.toFormattedString()
    }

    fun isDependantOn(inputNumber: NumberView) = inputNumber in arguments
}

class InputEnumeration(context: Context, attrs: AttributeSet) : NamedView(context, attrs), Input {
    val items: List<String>

    val menu: Spinner

    init {
        val a = context.obtainStyledAttributes(R.styleable.InputEnumeration)
        items = a.getString(R.styleable.InputEnumeration_enums).removeWhiteSpace().split(",")
        val initialSelection = a.getString(R.styleable.InputEnumeration_initialEnum)
        a.recycle()
        menu = createSpinner(context, items, initialSelection) { _, _ ->
            updateOutputs()
        }
    }

    operator fun invoke() = items[menu.selectedItemPosition]
}

class InputNumberMenu(context: Context, attrs: AttributeSet) : NumberView(context, attrs), Input {
    override var number = 0.0
    lateinit var items: List<Double>
        private set

    init {
        val a = context.obtainStyledAttributes(R.styleable.InputNumberMenu)
        number = a.getFloat(R.styleable.InputNumberText_initial, 0f).toDouble()
        items = a.getString(R.styleable.InputNumberMenu_numbers).split(",").map { it.toDouble() }
        a.recycle()
    }

    val menu = createSpinner(context, items, number) { _, _ ->
        NamedView.updateOutputs()
    }

    init {
        unitConverter.onChangeListener = {
            menu.adapter = ArrayAdapter<String>(context, R.layout.text_view_wrap, items.map { "${unitConverter.convertTo(it).toRoundedString()} ${convertedUnits}" })
        }
    }
}

class UnitMenu(context: Context, attrs: AttributeSet) : NamedView(context, attrs) {
    companion object {
        private val num = "([0-9]+)"
        private val unit = "(\\s+)"
        val unitRegex = Regex("$num$unit")
        val tempRegex = Regex("$num$unit\\+$num")
    }

    val unitStrings = mutableListOf<String>()

    val menu: Spinner

    init {
        val a = context.obtainStyledAttributes(R.styleable.UnitMenu)
        fun conversions(regex: Regex) = a.getString(R.styleable.UnitMenu_conversions).removeWhiteSpace().split(",").map { regex.find(it)!!.destructured }
        val baseUnit = a.getString(R.styleable.UnitMenu_base)
        menu = createSpinner(context, unitStrings, baseUnit) { oldVal, newVal ->
            MainActivity.numbers.forEach { field ->
                field.changeUnit(oldVal, newVal)
            }
        }
        a.recycle()
        if (name != "Temperature") {
            MultiConverter.setBaseUnit(baseUnit, name)
            conversions(unitRegex).forEach { conversion ->
                val (factor, unit) = conversion
                MultiConverter.addFactor(factor.toDouble(), unit, name)
                unitStrings += unit
            }
        } else {
            TemperatureConverter.setBaseUnit(baseUnit)
            conversions(tempRegex).forEach { conversion ->
                val (factor, unit, adder) = conversion
                TemperatureConverter.addFormula(factor.toDouble(), unit, adder.toDouble())
                unitStrings += unit
            }
        }
    }
}