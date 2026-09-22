package com.zinhao.chtholly.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zinhao.chtholly.databinding.ActivityScanNetBinding
import com.zinhao.chtholly.utils.LanScanner
import com.zinhao.chtholly.utils.DiscoveredServer
import com.zinhao.chtholly.utils.NetworkUtils
import com.zinhao.chtholly.view.adapter.RemoteServerAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 局域网扫描页：扫描当前子网内开放的服务器，点击条目返回 http://ip:port。
 *
 * Intent 契约：
 * - 可选 extra [EXTRA_START_PORT] / [EXTRA_END_PORT]：指定端口范围（全范围模式）；
 *   不传或非法时使用 [LanScanner.DEFAULT_PORTS] 白名单（快速模式）。
 * - 结果 extra "host"：选中服务器的 httpUrl。
 */
class ScanNetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanNetBinding
    private lateinit var adapter: RemoteServerAdapter
    private val servers = mutableListOf<DiscoveredServer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanNetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RemoteServerAdapter(this, servers)
        binding.listview.adapter = adapter
        binding.listview.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val dataIntent = Intent().putExtra("host", servers[position].httpUrl)
            setResult(RESULT_OK, dataIntent)
            finish()
        }

        startScan()
    }

    private fun resolvePorts(): List<Int> {
        val start = intent.getIntExtra(EXTRA_START_PORT, -1)
        val end = intent.getIntExtra(EXTRA_END_PORT, -1)
        return if (start in 1..65535 && end in start..65535) {
            (start..end).toList()
        } else {
            LanScanner.DEFAULT_PORTS
        }
    }

    private fun startScan() {
        val localIp = NetworkUtils.getLocalIpAddress(this)
        val prefixLength = NetworkUtils.getSubnetPrefixLength(this)
        val ips = LanScanner.buildTargetIps(localIp, prefixLength)
        if (ips.isEmpty()) {
            showStatus("无法确定本机网段，请检查网络连接", showProgress = false)
            return
        }

        val ports = resolvePorts()
        showStatus("正在扫描 ${ips.size} 个主机 × ${ports.size} 个端口…", showProgress = true)

        lifecycleScope.launch {
            LanScanner.scan(ips, ports)
                .catch { e ->
                    if (e is CancellationException) throw e
                    showStatus("扫描出错：${e.message}", showProgress = false)
                }
                .collect { server ->
                    servers.add(server)
                    adapter.notifyDataSetChanged()
                    showStatus("正在扫描…已发现 ${servers.size} 个服务", showProgress = true)
                }
            // Flow 正常完成：扫描结束（页面已销毁则不再更新 UI）
            if (isActive) {
                showStatus(
                    if (servers.isEmpty()) "扫描完成，未发现开放的服务"
                    else "扫描完成，发现 ${servers.size} 个服务",
                    showProgress = false
                )
            }
        }
    }

    private fun showStatus(text: String, showProgress: Boolean) {
        binding.tvStatus.text = text
        binding.tvStatus.visibility = View.VISIBLE
        binding.progressBar.visibility = if (showProgress) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_START_PORT = "start_port"
        const val EXTRA_END_PORT = "end_port"
    }
}
