package com.infoproject1

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

class AlwaysShowArrayAdapter(
    context: Context,
    private val allItems: List<String>
) : ArrayAdapter<String>(
    context,
    android.R.layout.simple_dropdown_item_1line,
    allItems.toMutableList()
) {

    override fun getCount(): Int = allItems.size

    override fun getItem(position: Int): String = allItems[position]

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults().apply {
                    values = allItems
                    count = allItems.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                addAll(allItems)
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return resultValue?.toString() ?: ""
            }
        }
    }
}