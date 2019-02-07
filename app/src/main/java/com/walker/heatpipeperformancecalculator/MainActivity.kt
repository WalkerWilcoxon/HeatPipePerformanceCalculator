package com.walker.heatpipeperformancecalculator

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import android.widget.ArrayAdapter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.walker.heatpipeperformancecalculator.R.layout.main
import kotlinx.android.synthetic.main.main.*
import java.lang.Math.pow
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    lateinit var numbers: List<NumberView>
    lateinit var inputs: List<NamedView>
    lateinit var inputNumbers: List<InputNumberText>
    lateinit var outputs: List<OutputNumberText>
    lateinit var units: List<UnitMenu>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(main)

        Globals.density = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }.density



        initializeFields()

        inputSpinner.init(inputNumbers) { _, _ ->
            val field = inputSpinner.selectedItem as InputNumberText
            startRangeText.inputType = field.numberText.inputType
            endRangeText.inputType = field.numberText.inputType
            endRangeUnits.text = selectedInputField.toUnits
            startRangeUnits.text = selectedInputField.toUnits
            startNum = field.convertedMinValue
            endNum = field.convertedMaxValue
            updateGraphOutputs()
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

        units = unitsLayout.getAllChildren()

        NamedView.updateOutputs = {
            outputs.forEach {
                it.updateNumberText()
            }
            updateGraph()
        }
        
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

        D_man.formula = {
            when ((D_hp() * 1000).toInt() / 1000.0) {
                D_hp_1 -> D_man_1
                D_hp_2 -> D_man_2
                D_hp_3 -> D_man_3
                D_hp_4 -> D_man_4
                D_hp_5 -> D_man_5
                D_hp_6 -> D_man_6
                else -> throw NamedViewException(D_man, "Unknown D_hp value: ${D_hp()}")
            }
        }
        t_wall.formula = {  if (D_hp() <= 0.006) 0.0003 else 0.0005 }
        t_wick.formula = {  (D_hp() - 2 * t_wall() - D_man()) / 2 }
        R_circ.formula = {

            R_cont + (t_wall() + t_wick() / Poros) / k_copper
        }
        r_vap.formula = {
            (D_hp() - 2 * t_wall() - 2 * t_wick()) / 2.0
        }
        A_wick.formula = {
            PI * (pow(0.5 * D_hp() - t_wall(), 2.0) - pow(r_vap(), 2.0))
        }
        L_adia.formula = {
            L_tot() - L_evap() - L_cond()
        }
        L_eff.formula = {
            L_adia() + (L_evap() + L_cond()) / 2.0
        }

        perm.formula = {
            when (powder()) {
                "Blue" -> 0.000000000086
                "Red" -> 0.0000000000094658
                "Orange" -> 0.000000000014269
                "Green" -> 0.00000000000715
                "White" -> 0.000000000013747
                else -> throw NamedViewException(perm, "Unknown powder value: ${powder()}")
            }
        }
//        R_cont.formula = {
//            L_eff() / k_copper / (PI / 4 * (D_hp() * D_hp() - r_vap() * r_vap()))
//        }
        r_powder.formula = {
            when (powder()) {
                "Blue" -> 0.0000608
                "Red" -> 0.000023524
                "Orange" -> 0.000031681
                "Green" -> 0.0000289
                "White" -> 0.000032101
                else -> throw NamedViewException(r_powder, "Unknown powder value: ${powder()}")
            }
        }
        R_cond.formula = {
            (R_cont + ((t_wall() + t_wick() / Poros) / k_copper)) / (PI * D_hp() * L_cond() * 0.5)
        }
        R_evap.formula = {
            (R_cont + ((t_wall() + t_wick() / Poros) / k_copper)) / (PI * D_hp() * L_evap() * 0.33)
        }
        R_axial.formula = {
            PI * pow(r_vap(), 2.0) * 100
        }
        R_tot.formula = {
            R_cond() + R_evap() + R_axial()
        }
        P_vapor.formula = {
            pow(10.0, 8.07131 - 1730.63 / (233.426 + T())) * 133.322
        }
        dens_liq.formula = {
            0.14395 / pow(0.0112, 1 + pow(1 - T_k / 649.727, 0.05107))
        }
        vis_liquid.formula = {
            exp(-3.7188 + 578.99 / (T_k - 137.546)) / 1000
        }
        dens_vap.formula = {
            0.0022 / T_k * exp(77.345 + 0.0057 * T_k - 7235 / T_k) / pow(T_k, 8.2)
        }
        vis_vap.formula = {
            1.512 * pow(T_k, 1.5) / 2.0 / (T_k + 120) / 1000000.0
        }
        tens_surface.formula = {
            235.8 * pow(1 - T_k / 647.098, 1.256) * (1 - 0.625 * (1 - T_k / 647.098)) / 1000
        }
        Q_latent.formula = {
            (2500.8 - 2.36 * T() + 0.0016 * T() * T() - 0.00006 * pow(T(), 3.0)) * 1000
        }
        k_liq.formula = {
            -0.000007933 * T_k * T_k + 0.006222 * T_k - 0.5361
        }
        P_cap_max.formula = {
            2.0 * tens_surface() / r_powder()

        }
        P_grav_drop.formula = {
            dens_liq() * g * L_tot() * -sin(theta() * PI / 180)
        }
        Q_limit.formula = {
            (P_cap_max() - P_grav_drop()) / (L_eff() * (8 * vis_vap() / (dens_vap() * PI * pow(r_vap(), 4.0) * Q_latent()) + vis_liquid() / (dens_liq() * perm() * A_wick() * Q_latent())))
        }
        P_vap_drop.formula = {
            8.0 * vis_vap() * Q_limit() * L_eff() / (dens_vap() * PI * pow(r_vap(), 4.0) * Q_latent())
        }
        P_liq_drop.formula = {
            vis_liquid() * L_eff() * Q_limit() / (dens_liq() * perm() * A_wick() * Q_latent())
        }
        P_cap_rem.formula = {
            P_cap_max() - P_grav_drop() - P_vap_drop() - P_liq_drop()
        }
        n_hp.formula = {
            ceil(power() / Q_limit())
        }
    }

    val T_k get() = T() + 273.15

        
    val inputRange get() = Range(startNum, endNum)
    var startNum
        get() = startRangeText.text.toString().toDouble()
        set(value) {
            startRangeText.setText(value.toString())
        }

    var endNum
        get() = endRangeText.text.toString().toDouble()
        set(value) {
            endRangeText.setText(value.toRoundedString())
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
        val initialDiameter = D_hp()
        val outputField = selectedOutputField
        var xMax = Double.MIN_VALUE
        var xMin = Double.MAX_VALUE
        var yMax = Double.MIN_VALUE
        var yMin = Double.MAX_VALUE
        for ((diameter, color) in D_hp.numbers.zip(graphColors)) {
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
            series.title = "${D_hp.convertedNumber.toRoundedString()} ${D_hp.toUnits}"
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
            val showAllProps = showAllPropertiesCheckBox.isChecked
            outputSpinner.adapter = ArrayAdapter<NumberView>(this, R.layout.text_view_wrap, outputs.filter { (if (!showAllProps) it.isImportant else true) && it.isDependantOn(selectedInputField) })
            isUpdatingGraphs = true
        }
    }

    fun changeUnit(oldUnit: String, newUnit: String) {
        for (field in numbers) {
            field.changeUnit(oldUnit, newUnit)
        }
        endRangeUnits.text = selectedInputField.toUnits
        startRangeUnits.text = selectedInputField.toUnits
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