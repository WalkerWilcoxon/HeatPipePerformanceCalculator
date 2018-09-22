package com.walker.heatpipeperformancecalculator

import java.util.HashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.set
import kotlin.math.pow

class UnitConverter(var units: String) {
    val unitsArray = ArrayList<Unit>()

    init {
        val splitUnits = units.split('*', '/')
        splitUnits.forEachIndexed { index, unit ->
            if (unit in Factors.allUnits) {
                val unitIndex = units.indexOf(unit)
                var power: Int
                if (unitIndex == 0)
                    power = 1
                else
                    power = if (units[unitIndex - 1] == '/') -1 else 1
                if ("^" in unit) {
                    power *= unit[unit.indexOf("^") + 1].toInt()
                }
                unitsArray.add(Unit(unit, power))
            }
        }
    }

    fun convertTo(number: Double, toConverter: UnitConverter): Double {
        if (unitsArray.size == 1 && unitsArray.first().unit.unitType() == Factors.UnitType.Temperature) {
            return TemperatureConverter.convert(number, unitsArray.first().unit, toConverter.unitsArray.first().unit)
        }
        val pairUnits = Array(unitsArray.size) {
            Pair(unitsArray[it], toConverter.unitsArray[it])
        }
        return pairUnits.fold(number) { acc, unit -> unit.first.convertTo(acc, unit.second.unit) }
    }

    fun changeUnit(oldUnit: String, newUnit: String) {
        val index = unitsArray.map { it.unit }.indexOf(oldUnit)
        if(index != -1) {
            unitsArray[index] = Unit(newUnit, unitsArray[index].power)
            units = units.replace(oldUnit, newUnit)
        }
    }

    data class Unit(val unit: String, val power: Int) {
        fun convertTo(number: Double, toUnit: String) : Double {
            val factor = Factors[unit, toUnit]
            return number * factor.pow(power)
        }
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
            if(type != toUnit.unitType()) throw Exception("Incompatible units")
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
            val toFormula = formulas[toUnit]?: throw Exception("ToUnit: $toUnit")
            val fromFormula = formulas[fromUnit]?: throw Exception("FromUnit: $fromUnit")
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