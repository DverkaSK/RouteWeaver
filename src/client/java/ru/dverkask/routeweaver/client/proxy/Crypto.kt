package ru.dverkask.routeweaver.client.proxy

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal fun evpBytesToKey(password: ByteArray, keyLen: Int): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    val result = ByteArrayOutputStream()
    var d = ByteArray(0)
    while (result.size() < keyLen) {
        md.reset()
        md.update(d)
        md.update(password)
        d = md.digest()
        result.write(d)
    }
    return result.toByteArray().copyOf(keyLen)
}

internal fun hkdfSha1(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(SecretKeySpec(salt, "HmacSHA1"))
    val prk = mac.doFinal(ikm)
    val result = ByteArrayOutputStream()
    var t = ByteArray(0)
    var counter = 1
    while (result.size() < length) {
        mac.init(SecretKeySpec(prk, "HmacSHA1"))
        mac.update(t)
        mac.update(info)
        mac.update(counter.toByte())
        t = mac.doFinal()
        result.write(t)
        counter++
    }
    return result.toByteArray().copyOf(length)
}
