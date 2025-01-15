package models;

import java.time.Clock;
import java.time.Duration;

/** Defines a lifecycle of a session, with a clock and maximum session duration. */
public final class SessionLifecycle {
  private final Clock clock;
  private final Duration maxSessionDuration;

  public SessionLifecycle(Clock clock, Duration maxSessionDuration) {
    this.clock = clock;
    this.maxSessionDuration = maxSessionDuration;
  }

  public Duration getMaxSessionDuration() {
    return maxSessionDuration;
  }

  public Clock getClock() {
    return clock;
  }
}
