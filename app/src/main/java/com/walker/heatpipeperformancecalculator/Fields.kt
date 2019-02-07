package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.*

/**
 * Created by walker on 3/16/18.
 */

abstract class NamedView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    companion object {
        var updateOutputs = {}
    }

    val name: String
    val nameText: TextView
    final override fun addView(child: View?) = super.addView(child)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.NamedView)
        name = a.getString(R.styleable.NamedView_name) ?: throw NamedViewException(this, "Name not found")
        a.recycle()
        nameText = createTextView(context, name)
        nameText.layoutParams = createRelativeLayoutParams(RIGHT_OF, this)
        addView(nameText)
    }
}

abstract class NumberView(context: Context, attrs: AttributeSet) : NamedView(context, attrs) {
    abstract val number: Double
    var numberUnits: String
    var unitConverter: UnitConverter

    init {
        val a = context.obtainStyledAttributes(R.styleable.NumberView)
        numberUnits = a.getString(R.styleable.NumberView_units) ?: ""
        val isStaticConverter = a.getBoolean(R.styleable.NumberView_staticUnits, false)
        unitConverter = createUnitConverter(numberUnits, isStaticConverter)
        a.recycle()
    }

    val toUnits get() = unitConverter.toUnits
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
            unitText.text = toUnits
            numberText.setText(convertedNumber.toRoundedString())
        }
    }

    val numberText: EditText = createEditText(context, number.toRoundedString())
    val unitText: TextView = createTextView(context, numberUnits)

    init {
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
        unitText.layoutParams = createRelativeLayoutParams(RelativeLayout.LEFT_OF, this)
        addView(unitText)
        numberText.layoutParams = createRelativeLayoutParams(RelativeLayout.LEFT_OF, unitText)
        addView(numberText)
    }
}

interface Output {
    fun updateNumberText()
}

class OutputNumberText(context: Context, attrs: AttributeSet) : NumberView(context, attrs), Output {
    lateinit var formula: () -> Double

    override val number get() = formula()

//    val equation: String

    val isImportant: Boolean

    init {
        val a = context.obtainStyledAttributes(R.styleable.OutputNumberText)
        isImportant = a.getBoolean(R.styleable.OutputNumberText_important, false)
//        equation = a.getString(R.styleable.OutputNumberText_equation)
        a.recycle()
        unitConverter.onChangeListener = {
            unitText.text = toUnits
            numberText.text = convertedNumber.toFormattedString()
        }
    }

    val numberText: TextView
    val unitText: TextView

    init {
        numberText = createTextView(context)
        unitText = createTextView(context, numberUnits)
        addView(unitText)
        unitText.layoutParams = createRelativeLayoutParams(LEFT_OF, this)
        numberText.layoutParams = createRelativeLayoutParams(LEFT_OF, unitText)
        addView(numberText)
    }

    override fun updateNumberText() {
        numberText.text = number.toFormattedString()
    }

    fun isDependantOn(inputNumber: NumberView) = true//inputNumber.symbol in equation
}

class InputEnumeration(context: Context, attrs: AttributeSet) : NamedView(context, attrs), Input {
    val items: List<String>

    val menu: Spinner

    init {
        val a = context.obtainStyledAttributes(R.styleable.InputEnumeration)
        items = (a.getString(R.styleable.InputEnumeration_enums)
                ?: throw NamedViewException(this, "Unable to get item list"))
                .removeWhiteSpace().split(",")
        val initialSelection = a.getString(R.styleable.InputEnumeration_initialEnum) ?: items[0]
        a.recycle()
        menu = createSpinner(context, items, initialSelection) { _, _ ->
            updateOutputs()
        }
        menu.layoutParams = createRelativeLayoutParams(LEFT_OF, this)
        addView(menu)
    }

    operator fun invoke() = items[menu.selectedItemPosition]
}

class InputNumberMenu(context: Context, attrs: AttributeSet) : NumberView(context, attrs), Input {
    override var number = 0.0
    val numbers: List<Double>

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.InputNumberMenu)
        number = (a.getString(R.styleable.InputNumberMenu_initialNum)
                ?: throw NamedViewException(this, "Unable to get number")).toDouble()
        numbers = (a.getString(R.styleable.InputNumberMenu_numbers)
                ?: throw NamedViewException(this, "Unable to get number list"))
                .removeWhiteSpace().split(",").map { it.toDouble() }
        a.recycle()
    }

    val menu: Spinner

    init {
        menu = createSpinner(context, numbers, number) { _, _ ->
            NamedView.updateOutputs()
        }
        menu.layoutParams = createRelativeLayoutParams(LEFT_OF, this)
        addView(menu)
        unitConverter.onChangeListener = {
            menu.adapter = ArrayAdapter<String>(context, R.layout.text_view_wrap, numbers.map { "${unitConverter.convertTo(it).toRoundedString()} $toUnits" })
        }
    }
}

class UnitMenu(context: Context, attrs: AttributeSet) : NamedView(context, attrs) {
    companion object {
        private val num = "([0-9]+)"
        private val unit = "([^0-9]+)"
        val unitRegex = Regex("$num$unit")
        val tempRegex = Regex("$num$unit\\+$num")
    }

    val unitStrings = mutableListOf<String>()

    val menu: Spinner

    lateinit var onChangeListener: (String, String) -> Unit

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.UnitMenu)
        val baseUnit = a.getString(R.styleable.UnitMenu_base) ?: "No Base"

        fun conversions(regex: Regex) =
                (a.getString(R.styleable.UnitMenu_conversions)
                        ?: throw NamedViewException(this, "Conversions not found"))
                        .removeWhiteSpace()
                        .split(",")
                        .map {
                            (regex.find(it)
                                    ?: throw NamedViewException(this, "Unable to parse conversion: $it")).destructured
                        }

        menu = createSpinner(context, unitStrings, baseUnit) { oldVal, newVal ->
            onChangeListener(oldVal, newVal)
        }
        menu.layoutParams = createRelativeLayoutParams(LEFT_OF, this)
        addView(menu)
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
        a.recycle()
    }
}

class NamedViewException(view: NamedView, message: String) : java.lang.Exception("Exception in ${view::class.simpleName} ${view.name}: $message")