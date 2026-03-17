package icu.lama.artifactory.keygen.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * License and product models compatible with Artifactory signed license format (base64 JSON).
 * No dependency on artifactory-addons-manager JAR.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LicenseProduct(
    @JsonProperty("id") var id: String = "",
    @JsonProperty("expires") var expires: Long = 0L,
    @JsonProperty("validFrom") var validFrom: Long = 0L,
    @JsonProperty("owner") var owner: String = "",
    @JsonProperty("type") var type: String = "ENTERPRISE_PLUS",
    @JsonProperty("isTrial") var isTrial: Boolean = false,
    @JsonProperty("signature") var signature: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SignedLicensePayload(
    @JsonProperty("version") var version: Int = 2,
    @JsonProperty("validateOnline") var validateOnline: Boolean = false,
    @JsonProperty("products") var products: Map<String, LicenseProduct> = emptyMap(),
    @JsonProperty("signature") var signature: String? = null
)
