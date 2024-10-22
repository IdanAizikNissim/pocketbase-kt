package io.pocketbase

import io.pocketbase.dtos.AdminModel
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestAdminService : TestService() {
    @Test
    fun test_0_authWithPassword() =
        runTest {
            val auth = authWithAdmin()
            assertNotNull(auth.token)
            assertEquals(config.adminEmail, auth.admin.email)
        }

    @Test
    fun test_1_authRefresh() =
        runTest {
            authWithAdmin()
            val auth = pb.admins.authRefresh()
            assertNotNull(auth.token)
            assertEquals(config.adminEmail, auth.admin.email)
        }

    @Test
    fun test_2_viewAdmin() =
        runTest {
            val auth = authWithAdmin()
            val admin = pb.admins.getOne(auth.admin.id)
            assertEquals(config.adminEmail, admin.email)
            assertEquals(auth.admin.id, admin.id)
        }

    @Test
    fun test_3_createAdmin() =
        runTest {
            authWithAdmin()
            val admin =
                pb.admins.create(
                    body =
                        AdminModel(
                            email = "new@example.com",
                            password = "1234567890",
                            passwordConfirm = "1234567890",
                            avatar = 8,
                        ),
                )

            assertTrue(admin.id.isNotBlank())
            assertEquals("new@example.com", admin.email)
            assertEquals(8, admin.avatar)
        }

    @Test
    fun test_4_updateAdmin() =
        runTest {
            authWithAdmin()

            val newAdmin = getAdminByEmail("new@example.com")
            assertNotNull(newAdmin)
            val admin =
                pb.admins.update(
                    id = newAdmin.id,
                    body =
                        AdminModel(
                            email = "new@example.com",
                            password = "0987654321",
                            passwordConfirm = "0987654321",
                            avatar = 4,
                        ),
                )

            assertEquals(newAdmin.id, admin.id)
            assertNotEquals(newAdmin.updated, admin.updated)
            assertEquals(4, admin.avatar)
        }

    @Test
    fun test_5_deleteAdmin() =
        runTest {
            authWithAdmin()
            val newAdmin = getAdminByEmail("new@example.com")
            assertNotNull(newAdmin)
            pb.admins.delete(newAdmin.id)
            assertNull(getAdminByEmail("new@example.com"))
        }

    private suspend fun getAdminByEmail(email: String): AdminModel? {
        val newAdmin =
            pb.admins.getFirstListItem(
                filter =
                    pb.filter(
                        expr = "email = {:email}",
                        query = mapOf("email" to email),
                    ),
            )

        return newAdmin
    }
}
