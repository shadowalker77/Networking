package ir.ayantech.ayannetworking.networking

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.security.MessageDigest
import java.security.cert.X509Certificate

class SHA256FingerprintInterceptor : Interceptor {

    @Volatile
    var sha256Fingerprint: String? = null

    private fun X509Certificate.getSha256Fingerprint(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val publicKey = this.encoded
        md.update(publicKey)
        val digest = md.digest()
        return digest.joinToString(separator = ":") { "%02X".format(it) }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val handshake = chain.connection()?.handshake()

        handshake?.peerCertificates()?.firstOrNull()?.let { certificate ->
            if (certificate is X509Certificate) {
                sha256Fingerprint = certificate.getSha256Fingerprint()
            }
        }

        return response
    }
}