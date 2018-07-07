package com.walker.heatpipeperformancecalculator

import android.R
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView

fun Spinner(context: Context, vararg items: String, itemSelected: (parent: AdapterView<*>?, view: View?, position: Int, id: Long) -> Unit): Spinner {
    val menu = Spinner(context)
    val arr = ArrayAdapter<String>(context, R.layout.simple_spinner_item, items)
    arr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    menu.adapter = arr
    menu.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(view: AdapterView<*>?) {

        }
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            itemSelected(parent, view, position, id)
        }
    }
    return menu
}

fun TextView(context: Context, text: String, size: Float = 20f): TextView {
    val view = TextView(context)
    view.text = text
    view.setPadding(50, 10, 0, 10)
    view.textSize = size
    view.setTextColor(Color.BLACK)
    return view
}