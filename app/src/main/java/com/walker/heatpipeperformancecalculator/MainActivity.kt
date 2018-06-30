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
import com.walker.heatpipeperformancecalculator.R.id.gridLayout
import com.walker.heatpipeperformancecalculator.R.layout.main
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

    private lateinit var T: InputField
    private lateinit var theta: InputField
    private lateinit var L_tot: InputField
    private lateinit var L_evap: InputField
    private lateinit var L_cond: InputField
    private lateinit var D: InputField
    private lateinit var P: InputField
    private lateinit var axial_R: OutputField
    private lateinit var k_copper: OutputField
    private lateinit var g: OutputField
    private lateinit var Poros: OutputField
    private lateinit var R_cont: OutputField
    private lateinit var t_wall: OutputField
    private lateinit var t_wick: OutputField
    private lateinit var D_man: OutputField
    private lateinit var circ_R: OutputField
    private lateinit var r_vap: OutputField
    private lateinit var A_wick: OutputField
    private lateinit var L_a: OutputField
    private lateinit var Leff: OutputField
    private lateinit var R_c: OutputField
    private lateinit var Perm: OutputField
    private lateinit var R_cond: OutputField
    private lateinit var P_vap: OutputField
    private lateinit var Dens_liq: OutputField
    private lateinit var Vis_liq: OutputField
    private lateinit var Dens_vap: OutputField
    private lateinit var Vis_vap: OutputField
    private lateinit var Surf_ten: OutputField
    private lateinit var H_lv: OutputField
    private lateinit var k_liq: OutputField
    private lateinit var Pc_max: OutputField
    private lateinit var D_pg: OutputField
    private lateinit var D_pv: OutputField
    private lateinit var D_pl: OutputField
    private lateinit var P_c_rem: OutputField
    private lateinit var Q_limit: OutputField
    private lateinit var n_hp: OutputField

    val T_k get() = UnitConverter.TemperatureConverter.convert(T(), "C", "K")

    var Powder = "Blue"

    var context = this
    var PermVals: MutableMap<String, Double> = HashMap()
    var R_cVals: MutableMap<String, Double> = HashMap()

    var D_manVals: MutableMap<Double, Double> = HashMap()
    var fields: MutableList<Field> = ArrayList()
    lateinit var inputText: TextView
    lateinit var outputText: TextView
    lateinit var layout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(main)
        layout = findViewById(gridLayout) as GridLayout

        Field.updateOutputs = { n_hp() }

        mapVals()

        Field.getContext = { context }
        T = InputField("Temperature", "C", 20.0, false)
        theta = InputField("Operating Angle", "°", 90.0, false)
        L_tot = InputField("Heat Pipe Length", "m", 0.15, false)
        L_evap = InputField("Evaporator Length", "m", 0.02, false)
        L_cond = InputField("Condenser Length", "m", 0.06, false)
        D = InputField("Heat Pipe Diameter", "m", 0.006, false)
        P = InputField("Input Power", "W", 10.0, false)
        axial_R = ConstantField("Axial Resistance", "C/W", true, 0.000001)
        k_copper = ConstantField("Copper Conductivity", "W/m/C", true, 380.0)
        g = ConstantField("Gravitational Acceleration", " m/s^2 ", false, 9.81)
        Poros = ConstantField("Porosity", " % ", true, 0.52)
        R_cont = ConstantField("Contact Resistance", "C*m^2/W", true, 0.000007)
        t_wall = CalculatedField("Wall Thickness", "m", false) { if (D() <= 0.006) 0.0003 else 0.0005 }
        t_wick = CalculatedField("Wick Thickness", "m", false) { (D() - 2 * t_wall() - D_man()) / 2 }
        D_man = CalculatedField("Mandrel Diameter", "m", false) {
            if (D_manVals[D()] != null) {
                D_manVals[D()]!!
            } else {
                Log.i("AppTag", "D_manVals:$D_manVals D:${D()}")
                0.0
            }
        }
        circ_R = CalculatedField("Circumferential Resistance", "C/W", true) {
            R_cont() + (t_wall() + t_wick() / Poros()) / k_copper()
        }
        r_vap = CalculatedField("Radius of Vapor Space", "m", false) {
            (D() - 2 * t_wall() - 2 * t_wick()) / 2.0
        }
        A_wick = CalculatedField("Cross Sectional Area", "m^2 ", false) {
            Math.PI * (Math.pow(0.5 * D() - t_wall(), 2.0) - Math.pow(r_vap(), 2.0))
        }
        L_a = CalculatedField("Adiabatic Length", "m", false) {
            L_tot() - L_evap() - L_cond()
        }
        Leff = CalculatedField("Effective Length", "m", false) {
            L_a() + (L_evap() + L_cond()) / 2.0
        }
        R_c = CalculatedField("Contact Resistance", "C/W", true) {
            R_cVals[Powder]!!
        }
        Perm = CalculatedField("Permeability", "m", false) {
            PermVals[Powder]!!
        }
        R_cond = CalculatedField("Conduction Resistance", "C/W", true) {
            Leff() / k_copper() / (Math.PI / 4 * (D() * D() - r_vap() * r_vap()))
        }
        P_vap = CalculatedField("Vapor Pressure", " kg/m^3 ", false) {
            Math.pow(10.0, 8.07131 - 1730.63 / (233.426 + T())) * 133.322
        }
        Dens_liq = CalculatedField("Liquid Density", "N*m/s", false) {
            0.14395 / Math.pow(0.0112, 1 + Math.pow(1 - T_k / 649.727, 0.05107))
        }
        Vis_liq = CalculatedField("Liquid Viscosity", "kg/m^3", false) {
            Math.exp(-3.7188 + 578.99 / (T_k - 137.546)) / 1000
        }
        Dens_vap = CalculatedField("Vapor Density", "N*m/s", false) {
            0.0022 / T_k * Math.exp(77.345 + 0.0057 * T_k - 7235 / T_k) / Math.pow(T_k, 8.2)
        }
        Vis_vap = CalculatedField("Vapor Viscosity", "Pa", false) {
            1.512 * Math.pow(T_k, 1.5) / 2.0 / (T_k + 120) / 1000000.0
        }
        Surf_ten = CalculatedField("Surface Tension", "N/m", false) {
            235.8 * Math.pow(1 - T_k / 647.098, 1.256) * (1 - 0.625 * (1 - T_k / 647.098)) / 1000
        }
        H_lv = CalculatedField("Latent Heat", "J/kg", true) {
            (2500.8 - 2.36 * T() + 0.0016 * T() * T() - 0.00006 * Math.pow(T(), 3.0)) * 1000
        }
        k_liq = CalculatedField("Liquid Conductivity", "W/m/C", true) {
            -0.000007933 * T_k * T_k + 0.006222 * T_k - 0.5361
        }
        Pc_max = CalculatedField("Max Capillary Pressure", "Pa", false) {
            2.0 * Surf_ten() / R_c()
        }
        D_pg = CalculatedField("Pressure Drop of Gravity", "Pa", false) {
            Dens_liq() * g() * L_tot() * -Math.sin(theta() * Math.PI / 180)
        }
        D_pv = CalculatedField("Pressure Drop of Vapor", "Pa", false) {
            8.0 * Vis_vap() * Q_limit() * Leff() / (Dens_vap() * Math.PI * Math.pow(r_vap(), 4.0) * H_lv())
        }
        D_pl = CalculatedField("Pressure Drop of Liquid", "Pa", false) {
            Vis_liq() * Leff() * Q_limit() / (Dens_liq() * Perm() * A_wick() * H_lv())
        }
        P_c_rem = CalculatedField("Capillary Pressure Remaining", "Pa", false) {
            Pc_max() - D_pg() - D_pv() - D_pl()
        }
        Q_limit = ImportantCalculatedField("Heat Limit", "W", false) {
            (Pc_max() - D_pg()) / (Leff() * (8 * Vis_vap() / (Dens_vap() * Math.PI * Math.pow(r_vap(), 4.0) * H_lv()) + Vis_liq() / (Dens_liq() * Perm() * A_wick() * H_lv())))
        }
        n_hp = ImportantCalculatedField("Required Heat Pipes", "#", true) {
            Math.ceil(P() / Q_limit())
        }

        n_hp()

        val grid = GridLayout(this)
        grid.columnCount = 2

        theta.numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED

        inputText = TextView(this)
        outputText = TextView(this)

        fields.add(T)
        fields.add(theta)
        fields.add(L_tot)
        fields.add(L_evap)
        fields.add(L_cond)
        fields.add(D)
        fields.add(P)
        fields.add(axial_R)
        fields.add(k_copper)
        fields.add(g)
        fields.add(Poros)
        fields.add(R_cont)
        fields.add(t_wall)
        fields.add(t_wick)
        fields.add(D_man)
        fields.add(circ_R)
        fields.add(r_vap)
        fields.add(A_wick)
        fields.add(L_a)
        fields.add(Leff)
        fields.add(R_c)
        fields.add(Perm)
        fields.add(R_cond)
        fields.add(P_vap)
        fields.add(Dens_liq)
        fields.add(Vis_liq)
        fields.add(Dens_vap)
        fields.add(Vis_vap)
        fields.add(Surf_ten)
        fields.add(H_lv)
        fields.add(k_liq)
        fields.add(Pc_max)
        fields.add(D_pg)
        fields.add(D_pv)
        fields.add(D_pl)
        fields.add(P_c_rem)
        fields.add(Q_limit)
        fields.add(n_hp)

        var index = 0

        inputText.text = "Inputs"
        inputText.setPadding(50, 10, 0, 10)
        inputText.textSize = 20f
        inputText.setTextColor(Color.BLACK)
        grid.addView(inputText, GridLayout.LayoutParams(GridLayout.spec(index), GridLayout.spec(0)) as ViewGroup.LayoutParams)
        index++

        for (field in fields) {
            if (field is InputField)
                field.addToLayout(grid, index++)
        }

        outputText.setPadding(50, 10, 0, 10)
        outputText.text = "Outputs"
        outputText.textSize = 20f
        outputText.setTextColor(Color.BLACK)
        grid.addView(outputText, GridLayout.LayoutParams(GridLayout.spec(index++), GridLayout.spec(0)))

        for (field in fields) {
            if (field is ImportantCalculatedField)
                field.addToLayout(grid, index++)
        }

        for (field in fields) {
            if (field is CalculatedField)
                field.addToLayout(grid, index++)
        }

        for (field in fields) {
            if (field is ConstantField)
                field.addToLayout(grid, index++)
        }

        layout.addView(grid)

        for (field in fields) {
            if (field is NumberField)
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
        for (field in fields) {
            if (field is CalculatedField)
                field.toggleVisibility()
        }
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
            if(field is NumberField)
                field.changeUnit(oldUnit, newUnit)
        }
    }
}