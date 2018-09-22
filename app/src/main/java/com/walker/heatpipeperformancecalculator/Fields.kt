package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.*
import com.walker.heatpipeperformancecalculator.MainActivity.Companion.numberFields
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import kotlin.math.abs

/**
 * Created by walker on 3/16/18.
 */

abstract class Field(val name: String) {
    companion object {
        var updateOutputs = {}
        lateinit var getContext: () -> Context
        val fields = ArrayList<Field>()
        val formatter = DecimalFormat("0.##E0")
    }

    val context: Context get() = getContext()

    val nameText = createTextView(context, name)
    abstract val fieldView: View

    init {
        fields.add(this)
    }

    fun toggleVisibility() {
        nameText.toggleVisibility()
        fieldView.toggleVisibility()
    }

    fun addToGrid(grid: GridLayout) {
        val row = grid.rowCount
        val nameParams = GridLayout.LayoutParams(GridLayout.spec(row, GridLayout.CENTER), GridLayout.spec(0))
        nameParams.leftMargin = 20.dpToPx()
        val fieldParams = GridLayout.LayoutParams(GridLayout.spec(row, GridLayout.CENTER), GridLayout.spec(1, GridLayout.RIGHT))
        //changeLayoutParams(fieldParams)
        if (this is Menu<*>) {
            nameParams.topMargin = 10.dpToPx()
            nameParams.bottomMargin = 5.dpToPx()
        } else if (this is OutputNumberTextField) {
            nameParams.topMargin = 10.dpToPx()
        } else if (this is InputNumberMenuField) {
            nameParams.topMargin = 5.dpToPx()
        }
        if (this is InputWordMenuField) {
            fieldParams.rightMargin = 20.dpToPx()
            fieldParams.width = 100.dpToPx()
        }
        if (this is InputNumberMenuField) {
            fieldParams.width = 60.dpToPx()
        }
        nameText.layoutParams = nameParams
        fieldView.layoutParams = fieldParams

        grid.addView(nameText)
        grid.addView(fieldView)
    }

    //abstract fun changeLayoutParams(fieldParams: GridLayout.LayoutParams)
}

abstract class NumberField(name: String, numberUnits: String, flags: Int = 0) : Field(name) {
    companion object {
        const val STATIC_UNITS = 1
        const val IS_IMPORTANT = 2
    }

    abstract val number: Double
    val convertedNumber: Double
        get() {
            val num = numberConverter.convertTo(number, textConverter)
            if (!num.isFinite()) {
//                throw ArithmeticException("Number: $number at $name is non finite")
            }
            return num
        }

    val convertedNumberString: String get() = BigDecimal(convertedNumber).round(MathContext(Globals.sigFigs)).stripTrailingZeros().toPlainString()

    val numberConverter = UnitConverter(numberUnits)
    val textConverter = UnitConverter(numberUnits)

    val textUnits get() = textConverter.units

    val numberUnitsText = createTextView(context, numberUnits)

    val hasStaticUnits = flags and STATIC_UNITS != 0 || textUnits.isEmpty()

    init {
        numberUnitsText.width = 60.dpToPx()
    }

    open fun changeUnit(oldUnit: String, newUnit: String) {
        if (!hasStaticUnits) {
            textConverter.changeUnit(oldUnit, newUnit)
            numberUnitsText.text = textUnits
            updateNumberText()
        }
    }

    abstract fun updateNumberText()

    override fun toString() = nameText.text.toString()

    operator fun invoke() = number
}

abstract class TextNumberField(name: String, numberUnits: String, flags: Int = 0) : NumberField(name, numberUnits, flags) {
    abstract val numberText: TextView
    abstract val numberLayout: LinearLayout
    override val fieldView: LinearLayout get() = numberLayout
}

interface Input

class InputNumberTextField(name: String, numberUnits: String, initialValue: Double, val maxValue: Double, val minValue: Double = 0.0) : TextNumberField(name, numberUnits), Input {
    val InputListener = {
        if (numberText.text.toString().toDoubleOrNull() != null) {
            var num = numberText.text.toString().toDouble().convertTo(textConverter, numberConverter)
            if(num < minValue) {
                num = minValue

            } else if(num > maxValue) {
                num = maxValue
            }
            number = num
            updateOutputs()
        } else {
            "Please enter a number".toast(context)
        }
    }

    override var number: Double = initialValue

    val convertedMaxValue get() = maxValue.convertTo(numberConverter, textConverter)
    val convertedMinValue get() = minValue.convertTo(numberConverter, textConverter)

    override val numberText: EditText = createEditText(context)

    override val numberLayout = createLinearLayout(context, numberText, numberUnitsText)

    init {
        numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or if (minValue < 0.0) InputType.TYPE_NUMBER_FLAG_SIGNED else 0
        updateNumberText()
        numberText.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(view: View?, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int) {

            }
        })
    }

    override fun updateNumberText() {
        numberText.setText(convertedNumberString)
    }
}


class OutputNumberTextField(name: String, numberUnits: String, flags: Int = 0, val forumla: () -> Double) : TextNumberField(name, numberUnits, flags) {
    override val number get() = forumla()
    override val numberText = createTextView(context)
    val isImportant = flags and NumberField.IS_IMPORTANT != 0
    override val numberLayout = createLinearLayout(context, numberText, numberUnitsText)

    override fun updateNumberText() {
        val num = BigDecimal(convertedNumber).round(MathContext(Globals.sigFigs)).stripTrailingZeros()
        if (abs(num.scale()) >= 3) {
            numberText.text = formatter.format(convertedNumber)
        } else {
            numberText.text = num.toPlainString()
        }
    }

    fun isDependantOn(inputField: InputNumberTextField): Boolean {
        val initialValue = inputField.number
        inputField.number = inputField.minValue
        val minValue = number
        inputField.number = inputField.maxValue
        val maxValue = number
        inputField.number = (inputField.minValue + inputField.maxValue) / 2
        val centerValue= number
        inputField.number = initialValue
        return !(abs(maxValue - minValue) < 0.1 || abs(maxValue - minValue) < 0.1 || abs(minValue - centerValue) < 0.1 || abs(maxValue - centerValue) < 0.1)
    }
}

interface Menu<T> {
    val menu: Spinner
    var selected: T
    val items: Array<out T>
    val initialSelection: T
    fun createMenu(context: Context, onSelect: (oldVal: T) -> Unit): Spinner {
        val spinner = createSpinner(context, *items) { _, position: Int ->
            val oldVal = selected
            selected = items[position]
            onSelect(oldVal)
        }
        spinner.setSelection(items.indexOf(initialSelection))
        return spinner
    }

    fun setSelection(selection: T) {
        menu.setSelection(items.indexOf(selection))
    }
}

class InputWordMenuField(name: String, override val initialSelection: String, override vararg val items: String) : Field(name), Menu<String>, Input {
    override val menu = createMenu(context) { Field.updateOutputs() }

    init {
        nameText.text = name
        menu.setPadding(menu.paddingLeft + 160, menu.paddingTop, 0, menu.paddingBottom)
    }

    override var selected = initialSelection

    override val fieldView: View get() = menu

    operator fun invoke() = selected
}

class InputNumberMenuField(name: String, numberUnits: String, override val initialSelection: Double, vararg nums: Double) : NumberField(name, numberUnits), Menu<Double>, Input {
    override val items = nums.toTypedArray()
    override var number: Double = items[0]
    override var menu = createMenu(context) { Field.updateOutputs() }
    override val fieldView get() = menu
    override var selected: Double
        get() = number
        set(value) {
            number = value
        }

    override fun updateNumberText() {
        menu.adapter = ArrayAdapter<String>(context, Globals.wrapSpinnerText, items.map { "${BigDecimal(numberConverter.convertTo(it, textConverter)).round(MathContext(Globals.sigFigs)).stripTrailingZeros().toPlainString()} $textUnits" })
    }
}

class UnitMenuField(name: String, override val initialSelection: String, override val items: Array<out String>) : Field(name), Menu<String> {
    override val fieldView: View get() = menu
    override var selected: String = initialSelection
    override val menu = createMenu(context) { oldUnit ->
        numberFields.forEach { field ->
            field.changeUnit(oldUnit, selected)
        }
    }
}