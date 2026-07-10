package ar.gob.malvinas.faltas.core.support;

import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test clock that advances by one second on each invocation of now().
 * Used to detect spurious multiple clock reads within a single operation.
 *
 * Usage: after an operation, assert invocationCount() == expected.
 * For MISMO_HECHO operations, assert all timestamps captured are equal.
 */
public final class CountingClock extends FaltasClock {

    private final Instant base;
    private final AtomicInteger count = new AtomicInteger(0);

    public CountingClock(Instant base) {
        super(Clock.fixed(base, FaltasClock.ZONE));
        this.base = base;
    }

    public static CountingClock startingAt(Instant base) {
        return new CountingClock(base);
    }

    public static CountingClock startingAt(String isoInstant) {
        return new CountingClock(Instant.parse(isoInstant));
    }

    @Override
    public LocalDateTime now() {
        int n = count.getAndIncrement();
        return LocalDateTime.ofInstant(base.plusSeconds(n), FaltasClock.ZONE);
    }

    /** Number of times now() has been called since creation (or last reset). */
    public int invocationCount() {
        return count.get();
    }

    public void reset() {
        count.set(0);
    }

    /** Returns the LocalDateTime this clock would return on its n-th call (0-indexed). */
    public LocalDateTime nthInstant(int n) {
        return LocalDateTime.ofInstant(base.plusSeconds(n), FaltasClock.ZONE);
    }
}
