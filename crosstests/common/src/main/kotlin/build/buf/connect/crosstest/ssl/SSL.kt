// Copyright 2022-2023 Buf Technologies, Inc.
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

package build.buf.connect.crosstest.ssl

import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.ByteString.Companion.encodeUtf8
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
    val certificate = clientCert.byteInputStream(Charsets.UTF_8).use { stream ->
        CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
    }
    val certificateAuthority = crosstestCACert.byteInputStream(Charsets.UTF_8).use { stream ->
        CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
    }
    val publicKey = certificate.getPublicKey() as RSAPublicKey
    val clientKeyBytes = clientKey
        .replace("-----(BEGIN|END) PRIVATE KEY-----".toRegex(), "")
        .replace("\r?\n".toRegex(), "")
        .encodeUtf8()
        .toByteArray()
    val decodedKey = Base64.getDecoder().decode(clientKeyBytes)
    val privateKey = KeyFactory.getInstance("RSA")
        .generatePrivate(PKCS8EncodedKeySpec(decodedKey)) as RSAPrivateKey
    if (publicKey.getModulus() != privateKey.getModulus()) {
        throw Exception("key does not match cert") // or other error handling
    }
    val clientHeldCertificate = HeldCertificate(KeyPair(publicKey, privateKey), certificate)
    val result = HandshakeCertificates.Builder()
        .heldCertificate(clientHeldCertificate)
        .addTrustedCertificate(certificateAuthority)
        .build()
    return result.sslSocketFactory() to result.trustManager
}

// https://github.com/bufbuild/connect-crosstest/blob/main/cert/client.crt
// cert issues: https://stackoverflow.com/questions/9210514/unable-to-find-valid-certification-path-to-requested-target-error-even-after-c
private const val clientCert = """-----BEGIN CERTIFICATE-----
MIIEODCCAiCgAwIBAgIRAJTCeo42f8lts3VeDnN7CVwwDQYJKoZIhvcNAQELBQAw
FjEUMBIGA1UEAxMLQ3Jvc3N0ZXN0Q0EwHhcNMjIwNTAzMTcxMDQwWhcNMjMxMTAz
MTcxOTU2WjARMQ8wDQYDVQQDEwZjbGllbnQwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQCoJI6BDesWPERm7zjLGA9Pp0XSR3rnpecXTKIBwamr35gr/It4
jAZMMBUBHhdvLB0pAj1/hlWLvDQSuQBvfsr2KrqOvtVOP0c5KCzwHjvLmyhhvjOV
5iEdtv5mUDwILcQH8mvK4XTyWqIDvslUs3KxWfuwrPHZE+qptVAE982pbYixQTTG
ynRKi+tlFqb0a070koKu5jj+x2TV6Kgh4SFmexHdBSYWiElUGAks2MJ09CT5+Dva
z4lePGlA6VlDIjwif/lziHASBvW+6J4ZpLyAeCc+1/DgI74Gmpy2oNGmb2LBMwrM
OZL5KsdiMyYY9ZjPmKWcxybjgGPfinbctOldAgMBAAGjgYUwgYIwDgYDVR0PAQH/
BAQDAgO4MB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAdBgNVHQ4EFgQU
kjyFVJG7YocJyghCFYWHEDdWrv0wHwYDVR0jBBgwFoAUpg6MnxjPVlc2QEjDWcAC
0qxir5IwEQYDVR0RBAowCIIGY2xpZW50MA0GCSqGSIb3DQEBCwUAA4ICAQAgqAI9
/yUVAyf3UoSYaZrA22/OPUwvoHoQOSWZDkHXIy9VOiQAJNE8N97XzIIgbjB93sVC
njwOUk+kXEfmPZuD8RdAR43m1s+WrKCMukAIyg7hobLHqolkUPCdKlsaXgUsNsX1
T5ka1imVaIyggXWBVu3Q3Wpt8ERl82ncBr65wzRnvAdsGOFag4ujamSAU8s/Mhfy
EXNkx9u4MqvfWqhU7e5uBULfic+e836ojDxa5If7/MZo892lCQq6t861e5SOHhRN
AtS7toBmR/h5vYGDqmIwGGaR6YqcIMZ9JbWyPRbr2KIiMDGU7EvVYpIuIKdodbkJ
aN9RTmaCJTJVHrwMos9sAjwUqis+5gZj7lsPozp7OKYlFwy5Ae4+3C8e8Q1sIzKv
FaiodJ712fK6xsYr8CbUm4XbP3FBCAiYwbbaJg6odOciexpyvFnbF7152t+a6wOc
52z4pz7cCjZun40eDxmSucRGx5Do1ohFNrXxriGih0nFF82GCSCZZ3ddkSTIy/Uj
15MCKRXIOq/0kFDtMIJZ6X73I6jLvk0hiakfrV+GyrVBkzG1pHJEsSyiy7f1edTG
FjL/opqnz8GV8od9hLHJfwclPBSEA0fp7yNvOzKm1lNPEX009ME1hK4dLKNCqv3x
g+mJcflVCfjEqJzfEy4wPq5SJzOIzXva6DyBpA==
-----END CERTIFICATE-----"""

// https://github.com/bufbuild/connect-crosstest/blob/main/cert/CrosstestCA.crt
// Certificate authority for the trusted cert.
private const val crosstestCACert = """-----BEGIN CERTIFICATE-----
MIIE7DCCAtSgAwIBAgIBATANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDEwtDcm9z
c3Rlc3RDQTAeFw0yMjA1MDMxNzA5NTlaFw0yMzExMDMxNzE5NTZaMBYxFDASBgNV
BAMTC0Nyb3NzdGVzdENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA
qEUpIjCMAu5K7GuFrAu4cUtKMZaX78L9DbCtq7tkwPX9JLgNUxrfgN1qdIFsxRfw
/XaRjK+A3Gbech4z1U5ujeImAqg6IzpdFvQg9NVlIXpaEhsTk/oU8YXJAouXzBCZ
LBteiK3L5410/O95dVNUKpPlX0qHPkxr+ZUeV+8+MRVge7xDwceBpnAUj1gSMmcE
93i6CnVr75cF2aScB37rquuCpYcjibAQq0V9qvlG7DtEPg1gMt2Hluc5Vjf/sWI3
HTxoBaWxQ4iqdwNRrNO+yjZ97IHe92bnEXzkynhWyd0hCcdwDaU4Gb33xcFjMlWr
cYtVmhHIIN3L/v/P69uhskEGqGRICcOHI+Y0jRz5eVwln87waMosM4ecSCnygmTf
RNRL3eRzZYZj5/eqJYPpErswdtoOiix5cStNpXU8GqmxMqAtrStgl86gLtJXtZgn
LETquRfHSQvNFbuDO6bm56eWf/PqXeCJYZkb3wuBVK8uU8BUSH1wCnhDdpaJIpR+
zY93XrRiVXFka0ZNvaAHZsHnHtuxKaP+fOSIhrCWqa2hhpUEw4ykKFZfznqOk+UU
jwgnRRZN6rZaRQQGuoR94WNoP2cy3JJOpnEzRsTfwG+FL+gJSEN4tg2DbO7th/Ae
fHvAJ0VfCllabvQbLGjJFkI4ddMSj5lhJyIBfu6YWlsCAwEAAaNFMEMwDgYDVR0P
AQH/BAQDAgEGMBIGA1UdEwEB/wQIMAYBAf8CAQAwHQYDVR0OBBYEFKYOjJ8Yz1ZX
NkBIw1nAAtKsYq+SMA0GCSqGSIb3DQEBCwUAA4ICAQByUDpOqgviWV/d3U9+84Sv
tkaL3Z4niKJxmGWwdzMhZFBlgEEsy5vBZ08uYkuel5Gg8Fl6pKVi2hPMU8NSZIMB
7OvomHfV6ag1CInbRozs7+ef/MKIC4ic7Tqmzf0zRpFjogkhUghMzYLmjjsPXwOb
JwJGmdyytHZ30qATIsoaR17/FRedl/FVlLoV48eMDIiu9ZuBqRLXqvJ4Xar0i/Td
TQdvFU5v4x785th5I9gWcKHZR1Frx4j4bfWWNi8m6cPp4i1R8prq5z+8lnIq/yJR
/QERkHQRZ1X6FK9aOnbMBFu+gLXcKSql7goGTIXVzqFeKmrZgqblu3axRQ8brs7p
1Va6Ha8yQyawV0FQKCcVEmXxpjSONWc9qyBDBx32/praLOET3UHmYcJilVAgZufR
9pAuCRf+7nDx9t2vynmT0C8MeFNjDRcN8QYr9lO9CUbcV+nOqiEG128XMhjsLj7m
lEP/lCyEGGVB+/LuxbRxOGzoZDb0WVan41zSNaFfnvXGMuTxLOhY6upiljL8k6Jm
eqI0hZhMnLbJFVnKgqaMpV+h2UvmEPXNx9oSKSG+11awVPdJjf4MLVOVVTPeLEv6
AsJXGUNptIhuKblQ0afNGOF37CCbDhdajKnMFVbXZWxFsDEs0J4EW0donSiJEGFF
iQ8d6tGEgzeekht617JMvQ==
-----END CERTIFICATE-----"""

// This is the decoded rsa client key below.
// openssl pkey -in key.pem -out outkey.pem
// (alt: openssl pkcs8 -topk8 -nocrypt -in key.pem -out output)
// https://stackoverflow.com/questions/68926722/how-to-read-a-certificate-chain-for-okhttpclient
private const val clientKey = """-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCoJI6BDesWPERm
7zjLGA9Pp0XSR3rnpecXTKIBwamr35gr/It4jAZMMBUBHhdvLB0pAj1/hlWLvDQS
uQBvfsr2KrqOvtVOP0c5KCzwHjvLmyhhvjOV5iEdtv5mUDwILcQH8mvK4XTyWqID
vslUs3KxWfuwrPHZE+qptVAE982pbYixQTTGynRKi+tlFqb0a070koKu5jj+x2TV
6Kgh4SFmexHdBSYWiElUGAks2MJ09CT5+Dvaz4lePGlA6VlDIjwif/lziHASBvW+
6J4ZpLyAeCc+1/DgI74Gmpy2oNGmb2LBMwrMOZL5KsdiMyYY9ZjPmKWcxybjgGPf
inbctOldAgMBAAECggEAVpOkML1K7AMSMXJB6wkzcQ8vO2wE09wv1mZmEMN4KwCR
aSy7X9GxuG8VkaK97EdpqGD3637u82lv4qgRmldOCFzN/Iw0ZvrcIIkYQnc+0MLO
ky2NgESjjppX+bSchJWUF4dyi191iQiz3XvSTOOtTbaIi1sxmFXSAZpTXot+ARuI
mkxLueZUqeYsSzTaDgbcf7EpdF9GFADtEh04hr+IeQmw1D6jgyInuzgbWGnfOpkF
Kce48zeqqP4wBQeht4kp0HtYJblRZcj06YCvJFKT5HooMOhfHJZlzQVDkEnQ6HAz
MGzGCqzngINY0P88uOay86O92EyqkpHLYCagxeTQnQKBgQDQyRuGH8zucR4bAozw
zGi9IQTS7SPaSxuC31bHZoJ+I/oZbEtlm6OjVpXtOASfszvSrJ5Oz0H99ZXOT607
TI3RLTtXGVpjlExXjgt0edDzlN5MQjFAgBplireQV1+Kb1lXOi5kcriYKpigqkpD
LHSwSNt+b36109uz/HnHNVrkRwKBgQDOKpTOaimxkzgKcKeq+4orwYftbANld3tK
sw6baoG69nE1Bmw02xMtH1JXbdgD3JiuSaR2p1VFL1ZfUeMWBtKtDX1vieTsZnd3
WmMIhnQFb5ZUjpmQg1EKeIKwr/Z9qQJHJqwdMX55kit7tIiMzZIrFBTsRsbUnUy3
dWHeKVLLOwKBgQC7DWIOYRE+ErQRKNDSr5+qyhlDS9gSK7YjIyrhDLMeheb8vewy
xSTvIpTB6a0i0vZmweY23zLVbx/erb2a4fQwkqCWYQ19J5DZ5FXY7YZJpHcgxTDR
A7QigwwAUnczVJ0rK+ICdlFxasBBOS+9TOfiQ/P6K7PO/VbZwvnrgp7C4wKBgQC4
0NdBC06BD5aHVWIZFxFQFFfD8LZCuY9e8ZFApYPSlKX7gNxzrYhNROzNz3x8Sb7d
TssOSwdX1A27uW63CjrGQ3eVC6qaeWjTJ8XrmIxdayD6gDMNp4p4tnuB5Nw03dNa
8UINkZbtaKluZfKbNpW35HK1NOV9J93kAGhRff3ojQKBgBJgXdODMVUaRAr+VWVM
4VpYB/QhnerBV8JaV7OLgZUMdz55nbufysRQ8UeAQeb0gPSO6f/gVCxSSRZCm9+X
YfnVHhS0VV49dKIqrsP8o1qZJRhjcq/J/Rrm2ZFHfdLCOOnd9VG4W2I1WB9MDc4t
raq4CptHPEywZgBR95C0Jv3y
-----END PRIVATE KEY-----"""

// https://github.com/bufbuild/connect-crosstest/blob/main/cert/client.key
private const val rsaClientKey = """-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAqCSOgQ3rFjxEZu84yxgPT6dF0kd656XnF0yiAcGpq9+YK/yL
eIwGTDAVAR4XbywdKQI9f4ZVi7w0ErkAb37K9iq6jr7VTj9HOSgs8B47y5soYb4z
leYhHbb+ZlA8CC3EB/JryuF08lqiA77JVLNysVn7sKzx2RPqqbVQBPfNqW2IsUE0
xsp0SovrZRam9GtO9JKCruY4/sdk1eioIeEhZnsR3QUmFohJVBgJLNjCdPQk+fg7
2s+JXjxpQOlZQyI8In/5c4hwEgb1vuieGaS8gHgnPtfw4CO+BpqctqDRpm9iwTMK
zDmS+SrHYjMmGPWYz5ilnMcm44Bj34p23LTpXQIDAQABAoIBAFaTpDC9SuwDEjFy
QesJM3EPLztsBNPcL9ZmZhDDeCsAkWksu1/RsbhvFZGivexHaahg9+t+7vNpb+Ko
EZpXTghczfyMNGb63CCJGEJ3PtDCzpMtjYBEo46aV/m0nISVlBeHcotfdYkIs917
0kzjrU22iItbMZhV0gGaU16LfgEbiJpMS7nmVKnmLEs02g4G3H+xKXRfRhQA7RId
OIa/iHkJsNQ+o4MiJ7s4G1hp3zqZBSnHuPM3qqj+MAUHobeJKdB7WCW5UWXI9OmA
ryRSk+R6KDDoXxyWZc0FQ5BJ0OhwMzBsxgqs54CDWND/PLjmsvOjvdhMqpKRy2Am
oMXk0J0CgYEA0Mkbhh/M7nEeGwKM8MxovSEE0u0j2ksbgt9Wx2aCfiP6GWxLZZuj
o1aV7TgEn7M70qyeTs9B/fWVzk+tO0yN0S07VxlaY5RMV44LdHnQ85TeTEIxQIAa
ZYq3kFdfim9ZVzouZHK4mCqYoKpKQyx0sEjbfm9+tdPbs/x5xzVa5EcCgYEAziqU
zmopsZM4CnCnqvuKK8GH7WwDZXd7SrMOm2qBuvZxNQZsNNsTLR9SV23YA9yYrkmk
dqdVRS9WX1HjFgbSrQ19b4nk7GZ3d1pjCIZ0BW+WVI6ZkINRCniCsK/2fakCRyas
HTF+eZIre7SIjM2SKxQU7EbG1J1Mt3Vh3ilSyzsCgYEAuw1iDmERPhK0ESjQ0q+f
qsoZQ0vYEiu2IyMq4QyzHoXm/L3sMsUk7yKUwemtItL2ZsHmNt8y1W8f3q29muH0
MJKglmENfSeQ2eRV2O2GSaR3IMUw0QO0IoMMAFJ3M1SdKyviAnZRcWrAQTkvvUzn
4kPz+iuzzv1W2cL564KewuMCgYEAuNDXQQtOgQ+Wh1ViGRcRUBRXw/C2QrmPXvGR
QKWD0pSl+4Dcc62ITUTszc98fEm+3U7LDksHV9QNu7lutwo6xkN3lQuqmnlo0yfF
65iMXWsg+oAzDaeKeLZ7geTcNN3TWvFCDZGW7WipbmXymzaVt+RytTTlfSfd5ABo
UX396I0CgYASYF3TgzFVGkQK/lVlTOFaWAf0IZ3qwVfCWlezi4GVDHc+eZ27n8rE
UPFHgEHm9ID0jun/4FQsUkkWQpvfl2H51R4UtFVePXSiKq7D/KNamSUYY3Kvyf0a
5tmRR33Swjjp3fVRuFtiNVgfTA3OLa2quAqbRzxMsGYAUfeQtCb98g==
-----END RSA PRIVATE KEY-----"""
