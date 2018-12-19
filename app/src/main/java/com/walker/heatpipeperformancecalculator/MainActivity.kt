package com.walker.heatpipeperformancecalculator

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import android.widget.ArrayAdapter
import com.walker.heatpipeperformancecalculator.R.layout.main
import kotlinx.android.synthetic.main.main.*
import org.mariuszgromada.math.mxparser.Tutorial

class MainActivity : AppCompatActivity() {
    val axial_R = 0.000001
    val k_copper = 380.0
    val g = 9.81
    val Poros = 0.52

    val R_cont = 0.000007
    val D_man_1 = 0.0016
    val D_man_2 = 0.0026
    val D_man_3 = 0.0036
    val D_man_4 = 0.0044
    val D_man_5 = 0.006
    val D_man_6 = 0.008

    val D_hp_1 = 0.003
    val D_hp_2 = 0.004
    val D_hp_3 = 0.005
    val D_hp_4 = 0.006
    val D_hp_5 = 0.008
    val D_hp_6 = 0.01

    lateinit var D_hp: InputNumberText //= TODO()

    //val T_k get() = UnitConverter.TemperatureConverter.convert(temp(), "°C", "K")

    val context = this

    companion object {
        lateinit var numbers: List<NumberView>
        lateinit var inputs: List<NamedView>
        lateinit var inputNumbers: List<InputNumberText>
        lateinit var outputs: List<OutputNumberText>
        lateinit var units: List<NamedView>//UnitMenu>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Tutorial.main(arrayOf())

        setContentView(main)

        Globals.density = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }.density

        initializeFields()

        inputSpinner.init(inputNumbers) { _, _ ->
            val field = inputSpinner.selectedItem as InputNumberText
            startRangeText.inputType = field.numberText.inputType
            endRangeText.inputType = field.numberText.inputType
            endRangeUnits.text = selectedInputField.convertedUnits
            startRangeUnits.text = selectedInputField.convertedUnits
            startNum = field.convertedMinValue
            endNum = field.convertedMaxValue
            updateGraphOutputs();
            updateGraph()
        }

        inputSpinner.setSelection(0)

        outputSpinner.init(outputs) { _, _ -> updateGraph() }

        outputSpinner.setSelection(0)

        toggleUnimportantFields(null)

        startRangeText.addTextChangedListener(createTextWatcher {
            if (startRangeText.text.toString().toDoubleOrNull() != null) {
                if (startNum < selectedInputField.convertedMinValue) {
                    startNum = selectedInputField.convertedMinValue
                }
                updateGraph()
            }
        })

        endRangeText.addTextChangedListener(createTextWatcher {
            if (endRangeText.text.toString().toDoubleOrNull() != null) {
                if (endNum > selectedInputField.convertedMaxValue) {
                    endNum = selectedInputField.convertedMaxValue
                }
                updateGraph()
            }
        })

        with(graph.viewport) {
            isScalable = true
            isXAxisBoundsManual = true
            isYAxisBoundsManual = true
        }
        changeUnit("m", "mm")

        updateGraphOutputs()

        NamedView.updateOutputs()
    }

    private fun initializeFields() {

        inputs = inputLayout.getAllChildren()
        numbers = inputs.filter { it is NumberView }.map { it as NumberView }
        inputNumbers = inputs.filter { it is InputNumberText }.map { it as InputNumberText }
        outputs = outputLayout.getAllChildren()

        inputs.sortedBy { it.nameText.text.toString() }
        outputs.sortedWith(compareBy(OutputNumberText::isImportant, { it.nameText.text.toString() }))

        units = unitsLayout.getAllChildren()

        NamedView.updateOutputs = {
            outputs.forEach {
                it.updateNumberText()
            }
            updateGraph()
        }
    }

    val inputRange get() = Range(startNum, endNum)
    var startNum
        get() = startRangeText.text.toString().toDouble()
        set(value) {
            startRangeText.setText(value.toString())
        }

    var endNum
        get() = endRangeText.text.toString().toDouble()
        set(value) {
            endRangeText.setText( value.toRoundedString())
        }

    val selectedInputField get() = inputSpinner.selectedItem as InputNumberText
    val selectedOutputField get() = outputSpinner.selectedItem as OutputNumberText

    val graphColors = arrayOf(
            Color.parseColor("#cc0000"),
            Color.parseColor("#339933"),
            Color.parseColor("#ff9900"),
            Color.parseColor("#0066ff"),
            Color.parseColor("#ffff00"),
            Color.parseColor("#993399")
    )

    fun updateGraph() {
        if (startNum >= endNum) {
            "Start number must be less than end number".toast(this)
            return
        }
        graph.removeAllSeries()
        val inputField = selectedInputField
        val initialDiameter = D_hp.number
        val outputField = selectedOutputField
        var xMax = Double.MIN_VALUE
        var xMin = Double.MAX_VALUE
        var yMax = Double.MIN_VALUE
        var yMin = Double.MAX_VALUE
        TODO("Uncomment code")
//        for ((diameter, color) in D_hp.items.zip(graphColors)) {
//            val initialInputNum = inputField.number
//            D_hp.number = diameter
//            val dataPoints = ArrayList<DataPoint>()
//            val graphRange = Range(0.0, 100.0)
//            for (i in 0..100) {
//                val inputNum = i.mapTo(graphRange, inputRange)
//                inputField.number = inputNum
//                val outputNum = outputField.convertedNumber
//                if (!(inputNum.isFinite() && outputNum.isFinite()))
//                    continue
//                dataPoints.add(DataPoint(inputNum, outputNum))
//                xMax = max(xMax, inputNum)
//                xMin = min(xMin, inputNum)
//                yMax = max(yMax, outputNum)
//                yMin = min(yMin, outputNum)
//            }
//            val series = LineGraphSeries<DataPoint>(dataPoints.toTypedArray())
//            series.title = "${D_hp.convertedNumber.toRoundedString()} ${D_hp.toUnits}"
//            series.color = color
//            graph.addSeries(series)
//
//            inputField.number = initialInputNum
//        }

        D_hp.number = initialDiameter

        with(graph) {
            viewport.setMaxX(xMax)
            viewport.setMinX(xMin)
            viewport.setMaxY(yMax)
            viewport.setMinY(yMin)
            legendRenderer.isVisible = true
//            legendRenderer.align = LegendRenderer.LegendAlign.
            title = "${inputField.name} vs ${outputField.name}"
        }
    }

    var isUpdatingGraphs = true

    fun updateGraphOutputs() {
        if (isUpdatingGraphs) {
            isUpdatingGraphs = false
            val showAllProps = showAllPropertiesCheckBox.isChecked
            outputSpinner.adapter = ArrayAdapter<NumberView>(this, R.layout.text_view_wrap, outputs.filter { (if (!showAllProps) it.isImportant else true) && it.isDependantOn(selectedInputField) })
            isUpdatingGraphs = true
        }
    }

    private fun changeUnit(oldUnit: String, newUnit: String) {
        for (field in numbers) {
            field.changeUnit(oldUnit, newUnit)
        }
        TODO("Uncomment code")
        //unitStrings.find { it.name.baseUnitName() == oldUnit }!!.menu.setSelection(newUnit)
        endRangeUnits.text = selectedInputField.convertedUnits
        startRangeUnits.text = selectedInputField.convertedUnits
    }

    fun toggleUnimportantFields(view: View?) {
        outputs.forEach {
            if (!it.isImportant) it.toggleVisibility()
        }
        updateGraphOutputs()
    }

    fun toggleUnits(view: View?) {
        unitsVisibilityLayout.toggleVisibility()
    }

    fun toggleGraph(view: View?) {
        graphLayout.toggleVisibility()
    }
}