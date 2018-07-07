package com.walker.heatpipeperformancecalculator

import android.content.Context
import android.view.View
import android.widget.GridLayout
import android.widget.Toast

/**
 * Created by walker on 3/16/18.
 */

fun Double.sin(): Double = Math.sin(this)
fun Double.cos(): Double = Math.cos(this)
fun Double.tan(): Double = Math.tan(this)
fun Double.asin(): Double = Math.asin(this)
fun Double.acos(): Double = Math.acos(this)
fun Double.atan(): Double = Math.atan(this)
fun Double.toRadians(): Double = Math.toRadians(this)
fun Double.toDegrees(): Double = Math.toDegrees(this)
fun Double.log(): Double = Math.log(this)
fun Double.log10(): Double = Math.log10(this)
fun Double.sqrt(): Double = Math.sqrt(this)
fun Double.cbrt(): Double = Math.cbrt(this)
fun Double.ceil(): Double = Math.ceil(this)
fun Double.floor(): Double = Math.floor(this)
fun Double.pow(exp: Double): Double = Math.pow(this, exp)
fun Double.round(): Long = Math.round(this)
fun Double.abs(): Double = Math.abs(this)
fun Double.signum(): Double = Math.signum(this)
fun Double.clamp(min: Double, max: Double): Double = Math.max(min, Math.min(this, max))
fun Double.between(min: Double, max: Double): Boolean = this > min && this < max
fun Double.between(min: Int, max: Int): Boolean = this > min && this < max
fun Double.convertTo(from: UnitConverter, to: UnitConverter) = from.convertTo(this, to)
fun Any.toast(context: Context) {
    Toast.makeText(context, this.toString(), Toast.LENGTH_LONG).show()
}

fun Float.sin(): Float = FloatMath.sin(this)
fun Float.cos(): Float = FloatMath.cos(this)
fun Float.tan(): Float = FloatMath.tan(this)
fun Float.asin(): Float = FloatMath.asin(this)
fun Float.acos(): Float = FloatMath.acos(this)
fun Float.atan(): Float = FloatMath.atan(this)
fun Float.toRadians(): Float = FloatMath.toRadians(this)
fun Float.toDegrees(): Float = FloatMath.toDegrees(this)
fun Float.log(): Float = FloatMath.log(this)
fun Float.log10(): Float = FloatMath.log10(this)
fun Float.sqrt(): Float = FloatMath.sqrt(this)
fun Float.cbrt(): Float = FloatMath.cbrt(this)
fun Float.ceil(): Float = FloatMath.ceil(this)
fun Float.floor(): Float = FloatMath.floor(this)
fun Float.pow(exp: Float): Float = FloatMath.pow(this, exp)
fun Float.round(): Int = Math.round(this)
fun Float.abs(): Float = Math.abs(this)
fun Float.signum(): Float = Math.signum(this)
fun Float.clamp(min: Float, max: Float): Float = Math.max(min, Math.min(this, max))

object FloatMath {
    val PI: Float = Math.PI.toFloat()
    val E: Float = Math.E.toFloat()

    fun sin(value: Float): Float = Math.sin(value.toDouble()).toFloat()
    fun cos(value: Float): Float = Math.cos(value.toDouble()).toFloat()
    fun tan(value: Float): Float = Math.tan(value.toDouble()).toFloat()
    fun sqrt(value: Float): Float = Math.sqrt(value.toDouble()).toFloat()
    fun acos(value: Float): Float = Math.acos(value.toDouble()).toFloat()
    fun asin(value: Float): Float = Math.asin(value.toDouble()).toFloat()
    fun atan(value: Float): Float = Math.atan(value.toDouble()).toFloat()
    fun pow(x: Float, y: Float): Float = Math.pow(x.toDouble(), y.toDouble()).toFloat()
    fun ceil(x: Float): Float = Math.ceil(x.toDouble()).toFloat()
    fun floor(x: Float): Float = Math.floor(x.toDouble()).toFloat()
    fun toRadians(angdeg: Float): Float = Math.toRadians(angdeg.toDouble()).toFloat()
    fun toDegrees(angrad: Float): Float = Math.toDegrees(angrad.toDouble()).toFloat()
    fun log(x: Float): Float = Math.log(x.toDouble()).toFloat()
    fun log10(x: Float): Float = Math.log10(x.toDouble()).toFloat()
    fun cbrt(x: Float): Float = Math.cbrt(x.toDouble()).toFloat()
    fun clamp(value: Float, min: Float, max: Float): Float = Math.max(min, Math.min(value, max))
}

fun String.type(): UnitConverter.Factors.UnitType {
    UnitConverter.Factors.units.forEach {
        if (this in it.value)
            return it.key
    }
    throw Exception("$this was not found ")
}

fun View.toggleVisibility() {
    this.visibility = if(this.visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

fun GridLayout.add(view: View, row: Int, column: Int) {
    this.addView(view, GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column)))
}