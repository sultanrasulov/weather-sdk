package com.weather.sdk.domain.model;

import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Immutable value object representing a timezone offset from UTC.
 *
 * <p>Valid range: UTC-12 (Baker Island, Howland Island) to UTC+14 (Line Islands, Kiribati).
 *
 * @param offsetSeconds timezone offset from UTC in seconds (range: -43200 to 50400)
 * @since 1.0.0
 */
public record TimezoneOffset(int offsetSeconds) implements Comparable<TimezoneOffset> {

  /** Minimum valid timezone offset: UTC-12 hours in seconds. */
  private static final int MIN_OFFSET_SECONDS = -43_200;

  /** Maximum valid timezone offset: UTC+14 hours in seconds. */
  private static final int MAX_OFFSET_SECONDS = 50_400;

  /** Seconds per hour conversion factor. */
  private static final int SECONDS_PER_HOUR = 3600;

  /**
   * Compact constructor with validation.
   *
   * @throws IllegalArgumentException if offset is outside valid range
   */
  public TimezoneOffset {
    validateOffset(offsetSeconds);
  }

  /**
   * Creates TimezoneOffset from hours.
   *
   * <p>Convenience factory method for creating timezone offsets from hour values, which are more
   * intuitive than seconds.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * TimezoneOffset utcPlus1 = TimezoneOffset.ofHours(1);   // UTC+1 (CET)
   * TimezoneOffset utcMinus5 = TimezoneOffset.ofHours(-5); // UTC-5 (EST)
   * TimezoneOffset utc = TimezoneOffset.ofHours(0);        // UTC
   * }</pre>
   *
   * @param hours timezone offset in hours (must be in range -12 to +14)
   * @return TimezoneOffset instance
   * @throws IllegalArgumentException if hours is outside valid range
   * @since 1.0.0
   */
  public static TimezoneOffset ofHours(int hours) {
    return new TimezoneOffset(hours * SECONDS_PER_HOUR);
  }

  /**
   * Converts to Java Time API's {@link ZoneOffset}.
   *
   * <p>This method provides seamless integration with Java's datetime API for operations like
   * converting UTC instants to local datetimes.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * TimezoneOffset offset = new TimezoneOffset(3600);  // UTC+1
   * ZoneOffset zoneOffset = offset.toZoneOffset();
   *
   * Instant utc = Instant.now();
   * OffsetDateTime local = utc.atOffset(zoneOffset);
   * }</pre>
   *
   * @return ZoneOffset representing the same timezone offset
   * @since 1.0.0
   */
  public ZoneOffset toZoneOffset() {
    return ZoneOffset.ofTotalSeconds(offsetSeconds);
  }

  /**
   * Compares this timezone offset with another for ordering.
   *
   * <p>Offsets are ordered by their value: more negative (western) comes first, more positive
   * (eastern) comes last.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * TimezoneOffset newYork = TimezoneOffset.ofHours(-5);  // UTC-5
   * TimezoneOffset tokyo = TimezoneOffset.ofHours(9);     // UTC+9
   *
   * int result = newYork.compareTo(tokyo);  // negative (west < east)
   * }</pre>
   *
   * @param other the timezone offset to compare to (must not be null)
   * @return negative if this &lt; other, zero if equal, positive if this &gt; other
   * @throws NullPointerException if other is null
   * @since 1.0.0
   */
  @Override
  public int compareTo(TimezoneOffset other) {
    Objects.requireNonNull(other, "Cannot compare to null");
    return Integer.compare(this.offsetSeconds, other.offsetSeconds);
  }

  /**
   * Validates timezone offset is within valid range.
   *
   * <p>Valid range: -12 hours (UTC-12) to +14 hours (UTC+14). These are the actual timezone
   * boundaries used on Earth.
   *
   * @param offsetSeconds timezone offset in seconds
   * @throws IllegalArgumentException if offset is out of valid range
   */
  private static void validateOffset(int offsetSeconds) {
    if (offsetSeconds < MIN_OFFSET_SECONDS || offsetSeconds > MAX_OFFSET_SECONDS) {
      throw new IllegalArgumentException(
          String.format(
              "Timezone offset must be between %d and %d seconds (UTC-12 to UTC+14), got: %d",
              MIN_OFFSET_SECONDS, MAX_OFFSET_SECONDS, offsetSeconds));
    }
  }
}
