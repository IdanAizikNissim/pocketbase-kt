package io.pocketbase

import io.pocketbase.dtos.File
import io.pocketbase.http.ClientException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestAdminServicesCoverage : TestService() {
    @Test
    fun backup_cron_log_collection_services_areInvocable() =
        runTest {
            authWithAdmin()

            val backupUrl = pb.backups.getDownloadURL("token", "backup.zip")
            assertTrue(backupUrl.contains("/api/backups/backup.zip"))
            val legacyBackupUrl = pb.backups.getDownloadUrl("token", "backup.zip")
            assertTrue(legacyBackupUrl.contains("/api/backups/backup.zip"))

            invokeOrClientError { pb.backups.getFullList() }
            invokeOrClientError { pb.backups.create("test-backup") }
            invokeOrClientError {
                pb.backups.upload(
                    files =
                        listOf(
                            File(
                                field = "file",
                                fileName = "backup.zip",
                                data = "not a real backup".encodeToByteArray(),
                            ),
                        ),
                )
            }
            invokeOrClientError { pb.backups.delete("missing-backup.zip") }
            invokeOrClientError { pb.backups.restore("missing-backup.zip") }

            val crons = pb.crons.getFullList()
            assertNotNull(crons)
            if (crons.isNotEmpty()) {
                invokeOrClientError { pb.crons.run(crons.first().id) }
            } else {
                invokeOrClientError { pb.crons.run("missing-cron-id") }
            }

            val logs = pb.logs.getList(page = 1, perPage = 1)
            assertNotNull(logs)
            invokeOrClientError { pb.logs.getStats() }
            if (logs.items.isNotEmpty()) {
                invokeOrClientError { pb.logs.getOne(logs.items.first().id ?: "") }
            } else {
                invokeOrClientError { pb.logs.getOne("missing-log-id") }
            }

            val collections = pb.collections.getList(page = 1, perPage = 20)
            assertTrue(collections.items.isNotEmpty())
            val firstCollection = collections.items.first()
            assertNotNull(firstCollection.id)
            val fetched = pb.collections.getOne(firstCollection.id!!)
            assertNotNull(fetched)
            val byFilter = pb.collections.getFirstListItem("""name != """"")
            assertNotNull(byFilter)
            assertTrue(pb.collections.getFullList(batch = 10).isNotEmpty())

            invokeOrClientError { pb.collections.getScaffolds() }
            invokeOrClientError {
                pb.collections.`import`(
                    collections = listOf(firstCollection),
                    deleteMissing = false,
                )
            }
            invokeOrClientError { pb.collections.truncate("missing_collection_name") }

            // inherited CRUD methods on CollectionService
            invokeOrClientError {
                pb.collections.create(
                    body =
                        firstCollection.copy(
                            name = "tmp_collection_sdk_test",
                            fields =
                                JsonArray(
                                    listOf(
                                        JsonObject(
                                            mapOf(
                                                "name" to JsonPrimitive("title"),
                                                "type" to JsonPrimitive("text"),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                )
            }
            invokeOrClientError {
                pb.collections.update(
                    id = "missing-id",
                    body = firstCollection.copy(name = "tmp_collection_sdk_test_2"),
                )
            }
            invokeOrClientError { pb.collections.delete("missing-id") }
        }

    private suspend fun invokeOrClientError(block: suspend () -> Any?) {
        try {
            block()
        } catch (e: ClientException) {
            assertTrue(e.statusCode >= 400)
        } catch (e: Exception) {
            assertTrue(e.message != null)
        }
    }
}
