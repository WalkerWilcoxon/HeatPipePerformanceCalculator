package com.walker.heatpipeperformancecalculator

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.walker.heatpipeperformancecalculator.Field.Companion.fields
import com.walker.heatpipeperformancecalculator.R.layout.main
import com.walker.heatpipeperformancecalculator.UnitConverter.Factors.baseUnits
import com.walker.heatpipeperformancecalculator.UnitConverter.Factors.units
import kotlinx.android.synthetic.main.main.*
import java.math.BigDecimal
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    var tempUnit = "C"
    var lengthUnit = "m"
    var forceUnit = "N"
    var massUnit = "kg"
    var pressureUnit = "Pa"

    val axial_R = 0.000001
    val k_copper = 380.0
    val g = 9.81
    val Poros = 0.52
    val R_cont = 0.000007

    val temp by lateInit { InputNumberText("Temperature", "°C", 20.0, 500.0, 10.0) }
    val theta by lateInit { InputNumberText("Operating Angle", "°", 90.0, 90.0, -90.0) }
    val L_tot by lateInit { InputNumberText("Heat Pipe Length", "m", 0.15, 0.50, 0.01) }
    val L_evap by lateInit { InputNumberText("Evaporator Length", "m", 0.02, 0.10, 0.01) }
    val L_cond by lateInit { InputNumberText("Condenser Length", "m", 0.06, 0.10, 0.01) }
    val D_hp by lateInit {
        InputNumberMenuField("Heat Pipe Diameter", "m", 0.006,
                0.003,
                0.004,
                0.005,
                0.006,
                0.008,
                0.01)
    }
    val power by lateInit { InputNumberText("Input Power", "W", 10.0, 100.0, 1.0) }
    val powder by lateInit { InputWordMenu("Powder", "Blue", "Red", "Blue", "Orange", "Green", "White") }
    val D_man by lateInit {
        OutputNumberText("Mandrel Diameter", "m") {
            when (D_hp()) {
                0.003 -> 0.0016
                0.004 -> 0.0026
                0.005 -> 0.0036
                0.006 -> 0.0044
                0.008 -> 0.006
                0.01 -> 0.008
                else -> throw NullPointerException("Mandrel Diameter couldn't be determined")
            }
        }

    }
    val t_wall by lateInit { OutputNumberText("Wall Thickness", "m") { if (D_hp() <= 0.006) 0.0003 else 0.0005 } }
    val t_wick by lateInit { OutputNumberText("Wick Thickness", "m") { (D_hp() - 2 * t_wall() - D_man()) / 2 } }
    val R_circ by lateInit {
        OutputNumberText("Circumferential Resistance", "°C/W", Number.STATIC_UNITS) {
            R_cont + (t_wall() + t_wick() / Poros) / k_copper
        }
    }
    val r_vap by lateInit {
        OutputNumberText("Radius of Vapor Space", "m") {
            (D_hp() - 2 * t_wall() - 2 * t_wick()) / 2.0
        }
    }
    val A_wick by lateInit {
        OutputNumberText("Cross Sectional Area", "m^2 ") {
            Math.PI * (Math.pow(0.5 * D_hp() - t_wall(), 2.0) - Math.pow(r_vap(), 2.0))
        }
    }
    val L_adia by lateInit {
        OutputNumberText("Adiabatic Length", "m") {
            L_tot() - L_evap() - L_cond()
        }
    }
    val L_eff by lateInit {
        OutputNumberText("Effective Length", "m") {
            L_adia() + (L_evap() + L_cond()) / 2.0
        }
    }
    val perm by lateInit {
        OutputNumberText("Permeability", "m") {
            when (powder()) {
                "Blue" -> 0.000000000086
                "Red" -> 0.0000000000094658
                "Orange" -> 0.000000000014269
                "Green" -> 0.00000000000715
                "White" -> 0.000000000013747
                else -> throw Exception("Permeability not found")
            }
        }
    }
    val R_conduct by lateInit {
        OutputNumberText("Conduction Resistance", "°C/W", Number.STATIC_UNITS) {
            L_eff() / k_copper / (Math.PI / 4 * (D_hp() * D_hp() - r_vap() * r_vap()))
        }
    }
    val r_powder by lateInit {
        OutputNumberText("Powder Radius", "m", Number.STATIC_UNITS) {
            when (powder()) {
                "Blue" -> 0.0000608
                "Red" -> 0.000023524
                "Orange" -> 0.000031681
                "Green" -> 0.0000289
                "White" -> 0.000032101
                else -> throw Exception("Powder Radius not found")
            }
        }
    }
    val R_condense by lateInit {
        OutputNumberText("Condenser Resistance", "°C/W") {
            (R_cont + ((t_wall() + t_wick() / Poros) / k_copper)) / (PI * D_hp() * L_cond() * 0.5)
        }
    }
    val R_evap by lateInit {
        OutputNumberText("Evaporator Resistance", "°C/W") {
            (R_cont + ((t_wall() + t_wick() / Poros) / k_copper)) / (PI * D_hp() * L_evap() * 0.33)
        }
    }
    val R_axial by lateInit {
        OutputNumberText("Axial Resistance", "°C/W") {
            PI * Math.pow(r_vap(), 2.0) * 100
        }
    }
    val R_total by lateInit {
        OutputNumberText("Heatpipe Thermal Resistance", "°C/W", Number.IS_IMPORTANT) {
            R_condense() + R_evap() + R_axial()
        }
    }
    val P_vapor by lateInit {
        OutputNumberText("Vapor Pressure", " kg/m^3 ") {
            Math.pow(10.0, 8.07131 - 1730.63 / (233.426 + temp())) * 133.322
        }
    }
    val dens_liquid by lateInit {
        OutputNumberText("Liquid Density", "N*m/s") {
            0.14395 / Math.pow(0.0112, 1 + Math.pow(1 - T_k / 649.727, 0.05107))
        }
    }
    val vis_liquid by lateInit {
        OutputNumberText("Liquid Viscosity", "kg/m^3") {
            Math.exp(-3.7188 + 578.99 / (T_k - 137.546)) / 1000
        }
    }
    val dens_vapor by lateInit {
        OutputNumberText("Vapor Density", "N*m/s") {
            0.0022 / T_k * Math.exp(77.345 + 0.0057 * T_k - 7235 / T_k) / Math.pow(T_k, 8.2)
        }
    }
    val vis_vapor by lateInit {
        OutputNumberText("Vapor Viscosity", "Pa")
        {
            1.512 * Math.pow(T_k, 1.5) / 2.0 / (T_k + 120) / 1000000.0
        }
    }
    val tens_surface by lateInit {
        OutputNumberText("Surface Tension", "N/m")
        {
            235.8 * Math.pow(1 - T_k / 647.098, 1.256) * (1 - 0.625 * (1 - T_k / 647.098)) / 1000
        }
    }
    val Q_latent by lateInit {
        OutputNumberText("Latent Heat", "J/kg", Number.STATIC_UNITS)
        {
            (2500.8 - 2.36 * temp() + 0.0016 * temp() * temp() - 0.00006 * Math.pow(temp(), 3.0)) * 1000
        }
    }
    val k_liquid by lateInit {
        OutputNumberText("Liquid Conductivity", "W/m/°C", Number.STATIC_UNITS)
        {
            -0.000007933 * T_k * T_k + 0.006222 * T_k - 0.5361
        }
    }
    val P_max_cap by lateInit {
        OutputNumberText("Max Capillary Pressure", "Pa")
        {
            2.0 * tens_surface() / r_powder()
        }
    }
    val P_gravity_drop by lateInit {
        OutputNumberText("Pressure Drop of Gravity", "Pa")
        {
            dens_liquid() * g * L_tot() * -Math.sin(theta() * Math.PI / 180)
        }
    }
    val Q_limit by lateInit {
        OutputNumberText("Heat Limit", "W", Number.IS_IMPORTANT)
        {
            (P_max_cap() - P_gravity_drop()) / (L_eff() * (8 * vis_vapor() / (dens_vapor() * Math.PI * Math.pow(r_vap(), 4.0) * Q_latent()) + vis_liquid() / (dens_liquid() * perm() * A_wick() * Q_latent())))
        }
    }
    val P_vapor_drop by lateInit {
        OutputNumberText("Pressure Drop of Vapor", "Pa")
        {
            8.0 * vis_vapor() * Q_limit() * L_eff() / (dens_vapor() * Math.PI * Math.pow(r_vap(), 4.0) * Q_latent())
        }
    }
    val P_liquid_drop by lateInit {
        OutputNumberText("Pressure Drop of Liquid", "Pa")
        {
            vis_liquid() * L_eff() * Q_limit() / (dens_liquid() * perm() * A_wick() * Q_latent())
        }
    }
    val P_cap_rem by lateInit {
        OutputNumberText("Capillary Pressure Remaining", "Pa")
        {
            P_max_cap() - P_gravity_drop() - P_vapor_drop() - P_liquid_drop()
        }
    }
    val n_hp by lateInit {
        OutputNumberText("Required Heat Pipes", "", Number.IS_IMPORTANT)
        {
            Math.ceil(power() / Q_limit())
        }
    }
    val T_k get() = UnitConverter.TemperatureConverter.convert(temp(), "°C", "K")

    val context = this

    companion object {
        lateinit var numberFields: Array<Number>
        lateinit var inputFields: Array<Field>
        lateinit var inputNumbers: Array<InputNumberText>
        lateinit var outputNumberFields: Array<OutputNumberText>
        lateinit var importantOutputFields: Array<OutputNumberText>
        lateinit var unimportantOutputFields: Array<OutputNumberText>
        lateinit var unitFields: Array<UnitMenuField>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(main)

        initializeFields()

        inputSpinner.init(inputNumbers, Globals.wrapSpinnerText) { _, _ ->
            val field = inputSpinner.selectedItem as InputNumberText
            startRangeText.inputType = field.numberText.inputType
            endRangeText.inputType = field.numberText.inputType
            endRangeUnits.text = selectedInputField.textUnits
            startRangeUnits.text = selectedInputField.textUnits
            startNum = field.convertedMinValue
            endNum = field.convertedMaxValue
            updateGraphOutputs();
            updateGraph()
        }

        inputSpinner.setSelection(0)

        outputSpinner.init(importantOutputFields, Globals.wrapSpinnerText) { _, _ -> updateGraph() }

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

        graph.viewport.isScalable = true
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.isYAxisBoundsManual = true

        Thread.sleep(500)

        changeUnit("m", "mm")

        updateGraphOutputs()

        Field.updateOutputs()

        Q_latent.isDependantOn(L_evap).Log(Tags.Default)
    }

    private fun initializeFields() {
        Field.getContext = { context }

        lateInit.inialize()

        numberFields = fields.filter { it is Number }.map { it as Number }.toTypedArray()
        inputFields = fields.filter { it is Input }.toTypedArray()
        inputNumbers = fields.filter { it is InputNumberText }.map { it as InputNumberText }.toTypedArray()
        outputNumberFields = fields.filter { it is OutputNumberText }.map { it as OutputNumberText }.toTypedArray()
        val (important, unimportant) = outputNumberFields.partition { it.isImportant }
        importantOutputFields = important.toTypedArray()
        unimportantOutputFields = unimportant.toTypedArray()

        inputFields.sortWith(compareBy { it.nameText.text.toString() })
        importantOutputFields.sortedWith(compareBy { it.nameText.text.toString() })
        unimportantOutputFields.sortedWith(compareBy { it.nameText.text.toString() })

        inputFields.forEach {
            it.addToGrid(inputGrid)
        }
        importantOutputFields.forEach {
            it.addToGrid(outputGrid)
        }
        unimportantOutputFields.forEach {
            it.addToGrid(outputGrid)
        }
        units.forEach {
            UnitMenuField(it.key.name, baseUnits[it.key]!!, it.value.toTypedArray()).addToGrid(unitsGrid)
        }
        unitFields = fields.filter { it is UnitMenuField }.map { it as UnitMenuField }.toTypedArray()

        Field.updateOutputs = {
            outputNumberFields.forEach {
                it.updateNumberText()
            }
            updateGraph()
        }
    }

    val inputRange get() = Range(startNum, endNum)
    var startNum
        get() = startRangeText.text.toString().toDouble()
        set(value) {
            (startRangeText as TextView).text = value.toString()
        }
    var endNum
        get() = endRangeText.text.toString().toDouble()
        set(value) {
            (endRangeText as TextView).text = BigDecimal(value).stripTrailingZeros().toPlainString()
        }

    val selectedInputField get() = inputSpinner.selectedItem as InputNumberText
    val selectedOutputField get() = outputSpinner.selectedItem as OutputNumberText

    val graphColors = arrayOf(
            Color.parseColor("#cc0000"),
            Color.parseColor("#339933"),
            Color.parseColor("#ff9900"),
            Color.parseColor("#0066ff"),
            Color.parseColor("#ffff00"),
            Color.parseColor("#993399"))

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
        for ((diameter, color) in D_hp.items.zip(graphColors)) {
            val initialInputNum = inputField.number
            D_hp.number = diameter
            val dataPoints = ArrayList<DataPoint>()
            val graphRange = Range(0.0, 100.0)
            for (i in 0..100) {
                val inputNum = i.mapTo(graphRange, inputRange)
                inputField.number = inputNum
                val outputNum = outputField.convertedNumber
                if (!(inputNum.isFinite() && outputNum.isFinite()))
                    continue
                dataPoints.add(DataPoint(inputNum, outputNum))
                xMax = max(xMax, inputNum)
                xMin = min(xMin, inputNum)
                yMax = max(yMax, outputNum)
                yMin = min(yMin, outputNum)
            }
            val series = LineGraphSeries<DataPoint>(dataPoints.toTypedArray())
            series.title = "${D_hp.convertedNumberString} ${D_hp.textUnits}"
            series.color = color
            graph.addSeries(series)

            inputField.number = initialInputNum
        }

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

    var shouldUpdateGraphInputs = true

    fun updateGraphOutputs() {
        if (shouldUpdateGraphInputs) {
            shouldUpdateGraphInputs = false
            if (showAllPropertiesCheckBox.isChecked) {
                outputSpinner.adapter = ArrayAdapter<Number>(this, Globals.wrapSpinnerText, (importantOutputFields + unimportantOutputFields).filter { it.isDependantOn(selectedInputField) })
            } else {
                outputSpinner.adapter = ArrayAdapter<Number>(this, Globals.wrapSpinnerText, importantOutputFields.filter { it.isDependantOn(selectedInputField) })
            }
            shouldUpdateGraphInputs = true
        }
    }

    private fun changeUnit(oldUnit: String, newUnit: String) {
        for (field in numberFields) {
            field.changeUnit(oldUnit, newUnit)
        }
        unitFields.find { it.name.baseUnitName() == oldUnit }!!.setSelection(newUnit)
        endRangeUnits.text = selectedInputField.textUnits
        startRangeUnits.text = selectedInputField.textUnits
    }

    fun toggleUnimportantFields(view: View?) {
        unimportantOutputFields.forEach {
            it.toggleVisibility()
        }
        updateGraphOutputs()
    }

    fun toggleUnits(view: View?) {
        unitsLayouts.toggleVisibility()
    }

    fun toggleGraph(view: View?) {
        graphLayout.toggleVisibility()
    }
}