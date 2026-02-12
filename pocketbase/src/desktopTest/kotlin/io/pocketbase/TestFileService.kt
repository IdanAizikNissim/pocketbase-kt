package io.pocketbase

import io.pocketbase.dtos.File
import io.pocketbase.models.Demo
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestFileService : TestService() {
    private val collection by lazy {
        pb.collection<Demo>("demo")
    }

    @Test
    fun test_0_generateProtectedFileToken() =
        runTest {
            authWithAdmin()
            generateToken()
        }

    @Test
    fun test_1_generateProtectedFileToken() =
        runTest {
            authWithAdmin()
            val record = uploadFile()
            val token = generateToken()

            val url =
                pb.files.getUrl(
                    record = record,
                    fileName = record.file!!,
                    token = token,
                )
            val url2 =
                pb.files.getURL(
                    record = record,
                    fileName = record.file!!,
                    token = token,
                )

            assertEquals("${config.host}/api/files/${record.collectionId}/${record.id}/${record.file}?token=$token", url)
            assertEquals(url, url2)

            collection.delete(record.id!!)
        }

    private suspend fun generateToken(): String {
        val token = pb.files.getToken()
        assertNotNull(token)

        return token
    }

    private suspend fun uploadFile(resource: String = "logo.svg"): Demo {
        val bytes = PocketBase::class.java.classLoader?.getResource(resource)?.readBytes()
        assertNotNull(bytes)

        val record =
            collection.create(
                body =
                    Demo(
                        title = "Lorem ipsum with a file",
                    ),
                files =
                    listOf(
                        File(
                            field = "file",
                            fileName = "logo.svg",
                            data = bytes,
                        ),
                    ),
            )

        assertTrue(Regex("logo_.+\\.svg").matches(record.file!!))

        return record
    }
}
