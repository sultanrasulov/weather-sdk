package com.weather.sdk.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable value object representing sun event times (sunrise and sunset).
 *
 * <p>Represents astronomical events that occur at specific moments in time. All times are stored as
 * {@link Instant} objects in UTC.
 *
 * <p><strong>Immutability:</strong> This is a Value Object - once created, it cannot be modified.
 *
 * <p><strong>Validation policy:</strong> Sunrise must occur before sunset. Both values must be
 * non-null.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Create from API response (Unix timestamp format)
 * Instant sunrise = Instant.ofEpochSecond(1675751262);
 * Instant sunset = Instant.ofEpochSecond(1675787560);
 * SunTimes sunTimes = new SunTimes(sunrise, sunset);
 *
 * // Calculate daylight duration
 * Duration daylight = sunTimes.daylightDuration();  // ~10 hours
 *
 * // Check if current time is daytime
 * if (sunTimes.isDaytime(Instant.now())) {
 *     System.out.println("It's daytime!");
 * }
 * }</pre>
 *
 * @param sunrise the instant when the sun rises above the horizon
 * @param sunset the instant when the sun sets below the horizon
 * @since 1.0.0
 */
public record SunTimes(Instant sunrise, Instant sunset) implements Comparable<SunTimes> {

  /**
   * Compact constructor with validation.
   *
   * @throws NullPointerException if sunrise or sunset is null
   * @throws IllegalArgumentException if sunrise occurs after sunset
   */
  public SunTimes {
    Objects.requireNonNull(sunrise, "sunrise must not be null");
    Objects.requireNonNull(sunset, "sunset must not be null");

    if (sunrise.isAfter(sunset)) {
      throw new IllegalArgumentException(
          String.format(
              "Sunrise must occur before sunset, got sunrise=%s, sunset=%s", sunrise, sunset));
    }
  }

  /**
   * Calculates the duration of daylight between sunrise and sunset.
   *
   * <p>The duration represents the length of time the sun is above the horizon for a given location
   * and date.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * SunTimes sunTimes = new SunTimes(
   *     Instant.parse("2024-01-15T06:30:00Z"),
   *     Instant.parse("2024-01-15T17:45:00Z")
   * );
   *
   * Duration daylight = sunTimes.daylightDuration();
   * long hours = daylight.toHours();  // 11
   * }</pre>
   *
   * @return the duration of daylight (always positive or zero)
   * @since 1.0.0
   */
  public Duration daylightDuration() {
    return Duration.between(this.sunrise, this.sunset);
  }

  /**
   * Checks if the given instant falls within daytime hours.
   *
   * <p>An instant is considered daytime if it occurs strictly after sunrise and strictly before
   * sunset. The boundaries (exact sunrise and sunset moments) are not considered daytime.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * SunTimes sunTimes = new SunTimes(
   *     Instant.parse("2024-01-15T06:30:00Z"),
   *     Instant.parse("2024-01-15T17:45:00Z")
   * );
   *
   * Instant noon = Instant.parse("2024-01-15T12:00:00Z");
   * boolean isDay = sunTimes.isDaytime(noon);  // true
   *
   * Instant night = Instant.parse("2024-01-15T22:00:00Z");
   * boolean isNight = !sunTimes.isDaytime(night);  // true
   * }</pre>
   *
   * @param instant the instant to check (must not be null)
   * @return true if instant is between sunrise and sunset (exclusive)
   * @throws NullPointerException if instant is null
   * @since 1.0.0
   */
  public boolean isDaytime(Instant instant) {
    Objects.requireNonNull(instant, "instant must not be null");
    return instant.isAfter(this.sunrise) && instant.isBefore(this.sunset);
  }

  /**
   * Compares this SunTimes with another for ordering.
   *
   * <p>Comparison is performed first by sunrise time (primary key), then by sunset time (secondary
   * key) if sunrise times are equal. Earlier sunrise times are considered "less than" later sunrise
   * times.
   *
   * <p>This natural ordering is consistent with {@link #equals(Object)}.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * SunTimes winter = new SunTimes(
   *     Instant.parse("2024-01-15T07:30:00Z"),
   *     Instant.parse("2024-01-15T16:45:00Z")
   * );
   *
   * SunTimes summer = new SunTimes(
   *     Instant.parse("2024-07-15T05:30:00Z"),
   *     Instant.parse("2024-07-15T20:45:00Z")
   * );
   *
   * int result = winter.compareTo(summer);  // positive (winter sunrise is later)
   * }</pre>
   *
   * @param other the SunTimes to compare to (must not be null)
   * @return negative if this &lt; other, zero if equal, positive if this &gt; other
   * @throws NullPointerException if other is null
   * @since 1.0.0
   */
  @Override
  public int compareTo(SunTimes other) {
    Objects.requireNonNull(other, "Cannot compare to null");
    int sunriseComparison = this.sunrise.compareTo(other.sunrise);
    if (sunriseComparison != 0) {
      return sunriseComparison;
    }
    return this.sunset.compareTo(other.sunset);
  }
}
