package ar.gob.malvinas.faltas.core.infrastructure.time;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class FaltasClock {
    public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private final Clock clock;

    public FaltasClock() { this(Clock.system(ZONE)); }
    public FaltasClock(Clock clock) { this.clock = clock; }

    public Clock clock() { return clock; }
    public LocalDateTime now() { return LocalDateTime.now(clock); }
    public ZoneId zone() { return ZONE; }
}
