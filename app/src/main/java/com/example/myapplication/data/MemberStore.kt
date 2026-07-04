package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.memberDataStore by preferencesDataStore(name = "members")

@Serializable
data class Member(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val status: String = "ACTIVE"
)

class MemberStore(private val context: Context) {
    private val membersKey = stringPreferencesKey("merchant_members")
    private val json = Json { ignoreUnknownKeys = true }

    val members: Flow<List<Member>> = context.memberDataStore.data.map { prefs ->
        val membersJson = prefs[membersKey] ?: "[]"
        try {
            json.decodeFromString<List<Member>>(membersJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addMember(member: Member) {
        context.memberDataStore.edit { prefs ->
            val current = json.decodeFromString<List<Member>>(prefs[membersKey] ?: "[]")
            val updated = current + member
            prefs[membersKey] = json.encodeToString(updated)
        }
    }

    suspend fun removeMember(id: String) {
        context.memberDataStore.edit { prefs ->
            val current = json.decodeFromString<List<Member>>(prefs[membersKey] ?: "[]")
            val updated = current.filterNot { it.id == id }
            prefs[membersKey] = json.encodeToString(updated)
        }
    }
}
