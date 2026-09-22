package com.zinhao.chtholly.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.zinhao.chtholly.R
import com.zinhao.chtholly.utils.DiscoveredServer

/**
 * 局域网扫描结果列表适配器。
 * 展示：IP:端口 / HTTP 状态（或 "TCP 开放"）/ Server 头 / httpUrl。
 */
class RemoteServerAdapter(
    context: Context,
    private val data: List<DiscoveredServer>
) : ArrayAdapter<DiscoveredServer>(context, 0, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.host_list_item, parent, false)

        val hostname = rowView.findViewById<android.widget.TextView>(R.id.serverAddress)
        val status = rowView.findViewById<android.widget.TextView>(R.id.serverStatus)
        val detail = rowView.findViewById<android.widget.TextView>(R.id.serverDetail)
        val url = rowView.findViewById<android.widget.TextView>(R.id.serverUrl)

        val item = data[position]
        hostname.text = "${item.ip}:${item.port}"
        status.text = if (item.isHttp && item.httpStatus != null) {
            "HTTP ${item.httpStatus}"
        } else {
            "TCP 开放"
        }
        detail.text = item.serverHeader ?: ""
        url.text = item.httpUrl

        return rowView
    }
}
