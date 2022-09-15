package tech.skot.libraries.security

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import tech.skot.core.components.SKActivity
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecurityActionsImpl(
    private val activity: SKActivity,
    private val fragment: Fragment?,
    private val root: View,
) : SecurityActions {
    override fun getBioAuthentAvailability(
        onResult: (availability: BioAuthentAvailability) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            when (BiometricManager.from(activity)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    onResult(BioAuthentAvailability.OK)
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    onResult(BioAuthentAvailability.KO)
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    onResult(BioAuthentAvailability.NONE_ENROLLED)
                }
            }
        } else {
            onResult(BioAuthentAvailability.KO)
        }
    }


    override fun doWithBioAuthent(
        title: String,
        subTitle: String?,
        onKo: (() -> Unit)?,
        onOk: () -> Unit,
    ) {
        val bioPrompt = BiometricPrompt(activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onOk()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onKo?.invoke()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onKo?.invoke()
                }
            }
        )
        bioPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subTitle)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
        )
    }

    override fun enrollBioAuthent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.startActivity(Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            })
        }
        else {
            throw Exception("enrollBioAuthent nécessite API >= 30")
        }
    }

    override fun encodeWithBioAuthent(keyName:String, strData:String):String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val specEncrypt =
                KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    // Accept either a biometric credential or a device credential.
                    // To accept only one type of credential, include only that type as the
                    // second argument.
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .setUserAuthenticationParameters(0 /* duration */,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or
                                KeyProperties.AUTH_DEVICE_CREDENTIAL)
                    .build()
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(specEncrypt)
            keyGenerator.generateKey()
            val keyStore = KeyStore.getInstance("AndroidKeyStore")

            // Before the keystore can be accessed, it must be loaded.
            keyStore.load(null)
            val secretKey = keyStore.getKey(keyName, null) as SecretKey
            
        }

        else {
            throw Exception("enrollBioAuthent nécessite API >= 30")
        }

    }

    override fun decodeWithBioAuthent(keyName:String, encodedStrData:String):String {
        TODO()
    }
}