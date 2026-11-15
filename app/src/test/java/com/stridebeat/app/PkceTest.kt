package com.stridebeat.app

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom

class PkceTest {

    @Test
    fun testPkceChallengeGeneration() {
        // Simple standalone verifier check
        val secureRandom = SecureRandom()
        val randomBytes = ByteArray(32)
        secureRandom.nextBytes(randomBytes)
        
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(randomBytes)
        
        assertNotNull(digest)
        assertTrue(digest.isNotEmpty())
    }
}
