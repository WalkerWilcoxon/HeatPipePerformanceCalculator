package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import java.math.BigDecimal
import java.math.MathContext
import kotlin.collections.ArrayList

/**
 * Created by walker on 3/16/18.
 */

abstract class Field {
    companion object {
        var updateOutputs: () -> Unit = {}
        var getContext: () -> Context = { throw ExceptionInInitializerError("Context is not initialized yet") }
        val fields: MutableList<Field> = ArrayList()
    }

    val context: Context get() = getContext()

    val nameText = TextView(context);
    abstract val fieldView: View

    init {
        fields.add(this)
    }

    fun toggleVisibility() {
        nameText.toggleVisibility()
        fieldView.toggleVisibility()
    }

    fun addToGrid(grid: GridLayout, row: Int) {
        var params = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(0))
        params.marginStart = 50
        nameText.layoutParams = params
        params = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(1))
        params.marginStart = 75
        fieldView.layoutParams = params
        grid.addView(nameText)
        grid.addView(fieldView)
    }
}

abstract class NumberField(val name: String, numberUnits: String, val staticUnits: Boolean) : Field() {
    abstract var number: Double
    val convertedNumber get() = number.convertTo(numberConverter, textConverter).format()

    val numberTextString: String
        get() = numberText.text.toString()

    var textUnits = numberUnits

    val numberConverter = UnitConverter(numberUnits)
    val textConverter = UnitConverter(textUnits)

    init {
        updateNameText()
    }

    abstract val numberText: TextView
    override val fieldView: View get() = numberText

    fun changeUnit(oldUnit: String, newUnit: String) {
        if (!staticUnits) {
            textConverter.changeUnit(oldUnit, newUnit)
            textUnits = textUnits.replace(oldUnit, newUnit)
            updateNumberText()
            updateNameText()
        }
    }

    fun updateNumberText() {
        numberText.text = convertedNumber
    }

    fun updateNameText() {
        nameText.text = name + if (textUnits != "") " ($textUnits)" else ""
    }

    abstract fun Double.format(): String
}

class InputNumberField(name: String, numberUnits: String, val initialValue: Double, staticUnits: Boolean) : NumberField(name, numberUnits, staticUnits) {
    override var number: Double = initialValue

    override val numberText: TextView = EditText(context)

    init {
        numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        numberText.text = initialValue.toString()
        numberText.textSize = 15f
        numberText.setTextColor(Color.BLACK)
        numberText.addTextChangedListener(makeTextWatcher())
        numberText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            updateNumberText()
        }
        updateNumberText()
    }

    override fun Double.format(): String {
        return BigDecimal(this).round(MathContext(3)).toPlainString()
    }

    operator fun invoke() = number

    fun makeTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(text: Editable?) {
                if (number != initialValue) {
                    number = numberTextString.toDouble().convertTo(textConverter, numberConverter)
                    updateOutputs()
                }
            }
        }
    }
}


class OutputNumberField(name: String, numberUnits: String, staticUnits: Boolean, val important: Boolean = false, val forumla: () -> Double) : NumberField(name, numberUnits, staticUnits) {
    override var number: Double = forumla()
    override val numberText = TextView(context)

    init {
//        if(!important)
//            toggleVisibility()
        numberText.textSize = 15f
        numberText.setTextColor(Color.BLACK)
        updateNumberText()
    }

    override fun Double.format(): String {
        try {
            return BigDecimal(number).round(MathContext(MainActivity.sigFigs)).toEngineeringString()
        } catch (e: Exception) {
            "Number:$number Name:$name $e".toast(context)
            Log.i("AppTag", e.toString())
            if(number.isNaN())
                Log.i("AppTag", "NaN")
            else
                Log.i("AppTag", "Inf")
            return "-1"
        }
    }

    operator fun invoke(): Double {
        number = forumla()
        updateNumberText()
        return number
    }
}

class MenuField(val name: String, vararg val items: String) : Field() {
    val menu = Spinner(context, *items) { _, _, position: Int, _ ->
        selected = items[position]
    }

    init {
        nameText.text = name;
    }

    var selected = items[0]

    override val fieldView: View get() = menu

    operator fun invoke() = selected
}