package services.apibridge;

import static com.google.api.client.util.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Provides string parsing of the value from a Retry-After http header */
@Singleton
public final class RetryAfterHeaderParser {
  private final Clock clock;

  @Inject
  public RetryAfterHeaderParser(Clock clock) {
    this.clock = checkNotNull(clock);
  }

  /**
   * Attempts to parse the value of a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Retry-After">Retry-After</a>
   * HTTP header.
   *
   * <p>The Retry-After could be in one of two formats
   *
   * <ul>
   *   <li>http-date - See {@link DateTimeFormatter#RFC_1123_DATE_TIME}
   *   <li>delay-seconds - See {@link Long}
   * </ul>
   *
   * @param value Value from Retry-After http header
   * @return The amount of time to wait before making next attempt or a Duration of 0 if the value
   *     cannot be successfully parsed.
   */
  public Duration parse(String value) {
    if (value == null || value.isBlank()) {
      return Duration.ZERO;
    }

    value = value.trim();

    // Attempt to parse as a number
    try {
      long seconds = Long.parseLong(value);
      return Duration.ofSeconds(seconds);
    } catch (NumberFormatException ignored) {
      // no-op
    }

    // Attempt to parse as a date
    try {
      ZonedDateTime retryTime = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
      ZonedDateTime now = ZonedDateTime.now(clock);
      Duration duration = Duration.between(now, retryTime);

      // Negative duration means the date is already in the past so run now
      return duration.isNegative() ? Duration.ZERO : duration;

    } catch (DateTimeParseException ignored) {
      // no-op
    }

    return Duration.ZERO;
  }
}
