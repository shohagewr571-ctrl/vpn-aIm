package com.aim.vpn.data.auth

import com.aim.vpn.domain.AccessKeyResult
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val sessionStore: SessionStore
) {
    private val functions: FirebaseFunctions = Firebase.functions

    suspend fun validateAccessKey(rawKey: String): AccessKeyResult {
        val normalized = rawKey.trim()
        if (normalized.isEmpty()) {
            return AccessKeyResult(valid = false, message = "Access key cannot be empty")
        }
        return try {
            val data = hashMapOf("key" to normalized)
            val result = functions
                .getHttpsCallable("validateAccessKey")
                .call(data)
                .await()
            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any?> ?: emptyMap()
            val valid = map["valid"] as? Boolean ?: false
            val message = map["message"] as? String ?: if (valid) "OK" else "Invalid Access Key"
            val keyId = map["keyId"] as? String
            if (valid && keyId != null) {
                sessionStore.saveSession(keyId)
            }
            AccessKeyResult(valid = valid, message = message, keyId = keyId)
        } catch (e: Exception) {
            AccessKeyResult(
                valid = false,
                message = "Network error: ${e.message ?: "Unable to reach server"}"
            )
        }
    }

    suspend fun hasValidSession(): Boolean = sessionStore.hasValidSession()

    suspend fun clearSession() = sessionStore.clear()
}
