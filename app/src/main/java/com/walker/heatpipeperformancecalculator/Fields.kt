package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.*
import com.walker.heatpipeperformancecalculator.Field.Companion.formatter
import com.walker.heatpipeperformancecalculator.MainActivity.Companion.numberFields
import com.walker.heatpipeperformancecalculator.R.attr.name
import org.mariuszgromada.math.mxparser.Function

/**
 * Created by walker on 3/16/18.
 */

abstract class Field<T>(context: Context, attrs: AttributeSet) : LinearLayout (context, attrs){
    companion object {
        var updateOutputs = {}
        lateinit var getContext: () -> Context
        val fields = ArrayList<Field<*>>()
    }


    val name: String = attrs.getAttributeValue(R.styleable.Field_name)?: "No Name"

    val nameText = findViewById<TextView>(R.id.field_text)

    abstract val fieldView: View

    abstract val value: T

    init {
        fields.add(this)
    }

    operator fun invoke() = value
}

abstract class Number(context: Context, attrs: AttributeSet) : Field<Double>(context, attrs) {
    companion object {
        const val STATIC_UNITS = 1
        const val IS_IMPORTANT = 2
    }

    abstract val number: Double

    val convertedNumberString: String get() = unitConverter.convertTo(number).toRoundedString()

    private val numberUnits: String = attrs.getAttributeValue(R.styleable.NumberField_numberUnits)

    val unitConverter = UnitConverter(numberUnits)

    val textUnits get() = unitConverter.baseUnits

    val numberUnitsText = createTextView(context, numberUnits)

    val hasStaticUnits = attrs.getAttributeIntValue(R.styleable.NumberField_type, 0) and STATIC_UNITS != 0 || textUnits.isEmpty()

    open fun changeUnit(oldUnit: String, newUnit: String) {
        if (!hasStaticUnits) {
            unitConverter.changeUnit(oldUnit, newUnit)
            numberUnitsText.text = textUnits
            updateNumberText()
        }
    }

    abstract fun updateNumberText()

    override fun toString() = name.toString()

    override val value: Double
        get() = unitConverter.convertTo(number)
}

abstract class TextNumberField(context: Context, attrs: AttributeSet) : Number(context, attrs) {
    abstract val numberText: TextView
    abstract val numberLayout: LinearLayout
    override val fieldView: LinearLayout get() = numberLayout
}

interface Input

class InputNumberText(context: Context, attrs: AttributeSet) : TextNumberField(context, attrs), Input {
    val initialValue: Double = attrs.getAttributeFloatValue(R.styleable.InputNumberTextField_initialValue, 0f).toDouble()
    val minValue: Double = attrs.getAttributeFloatValue(R.styleable.InputNumberTextField_minValue, 0f).toDouble()
    val maxValue: Double = attrs.getAttributeFloatValue(R.styleable.InputNumberTextField_maxValue, 0f).toDouble()
    val inputListener = {
        val textNumber = numberText.text.toString().toDoubleOrNull()
        if (textNumber != null) {
            number = unitConverter.convertTo(textNumber).clamp(minValue, maxValue)
            updateOutputs()
        } else {
            "Please enter a number".toast(context)
        }
    }

    override var number: Double = initialValue

    val convertedMaxValue get() = unitConverter.convertTo(maxValue)
    val convertedMinValue get() = unitConverter.convertTo(minValue)

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


class OutputNumberText(context: Context, attrs: AttributeSet) : TextNumberField(context, attrs) {
    val equationString = attrs.getAttributeValue(R.styleable.OutputNumberText_equation)
    val equation = Function(equationString)
    override val number get() = equation.calculate()
    override val numberText = createTextView(context)
    val isImportant = attrs.getAttributeIntValue(R.styleable.NumberField_type, 0) and Number.IS_IMPORTANT != 0
    override val numberLayout = createLinearLayout(context, numberText, numberUnitsText)

    override fun updateNumberText() {
        numberText.text = unitConverter.convertTo(number).toFormattedString()
    }

    fun isDependantOn(inputField: InputNumberText): Boolean {

    }
}

interface Menu<T> {
    val menu: Spinner
    var selected: T
    val items: Array<out T>
    fun createMenu(context: Context, onSelect: (oldVal: T) -> Unit): Spinner {
        val spinner = createSpinner(context, *items) { _, position: Int ->
            val oldVal = selected
            selected = items[position]
            onSelect(oldVal)
        }
        return spinner
    }

    fun setSelection(selection: T) {
        menu.setSelection(items.indexOf(selection))
    }
}

class InputWordMenu(context: Context, attrs: AttributeSet) : Field(context, attrs), Menu<String>, Input {
    override val menu = createMenu(context) { Field.updateOutputs() }

    override val items: Array<String> = context.obtainStyledAttributes(attrs, R.styleable.InputWordMenuField).run { resources.getStringArray(getResourceId(R.styleable.InputWordMenuField_items, 0)) }
    init {
        nameText.text = name
        menu.setPadding(menu.paddingLeft + 160, menu.paddingTop, 0, menu.paddingBottom)
    }

    override var selected = items[0]

    override val fieldView: View get() = menu

    operator fun invoke() = selected
}

class InputNumberMenuField(context: Context, attrs: AttributeSet) : Number(context, attrs), Menu<Double>, Input {
    override val items = context.obtainStyledAttributes(attrs, R.styleable.InputNumberMenuField).run { resources.getStringArray(getResourceId(R.styleable.InputNumberMenuField_items, 0)) }.map {it.toDouble()}.toTypedArray()
    override var number: Double = items[0]
    override var menu = createMenu(context) { Field.updateOutputs() }
    override val fieldView get() = menu
    override var selected: Double
        get() = number
        set(value) {
            number = value
        }

    override fun updateNumberText() {
        menu.adapter = ArrayAdapter<String>(context, R.layout.text_view_wrap, items.map { "${unitConverter.convertTo(it).toRoundedString()} $textUnits" })
    }
}

class UnitMenuField(context: Context, attrs: AttributeSet) : Field(context, attrs), Menu<String> {
    override val items: Array<out String> = context.obtainStyledAttributes(attrs, R.styleable.InputWordMenuField).run { resources.getStringArray(getResourceId(R.styleable.InputWordMenuField_items, 0)) }
    override val fieldView: View get() = menu
    override var selected: String = items[0]
    override val menu = createMenu(context) { oldUnit ->
        numberFields.forEach { field ->
            field.changeUnit(oldUnit, selected)
        }
    }
}