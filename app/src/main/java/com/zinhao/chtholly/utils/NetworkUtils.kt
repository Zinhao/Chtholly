package com.zinhao.chtholly.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteOrder

object NetworkUtils {

    /**
     * 获取本机 IPv4 地址（推荐方法 - 适用于 Android 10+）
     */
    fun getLocalIpAddress(context: Context): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return null
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return null

        // 遍历所有地址，找到 IPv4 地址
        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            if (address is Inet4Address && !address.isLoopbackAddress) {
                return address.hostAddress
            }
        }
        return null
    }

    fun getLocalIpAddressInt(context: Context): Int {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return 0
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return 0

        // 遍历所有地址，找到 IPv4 地址
        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            if (address is Inet4Address && !address.isLoopbackAddress) {
                return address.hashCode()
            }
        }
        return 0
    }

    /**
     * 获取 WiFi IP 地址（传统方法 - 适用于 Android 9 及以下）
     */
    @Suppress("DEPRECATION")
    fun getWifiIpAddressLegacy(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo ?: return null

        val ipInt = wifiInfo.ipAddress
        if (ipInt == 0) return null

        // 转换字节序（Android 是小端序）
        val ip = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            Integer.reverseBytes(ipInt)
        } else {
            ipInt
        }

        return InetAddress.getByAddress(
            byteArrayOf(
                (ip shr 24 and 0xFF).toByte(),
                (ip shr 16 and 0xFF).toByte(),
                (ip shr 8 and 0xFF).toByte(),
                (ip and 0xFF).toByte()
            )
        ).hostAddress
    }

    /**
     * 获取网段前缀（如 192.168.1）
     */
    fun getNetworkPrefix(context: Context): String? {
        val ip = getLocalIpAddress(context) ?: return null
        val parts = ip.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}"
        } else null
    }

    /**
     * 获取子网掩码长度（CIDR，如 /24）
     */
    fun getSubnetPrefixLength(context: Context): Int? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return null

        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            if (address is Inet4Address && !address.isLoopbackAddress) {
                return linkAddress.prefixLength
            }
        }
        return null
    }

    /**
     * 获取子网掩码（如 255.255.255.0）
     */
    fun getSubnetMask(context: Context): String? {
        val prefixLength = getSubnetPrefixLength(context) ?: return null
        val mask = -1 shl (32 - prefixLength)
        return String.format(
            "%d.%d.%d.%d",
            mask shr 24 and 0xFF,
            mask shr 16 and 0xFF,
            mask shr 8 and 0xFF,
            mask and 0xFF
        )
    }

    /**
     * 获取网关地址
     */
    @Suppress("DEPRECATION")
    fun getGateway(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo ?: return null

        val gateway = dhcpInfo.gateway
        return String.format(
            "%d.%d.%d.%d",
            gateway and 0xFF,
            gateway shr 8 and 0xFF,
            gateway shr 16 and 0xFF,
            gateway shr 24 and 0xFF
        )
    }

    /**
     * 获取 DNS 服务器地址
     */
    fun getDnsServers(context: Context): List<String> {
        val dnsList = mutableListOf<String>()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return dnsList
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return dnsList

        for (dns in linkProperties.dnsServers) {
            if (dns is Inet4Address) {
                dnsList.add(dns.hostAddress ?: continue)
            }
        }
        return dnsList
    }

    /**
     * 获取完整的网络信息（用于调试）
     */
    fun getNetworkInfo(context: Context): NetworkInfo {
        return NetworkInfo(
            ipAddress = getLocalIpAddress(context),
            networkPrefix = getNetworkPrefix(context),
            subnetMask = getSubnetMask(context),
            prefixLength = getSubnetPrefixLength(context),
            gateway = getGateway(context),
            dnsServers = getDnsServers(context),
            isWifi = isWifiConnected(context)
        )
    }

    data class NetworkInfo(
        val ipAddress: String?,
        val networkPrefix: String?,
        val subnetMask: String?,
        val prefixLength: Int?,
        val gateway: String?,
        val dnsServers: List<String>,
        val isWifi: Boolean
    ) {
        fun getScanRange(): Pair<Int, Int> {
            // 通常扫描 1-254（排除网络地址和广播地址）
            return 1 to 254
        }

        override fun toString(): String {
            return """
                IP 地址: $ipAddress
                网段前缀: $networkPrefix
                子网掩码: $subnetMask (/${prefixLength})
                网关: $gateway
                DNS: ${dnsServers.joinToString(", ")}
                WiFi连接: $isWifi
            """.trimIndent()
        }
    }

    /**
     * 检查是否连接到 WiFi
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * 获取所有网络接口的 IP（备用方法）
     */
    fun getAllLocalIpAddresses(): List<String> {
        val ipList = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        ipList.add(address.hostAddress ?: continue)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "获取 IP 失败", e)
        }
        return ipList
    }
}