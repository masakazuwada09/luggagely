package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaggageRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createItem_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = BaggageRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createItem(
                name = "MacBook Pro 16",
                weightGrams = 2150L,
                source = "AI_SCANNED",
                category = "COMPUTE"
            )
        }
        assertTrue(createResult.isSuccess)
        val itemId = createResult.getOrThrow()
        assertTrue(itemId.isNotEmpty())
    }

    @Test
    fun getUserItems_authenticatedOwner_returnsMatchingItems() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = BaggageRepository(firestore)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createItem(
                name = "Packed Boots",
                weightGrams = 1400L,
                source = "MANUAL_ENTRY",
                category = "APPAREL"
            ).getOrThrow()
        }

        val items = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserItems().getOrThrow() }
        assertTrue(items.any { it.id == itemId })
    }

    @Test
    fun observeItems_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = BaggageRepository(firestore)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createItem(
                name = "Anker Power Bank",
                weightGrams = 650L,
                source = "AI_SCANNED",
                category = "POWER"
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeItems(uid).first { list -> list.any { it.id == itemId } }
        }
        assertTrue(emitted.any { it.id == itemId })
    }

    @Test
    fun getItemById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = BaggageRepository(firestore)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createItem(
                name = "Sony Camera",
                weightGrams = 1350L,
                source = "AI_SCANNED",
                category = "COMPUTE"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = BaggageRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getItemById(aliceUid, itemId)
        }
        assertTrue(result.isFailure)
        val fsError = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    @Test
    fun observeItems_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = BaggageRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeItems(aliceUid).first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val fsError = caughtException?.unwrapFirestoreException()
        assertNotNull(fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
