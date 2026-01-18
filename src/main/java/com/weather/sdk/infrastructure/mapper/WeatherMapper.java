package com.weather.sdk.infrastructure.mapper;

import com.weather.sdk.domain.model.SunTimes;
import com.weather.sdk.domain.model.Temperature;
import com.weather.sdk.domain.model.TimezoneOffset;
import com.weather.sdk.domain.model.Visibility;
import com.weather.sdk.domain.model.Weather;
import com.weather.sdk.domain.model.WeatherCondition;
import com.weather.sdk.domain.model.Wind;
import com.weather.sdk.infrastructure.dto.OpenWeatherApiResponse;
import java.time.Instant;
import java.util.Objects;

/**
 * Maps OpenWeatherMap API response DTOs to domain model objects.
 *
 * <p>Handles conversion from infrastructure layer DTOs to rich domain value objects with validation
 * and type safety.
 *
 * @since 1.0.0
 */
public final class WeatherMapper {

  /** Private constructor to prevent instantiation. */
  private WeatherMapper() {
    throw new AssertionError("Utility class - do not instantiate");
  }

  /**
   * Maps OpenWeatherMap API response to domain Weather object.
   *
   * @param response API response to map
   * @return Weather domain object
   * @throws NullPointerException if response or any required nested object is null
   * @throws IllegalArgumentException if weather list is empty or data is invalid
   */
  public static Weather map(OpenWeatherApiResponse response) {
    Objects.requireNonNull(response, "response must not be null");

    return new Weather(
        mapWeatherCondition(response),
        mapTemperature(response.main()),
        mapWind(response.wind()),
        mapVisibility(response.visibility()),
        mapSunTimes(response.sys()),
        response.name(),
        mapTimezoneOffset(response.timezone()),
        mapDatetime(response.dt()));
  }

  /**
   * Maps weather condition from API response to domain model.
   *
   * <p>Extracts the first weather condition from the response array as per API specification.
   *
   * @param response API response containing weather conditions
   * @return WeatherCondition domain object
   * @throws NullPointerException if response is null
   * @throws IllegalArgumentException if weather list is null or empty
   */
  private static WeatherCondition mapWeatherCondition(OpenWeatherApiResponse response) {
    Objects.requireNonNull(response, "response must not be null");

    if (response.weather() == null || response.weather().isEmpty()) {
      throw new IllegalArgumentException("weather list must not be null or empty");
    }

    OpenWeatherApiResponse.WeatherDto weatherDto = response.weather().get(0);
    return new WeatherCondition(weatherDto.main(), weatherDto.description());
  }

  /**
   * Maps temperature data from API DTO to domain model.
   *
   * @param main temperature data from API
   * @return Temperature domain object
   * @throws NullPointerException if main is null
   */
  private static Temperature mapTemperature(OpenWeatherApiResponse.MainDto main) {
    Objects.requireNonNull(main, "main must not be null");
    return new Temperature(main.temp(), main.feelsLike());
  }

  /**
   * Maps wind data from API DTO to domain model.
   *
   * @param wind wind data from API
   * @return Wind domain object
   * @throws NullPointerException if wind is null
   */
  private static Wind mapWind(OpenWeatherApiResponse.WindDto wind) {
    Objects.requireNonNull(wind, "wind must not be null");
    return new Wind(wind.speed(), wind.deg());
  }

  /**
   * Maps visibility from meters to domain model.
   *
   * @param visibilityMeters visibility distance in meters
   * @return Visibility domain object
   */
  private static Visibility mapVisibility(int visibilityMeters) {
    return new Visibility(visibilityMeters);
  }

  /**
   * Maps sun times from API DTO to domain model.
   *
   * @param sys system data containing sunrise and sunset timestamps
   * @return SunTimes domain object
   * @throws NullPointerException if sys is null
   */
  private static SunTimes mapSunTimes(OpenWeatherApiResponse.SysDto sys) {
    Objects.requireNonNull(sys, "sys must not be null");

    Instant sunrise = Instant.ofEpochSecond(sys.sunrise());
    Instant sunset = Instant.ofEpochSecond(sys.sunset());

    return new SunTimes(sunrise, sunset);
  }

  /**
   * Maps timezone offset from seconds to domain model.
   *
   * @param offsetSeconds timezone offset in seconds from UTC
   * @return TimezoneOffset domain object
   */
  private static TimezoneOffset mapTimezoneOffset(int offsetSeconds) {
    return new TimezoneOffset(offsetSeconds);
  }

  /**
   * Maps Unix timestamp to Instant.
   *
   * @param unixTimestamp Unix timestamp in seconds
   * @return Instant representing the datetime
   */
  private static Instant mapDatetime(long unixTimestamp) {
    return Instant.ofEpochSecond(unixTimestamp);
  }
}
