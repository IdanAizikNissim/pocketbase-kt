package io.pocketbase

import io.pocketbase.models.Demo
import io.pocketbase.models.User
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestRecordService : TestService() {
    private val collection by lazy {
        pb.collection<Demo>("demo")
    }

    @Test
    fun test_0_crud_createRecord() =
        runTest {
            authWithAdmin()
            val record =
                collection.create(
                    body =
                        Demo(
                            title = "Lorem ipsum",
                        ),
                )

            assertFalse(record.id.isNullOrBlank())
            assertFalse(record.created.isNullOrBlank())
            assertFalse(record.updated.isNullOrBlank())
            assertEquals("Lorem ipsum", record.title)
        }

    @Test
    fun test_1_crud_viewRecord() =
        runTest {
            authWithAdmin()
            val record = getRecordByTitle("Lorem ipsum")
            assertNotNull(record)
            val recordById = collection.getOne(record.id!!)
            assertEquals(record, recordById)
        }

    @Test
    fun test_2_crud_updateRecord() =
        runTest {
            authWithAdmin()
            val record = getRecordByTitle("Lorem ipsum")

            assertNotNull(record)
            assertNotNull(record.id)

            val updatedRecord =
                collection.update(
                    id = record.id!!,
                    body =
                        record.copy(
                            title = "Lorem ipsum updated",
                        ),
                )

            assertEquals(record.id, updatedRecord.id)
            assertEquals(record.created, updatedRecord.created)
            assertEquals("Lorem ipsum updated", updatedRecord.title)
            assertNotEquals(record.updated, updatedRecord.updated)
        }

    @Test
    fun test_3_crud_deleteRecord() =
        runTest {
            authWithAdmin()
            val record = getRecordByTitle("Lorem ipsum updated")
            assertNotNull(record)
            assertNotNull(record.id)

            collection.delete(record.id!!)

            val deletedRecord = getRecordByTitle("Lorem ipsum updated")
            assertNull(deletedRecord)
        }

    @Test
    fun test_4_auth_listAuthMethods() =
        runTest {
            val methods = pb.collection<User>("users").listAuthMethods()
            assertTrue(methods.password.enabled)
            assertEquals(listOf("email", "username"), methods.password.identityFields)
            assertTrue(methods.oauth2.providers.isEmpty())
        }

    @Test
    fun test_5_auth_authWithPassword() =
        runTest {
            authWithAdmin()

            val user =
                createUser(
                    username = "test",
                    password = "test012345",
                )
            val authData =
                pb.collection<User>("users").authWithPassword(
                    usernameOrEmail = "test",
                    password = "test012345",
                )

            assertEquals(user.id, authData.record.id)
            assertEquals(user.username, authData.record.username)
            assertTrue(pb.authStore.isValid)
            assertEquals(pb.authStore.token, authData.token)

            pb.collection<User>("users").delete(user.id!!)
        }

    @Test
    fun test_6_auth_authRefresh() =
        runTest {
            authWithAdmin()

            val user =
                createUser(
                    username = "test",
                    password = "test012345",
                )
            val authData =
                pb.collection<User>("users").authWithPassword(
                    usernameOrEmail = "test",
                    password = "test012345",
                )

            assertTrue(pb.authStore.isValid)
            assertEquals(pb.authStore.token, authData.token)

            val refreshAuthData = pb.collection<User>("users").authRefresh()
            assertEquals(authData.record.id, refreshAuthData.record.id)
            assertTrue(pb.authStore.isValid)
            assertEquals(pb.authStore.token, refreshAuthData.token)
            assertEquals(authData.token, refreshAuthData.token)

            pb.collection<User>("users").delete(user.id!!)
        }

    private suspend fun getRecordByTitle(title: String): Demo? {
        return collection.getFirstListItem(
            filter =
                pb.filter(
                    expr = "title = {:title}",
                    query = mapOf("title" to title),
                ),
        )
    }

    private suspend fun createUser(
        username: String,
        password: String,
        email: String = "",
    ): User {
        val user =
            pb.collection<User>("users").create(
                body =
                    User(
                        username = username,
                        email = email,
                        password = password,
                        passwordConfirm = password,
                    ),
            )

        assertTrue(!user.id.isNullOrBlank())

        return user
    }
}
