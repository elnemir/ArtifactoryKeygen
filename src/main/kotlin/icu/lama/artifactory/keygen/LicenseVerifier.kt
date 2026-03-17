package icu.lama.artifactory.keygen

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import icu.lama.artifactory.keygen.model.SignedLicensePayload
import org.jfrog.security.util.BCProviderFactory
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64

/**
 * Verifies Artifactory signed license (base64 JSON with RSA-SHA256).
 * No dependency on artifactory-addons-manager JAR.
 */
object LicenseVerifier {

    private val mapper = jacksonObjectMapper()

    fun verify(licenseBase64: String, publicKey: RSAPublicKey): SignedLicensePayload {
        val jsonBytes = try {
            Base64.getDecoder().decode(licenseBase64.trim())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid base64 license", e)
        }
        val jsonStr = String(jsonBytes, Charsets.UTF_8)
        val payload = mapper.readValue<SignedLicensePayload>(jsonStr)
        val signatureB64 = payload.signature ?: throw IllegalArgumentException("License has no signature")
        val signatureBytes = Base64.getDecoder().decode(signatureB64)
        val payloadWithoutSig = payload.copy(signature = null)
        val payloadBytes = mapper.writeValueAsBytes(payloadWithoutSig)
        val sign = Signature.getInstance("SHA256withRSA", BCProviderFactory.getProvider())
        sign.initVerify(publicKey)
        sign.update(payloadBytes)
        if (!sign.verify(signatureBytes))
            throw SecurityException("Invalid license signature")
        return payload
    }
}
