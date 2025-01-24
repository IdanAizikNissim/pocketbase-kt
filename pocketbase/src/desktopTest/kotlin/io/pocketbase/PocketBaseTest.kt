package io.pocketbase

import io.pocketbase.config.TestConfig
import kotlin.test.BeforeTest

class PocketBaseTest {
    private val config = TestConfig.load()
    private lateinit var pb: PocketBase

    @BeforeTest
    fun setup() {
        pb = PocketBase(config.host)
    }

//    @Test
//    fun requestVerification() =
//        runTest {
//            auth()
//            pb.collection<User>("users").requestVerification("idan.aizik.nissim@gmail.com")
//        }

//    @Test
//    fun verifyEmail() = runTest {
//        auth()
//        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb2xsZWN0aW9uSWQiOiJfcGJfdXNlcnNfYXV0aF8iLCJlbWFpbCI6ImlkYW4uYWl6aWsubmlzc2ltQGdtYWlsLmNvbSIsImV4cCI6MTcyMjYxMDA2MCwiaWQiOiJzNHVtNWhtNndxMHE2NDUiLCJ0eXBlIjoiYXV0aFJlY29yZCJ9.zyEaIERmdLlKopLB6WHN7OwUwEoxLj-Oy43RxnSgQR4"
//        pb.collection<User>("users").confirmVerification(token)
//    }
//
//    @Test
//    fun requestPasswordReset() = runTest {
//        pb.collection<User>("users").requestPasswordReset("idan.aizik.nissim@gmail.com")
//    }
//
//    @Test
//    fun confirmPasswordReset() = runTest {
//        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb2xsZWN0aW9uSWQiOiJfcGJfdXNlcnNfYXV0aF8iLCJlbWFpbCI6ImlkYW4uYWl6aWsubmlzc2ltQGdtYWlsLmNvbSIsImV4cCI6MTcyMjAwODE5MSwiaWQiOiJzNHVtNWhtNndxMHE2NDUiLCJ0eXBlIjoiYXV0aFJlY29yZCJ9.lpu5O3xWC1OhM95gQnMYJh3z8rBF_VxPuuii5dTNzao"
//        val password = "1234567890"
//        pb.collection<User>("users").confirmPasswordReset(token, password, password)
//    }
}
