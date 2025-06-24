package com.example.mydiplom.ui.tokenManager

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.json.JSONObject

class TokenManager(private val context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("jwt_token", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        sharedPrefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return sharedPrefs.getString("jwt_token", null)
    }

    fun clearToken() {
        sharedPrefs.edit().remove("jwt_token").apply()
    }

    fun getCurrentUserId(): Long? {
        return try {
            val token = getToken() ?: run {
                Log.e("TokenManager", "Token is null")
                return null
            }
            Log.d("TokenManager", "Token: $token")
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e("TokenManager", "Invalid token format, parts count: ${parts.size}")
                return null
            }

            val payloadJson = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP),
                Charsets.UTF_8
            )
            Log.d("TokenManager", "Payload JSON: $payloadJson")

            val json = JSONObject(payloadJson)
            val idString = json.opt("nameid")?.toString()
                ?: json.opt("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier")?.toString()
                ?: json.opt("sub")?.toString()
            Log.d("TokenManager", "ID string: $idString")

            idString?.toLongOrNull() ?: run {
                Log.e("TokenManager", "ID is not a valid Long")
                null
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error parsing JWT: ${e.message}")
            null
        }
    }

    fun getCurrentUserRole(): String? {
        return try {
            val token = getToken() ?: return null
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
                Charsets.UTF_8
            )

            val json = JSONObject(payload)


            json.optString("role").takeIf { it.isNotEmpty() }
                ?: json.optString("http://schemas.microsoft.com/ws/2008/06/identity/claims/role")
                    .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error parsing JWT role: ${e.message}")
            null
        }
    }



}