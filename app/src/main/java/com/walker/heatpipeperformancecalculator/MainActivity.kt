package com.walker.heatpipeperformancecalculator

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import com.walker.heatpipeperformancecalculator.R.id.gridLayout
import com.walker.heatpipeperformancecalculator.R.layout.main
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        var sigFigs = 3
    }

    var tempUnit = "C"
    var lengthUnit = "m"
    var forceUnit = "N"
    var massUnit = "kg"
    var pressureUnit = "Pa"

    private lateinit var T: InputNumberField
    private lateinit var theta: InputNumberField
    private lateinit var L_tot: InputNumberField
    private lateinit var L_evap: InputNumberField
    private lateinit var L_cond: InputNumberField
    private lateinit var D: InputNumberField
    private lateinit var P: InputNumberField

    private lateinit var Powder: MenuField

    private val axial_R = 0.000001
    private val k_copper = 0.0
    private val g = 9.81
    private val Poros = 0.52
    private val R_cont = 0.000007
    private lateinit var t_wall: OutputNumberField
    private lateinit var t_wick: OutputNumberField
    private lateinit var D_man: OutputNumberField
    private lateinit var circ_R: OutputNumberField
    private lateinit var r_vap: OutputNumberField
    private lateinit var A_wick: OutputNumberField
    private lateinit var L_a: OutputNumberField
    private lateinit var Leff: OutputNumberField
    private lateinit var R_c: OutputNumberField
    private lateinit var Perm: OutputNumberField
    private lateinit var R_cond: OutputNumberField
    private lateinit var P_vap: OutputNumberField
    private lateinit var Dens_liq: OutputNumberField
    private lateinit var Vis_liq: OutputNumberField
    private lateinit var Dens_vap: OutputNumberField
    private lateinit var Vis_vap: OutputNumberField
    private lateinit var Surf_ten: OutputNumberField
    private lateinit var H_lv: OutputNumberField
    private lateinit var k_liq: OutputNumberField
    private lateinit var Pc_max: OutputNumberField
    private lateinit var D_pg: OutputNumberField
    private lateinit var D_pv: OutputNumberField
    private lateinit var D_pl: OutputNumberField
    private lateinit var P_c_rem: OutputNumberField
    private lateinit var Q_limit: OutputNumberField
    private lateinit var n_hp: OutputNumberField

    val T_k get() = UnitConverter.TemperatureConverter.convert(T(), "C", "K")

    var context = this
    var PermVals: Map<String, Double> = mapOf(
            "Blue" to 0.000000000086,
            "Red" to 0.0000000000094658,
            "Orange" to 0.000000000014269,
            "Green" to 0.00000000000715,
            "White" to 0.000000000013747)

    var R_cVals: Map<String, Double> = mapOf(
            "Blue" to 0.0000608,
            "Red" to 0.000023524,
            "Orange" to 0.000031681,
            "Green" to 0.0000289,
            "White" to 0.000032101)

    var D_manVals: Map<Double, Double> = mapOf(
            0.003 to 0.0016,
            0.004 to 0.0026,
            0.005 to 0.0036,
            0.006 to 0.0044,
            0.008 to 0.006,
            0.01 to 0.008)
    lateinit var fields: MutableList<Field>
    lateinit var inputText: TextView
    lateinit var outputText: TextView
    lateinit var layout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(main)
        layout = findViewById(gridLayout) as GridLayout

        Field.updateOutputs = { n_hp() }

        Field.getContext = { context }
        fields = Field.fields

        T = InputNumberField("Temperature", "C", 20.0, false)
        theta = InputNumberField("Operating Angle", "°", 90.0, false)
        L_tot = InputNumberField("Heat Pipe Length", "m", 0.15, false)
        L_evap = InputNumberField("Evaporator Length", "m", 0.02, false)
        L_cond = InputNumberField("Condenser Length", "m", 0.06, false)
        D = InputNumberField("Heat Pipe Diameter", "m", 0.006, false)
        P = InputNumberField("Input Power", "W", 10.0, false)

        Powder = MenuField("Powder", "Blue", "Red", "Orange", "Green", "White")

        //axial_R = ConstantField("Axial Resistance", "C/W", true, 0.000001)
        //k_copper = ConstantField("Copper Conductivity", "W/m/C", true, 380.0)
        //g = ConstantField("Gravitational Acceleration", " m/s^2 ", false, 9.81)
        //Poros = ConstantField("Porosity", " % ", true, 0.52)
        //R_cont = ConstantField("Contact Resistance", "C*m^2/W", true, 0.000007)

        D_man = OutputNumberField("Mandrel Diameter", "m", false) {
            if (D_manVals[D()] != null) {
                D_manVals[D()]!!
            } else {
                "D_manVals:$D_manVals D:${D()}".toast(context)
                0.0
            }
        }
        t_wall = OutputNumberField("Wall Thickness", "m", false) { if (D() <= 0.006) 0.0003 else 0.0005 }
        t_wick = OutputNumberField("Wick Thickness", "m", false) { (D() - 2 * t_wall() - D_man()) / 2 }
        circ_R = OutputNumberField("Circumferential Resistance", "C/W", true) {
            R_cont + (t_wall() + t_wick() / Poros) / k_copper
        }
        r_vap = OutputNumberField("Radius of Vapor Space", "m", false) {
            (D() - 2 * t_wall() - 2 * t_wick()) / 2.0
        }
        A_wick = OutputNumberField("Cross Sectional Area", "m^2 ", false) {
            Math.PI * (Math.pow(0.5 * D() - t_wall(), 2.0) - Math.pow(r_vap(), 2.0))
        }
        L_a = OutputNumberField("Adiabatic Length", "m", false) {
            L_tot() - L_evap() - L_cond()
        }
        Leff = OutputNumberField("Effective Length", "m", false) {
            L_a() + (L_evap() + L_cond()) / 2.0
        }
        R_c = OutputNumberField("Contact Resistance", "C/W", true) {
            R_cVals[Powder()]!!
        }
        Perm = OutputNumberField("Permeability", "m", false) {
            PermVals[Powder()]!!
        }
        R_cond = OutputNumberField("Conduction Resistance", "C/W", true) {
            Leff() / k_copper / (Math.PI / 4 * (D() * D() - r_vap() * r_vap()))
        }
        P_vap = OutputNumberField("Vapor Pressure", " kg/m^3 ", false) {
            Math.pow(10.0, 8.07131 - 1730.63 / (233.426 + T())) * 133.322
        }
        Dens_liq = OutputNumberField("Liquid Density", "N*m/s", false) {
            0.14395 / Math.pow(0.0112, 1 + Math.pow(1 - T_k / 649.727, 0.05107))
        }
        Vis_liq = OutputNumberField("Liquid Viscosity", "kg/m^3", false) {
            Math.exp(-3.7188 + 578.99 / (T_k - 137.546)) / 1000
        }
        Dens_vap = OutputNumberField("Vapor Density", "N*m/s", false) {
            0.0022 / T_k * Math.exp(77.345 + 0.0057 * T_k - 7235 / T_k) / Math.pow(T_k, 8.2)
        }
        Vis_vap = OutputNumberField("Vapor Viscosity", "Pa", false) {
            1.512 * Math.pow(T_k, 1.5) / 2.0 / (T_k + 120) / 1000000.0
        }
        Surf_ten = OutputNumberField("Surface Tension", "N/m", false) {
            235.8 * Math.pow(1 - T_k / 647.098, 1.256) * (1 - 0.625 * (1 - T_k / 647.098)) / 1000
        }
        H_lv = OutputNumberField("Latent Heat", "J/kg", true) {
            (2500.8 - 2.36 * T() + 0.0016 * T() * T() - 0.00006 * Math.pow(T(), 3.0)) * 1000
        }
        k_liq = OutputNumberField("Liquid Conductivity", "W/m/C", true) {
            -0.000007933 * T_k * T_k + 0.006222 * T_k - 0.5361
        }
        Pc_max = OutputNumberField("Max Capillary Pressure", "Pa", false) {
            2.0 * Surf_ten() / R_c()
        }
        D_pg = OutputNumberField("Pressure Drop of Gravity", "Pa", false) {
            Dens_liq() * g * L_tot() * -Math.sin(theta() * Math.PI / 180)
        }
        Q_limit = OutputNumberField("Heat Limit", "W", false, true) {
            (Pc_max() - D_pg()) / (Leff() * (8 * Vis_vap() / (Dens_vap() * Math.PI * Math.pow(r_vap(), 4.0) * H_lv()) + Vis_liq() / (Dens_liq() * Perm() * A_wick() * H_lv())))
        }
        D_pv = OutputNumberField("Pressure Drop of Vapor", "Pa", false) {
            8.0 * Vis_vap() * Q_limit() * Leff() / (Dens_vap() * Math.PI * Math.pow(r_vap(), 4.0) * H_lv())
        }
        D_pl = OutputNumberField("Pressure Drop of Liquid", "Pa", false) {
            Vis_liq() * Leff() * Q_limit() / (Dens_liq() * Perm() * A_wick() * H_lv())
        }
        P_c_rem = OutputNumberField("Capillary Pressure Remaining", "Pa", false) {
            Pc_max() - D_pg() - D_pv() - D_pl()
        }
        n_hp = OutputNumberField("Required Heat Pipes", "#", true, true) {
            Math.ceil(P() / Q_limit())
        }

        for(field in fields) {
            if(field is NumberField)
                field.updateNumberText()
        }

        n_hp()

        val grid = GridLayout(this)
        grid.columnCount = 2

        theta.numberText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED

        inputText = TextView(this, "Inputs", 20f)
        outputText = TextView(this, "Outputs", 20f)

        var row = 0

        grid.add(inputText, row++, 0)

        for (field in fields) {
            if (field is InputNumberField || field is MenuField)
                field.addToGrid(grid, row++)
        }

        grid.add(outputText, row++, 0)

        for (field in fields) {
            if (field is OutputNumberField)
                field.addToGrid(grid, row++)
        }

        layout.addView(grid)

        for (field in fields) {
            if (field is NumberField)
                field.changeUnit(lengthUnit, "mm")
        }

        lengthUnit = "mm"
    }

    fun toggleCalculatedProperties(v: View) {
        for (field in fields) {
            if (field is OutputNumberField)
                if(field.important)
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
            if (field is NumberField)
                field.changeUnit(oldUnit, newUnit)
        }
    }

    fun toggleOptions(view: View) {

    }
}