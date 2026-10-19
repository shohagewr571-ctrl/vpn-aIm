package com.aim.vpn.data.config

import com.aim.vpn.domain.AppConfig
import com.aim.vpn.domain.DestinationConfig
import com.aim.vpn.domain.DestinationType
import com.aim.vpn.domain.SeekBarConfig
import com.aim.vpn.domain.SwitchConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor() {
    private val db: FirebaseFirestore = Firebase.firestore

    fun observeAppConfig(): Flow<AppConfig> = callbackFlow {
        val reg = db.collection("appConfig").document("main")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(AppConfig())
                    return@addSnapshotListener
                }
                val data = snap?.data
                if (data == null) {
                    trySend(AppConfig())
                    return@addSnapshotListener
                }
                trySend(
                    AppConfig(
                        appName = data["appName"] as? String ?: "VIP COOL WIRE",
                        logoUrl = data["logoUrl"] as? String,
                        creditText = data["creditText"] as? String ?: "CREDIT BY @MODDERSUNNY©",
                        creditLogoUrl = data["creditLogoUrl"] as? String,
                        floatingIconUrl = data["floatingIconUrl"] as? String,
                        backgroundColor = (data["backgroundColor"] as? Number)?.toLong() ?: 0xFF0A0A0F,
                        accentColor = (data["accentColor"] as? Number)?.toLong() ?: 0xFFE53935,
                        startLabel = data["startLabel"] as? String ?: "START",
                        exitLabel = data["exitLabel"] as? String ?: "EXIT APP",
                        particleAnimationEnabled = data["particleAnimationEnabled"] as? Boolean ?: true,
                        configVersion = (data["configVersion"] as? Number)?.toInt() ?: 1,
                        minimumSupportedVersion = (data["minimumSupportedVersion"] as? Number)?.toInt() ?: 1
                    )
                )
            }
        awaitClose { reg.remove() }
    }

    fun observeSwitches(): Flow<List<SwitchConfig>> = callbackFlow {
        val reg = db.collection("switchConfigs")
            .whereEqualTo("active", true)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    SwitchConfig(
                        switchId = doc.id,
                        name = d["name"] as? String ?: doc.id,
                        active = d["active"] as? Boolean ?: true,
                        sortOrder = (d["sortOrder"] as? Number)?.toInt() ?: 0,
                        onDownloadUrl = d["onDownloadUrl"] as? String,
                        offDownloadUrl = d["offDownloadUrl"] as? String,
                        destinationId = d["destinationId"] as? String ?: "app_private",
                        expectedSha256On = d["expectedSha256On"] as? String,
                        expectedSha256Off = d["expectedSha256Off"] as? String,
                        version = (d["version"] as? Number)?.toInt() ?: 1,
                        iconUrl = d["iconUrl"] as? String,
                        minAppVersion = (d["minAppVersion"] as? Number)?.toInt() ?: 1
                    )
                }
                trySend(list.sortedBy { it.sortOrder })
            }
        awaitClose { reg.remove() }
    }

    suspend fun getDestinations(): List<DestinationConfig> {
        return try {
            val snap = db.collection("destinationConfigs").get().await()
            snap.documents.mapNotNull { doc ->
                val d = doc.data ?: return@mapNotNull null
                val typeStr = d["destinationType"] as? String ?: "APP_PRIVATE"
                DestinationConfig(
                    destinationId = doc.id,
                    displayName = d["displayName"] as? String ?: doc.id,
                    destinationType = runCatching { DestinationType.valueOf(typeStr) }
                        .getOrDefault(DestinationType.APP_PRIVATE),
                    relativePath = d["relativePath"] as? String ?: "",
                    requiresUserSelection = d["requiresUserSelection"] as? Boolean ?: false,
                    enabled = d["enabled"] as? Boolean ?: true
                )
            }
        } catch (_: Exception) {
            listOf(
                DestinationConfig(
                    destinationId = "app_private",
                    displayName = "App Private Storage",
                    destinationType = DestinationType.APP_PRIVATE
                )
            )
        }
    }

    fun getDefaultSeekBar(): SeekBarConfig = SeekBarConfig()
}
