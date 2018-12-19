package com.walker.heatpipeperformancecalculator

import java.util.HashMap
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.math.pow


abstract class UnitConverter(val fromUnits: String) {
    open var onChangeListener: (() -> Unit)? = null
    var toUnits = fromUnits
        protected set

    abstract fun changeUnit(oldUnit: String, newUnit: String)

    abstract fun convertTo(number: Double): Double

    abstract fun convertFrom(number: Double): Double
}

class StaticConverter(units: String) : UnitConverter(units) {
    override var onChangeListener: (() -> Unit)?
        get() = null
        set(value) {}
    override fun changeUnit(oldUnit: String, newUnit: String) {}
    override fun convertTo(number: Double) = number
    override fun convertFrom(number: Double) = number
}

class MultiConverter(fromUnits: String) : UnitConverter(fromUnits) {
    private val unitConversions = ArrayList<UnitConversion>()

    init {
        val regex = Regex("(/)?(\\w)+(?:\\^(\\d))?")
        val matches = regex.findAll(fromUnits)
        matches.forEach { match ->
            val (divGroup, unitGroup, powerGroup) = match.destructured
            var power = if (powerGroup == "") 1 else powerGroup.toInt()
            if (divGroup == "/")
                power *= -1
            unitConversions += UnitConversion(unitGroup, power)
        }
    }

    override fun changeUnit(oldUnit: String, newUnit: String) {
        val index = unitConversions.map { it.toUnit }.indexOf(oldUnit)
        if (index != -1) {
            unitConversions[index].toUnit = newUnit
            toUnits = toUnits.replace(oldUnit, newUnit)
            factor = unitConversions.fold(1.0) { acc, converter -> acc * converter.factor }
            onChangeListener?.invoke()
        }
    }

    var factor = unitConversions.fold(1.0) { acc, converter -> acc * converter.factor }

    override fun convertTo(number: Double) = number * factor
    override fun convertFrom(number: Double) = number / factor

    data class UnitConversion(val fromUnit: String, val power: Int) {
        var toUnit: String = fromUnit
            set(value) {
                field = value
                factor = Factors[fromUnit, toUnit].pow(power)
            }
        var factor = Factors[fromUnit, toUnit].pow(power)
            private set
    }

    companion object Factors {
        private val factors = NonNullMap<String, NonNullMap<String, Double>>()

        private val typeToBaseMap = NonNullMap<String, String>()

        private val unitToBaseMap = NonNullMap<String, String>()

        fun setBaseUnit(baseUnit: String, type: String) {
            typeToBaseMap[type] = baseUnit
            factors[baseUnit] = NonNullMap()
        }

        fun addFactor(factor: Double, unit: String, type: String) {
            val baseUnit = typeToBaseMap[type]
            factors[baseUnit][unit] = factor
            unitToBaseMap[unit] = baseUnit
        }

        operator fun get(fromUnit: String, toUnit: String): Double {
            val baseUnit = unitToBaseMap[fromUnit]
            val factors = factors[baseUnit]
            return when {
                fromUnit == toUnit -> 1.0
                fromUnit == baseUnit -> factors[toUnit]
                toUnit == baseUnit -> 1 / factors[fromUnit]
                else -> factors[toUnit] / factors[fromUnit]
            }
        }
    }
}

class TemperatureConverter(fromUnits: String) : UnitConverter(fromUnits) {
    private var formula = unitFormula

    override fun changeUnit(oldUnit: String, newUnit: String) {
        toUnits = toUnits.replace(oldUnit, newUnit)
        formula = Formulas[fromUnits, toUnits]
    }

    override fun convertTo(number: Double) = formula(number)

    override fun convertFrom(number: Double) = formula.inverse(number)

    companion object Formulas {
        private lateinit var baseTempUnit: String
        private val formulas = HashMap<String, Formula>()
        var allUnits = ""
            private set
        val unitFormula = Formula(1.0, 0.0)

        fun setBaseUnit(baseUnit: String) {
            baseTempUnit = baseUnit
            allUnits += "$baseUnit "
        }

        fun addFormula(factor: Double, toUnit: String, adder: Double) {
            formulas[toUnit] = Formula(factor, adder)
            allUnits += " $toUnit "
        }

        class Formula(val factor: Double, val adder: Double, inverse: Formula? = null) {
            val inverse = inverse ?: Formula(1 / factor, -adder / factor, this)
            operator fun invoke(number: Double) = number * factor + adder
            operator fun invoke(formula: Formula) = Formula(factor * formula.factor, adder + factor * formula.adder)
        }

        operator fun get(toUnit: String, fromUnit: String): Formulas.Formula {
            val toFormula = formulas[toUnit] ?: throw Exception("ToUnit: $toUnit")
            val fromFormula = formulas[fromUnit] ?: throw Exception("FromUnit: $fromUnit")
            return when {
                fromUnit == toUnit -> unitFormula
                fromUnit == baseTempUnit -> toFormula
                toUnit == baseTempUnit -> fromFormula.inverse
                else -> fromFormula.inverse(toFormula)
            }
        }
    }
}

