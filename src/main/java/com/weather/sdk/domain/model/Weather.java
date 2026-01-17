package com.weather.sdk.domain.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Immutable aggregate root representing a weather observation for a specific location and time.
 *
 * <p>Coordinates weather measurements, solar data, and location context to provide complete weather
 * information. All timestamps are in UTC.
 *
 * @param condition atmospheric conditions (category and description)
 * @param temperature actual and perceived air temperature
 * @param wind wind speed and direction
 * @param visibility atmospheric visibility distance
 * @param sunTimes sunrise and sunset times
 * @param cityName name of the city for this observation
 * @param timezone timezone offset from UTC
 * @param datetime timestamp when weather was observed (UTC)
 * @since 1.0.0
 */
public record Weather(
    WeatherCondition condition,
    Temperature temperature,
    Wind wind,
    Visibility visibility,
    SunTimes sunTimes,
    String cityName,
    TimezoneOffset timezone,
    Instant datetime)
    implements Comparable<Weather> {

  /**
   * Compact constructor with validation.
   *
   * @throws NullPointerException if any parameter is null
   * @throws IllegalArgumentException if cityName is blank
   */
  public Weather {
    Objects.requireNonNull(condition, "condition must not be null");
    Objects.requireNonNull(temperature, "temperature must not be null");
    Objects.requireNonNull(wind, "wind must not be null");
    Objects.requireNonNull(visibility, "visibility must not be null");
    Objects.requireNonNull(sunTimes, "sunTimes must not be null");
    Objects.requireNonNull(timezone, "timezone must not be null");
    Objects.requireNonNull(datetime, "datetime must not be null");

    validateCityName(cityName);
  }

  /**
   * Returns local datetime for this weather observation.
   *
   * <p>Converts the UTC datetime to local datetime using the timezone offset.
   *
   * @return local datetime (datetime + timezone offset)
   * @since 1.0.0
   */
  public ZonedDateTime localDatetime() {
    return this.datetime.atZone(this.timezone.toZoneOffset());
  }

  /**
   * Compares this weather observation with another for ordering.
   *
   * <p>Weather observations are ordered chronologically by datetime. Earlier observations are
   * considered "less than" later observations.
   *
   * @param other the weather observation to compare to (must not be null)
   * @return negative if this &lt; other, zero if equal, positive if this &gt; other
   * @throws NullPointerException if other is null
   * @since 1.0.0
   */
  @Override
  public int compareTo(Weather other) {
    Objects.requireNonNull(other, "Cannot compare to null");
    return this.datetime.compareTo(other.datetime);
  }

  /**
   * Validates city name.
   *
   * @param cityName the city name to validate
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   */
  private static void validateCityName(String cityName) {
    Objects.requireNonNull(cityName, "cityName must not be null");
    if (cityName.isBlank()) {
      throw new IllegalArgumentException("cityName must not be blank");
    }
  }
}
