package com.zenpeartree.karoometricsoverlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamHealthPolicyTest {

    @Test
    fun `does not refresh before any subscription was started`() {
        assertFalse(
            StreamHealthPolicy.shouldRefresh(
                now = 10_000L,
                lastSubscriptionAt = 0L,
                lastDataAt = 0L,
                initialDataTimeoutMs = 15_000L,
                staleDataTimeoutMs = 15_000L,
            ),
        )
    }

    @Test
    fun `refreshes when no initial data arrives in time`() {
        assertTrue(
            StreamHealthPolicy.shouldRefresh(
                now = 20_000L,
                lastSubscriptionAt = 1_000L,
                lastDataAt = 0L,
                initialDataTimeoutMs = 15_000L,
                staleDataTimeoutMs = 15_000L,
            ),
        )
    }

    @Test
    fun `does not refresh while recent data is still arriving`() {
        assertFalse(
            StreamHealthPolicy.shouldRefresh(
                now = 20_000L,
                lastSubscriptionAt = 1_000L,
                lastDataAt = 10_000L,
                initialDataTimeoutMs = 15_000L,
                staleDataTimeoutMs = 15_000L,
            ),
        )
    }

    @Test
    fun `refreshes when a previously active stream goes stale`() {
        assertTrue(
            StreamHealthPolicy.shouldRefresh(
                now = 30_000L,
                lastSubscriptionAt = 1_000L,
                lastDataAt = 10_000L,
                initialDataTimeoutMs = 15_000L,
                staleDataTimeoutMs = 15_000L,
            ),
        )
    }
}
