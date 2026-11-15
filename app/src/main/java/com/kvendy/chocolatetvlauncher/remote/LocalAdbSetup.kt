package com.kvendy.chocolatetvlauncher.remote

import android.util.Base64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

internal enum class LocalAdbSetupResult {
    Allowed, Unreachable, AuthorizationFailed, PermissionDenied
}

private val allowedOp = Regex("""(?m)^\s*ACCESS_RESTRICTED_SETTINGS:\s*allow(?:[;\s]|$)""")

internal suspend fun allowRestrictedSettings(packageName: String): LocalAdbSetupResult =
    runInterruptible(Dispatchers.IO) {
        require(packageName.matches(Regex("[A-Za-z0-9_.]+")))
        Socket().use { socket ->
            try {
                socket.connect(InetSocketAddress("127.0.0.1", 5555), 3_000)
            } catch (_: IOException) {
                return@runInterruptible LocalAdbSetupResult.Unreachable
            }
            socket.soTimeout = 45_000
            val key = AdbCrypto.generateAdbKeyPair { bytes -> Base64.encodeToString(bytes, Base64.NO_WRAP) }
            AdbConnection.create(socket, key).use { connection ->
                val authorized = try {
                    connection.connect(40, TimeUnit.SECONDS, false)
                } catch (_: IOException) {
                    false
                }
                if (!authorized) return@runInterruptible LocalAdbSetupResult.AuthorizationFailed

                val operation = "--user current $packageName ACCESS_RESTRICTED_SETTINGS"
                try {
                    connection.open("shell:cmd appops set $operation allow && cmd appops get $operation").use { stream ->
                        val output = ByteArrayOutputStream()
                        while (!stream.isClosed && output.size() < 4096) {
                            val bytes = try {
                                stream.read()
                            } catch (error: IOException) {
                                if (stream.isClosed) break else throw error
                            }
                            output.write(bytes)
                        }
                        if (allowedOp.containsMatchIn(output.toString(Charsets.UTF_8.name()))) {
                            LocalAdbSetupResult.Allowed
                        } else {
                            LocalAdbSetupResult.PermissionDenied
                        }
                    }
                } catch (_: IOException) {
                    LocalAdbSetupResult.PermissionDenied
                }
            }
        }
    }
