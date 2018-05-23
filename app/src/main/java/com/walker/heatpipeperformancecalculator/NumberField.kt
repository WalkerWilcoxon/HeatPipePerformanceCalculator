package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.MathContext

/**
 * Created by walker on 3/16/18.
 */
sealed class NumberField(val name: String, val numberUnits: String, val staticUnits: Boolean) {
    companion object {
        var updater: () -> Unit = {}
        var getContext: () -> Context = { throw ExceptionInInitializerError("Context is not initialized yet") }
    }

    val context: Context get() = getContext()

    open var number: Double = 0.0
        set(value) {
            field = value
            numberText.text = format(numberConverter.convert(value, textConverter))
        }

    var convertedNumber: Double
        get() = numberConverter.convert(number, textConverter)
        set(value) {
            number = textConverter.convert(value, numberConverter)
        }

    val numberConverter = UnitConverter(numberUnits)

    var textUnits = numberUnits
        set(value) {
            field = value
            nameText.text = name + " (" + value + ")"
            textConverter = UnitConverter(value)
        }

    var textConverter = UnitConverter(textUnits)

    abstract var numberText: TextView

    var nameText = TextView(context)

    init {
        nameText.textSize = 15f
        nameText.setTextColor(Color.BLACK)
        nameText.text = name + " (" + textUnits + ")"
    }

    fun toggleVisibility() {
        numberText.visibility = if (numberText.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        nameText.visibility = if (numberText.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    fun addToLayout(layout: GridLayout, row: Int) {
        var params = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(0))
        params.marginStart = 50
        nameText.layoutParams = params
        params = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(1))
        params.marginStart = 75
        numberText.layoutParams = params
        layout.addView(nameText)
        layout.addView(numberText)
    }

    fun changeUnit(oldUnit: String, newUnit: String) {
        if (!staticUnits) {
            textConverter.changeUnit(oldUnit, newUnit)
            numberText.text = format(convertedNumber)
        }
    }

    abstract fun format(number: Double): String
}

class InputField(name: String, numberUnits: String, initialValue: Double, staticUnits: Boolean) : NumberField(name, numberUnits, staticUnits) {
    override var number: Double
        get() = super.number
        set(value) {
            super.number = value
            updater()
        }

    override var numberText: TextView = EditText(context)

    init {
        super.number = initialValue
        numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        numberText.text = initialValue.toString()
        numberText.textSize = 15f
        numberText.setTextColor(Color.BLACK)
        numberText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(text: Editable?) {
                val c = convertedNumber
                val f = format(convertedNumber)
                if (text.toString() != format(convertedNumber)) {
                    try {
                        convertedNumber = text.toString().toDouble()
                    } catch (e: NumberFormatException) {
                        "Please Enter a number".toast(context)
                        return
                    }
                    if (name == "Heat Pipe Diameter") {
                        val diameter_in_mm = number * 1000
                        if (diameter_in_mm < 3)
                            number = 3.0
                        if (diameter_in_mm > 9)
                            number = 10.0
                        if (diameter_in_mm.between(6, 7))
                            number = 6.0
                        if (diameter_in_mm.between(7, 9))
                            number = 8.0
                    }
                }
            }
        })
    }

    override fun format(number: Double): String {
        return BigDecimal(number).round(MathContext(MainActivity.sigFigs)).toPlainString()
    }

    operator fun invoke() = number
}

class OuputField(name: String, numberUnits: String, staticUnits: Boolean, val forumla: () -> Double) : NumberField(name, numberUnits, staticUnits) {
    override var numberText = TextView(context)

    init {
        numberText.textSize = 15f
        numberText.setTextColor(Color.BLACK)
    }

    override fun format(number: Double): String {
        try {
            return BigDecimal(number).round(MathContext(MainActivity.sigFigs)).toEngineeringString()
        } catch (e: Exception) {
            Log.i("AppTag", "Number:$number Name:$name")
            return "0"
        }
    }

    operator fun invoke(): Double {
        number = forumla()
        return number
    }
}