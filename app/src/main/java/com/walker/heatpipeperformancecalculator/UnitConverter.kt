package com.walker.heatpipeperformancecalculator

import java.util.HashMap
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.set
import kotlin.math.pow

private fun String.unitType(): UnitConverter.Factors.UnitType {
    UnitConverter.Factors.units.forEach {
        if (this in it.value)
            return it.key
    }
    throw NoSuchElementException("Unit type of $this could not be determined")
}

class UnitConverter(val baseUnits: String) {
    private val baseUnitsArray = ArrayList<Unit>()
    var convertedUnits = baseUnits
    private val convertedUnitsArray = ArrayList<Unit>()

    init {
        val splitUnits = baseUnits.split('*', '/')
        splitUnits.forEachIndexed { index, unitStr ->
            if (unitStr in Factors.allUnits) {
                
                val unitIndex = baseUnits.indexOf(unitStr)
                var power: Int = when {
                    unitIndex == 0 -> 1
                    baseUnits[unitIndex - 1] == '/' -> -1
                    else -> 1
                }
                if ("^" in unitStr) {
                    power *= unitStr[unitStr.indexOf("^") + 1].toInt()
                }
                val unit = Unit(unitStr, power)
                baseUnitsArray += unit
                convertedUnitsArray += unit
            }
        }
    }

    private val numUnits = baseUnitsArray.size

    fun changeUnit(oldUnit: String, newUnit: String) {
        val index = baseUnitsArray.map { it.str }.indexOf(oldUnit)
        if (index != -1) {
            convertedUnitsArray[index] = Unit(newUnit, convertedUnitsArray[index].power)
            convertedUnits = convertedUnits.replace(oldUnit, newUnit)
        }
    }

    fun convertTo(number: Double): Double = convert(number, baseUnitsArray, convertedUnitsArray)

    fun convertFrom(number: Double): Double = convert(number, convertedUnitsArray, baseUnitsArray)

    fun convert(number: Double, toUnits: ArrayList<Unit>, fromUnits: ArrayList<Unit>): Double {
        if (numUnits == 1 && toUnits.first().type == Factors.UnitType.Temperature) {
            return TemperatureConverter.convert(number, toUnits.first().str, fromUnits.first().str)
        }
        return (0 until numUnits).fold(number) { acc, i -> toUnits[i].convertTo(acc, fromUnits[i].str)}
    }

    data class Unit(val str: String, val power: Int) {
        fun convertTo(number: Double, toUnit: String): Double {
            val factor = Factors[str, toUnit]
            return number * factor.pow(power)
        }

        val type = str.unitType()
    }



    object Factors {
        enum class UnitType {
            Length,
            Pressure,
            Temperature,
            Mass,
            Force;
        }

        data class BaseFactor(val toUnit: String, val factor: Double)

        val factorsMap = HashMap<UnitType, HashMap<String, Double>>()

        val units = HashMap<UnitType, MutableSet<String>>()

        val baseUnits = HashMap<UnitType, String>()

        val allUnits = HashSet<String>()

        init {
            addBaseFactors("m", UnitType.Length,
                    BaseFactor("cm", 100.0),
                    BaseFactor("mm", 1000.0),
                    BaseFactor("in", 39.3701),
                    BaseFactor("ft", 3.28084)
            )
            addBaseFactors("°C", UnitType.Temperature,
                    BaseFactor("K", 1.0),
                    BaseFactor("°F", 1.8),
                    BaseFactor("R", 1.8)
            )
            addBaseFactors("kg", UnitType.Mass,
                    BaseFactor("g", 1000.0),
                    BaseFactor("lbm", 2.20462)

            )
            addBaseFactors("N", UnitType.Force,
                    BaseFactor("lbf", 0.224809)
            )
            addBaseFactors("Pa", UnitType.Pressure,
                    BaseFactor("psi", 0.000145038)
            )
        }

        private fun addBaseFactors(baseUnit: String, type: UnitType, vararg factors: BaseFactor) {
            factorsMap[type] = HashMap()
            units[type] = HashSet()
            units[type]!! += baseUnit
            baseUnits[type] = baseUnit
            factors.forEach {
                addBaseFactor(it, type)
                units[type]!! += it.toUnit
            }
            allUnits += units[type]!!
        }

        fun addBaseFactor(baseFactor: BaseFactor, type: UnitType) {
            factorsMap[type]!![baseFactor.toUnit] = baseFactor.factor
        }

        operator fun get(fromUnit: String, toUnit: String): Double {
            val type = fromUnit.unitType()
            if (type != toUnit.unitType()) throw Exception("Incompatible units")
            val baseUnit = type.baseUnit
            val factors = factorsMap[type]!!
            return when {
                fromUnit == toUnit -> 1.0
                fromUnit == baseUnit -> factors[toUnit]!!
                toUnit == baseUnit -> 1 / factors[fromUnit]!!
                else -> factors[toUnit]!! / factors[fromUnit]!!
            }
        }

        val UnitType.baseUnit get() = baseUnits[this]!!
    }

    object TemperatureConverter {
        val baseUnit = "C"

        val formulas = HashMap<String, BaseFormula>()

        init {
            addFormulas(
                    BaseFormula("°C", 1.0, 0.0),
                    BaseFormula("K", 1.0, 273.15),
                    BaseFormula("°F", 1.8, 32.0),
                    BaseFormula("R", 1.8, 491.67)
            )
        }

        fun convert(number: Double, fromUnit: String, toUnit: String): Double {
            val toFormula = formulas[toUnit] ?: throw Exception("ToUnit: $toUnit")
            val fromFormula = formulas[fromUnit] ?: throw Exception("FromUnit: $fromUnit")
            return when {
                fromUnit == toUnit -> number
                fromUnit == baseUnit -> toFormula(number)
                toUnit == baseUnit -> fromFormula.inverse(number)
                else -> fromFormula.inverse(toFormula(number))
            }
        }

        private fun addFormulas(vararg formulaArray: BaseFormula) {
            for ((toUnit, factor, adder) in formulaArray) {
                addBaseFormula(BaseFormula(toUnit, factor, adder))
            }
        }

        fun addBaseFormula(baseFormula: BaseFormula) {
            formulas[baseFormula.toUnit] = baseFormula
        }

        data class BaseFormula(val toUnit: String, val factor: Double, val adder: Double) {
            val inverse get() = BaseFormula(baseUnit, 1 / factor, -adder / factor)
            operator fun invoke(number: Double): Double {
                return number * factor + adder
            }
        }
    }
}