package io.pocketbase

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestServiceParity : TestService() {
    @Test
    fun test_0_servicesAreExposed() {
        assertNotNull(pb.collections)
        assertNotNull(pb.logs)
        assertNotNull(pb.backups)
        assertNotNull(pb.crons)
        assertNotNull(pb.health)
    }

    @Test
    fun test_1_backupDownloadUrl() {
        val url = pb.backups.getDownloadURL(token = "abc", key = "backup.zip")
        assertTrue(url.contains("/api/backups/backup.zip?token=abc"))
    }
}
