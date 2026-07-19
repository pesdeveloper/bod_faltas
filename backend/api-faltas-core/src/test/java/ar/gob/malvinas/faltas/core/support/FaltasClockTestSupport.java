package ar.gob.malvinas.faltas.core.support;

import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.Clock;
import java.time.Instant;

public final class FaltasClockTestSupport {
    public static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-09T18:00:00Z"),
            FaltasClock.ZONE);
    public static final FaltasClock FIXED = new FaltasClock(FIXED_CLOCK);

    private FaltasClockTestSupport() {}
}
