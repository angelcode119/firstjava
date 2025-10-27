package com.example.test.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

object SimInfoHelper {
    private const val TAG = "SimInfoHelper"

    fun getSimInfo(context: Context): JSONArray {
        val simArray = JSONArray()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) return simArray

        try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val sims = subManager.activeSubscriptionInfoList

            if (!sims.isNullOrEmpty()) {
                sims.forEach { info ->
                    val sim = JSONObject().apply {
                        put("simSlot", info.simSlotIndex)
                        put("subscriptionId", info.subscriptionId)
                        put("carrierName", info.carrierName?.toString() ?: "")
                        put("displayName", info.displayName?.toString() ?: "")
                        put("phoneNumber", info.number ?: "")
                        put("countryIso", info.countryIso ?: "")
                        put("mcc", info.mccString ?: "")
                        put("mnc", info.mncString ?: "")
                        put("isNetworkRoaming", info.dataRoaming == SubscriptionManager.DATA_ROAMING_ENABLE)
                        put("iconTint", info.iconTint)
                        put("cardId", info.cardId)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put("carrierId", info.carrierId)
                            put("isEmbedded", info.isEmbedded)
                            put("isOpportunistic", info.isOpportunistic)
                            put("iccId", info.iccId ?: "")
                            val groupUuid = info.groupUuid
                            put("groupUuid", groupUuid?.toString() ?: "")
                        } else {
                            put("carrierId", -1)
                            put("isEmbedded", false)
                            put("isOpportunistic", false)
                            put("iccId", "")
                            put("groupUuid", "")
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                put("portIndex", info.portIndex)
                            } catch (e: Exception) {
                                put("portIndex", -1)
                            }
                        } else {
                            put("portIndex", -1)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            try {
                                val tm = telephonyManager.createForSubscriptionId(info.subscriptionId)

                                put("networkType", getNetworkTypeName(tm.dataNetworkType))
                                put("networkOperatorName", tm.networkOperatorName ?: "")
                                put("networkOperator", tm.networkOperator ?: "")
                                put("simOperatorName", tm.simOperatorName ?: "")
                                put("simOperator", tm.simOperator ?: "")
                                put("simState", getSimStateName(tm.simState))
                                put("phoneType", getPhoneTypeName(tm.phoneType))

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    try {
                                        put("imei", tm.imei ?: "")
                                        put("meid", tm.meid ?: "")
                                    } catch (e: Exception) {
                                        put("imei", "")
                                        put("meid", "")
                                    }
                                } else {
                                    put("imei", "")
                                    put("meid", "")
                                }

                                put("dataEnabled", tm.isDataEnabled)
                                put("dataRoamingEnabled", tm.isDataRoamingEnabled)
                                put("voiceCapable", tm.isVoiceCapable)
                                put("smsCapable", tm.isSmsCapable)
                                put("hasIccCard", tm.hasIccCard())

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    try {
                                        put("deviceSoftwareVersion", tm.deviceSoftwareVersion ?: "")
                                        put("visualVoicemailPackageName", tm.visualVoicemailPackageName ?: "")
                                    } catch (e: Exception) {
                                        put("deviceSoftwareVersion", "")
                                        put("visualVoicemailPackageName", "")
                                    }
                                } else {
                                    put("deviceSoftwareVersion", "")
                                    put("visualVoicemailPackageName", "")
                                }

                                put("networkCountryIso", tm.networkCountryIso ?: "")
                                put("simCountryIso", tm.simCountryIso ?: "")

                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error reading TelephonyManager for SIM ${info.simSlotIndex}: ${e.message}")
                            }
                        }
                    }
                    simArray.put(sim)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SIM Info error: ${e.message}", e)
        }
        return simArray
    }

    private fun getNetworkTypeName(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS (2G)"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE (2G)"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS (3G)"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA (2G)"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO Rev.0 (3G)"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO Rev.A (3G)"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT (2G)"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA (3G)"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA (3G)"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA (3G)"
            TelephonyManager.NETWORK_TYPE_IDEN -> "iDEN (2G)"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO Rev.B (3G)"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE (4G)"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD (3G)"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+ (3G)"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM (2G)"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA (3G)"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                networkType == TelephonyManager.NETWORK_TYPE_NR) {
                "5G NR"
            } else {
                "Unknown"
            }
        }
    }

    private fun getSimStateName(state: Int): String {
        return when (state) {
            TelephonyManager.SIM_STATE_ABSENT -> "Absent"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
            TelephonyManager.SIM_STATE_READY -> "Ready"
            TelephonyManager.SIM_STATE_NOT_READY -> "Not Ready"
            TelephonyManager.SIM_STATE_PERM_DISABLED -> "Permanently Disabled"
            TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "Card IO Error"
            TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "Card Restricted"
            else -> "Unknown"
        }
    }

    private fun getPhoneTypeName(phoneType: Int): String {
        return when (phoneType) {
            TelephonyManager.PHONE_TYPE_NONE -> "None"
            TelephonyManager.PHONE_TYPE_GSM -> "GSM"
            TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
            TelephonyManager.PHONE_TYPE_SIP -> "SIP"
            else -> "Unknown"
        }
    }
}