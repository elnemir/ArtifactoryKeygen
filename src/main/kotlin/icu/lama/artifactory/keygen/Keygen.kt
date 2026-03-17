package icu.lama.artifactory.keygen

import org.jfrog.license.a.ObfuscatedString
import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonInclude
import org.jfrog.license.api.Product
import org.jfrog.license.exception.LicenseRuntimeException
import org.jfrog.license.multiplatform.License
import org.jfrog.license.multiplatform.SignedLicense
import org.jfrog.license.multiplatform.SignedProduct
import org.jfrog.security.util.BCProviderFactory
import org.yaml.snakeyaml.Yaml
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringWriter
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


private const val VERSION = "2.0-SNAPSHOT"

fun main(vararg args: String) {
    println("ArtifactoryKeygen v$VERSION")
    println("Educational purposes only!")
    println()
    println("ALERT! This project is for educational purposes only. Do not use to crack JFrog software.")
    println("For help use 'help' sub-command.")
    println()
    val parameters = args.toMutableList()

    if (args.isEmpty()) {
        parameters += prompt("Please enter a sub-command: ").split(" ")
    }

    when (parameters.removeAt(0)) {
        "obf" -> {
            println(ObfuscatedString.createObfuscation(parameters.joinToString(" ") { it }))
        }

        "pub" -> {
            println(Constants.PUBLIC_KEY)
        }

        "pri" -> {
            println(Constants.PRIVATE_KEY)
        }

        "genkey" -> {
            val keygen = KeyPairGenerator.getInstance("RSA", BCProviderFactory.getProvider())
            keygen.initialize(4096)
            val kp = keygen.generateKeyPair()
            println("PrivateKey: " + Base64.getEncoder().encodeToString(kp.private.encoded))
            println("PublicKey: " + Base64.getEncoder().encodeToString(kp.public.encoded))
        }

        "gen" -> {
            val licenseTypeStr = prompt("Enter License Type [Trial/Commercial/Enterprise]: ", listOf("trial", "commercial", "enterprise"), "enterprise")
            val licensedTo = prompt("Enter License Holder: ", "My Company")
            val email = prompt("Enter Email: ", "admin@example.com")
            val validDaysStr = prompt("Enter Valid Days [default: 3650]: ", "3650")
            val validDays = validDaysStr.toIntOrNull()?.coerceIn(1, 365 * 100) ?: 3650

            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, validDays)
            val validThrough = cal.time
            val validThroughFormatted = SimpleDateFormat("yyyy-MM-dd").format(validThrough)

            val private = KeyFactory.getInstance("RSA", BCProviderFactory.getProvider()).generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(Constants.PRIVATE_KEY)))
            val sign = Signature.getInstance("SHA256withRSA", BCProviderFactory.getProvider())
            val products = mutableMapOf<String, SignedProduct>()

            while (true) {
                val productId = prompt("Enter product id: ", "artifactory")
                if (products.containsKey(productId)) {
                    println("Product '$productId' already added. Use another id.")
                    continue
                }
                val owner = prompt("  Owner [$licensedTo]: ", licensedTo)
                val product = Product()
                product.id = productId
                product.expires = validThrough
                product.isTrial = (licenseTypeStr == "trial")
                product.owner = owner.ifEmpty { licensedTo }
                product.validFrom = Date()
                product.type = Product.Type.ENTERPRISE_PLUS
                products[product.id] = SignedProduct(product, private, sign)
                if (prompt("Add more products to this license? [y/N]: ", listOf("y", "n", "yes", "no"), "n").lowercase() in listOf("n", "no")) break
            }

            val license = License()
            license.version = 2
            license.validateOnline = false
            license.products = products
            val sLicense = SignedLicense(license, private, sign)

            println("Generating license...")
            println()
            val licenseB64 = createFinalLicense(sLicense)
            val licenseTypeDisplay = licenseTypeStr.replaceFirstChar { it.uppercase() }
            println("===== LICENSE =====")
            println("""
                {
                  "licenseType": "$licenseTypeDisplay",
                  "licensedTo": "$licensedTo",
                  "email": "$email",
                  "validThrough": "$validThroughFormatted",
                  "products": [${products.keys.joinToString { "\"$it\"" }}],
                  "signature": "..."
                }
            """.trimIndent())
            println()
            val saveToFile = prompt("Save to file? [y/N]: ", listOf("y", "n", "yes", "no"), "n").lowercase()
            if (saveToFile == "y" || saveToFile == "yes") {
                File("license.json").writeText(licenseB64)
                println("License saved to: license.json")
            }
            println()
            println("Raw license (for manual copy):")
            println(chunkString(licenseB64))
        }

        "verify" -> {
            if (parameters.isEmpty()) {
                return println("Usage: verify <license> — provide license file path (e.g. license.json) or license string.")
            }
            val licenseInput = parameters[0]
            val licenseStr = if (File(licenseInput).takeIf { it.exists() && it.isFile } != null) {
                File(licenseInput).readText().trim()
            } else {
                licenseInput
            }
            runCatching {
                val a = org.jfrog.license.api.a()
                a.b(licenseStr, Constants.PUBLIC_KEY)
            }.onFailure {
                it.printStackTrace()
                println("Invalid license or signature.")
            }.onSuccess {
                println(Yaml().dump(it))
            }
        }

        "enc" -> {
            if (parameters.isEmpty()) {
                return println("input file(artifactory-addons-manager) is missing")
            }
            println("WARNING! You are using a untested feature!")

            val keyFactory = SecretKeyFactory.getInstance("PBEWithSHAAnd3-KeyTripleDES-CBC", BCProviderFactory.getProvider())
            val cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding", BCProviderFactory.getProvider())
            cipher.init(1, keyFactory.generateSecret(PBEKeySpec("just!for@fun#".toCharArray())), PBEParameterSpec(byteArrayOf(-54, -2, -70, -66, -21, -85, -17, -84), 20))
            println(Base64.getEncoder().encodeToString(cipher.update(Base64.getDecoder().decode(parameters[0]))))
        }

        "mkconfig" -> {
            val key = prompt("Enter your RSA Public Key (X.509 format): ")
            if (!key.isBlank()) {
                try {
                    val x509Key = X509EncodedKeySpec(Base64.getDecoder().decode(key.trim()))
                    val parsedKey = KeyFactory.getInstance("RSA", BCProviderFactory.getProvider()).generatePublic(x509Key) as RSAPublicKey
                    if (parsedKey.modulus.bitLength() < 4096) {
                        throw Exception("Key is too short")
                    }
                    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
            val root = doc.createElement("config")
            doc.appendChild(root)
            val keyEl = doc.createElement("publicKey")
            keyEl.appendChild(doc.createTextNode(key.trim()))
            root.appendChild(keyEl)
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            val writer = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(writer))
            val configHex = writer.buffer.toString().toByteArray(Charsets.UTF_8).toHex().uppercase()
            println("Generated Agent Configuration:")
            println(configHex)
            println()
            println("Add to setenv.sh:")
            println("CATALINA_OPTS=\"\$CATALINA_OPTS -javaagent:/path/to/agent.jar=$configHex\"")
                } catch (err: Exception) {
                    err.printStackTrace()
                    println("Illegal key! Please make sure your key is a standard RSA public key in X.509 format and at least 4096 bits long.")
                }
            } else {
                println("Key must not be null or blank!")
            }
        }

        "verifyAgent" -> {
            val keyField = org.jfrog.license.api.a::class.java.getDeclaredField("d")
            keyField.trySetAccessible()
            val keyFound = keyField.get(null)
            if (keyFound == "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmquSyL1wSs6DvU5iceNvbfha4MPX040iHFuoBTeEZA43rsHWLNLIECmVJ3Zv1xV9+NkrORKQEuJckEXzbTHSrpzZDfF/sjlKhalHKN3joNgIoIFG5MoM9kPFEB0mAJx/Hpiojj+/LZ5uTHIWiEGKm6C4EQQL9F+2FpcQbj6ve26t03YNZVIhzgmGw4TEb/1WXje7ywtMv3bGkKqAak6VoTKnn4MOm3ULzck9K+KPIgOd01Wa00PPbVqitB7Tqej7y3wZKMPxgzM0n7fouH7lu0yowiV+V5SyO/6g/Wq+DNgniKpnbcFsVLxFE7LcyHr94cAorBH+EUcepGKqqml4cQIDAQAB") {
                println("Validation Failed!")
            } else {
                println("Validation Success! New key is: $keyFound")
            }
        }

        "help" -> {
            println("""
                List of all sub-commands:
                    obf <text>:
                        Obfuscate text with JFrog's 'ObfuscatedString' class
                    pub:
                        Get the current public key (RSA)
                    pri:
                        Get the current private key (RSA)
                    genkey:
                        Generate a key pair
                    gen:
                        Generate a license with the current private key
                    verify <license>:
                        Verify a license with the current public key
                    enc: [ NOT TESTED ]
                        Encrypt a license (I guess this is used for the license of the old version)
                    verifyAgent:
                        You can verify ArtifactoryAgent by attaching the agent to ArtifactoryKeygen
                    mkconfig:
                        Create agent config but friendly!
                To inject into artifactory
                    Open Artifactory's tomcat folder and add the following JVM options to tomcat.
                    In case you don't know how to pass extra JVM args you can see the README of this project
                    OR Google it by yourself!
                    
                    -javaagent:</path/to/this/jar/file>
                    
                    This will patch class 'org.jfrog.license.api.a'
                    
            """.trimIndent())
        }

        else -> {
            println("Nothing todo. Use 'help' subcommand to see all available subcommands.")
        }
    }
}


fun prompt(msg: String, default: String? = null): String {
    print(msg)
    return readlnOrNull().orEmpty().ifEmpty { default ?: "" }
}

fun prompt(msg: String, options: List<String>, default: String? = null): String {
    val rd = prompt(msg, default).lowercase()
    return if (rd in options) {
        rd
    } else {
        println("Illegal input! Please enter one of following options only: " + options.joinToString())
        prompt(msg, options, default)
    }
}

fun date(yrs: Int, mo: Int, dys: Int): Date {
    val cal = Calendar.getInstance()
    cal.set(yrs, mo, dys)
    return cal.time
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun createFinalLicense(obj: Any): String {
    val bos = ByteArrayOutputStream()
    try {
        val factory = JsonFactory()
        val generator = factory.createGenerator(bos, JsonEncoding.UTF8)
        val objectMapper = ObjectMapper(factory)
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        generator.setCodec(objectMapper)
        generator.writeObject(obj)
        generator.close()
    } catch (e: IOException) {
        throw LicenseRuntimeException("Failed to serialize license", e)
    }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}

fun chunkString(str: String, chunkLength: Int = 76) = str.chunked(chunkLength).joinToString("\n")