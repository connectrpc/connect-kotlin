// Copyright 2022-2023 The Connect Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.connectrpc.conformance.ssl

import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.security.KeyFactory
import java.security.KeyPair
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

fun sslContext(): Pair<SSLSocketFactory, X509TrustManager> {
    val certificate = object {}.javaClass.getResourceAsStream("/cert/client.crt").use { stream ->
        CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
    }
    val certificateAuthority = object {}.javaClass.getResourceAsStream("/cert/ConformanceCA.crt").use { stream ->
        CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
    }
    val publicKey = certificate.publicKey as RSAPublicKey
    val privateKeyBytes = object {}.javaClass.getResourceAsStream("/cert/client.key")!!.bufferedReader().use {
        val lines = it.readLines().toMutableList()
        // Remove BEGIN RSA PRIVATE KEY / END RSA PRIVATE KEY lines
        lines.removeFirst()
        lines.removeLast()
        Base64.getDecoder().decode(lines.joinToString(separator = ""))
    }
    val privateKey = KeyFactory.getInstance("RSA")
        .generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes)) as RSAPrivateKey
    if (publicKey.modulus != privateKey.modulus) {
        throw Exception("key does not match cert") // or other error handling
    }
    val clientHeldCertificate = HeldCertificate(KeyPair(publicKey, privateKey), certificate)
    val result = HandshakeCertificates.Builder()
        .heldCertificate(clientHeldCertificate)
        .addTrustedCertificate(certificateAuthority)
        .build()

    return result.sslSocketFactory() to result.trustManager
}
