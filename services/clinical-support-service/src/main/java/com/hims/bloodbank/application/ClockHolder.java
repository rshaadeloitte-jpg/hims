package com.hims.bloodbank.application;

import org.springframework.stereotype.Component;

import java.time.Clock;

/** Indirection so services can see the injected {@link Clock} bean. */
@Component
public class ClockHolder {

    private final Clock clock;

    public ClockHolder(Clock clock) {
        this.clock = clock;
    }

    public Clock clock() {
        return clock;
    }
}
