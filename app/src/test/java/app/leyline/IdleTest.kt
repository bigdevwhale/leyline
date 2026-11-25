package app.leyline

import app.leyline.data.Idle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleTest {
    private val min = 60_000L
    private val t0 = 1_000_000_000L

    @Test fun aLongJamDoesNotEndTheRide() {
        // standing for 15 minutes with the GPS on
        assertFalse(Idle.over(t0 + 15 * min, t0 - 30 * min, t0, 0, t0 + 15 * min, metro = false))
    }

    @Test fun standingStillEndsItAtLast() {
        assertTrue(Idle.over(t0 + 21 * min, t0 - 30 * min, t0, 0, t0 + 21 * min, metro = false))
    }

    @Test fun theGpsGoneQuietIsNotTheRideOver() {
        // last fix of any kind 25 minutes ago: a train or a tunnel, the ride may well go on
        assertFalse(Idle.over(t0 + 25 * min, t0 - 30 * min, t0, 0, t0, metro = false))
        assertTrue(Idle.over(t0 + 46 * min, t0 - 30 * min, t0, 0, t0, metro = false))
    }

    @Test fun roughPositionsKeepItGoing() {
        // no good fix for half an hour, but coarse ones showed it moving 5 minutes ago
        assertFalse(Idle.over(t0 + 30 * min, t0 - 30 * min, t0, t0 + 25 * min, t0 + 30 * min, metro = false))
    }

    @Test fun theMetroWaitsLong() {
        assertFalse(Idle.over(t0 + 60 * min, t0, 0, 0, 0, metro = true))
    }
}
