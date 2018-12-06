package com.walker.heatpipeperformancecalculator

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.walker.heatpipeperformancecalculator.Field.Companion.fields
import com.walker.heatpipeperformancecalculator.R.layout.main
import com.walker.heatpipeperformancecalculator.UnitConverter.Factors.baseUnits
import kotlinx.android.synthetic.main.main.*
import java.math.BigDecimal
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

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

    val temp by lateInit { InputNumberText("Temperature", "°C", 20.0, 500.0, 10.0) }
    val theta by lateInit { InputNumberText("Operating Angle", "°", 90.0, 90.0, -90.0) }
    val L_tot by lateInit { InputNumberText("Heat Pipe Length", "m", 0.15, 0.50, 0.01) }
    val L_evap by lateInit { InputNumberText("Evaporator Length", "m", 0.02, 0.10, 0.01) }
    val L_cond by lateInit { InputNumberText("Condenser Length", "m", 0.06, 0.10, 0.01) }
    val D_hp by lateInit {
        InputNumberMenu("Heat Pipe Diameter", "m", 0.006,
                arrayOf(
                D_hp_1,
                D_hp_2,
                D_hp_3,
                D_hp_4,
                D_hp_5,
                D_hp_6)
        )
    }
    val power by lateInit { InputNumberText("Input Power", "W", 10.0, 100.0, 1.0) }
    val powder by lateInit { InputEnumeration("Powder", "Blue", arrayOf("Blue", "Red", "Blue", "Orange", "Green", "White")) }
    val D_man by lateInit {
        OutputNumber("Mandrel Diameter", "m") {
            when (D_hp()) {
                D_hp_1 -> D_man_1
                D_hp_2 -> D_man_2
                D_hp_3 -> D_man_3
                D_hp_4 -> D_man_4
                D_hp_5 -> D_man_5
                D_hp_6 -> D_man_6
                else -> throw NullPointerException("Unknown D_hp value:${D_hp()}")
            }
        }

    }
    val t_wall by lateInit { OutputNumber("Wall Thickness", "m") { if (D_hp() <= 0.006) 0.0003 else 0.0005 } }
    val t_wick by lateInit { OutputNumber("Wick Thickness", "m") { (D_hp() - 2 * t_wall() - D_man()) / 2 } }
    val R_circ by lateInit {
        OutputNumber("Circumferential Resistance", "°C/W", Number.STATIC_UNITS) {
            R_cont + (t_wall() + t_wick() / Poros) / k_copper
        }
    }
    val r_vap by lateInit {
        OutputNumber("Radius of Vapor Space", "m") {
            (D_hp() - 2 * t_wall() - 2 * t_wick()) / 2.0
        }
    }
    val A_wick by lateInit {
        OutputNumber("Cross Sectional Area", "m^2 ") {
            Math.PI * (Math.pow(0.5 * D_hp() - t_wall(), 2.0) - Math.pow(r_vap(), 2.0))
        }
    }
    val L_adia by lateInit {
        OutputNumber("Adiabatic Length", "m") {
            L_tot() - L_evap() - L_cond()
        }
    }
    val L_eff by lateInit {
        OutputNumber("Effective Length", "m") {
            L_adia() + (L_evap() + L_cond()) / 2.0
        }
    }
    val perm by lateInit {
        OutputNumber("Permeability", "m") {
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
        OutputNumber("Conduction Resistance", "°C/W", Number.STATIC_UNITS) {
            L_eff() / k_copper / (Math.PI / 4 * (D_hp() * D_hp() - r_vap() * r_vap()))
        }
    }
    val r_powder by lateInit {
        OutputNumber("Powder Radius", "m", Number.STATIC_UNITS) {
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
        OutputNumber("Condenser Resistance", "°C/W") {
            (R_cont + ((t_wall() + t_wick() / Poros) / k_copper)) / (PI * D_hp() * L_cond() * 0.5)
        }
    }
    val R_evap by lateInit {
        OutputNumber("Evaporator Resistance", "°C/W") {
            (R_cont + ((t_wall() + t_wick() / Poros) / k_copper)) / (PI * D_hp() * L_evap() * 0.33)
        }
    }
    val R_axial by lateInit {
        OutputNumber("Axial Resistance", "°C/W") {
            PI * Math.pow(r_vap(), 2.0) * 100
        }
    }
    val R_total by lateInit {
        OutputNumber("Heatpipe Thermal Resistance", "°C/W", Number.IS_IMPORTANT) {
            R_condense() + R_evap() + R_axial()
        }
    }
    val P_vapor by lateInit {
        OutputNumber("Vapor Pressure", " kg/m^3 ") {
            Math.pow(10.0, 8.07131 - 1730.63 / (233.426 + temp())) * 133.322
        }
    }
    val dens_liquid by lateInit {
        OutputNumber("Liquid Density", "N*m/s") {
            0.14395 / Math.pow(0.0112, 1 + Math.pow(1 - T_k / 649.727, 0.05107))
        }
    }
    val vis_liquid by lateInit {
        OutputNumber("Liquid Viscosity", "kg/m^3") {
            Math.exp(-3.7188 + 578.99 / (T_k - 137.546)) / 1000
        }
    }
    val dens_vapor by lateInit {
        OutputNumber("Vapor Density", "N*m/s") {
            0.0022 / T_k * Math.exp(77.345 + 0.0057 * T_k - 7235 / T_k) / Math.pow(T_k, 8.2)
        }
    }
    val vis_vapor by lateInit {
        OutputNumber("Vapor Viscosity", "Pa")
        {
            1.512 * Math.pow(T_k, 1.5) / 2.0 / (T_k + 120) / 1000000.0
        }
    }
    val tens_surface by lateInit {
        OutputNumber("Surface Tension", "N/m")
        {
            235.8 * Math.pow(1 - T_k / 647.098, 1.256) * (1 - 0.625 * (1 - T_k / 647.098)) / 1000
        }
    }
    val Q_latent by lateInit {
        OutputNumber("Latent Heat", "J/kg", Number.STATIC_UNITS)
        {
            (2500.8 - 2.36 * temp() + 0.0016 * temp() * temp() - 0.00006 * Math.pow(temp(), 3.0)) * 1000
        }
    }
    val k_liquid by lateInit {
        OutputNumber("Liquid Conductivity", "W/m/°C", Number.STATIC_UNITS)
        {
            -0.000007933 * T_k * T_k + 0.006222 * T_k - 0.5361
        }
    }
    val P_max_cap by lateInit {
        OutputNumber("Max Capillary Pressure", "Pa")
        {
            2.0 * tens_surface() / r_powder()
        }
    }
    val P_gravity_drop by lateInit {
        OutputNumber("Pressure Drop of Gravity", "Pa")
        {
            dens_liquid() * g * L_tot() * -Math.sin(theta() * Math.PI / 180)
        }
    }
    val Q_limit by lateInit {
        OutputNumber("Heat Limit", "W", Number.IS_IMPORTANT)
        {
            (P_max_cap() - P_gravity_drop()) / (L_eff() * (8 * vis_vapor() / (dens_vapor() * Math.PI * Math.pow(r_vap(), 4.0) * Q_latent()) + vis_liquid() / (dens_liquid() * perm() * A_wick() * Q_latent())))
        }
    }
    val P_vapor_drop by lateInit {
        OutputNumber("Pressure Drop of Vapor", "Pa")
        {
            8.0 * vis_vapor() * Q_limit() * L_eff() / (dens_vapor() * Math.PI * Math.pow(r_vap(), 4.0) * Q_latent())
        }
    }
    val P_liquid_drop by lateInit {
        OutputNumber("Pressure Drop of Liquid", "Pa")
        {
            vis_liquid() * L_eff() * Q_limit() / (dens_liquid() * perm() * A_wick() * Q_latent())
        }
    }
    val P_cap_rem by lateInit {
        OutputNumber("Capillary Pressure Remaining", "Pa")
        {
            P_max_cap() - P_gravity_drop() - P_vapor_drop() - P_liquid_drop()
        }
    }
    val n_hp by lateInit {
        OutputNumber("Required Heat Pipes", "", Number.IS_IMPORTANT)
        {
            Math.ceil(power() / Q_limit())
        }
    }
    val T_k get() = UnitConverter.TemperatureConverter.convert(temp(), "°C", "K")

    val context = this

    companion object {
        lateinit var numbers: Array<Number>
        lateinit var inputs: Array<Field>
        lateinit var inputNumbers: Array<InputNumberText>
        lateinit var outputs: Array<OutputNumber>
        lateinit var units: Array<UnitMenu>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        graph.viewport.isScalable = true
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.isYAxisBoundsManual = true

        changeUnit("m", "mm")

        updateGraphOutputs()

        Field.updateOutputs()

        Q_latent.isDependantOn(L_evap).Log(Tags.Default)
    }

    private fun initializeFields() {
        Field.getContext = { context }

        lateInit.inialize()

        numbers = fields.filter { it is Number }.map { it as Number }.toTypedArray()
        inputs = fields.filter { it is Input }.toTypedArray()
        inputNumbers = fields.filter { it is InputNumberText }.map { it as InputNumberText }.toTypedArray()
        outputs = fields.filter { it is OutputNumber }.map { it as OutputNumber }.toTypedArray()

        inputs.sortWith(compareBy { it.nameText.text.toString() })
        outputs.sortWith(compareBy(OutputNumber::isImportant, { it.nameText.text.toString() }))

        inputs.forEach {
            it.addToGrid(inputGrid)
        }
        outputs.forEach {
            it.addToGrid(outputGrid)
        }
        UnitConverter.Factors.units.forEach {
            UnitMenu(it.key.name, baseUnits[it.key]!!, it.value.toTypedArray()).addToGrid(unitsGrid)
        }
        units = fields.filter { it is UnitMenu }.map { it as UnitMenu }.toTypedArray()

        Field.updateOutputs = {
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
            (startRangeText as TextView).text = value.toString()
        }
    var endNum
        get() = endRangeText.text.toString().toDouble()
        set(value) {
            (endRangeText as TextView).text = BigDecimal(value).stripTrailingZeros().toPlainString()
        }

    val selectedInputField get() = inputSpinner.selectedItem as InputNumberText
    val selectedOutputField get() = outputSpinner.selectedItem as OutputNumber

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
            series.title = "${D_hp.convertedNumber.toRoundedString()} ${D_hp.convertedUnits}"
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

    var isUpdatingGraphs = true

    fun updateGraphOutputs() {
        if (isUpdatingGraphs) {
            isUpdatingGraphs = false
            val showAll = showAllPropertiesCheckBox.isChecked
            outputSpinner.adapter = ArrayAdapter<Number>(this, R.layout.text_view_wrap, outputs.filter { (if (!showAll) it.isImportant else true) && it.isDependantOn(selectedInputField) })
            isUpdatingGraphs = true
        }
    }

    private fun changeUnit(oldUnit: String, newUnit: String) {
        for (field in numbers) {
            field.changeUnit(oldUnit, newUnit)
        }
        units.find { it.name.baseUnitName() == oldUnit }!!.menu.setSelection(newUnit)
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
        unitsLayout.toggleVisibility()
    }

    fun toggleGraph(view: View?) {
        graphLayout.toggleVisibility()
    }
}