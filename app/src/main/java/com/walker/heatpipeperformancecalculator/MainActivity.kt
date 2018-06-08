package com.walker.heatpipeperformancecalculator

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import com.walker.heatpipeperformancecalculator.R.layout.main
import kotlinx.android.synthetic.main.main.*
import java.util.*

class MainActivity : Activity() {

    companion object {
        var sigFigs = 3
    }

    var tempUnit = "C"
    var lengthUnit = "m"
    var forceUnit = "N"
    var massUnit = "kg"
    var pressureUnit = "Pa"

    var metric = true

    lateinit var T: InputField
    lateinit var theta: InputField
    lateinit var L_tot: InputField
    lateinit var L_evap: InputField
    lateinit var L_cond: InputField
    lateinit var D: InputField
    lateinit var P: InputField
    lateinit var axial_R: OuputField
    lateinit var k_copper: OuputField
    lateinit var g: OuputField
    lateinit var Poros: OuputField
    lateinit var R_cont: OuputField
    lateinit var t_wall: OuputField
    lateinit var t_wick: OuputField
    lateinit var D_man: OuputField
    lateinit var circ_R: OuputField
    lateinit var r_vap: OuputField
    lateinit var A_wick: OuputField
    lateinit var L_a: OuputField
    lateinit var Leff: OuputField
    lateinit var R_c: OuputField
    lateinit var Perm: OuputField
    lateinit var R_cond: OuputField
    lateinit var P_vap: OuputField
    lateinit var Dens_liq: OuputField
    lateinit var Vis_liq: OuputField
    lateinit var Dens_vap: OuputField
    lateinit var Vis_vap: OuputField
    lateinit var Surf_ten: OuputField
    lateinit var H_lv: OuputField
    lateinit var k_liq: OuputField
    lateinit var Pc_max: OuputField
    lateinit var D_pg: OuputField
    lateinit var D_pv: OuputField
    lateinit var D_pl: OuputField
    lateinit var P_c_rem: OuputField
    lateinit var Q_limit: OuputField
    lateinit var n_hp: OuputField

    val T_k get() = UnitConverter.TemperatureConverter.convert(T(), "C", "K")

    var Powder = "Blue"

    var context = this
    var PermVals: MutableMap<String, Double> = HashMap()
    var R_cVals: MutableMap<String, Double> = HashMap()

    var D_manVals: MutableMap<Double, Double> = HashMap()
    var fields: MutableList<NumberField> = ArrayList()
    var inputs: MutableList<NumberField> = ArrayList()
    var constants: MutableList<NumberField> = ArrayList()
    var properties: MutableList<NumberField> = ArrayList()

    var outputs: MutableList<NumberField> = ArrayList()
    lateinit var inputText: TextView
    lateinit var outputText: TextView
    lateinit var propertiesText: TextView

    lateinit var constantsText: TextView

    lateinit var layout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(main)
        layout = gridLayout

        NumberField.updater = { n_hp() }

        mapVals()

        NumberField.getContext = { context }

        T = InputField("Temperature", "C", 20.0, false)
        theta = InputField("Operating Angle", "°", 90.0, false)
        L_tot = InputField("Heat Pipe Length", "m", 0.15, false)
        L_evap = InputField("Evaporator Length", "m", 0.02, false)
        L_cond = InputField("Condenser Length", "m", 0.06, false)
        D = InputField("Heat Pipe Diameter", "m", 0.006, false)
        P = InputField("Input Power", "W", 10.0, false)
        axial_R = OuputField("Axial Resistance", "C/W", true) { 0.000001 }
        k_copper = OuputField("Copper Conductivity", "W/m/C", true) { 380.0 }
        g = OuputField("Gravitational Acceleration", " m/s^2 ", false) { 9.81 }
        Poros = OuputField("Porosity", " % ", true) { 0.52 }
        R_cont = OuputField("Contact Resistance", "C*m^2/W", true) { 0.000007 }
        t_wall = OuputField("Wall Thickness", "m", false) { if (D() <= 0.006) 0.0003 else 0.0005 }
        t_wick = OuputField("Wick Thickness", "m", false) { (D() - 2 * t_wall() - D_man()) / 2 }
        D_man = OuputField("Mandrel Diameter", "m", false) {
            if (D_manVals[D()] != null) {
                D_manVals[D()]!!
            } else {
                Log.i("AppTag", "D_manVals:$D_manVals D:${D()}")
                0.0
            }
        }
        circ_R = OuputField("Circumferential Resistance", "C/W", true) { R_cont() + (t_wall() + t_wick() / Poros()) / k_copper() }
        r_vap = OuputField("Radius of Vapor Space", "m", false) { (D() - 2 * t_wall() - 2 * t_wick()) / 2.0 }
        A_wick = OuputField("Cross Sectional Area", "m^2 ", false) { Math.PI * (Math.pow(0.5 * D() - t_wall(), 2.0) - Math.pow(r_vap(), 2.0)) }
        L_a = OuputField("Adiabatic Length", "m", false) { L_tot() - L_evap() - L_cond() }
        Leff = OuputField("Effective Length", "m", false) { L_a() + (L_evap() + L_cond()) / 2.0 }
        R_c = OuputField("Contact Resistance", "C/W", true) { R_cVals[Powder]!! }
        Perm = OuputField("Permeability", "m", false) { PermVals[Powder]!! }
        R_cond = OuputField("Conduction Resistance", "C/W", true) { Leff() / k_copper() / (Math.PI / 4 * (D() * D() - r_vap() * r_vap())) }
        P_vap = OuputField("Vapor Pressure", " kg/m^3 ", false) { Math.pow(10.0, 8.07131 - 1730.63 / (233.426 + T())) * 133.322 }
        Dens_liq = OuputField("Liquid Density", "N*m/s", false) { 0.14395 / Math.pow(0.0112, 1 + Math.pow(1 - T_k / 649.727, 0.05107)) }
        Vis_liq = OuputField("Liquid Viscosity", "kg/m^3", false) { Math.exp(-3.7188 + 578.99 / (T_k - 137.546)) / 1000 }
        Dens_vap = OuputField("Vapor Density", "N*m/s", false) { 0.0022 / T_k * Math.exp(77.345 + 0.0057 * T_k - 7235 / T_k) / Math.pow(T_k, 8.2) }
        Vis_vap = OuputField("Vapor Viscosity", "Pa", false) { 1.512 * Math.pow(T_k, 1.5) / 2.0 / (T_k + 120) / 1000000.0 }
        Surf_ten = OuputField("Surface Tension", "N/m", false) { 235.8 * Math.pow(1 - T_k / 647.098, 1.256) * (1 - 0.625 * (1 - T_k / 647.098)) / 1000 }
        H_lv = OuputField("Latent Heat", "J/kg", true) { (2500.8 - 2.36 * T() + 0.0016 * T() * T() - 0.00006 * Math.pow(T(), 3.0)) * 1000 }
        k_liq = OuputField("Liquid Conductivity", "W/m/C", true) { -0.000007933 * T_k * T_k + 0.006222 * T_k - 0.5361 }
        Pc_max = OuputField("Max Capillary Pressure", "Pa", false) { 2.0 * Surf_ten() / R_c() }
        D_pg = OuputField("Pressure Drop of Gravity", "Pa", false) { Dens_liq() * g() * L_tot() * -Math.sin(theta() * Math.PI / 180) }
        D_pv = OuputField("Pressure Drop of Vapor", "Pa", false) { 8.0 * Vis_vap() * Q_limit() * Leff() / (Dens_vap() * Math.PI * Math.pow(r_vap(), 4.0) * H_lv()) }
        D_pl = OuputField("Pressure Drop of Liquid", "Pa", false) { Vis_liq() * Leff() * Q_limit() / (Dens_liq() * Perm() * A_wick() * H_lv()) }
        P_c_rem = OuputField("Capillary Pressure Remaining", "Pa", false) { Pc_max() - D_pg() - D_pv() - D_pl() }
        Q_limit = OuputField("Heat Limit", "W", false) { (Pc_max() - D_pg()) / (Leff() * (8 * Vis_vap() / (Dens_vap() * Math.PI * Math.pow(r_vap(), 4.0) * H_lv()) + Vis_liq() / (Dens_liq() * Perm() * A_wick() * H_lv()))) }
        n_hp = OuputField("Required Heat Pipes", "#", true) { Math.ceil(P() / Q_limit()) }

        n_hp()

        val grid = GridLayout(this)
        grid.columnCount = 2

        theta.numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED

        inputText = TextView(this)
        outputText = TextView(this)
        propertiesText = TextView(this)
        constantsText = TextView(this)

        inputs.add(T)
        inputs.add(theta)
        inputs.add(L_tot)
        inputs.add(L_evap)
        inputs.add(L_cond)
        inputs.add(D)
        inputs.add(P)

        constants.add(axial_R)
        constants.add(k_copper)
        constants.add(g)
        constants.add(Poros)
        constants.add(R_cont)

        properties.add(t_wall)
        properties.add(t_wick)
        properties.add(D_man)
        properties.add(circ_R)
        properties.add(r_vap)
        properties.add(A_wick)
        properties.add(L_a)
        properties.add(Leff)
        properties.add(R_c)
        properties.add(Perm)
        properties.add(R_cond)
        properties.add(P_vap)
        properties.add(Dens_liq)
        properties.add(Vis_liq)
        properties.add(Dens_vap)
        properties.add(Vis_vap)
        properties.add(Surf_ten)
        properties.add(H_lv)
        properties.add(k_liq)
        properties.add(Pc_max)
        properties.add(D_pg)
        properties.add(D_pv)
        properties.add(D_pl)
        properties.add(P_c_rem)

        outputs.add(Q_limit)
        outputs.add(n_hp)

        fields.addAll(inputs)
        fields.addAll(properties)
        fields.addAll(outputs)
        fields.addAll(constants)

        var index = 0

        inputText.text = "Inputs"
        inputText.setPadding(50, 10, 0, 10)
        inputText.textSize = 20f
        inputText.setTextColor(Color.BLACK)
        grid.addView(inputText, GridLayout.LayoutParams(GridLayout.spec(index), GridLayout.spec(0)) as ViewGroup.LayoutParams)
        index++

        for (field in inputs) {
            field.addToLayout(grid, index)
            index++
        }

        outputText.setPadding(50, 10, 0, 10)
        outputText.text = "Outputs"
        outputText.textSize = 20f
        outputText.setTextColor(Color.BLACK)
        grid.addView(outputText, GridLayout.LayoutParams(GridLayout.spec(index), GridLayout.spec(0)))
        index++

        for (field in outputs) {
            field.addToLayout(grid, index)
            index++
        }

        propertiesText.setPadding(50, 10, 0, 10)
        propertiesText.text = "Properties"
        propertiesText.textSize = 20f
        propertiesText.setTextColor(Color.BLACK)
        propertiesText.visibility = View.GONE
        grid.addView(propertiesText, GridLayout.LayoutParams(GridLayout.spec(index), GridLayout.spec(0)))
        index++

        for (field in properties) {
            field.addToLayout(grid, index)
            index++
        }

        constantsText.setPadding(50, 10, 0, 10)
        constantsText.text = "Constants"
        constantsText.textSize = 20f
        constantsText.setTextColor(Color.BLACK)
        constantsText.visibility = View.GONE
        grid.addView(constantsText, GridLayout.LayoutParams(GridLayout.spec(index), GridLayout.spec(0)))
        index++

        for (field in constants) {
            field.addToLayout(grid, index)
            index++
        }
        layout.addView(grid)

        for (field in fields) {
            field.changeUnit(lengthUnit, "mm")
        }
        lengthUnit = "mm"
    }

    fun mapVals() {
        R_cVals["Blue"] = 0.0000608
        R_cVals["Red"] = 0.000023524
        R_cVals["Orange"] = 0.000031681
        R_cVals["Green"] = 0.0000289
        R_cVals["White"] = 0.000032101

        PermVals["Blue"] = 0.000000000086
        PermVals["Red"] = 0.0000000000094658
        PermVals["Orange"] = 0.000000000014269
        PermVals["Green"] = 0.00000000000715
        PermVals["White"] = 0.000000000013747

        D_manVals[0.003] = 0.0016
        D_manVals[0.004] = 0.0026
        D_manVals[0.005] = 0.0036
        D_manVals[0.006] = 0.0044
        D_manVals[0.008] = 0.006
        D_manVals[0.01] = 0.008
    }

    fun toggleCalculatedProperties(v: View) {
        for (field in constants) {
            field.toggleVisibility()
        }
        propertiesText.toggleVisibility()
    }

    fun toggleConstants(v: View) {
        for (field in constants) {
            field.toggleVisibility()
        }
        constantsText.toggleVisibility()
    }

    fun changeTempUnit(v: View) {
        changeTempUnit(v.tag.toString())
    }

    fun changeTempUnit(newTempUnit: String) {
        changeUnit(tempUnit, newTempUnit)
        tempUnit = newTempUnit
    }

    fun changeLengthUnit(v: View) {
        changeLengthUnit(v.tag.toString())
    }

    fun changeLengthUnit(newLengthUnit: String) {
        changeUnit(lengthUnit, newLengthUnit)

        lengthUnit = newLengthUnit
    }

    fun changeMassUnit(v: View) {
        changeMassUnit(v.tag.toString())
    }

    fun changeMassUnit(newMassUnit: String) {
        changeUnit(massUnit, newMassUnit)

        massUnit = newMassUnit
    }

    fun changeForceUnit(newForceUnit: String) {
        changeUnit(forceUnit, newForceUnit)
        forceUnit = newForceUnit
    }

    fun changePressureUnit(newPressureUnit: String) {
        changeUnit(pressureUnit, newPressureUnit)
        pressureUnit = newPressureUnit
    }

    fun changeUnit(oldUnit: String, newUnit: String) {
        for (field in fields) {
            field.changeUnit(oldUnit, newUnit)
        }
    }
}