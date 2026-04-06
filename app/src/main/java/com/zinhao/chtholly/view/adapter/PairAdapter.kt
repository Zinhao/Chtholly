package com.zinhao.chtholly.view.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView

class PairAdapter(
    context: Context,
    private val items: List<Pair<String, String>>
) : ArrayAdapter<Pair<String, String>>(
    context,
    android.R.layout.simple_dropdown_item_1line,
    items
) {
    private var displayItems = items.toList()
    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        (view as TextView).text =  "${displayItems[position].first} ${displayItems[position].second}"
        return view
    }
    @SuppressLint("SetTextI18n")
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        (view as TextView).text = "${displayItems[position].first} ${displayItems[position].second}"
        return view
    }
    override fun getCount() = displayItems.size
    override fun getItem(position: Int) = displayItems[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase()?.trim() ?: ""
                val filtered = if (query.isEmpty() || query.contains('/')) {
                    items  // 空查询时返回全部原始数据
                } else {
                    items.filter {
                        it.first.lowercase().contains(query) ||
                                it.second.contains(query)
                    }
                }
                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                displayItems = results?.values as? List<Pair<String, String>> ?: items.toList()
                notifyDataSetChanged()
            }
        }
    }
}
