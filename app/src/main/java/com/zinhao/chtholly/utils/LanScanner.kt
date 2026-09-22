package com.zinhao.chtholly.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * 局域网开放服务器扫描结果。
 *
 * @param ip          目标 IP
 * @param port        开放的端口
 * @param httpStatus  HTTP 探测成功时的状态码（如 200）；非 HTTP 服务为 null
 * @param serverHeader HTTP 响应的 Server 头（如 "nginx/1.24"）
 * @param isHttp      是否确认为 HTTP 服务（仅 TCP 开放但非 HTTP 时为 false）
 */
data class DiscoveredServer(
    val ip: String,
    val port: Int,
    val httpStatus: Int?,
    val serverHeader: String?,
    val isHttp: Boolean
) {
    val httpUrl: String get() = "http://$ip:$port"
}

/**
 * 局域网扫描器（自研实现，替代 net_scaner AAR）。
 *
 * - 主机发现与端口探测合并为一次 TCP connect（Android 非 root 无法发 ICMP，且无需定位权限）
 * - TCP 通了之后追加一次轻量 `GET /` HTTP 探测，标注状态码与 Server 头
 * - 固定工作线程池（[WORKERS] 个协程消费任务队列），避免为每个 ip×port 预创建协程导致内存暴涨
 * - 以 [Flow] 增量发射结果，collect 取消即停止扫描
 */
object LanScanner {

    /** 常见 Web / 本地 AI 服务端口白名单 */
    val DEFAULT_PORTS = listOf(
        80, 443, 3000, 3001, 5000, 5173, 7860,
        8000, 8001, 8080, 8088, 8443, 11434, 18080
    )

    /** TCP 连接超时（毫秒） */
    private const val CONNECT_TIMEOUT_MS = 300

    /** HTTP 响应读取超时（毫秒） */
    private const val HTTP_TIMEOUT_MS = 400

    /** 并发工作协程数 */
    private const val WORKERS = 64

    /** 大网段兜底使用的前缀长度 */
    private const val DEFAULT_PREFIX = 24

    private const val MAX_HEADER_LINES = 50

    /**
     * 计算需要扫描的目标 IP 列表（不含自身 IP、网络地址与广播地址）。
     *
     * prefix < 24（超大网段如 /16）或 > 30（/31、/32 无扫描意义）时
     * 退化为按 /24 扫描，避免地址数量失控。
     */
    fun buildTargetIps(localIp: String?, prefixLength: Int?): List<String> {
        val segs = localIp?.split(".") ?: return emptyList()
        if (segs.size != 4) return emptyList()
        val octets = segs.map { it.trim().toIntOrNull() ?: return emptyList() }
        if (octets.any { it !in 0..255 }) return emptyList()

        val prefix = when {
            prefixLength == null -> DEFAULT_PREFIX
            prefixLength < 24 -> DEFAULT_PREFIX
            prefixLength > 30 -> DEFAULT_PREFIX
            else -> prefixLength
        }

        val ipNum = octets.fold(0L) { acc, o -> (acc shl 8) or o.toLong() }
        val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val network = ipNum and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)

        val result = ArrayList<String>(254)
        var host = network + 1
        while (host < broadcast) {
            val ip = longToIp(host)
            if (ip != localIp) result.add(ip)
            host++
        }
        return result
    }

    /**
     * 扫描 [ips] × [ports] 矩阵，命中（TCP 开放）的主机增量发射。
     *
     * TCP 开放后会追加 HTTP 探测；非 HTTP 服务也会发射（isHttp = false）。
     * 返回的 Flow 正常完成表示扫描结束，collect 取消即停止剩余探测。
     */
    fun scan(ips: List<String>, ports: List<Int>): Flow<DiscoveredServer> = channelFlow {
        if (ips.isEmpty() || ports.isEmpty()) return@channelFlow

        // 固定 WORKERS 个消费协程从任务队列取 (ip, port)，内存与并发均有界
        val tasks = Channel<Pair<String, Int>>(Channel.RENDEZVOUS)
        repeat(WORKERS) {
            launch(Dispatchers.IO) {
                for ((ip, port) in tasks) {
                    probe(ip, port)?.let { send(it) }
                }
            }
        }

        for (ip in ips) {
            for (port in ports) {
                tasks.send(ip to port)
            }
        }
        tasks.close()
    }

    /**
     * 单点探测：TCP connect 成功后追加 HTTP GET 探测。
     * TCP 不通返回 null；TCP 通但非 HTTP 返回 isHttp = false 的结果。
     */
    private fun probe(ip: String, port: Int): DiscoveredServer? {
        return try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
                probeHttp(socket, ip, port)
            }
        } catch (e: Exception) {
            // 连接超时 / 拒绝 / 网络错误 → 该端口视为关闭
            null
        }
    }

    /**
     * 向已建立的连接发送 `GET /` 并解析状态行与 Server 头。
     * 任何异常都降级为"TCP 开放但非 HTTP"，绝不向上抛。
     */
    private fun probeHttp(socket: Socket, ip: String, port: Int): DiscoveredServer {
        var status: Int? = null
        var serverHeader: String? = null
        var isHttp = false
        try {
            socket.soTimeout = HTTP_TIMEOUT_MS
            val hostHeader = if (port == 80) ip else "$ip:$port"
            val request = buildString {
                append("GET / HTTP/1.1\r\n")
                append("Host: ").append(hostHeader).append("\r\n")
                append("User-Agent: ChthollyLanScanner/1.0\r\n")
                append("Accept: */*\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
            socket.getOutputStream().apply {
                write(request.toByteArray(Charsets.US_ASCII))
                flush()
            }

            val reader = socket.getInputStream().bufferedReader(Charsets.US_ASCII)
            val statusLine = reader.readLine()
            if (statusLine != null && statusLine.startsWith("HTTP/")) {
                isHttp = true
                status = statusLine.trim()
                    .split(Regex("\\s+"))
                    .getOrNull(1)?.toIntOrNull()

                var lines = 0
                while (lines < MAX_HEADER_LINES) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) break
                    if (line.startsWith("Server:", ignoreCase = true)) {
                        serverHeader = line.substringAfter(':').trim().takeIf { it.isNotEmpty() }
                    }
                    lines++
                }
            }
        } catch (e: SocketTimeoutException) {
            // 超时未响应 HTTP（如 SSH、裸 TCP 服务）
        } catch (e: IOException) {
            // 连接被重置等，视为非 HTTP
        } catch (e: Exception) {
            // 兜底，不因探测失败丢失"TCP 开放"这一事实
        }
        return DiscoveredServer(
            ip = ip,
            port = port,
            httpStatus = status,
            serverHeader = serverHeader,
            isHttp = isHttp
        )
    }

    private fun longToIp(n: Long): String =
        "${(n shr 24) and 0xFF}.${(n shr 16) and 0xFF}.${(n shr 8) and 0xFF}.${n and 0xFF}"
}
