package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class BaggageItemEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val weightGrams: Long = 1000L,
    val source: String = "MANUAL_ENTRY",
    val category: String = "ACCESSORY",
    val isShiftedToPersonal: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class BaggageRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun DocumentSnapshot.toBaggageItemEntity(): BaggageItemEntity? {
        if (!exists()) return null
        return BaggageItemEntity(
            id = id,
            userId = getString("userId") ?: "",
            name = getString("name") ?: "",
            weightGrams = getLong("weightGrams") ?: 1000L,
            source = getString("source") ?: "MANUAL_ENTRY",
            category = getString("category") ?: "ACCESSORY",
            isShiftedToPersonal = getBoolean("isShiftedToPersonal") ?: false,
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    fun observeItems(userId: String = requireUserId()): Flow<List<BaggageItemEntity>> {
        val path = "users/$userId/baggage_items"
        return db.collection("users").document(userId).collection("baggage_items")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toBaggageItemEntity() }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getUserItems(userId: String = requireUserId()): Result<List<BaggageItemEntity>> {
        val path = "users/$userId/baggage_items"
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("baggage_items")
                .get()
                .await()
            Result.success(
                snapshot.documents.mapNotNull { it.toBaggageItemEntity() }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getItemById(targetUserId: String, itemId: String): Result<BaggageItemEntity?> {
        val path = "users/$targetUserId/baggage_items/$itemId"
        return try {
            val doc = db.collection("users")
                .document(targetUserId)
                .collection("baggage_items")
                .document(itemId)
                .get()
                .await()
            Result.success(doc.toBaggageItemEntity())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createItem(
        name: String,
        weightGrams: Long,
        source: String,
        category: String,
        isShiftedToPersonal: Boolean = false
    ): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("baggage_items")
        val docRef = colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.take(120),
            "weightGrams" to weightGrams.coerceIn(50L, 50000L),
            "source" to source,
            "category" to category,
            "isShiftedToPersonal" to isShiftedToPersonal,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateItemWeightOrShift(
        itemId: String,
        weightGrams: Long,
        isShiftedToPersonal: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("baggage_items").document(itemId)
        val updates = mapOf(
            "weightGrams" to weightGrams.coerceIn(50L, 50000L),
            "isShiftedToPersonal" to isShiftedToPersonal,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteItem(itemId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("baggage_items").document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
