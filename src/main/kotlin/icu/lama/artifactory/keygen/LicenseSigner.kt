package icu.lama.artifactory.keygen

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import icu.lama.artifactory.keygen.model.LicenseProduct
import icu.lama.artifactory.keygen.model.SignedLicensePayload
import org.jfrog.security.util.BCProviderFactory
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.util.Base64

/**
 * Builds and signs a license in Artifactory-compatible format (JSON with RSA-SHA256 signatures).
 * No dependency on artifactory-addons-manager JAR.
 */
object LicenseSigner {

    private val mapper: ObjectMapper = jacksonObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)

    fun signProduct(product: LicenseProduct, privateKey: RSAPrivateKey): LicenseProduct {
        val sign = Signature.getInstance("SHA256withRSA", BCProviderFactory.getProvider())
        sign.initSign(privateKey)
        val copy = product.copy(signature = null)
        val jsonBytes = mapper.writeValueAsBytes(copy)
        sign.update(jsonBytes)
        val sigBytes = sign.sign()
        return product.copy(signature = Base64.getEncoder().encodeToString(sigBytes))
    }

    fun buildAndSignLicense(
        products: Map<String, LicenseProduct>,
        validateOnline: Boolean,
        privateKey: RSAPrivateKey
    ): String {
        val signedProducts = products.mapValues { (_, p) -> signProduct(p, privateKey) }
        val payload = SignedLicensePayload(
            version = 2,
            validateOnline = validateOnline,
            products = signedProducts,
            signature = null
        )
        val payloadBytes = mapper.writeValueAsBytes(payload)
        val sign = Signature.getInstance("SHA256withRSA", BCProviderFactory.getProvider())
        sign.initSign(privateKey)
        sign.update(payloadBytes)
        val topSignature = Base64.getEncoder().encodeToString(sign.sign())
        val finalPayload = payload.copy(signature = topSignature)
        val finalJson = mapper.writeValueAsString(finalPayload)
        return Base64.getEncoder().encodeToString(finalJson.toByteArray(Charsets.UTF_8))
    }
}
