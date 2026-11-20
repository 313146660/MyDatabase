package com.example.mementoclone.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Base64

data class BkCfg(
    val url: String = "",
    val port: String = "",
    val user: String = "",
    val pass: String = "",
    val path: String = "Memento",
    val auto: Boolean = false,
    val lastLocalAt: Long = 0L,
    val lastCloudAt: Long = 0L
)

data class CloudResult(val ok: Boolean, val message: String)

object Cloud {
    private const val TIMEOUT = 15000

    fun configFromJson(o: org.json.JSONObject): BkCfg = BkCfg(
        url = o.optString("url", ""),
        port = o.optString("port", ""),
        user = o.optString("user", ""),
        pass = o.optString("pass", ""),
        path = o.optString("path", "Memento").ifBlank { "Memento" },
        auto = o.optBoolean("auto", false),
        lastLocalAt = o.optLong("lastLocalAt", 0L),
        lastCloudAt = o.optLong("lastCloudAt", 0L)
    )

    fun configToJson(c: BkCfg): org.json.JSONObject = org.json.JSONObject()
        .put("url", c.url).put("port", c.port).put("user", c.user).put("pass", c.pass)
        .put("path", c.path).put("auto", c.auto)
        .put("lastLocalAt", c.lastLocalAt).put("lastCloudAt", c.lastCloudAt)

    private fun baseUrl(c: BkCfg): String {
        var raw = c.url.trim()
        require(raw.isNotEmpty()) { "先填写 WebDAV 地址" }
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) raw = "https://$raw"
        if (c.port.trim().isEmpty()) return raw.trimEnd('/')
        val uri = URI(raw)
        val scheme = uri.scheme ?: "https"
        val host = uri.host ?: return raw.trimEnd('/')
        val path = uri.rawPath?.trimEnd('/') ?: ""
        return "$scheme://$host:${c.port.trim()}$path"
    }

    private fun fileUrl(c: BkCfg): String {
        val base = baseUrl(c)
        val dir = c.path.trim().trim('/')
        return if (dir.isEmpty()) "$base/memento-backup.json" else "$base/$dir/memento-backup.json"
    }

    private fun dirUrl(c: BkCfg): String? {
        val dir = c.path.trim().trim('/')
        return if (dir.isEmpty()) null else baseUrl(c) + "/" + dir
    }

    private fun open(method: String, url: String, c: BkCfg): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = TIMEOUT
        conn.readTimeout = TIMEOUT
        conn.instanceFollowRedirects = true
        if (c.user.isNotBlank()) {
            val token = Base64.getEncoder().encodeToString("${c.user}:${c.pass}".toByteArray())
            conn.setRequestProperty("Authorization", "Basic $token")
        }
        return conn
    }

    private fun codeText(code: Int, action: String): String = when (code) {
        in 200..299 -> "${action}成功"
        401, 403 -> "${action}失败：账号或密码不对（$code）"
        404 -> "${action}失败：地址或目录不存在（404）"
        405 -> "${action}失败：服务器不允许这个操作（405）"
        else -> "${action}失败：HTTP $code"
    }

    suspend fun upload(c: BkCfg, json: String): CloudResult = withContext(Dispatchers.IO) {
        try {
            dirUrl(c)?.let { dir ->
                try {
                    val mk = open("MKCOL", dir, c)
                    mk.doOutput = false
                    mk.connect()
                    mk.responseCode
                    mk.disconnect()
                } catch (_: Exception) {
                }
            }
            val conn = open("PUT", fileUrl(c), c)
            val bytes = json.toByteArray(Charsets.UTF_8)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            val code = conn.responseCode
            conn.disconnect()
            CloudResult(code in 200..299, codeText(code, "上传"))
        } catch (e: Exception) {
            CloudResult(false, "上传失败：" + (e.message ?: "网络错误"))
        }
    }

    suspend fun download(c: BkCfg): Pair<CloudResult, String?> = withContext(Dispatchers.IO) {
        try {
            val conn = open("GET", fileUrl(c), c)
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return@withContext CloudResult(false, codeText(code, "恢复")) to null
            }
            val text = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            conn.disconnect()
            CloudResult(true, "已从云端恢复") to text
        } catch (e: Exception) {
            CloudResult(false, "恢复失败：" + (e.message ?: "网络错误")) to null
        }
    }
}
