package com.weather.sdk.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link Weather} aggregate root.
 *
 * <p>Test strategy: TDD approach with focus on validation, domain coordination, and contract
 * verification. Uses nested test classes for logical grouping.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("Weather Aggregate Root")
class WeatherTest {

  @Nested
  @DisplayName("Constructor and Validation")
  class ConstructionTests {

    @Test
    @DisplayName("should create Weather with valid parameters")
    void shouldCreateWeatherWithValidParameters() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act
      Weather weather =
          new Weather(
              condition, temperature, wind, visibility, sunTimes, cityName, timezone, datetime);

      // Assert
      assertThat(weather.condition()).isEqualTo(condition);
      assertThat(weather.temperature()).isEqualTo(temperature);
      assertThat(weather.wind()).isEqualTo(wind);
      assertThat(weather.visibility()).isEqualTo(visibility);
      assertThat(weather.sunTimes()).isEqualTo(sunTimes);
      assertThat(weather.cityName()).isEqualTo(cityName);
      assertThat(weather.timezone()).isEqualTo(timezone);
      assertThat(weather.datetime()).isEqualTo(datetime);
    }

    @Test
    @DisplayName("should reject null condition")
    void shouldRejectNullCondition() {
      // Arrange
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      null, temperature, wind, visibility, sunTimes, cityName, timezone, datetime))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("condition must not be null");
    }

    @Test
    @DisplayName("should reject null temperature")
    void shouldRejectNullTemperature() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      condition, null, wind, visibility, sunTimes, cityName, timezone, datetime))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("temperature must not be null");
    }

    @Test
    @DisplayName("should reject null wind")
    void shouldRejectNullWind() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      condition,
                      temperature,
                      null,
                      visibility,
                      sunTimes,
                      cityName,
                      timezone,
                      datetime))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("wind must not be null");
    }

    @Test
    @DisplayName("should reject null visibility")
    void shouldRejectNullVisibility() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      condition, temperature, wind, null, sunTimes, cityName, timezone, datetime))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("visibility must not be null");
    }

    @Test
    @DisplayName("should reject null sunTimes")
    void shouldRejectNullSunTimes() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      condition, temperature, wind, visibility, null, cityName, timezone, datetime))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("sunTimes must not be null");
    }

    @Test
    @DisplayName("should reject null timezone")
    void shouldRejectNullTimezone() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      condition, temperature, wind, visibility, sunTimes, cityName, null, datetime))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("timezone must not be null");
    }

    @Test
    @DisplayName("should reject null datetime")
    void shouldRejectNullDatetime() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      condition, temperature, wind, visibility, sunTimes, cityName, timezone, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("datetime must not be null");
    }

    @Test
    @DisplayName("should reject null cityName")
    void shouldRejectNullCityName() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      condition, temperature, wind, visibility, sunTimes, null, timezone, datetime))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank cityName: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank cityName")
    void shouldRejectBlankCityName(String invalidCityName) {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act & Assert
      assertThatThrownBy(
              () ->
                  new Weather(
                      condition,
                      temperature,
                      wind,
                      visibility,
                      sunTimes,
                      invalidCityName,
                      timezone,
                      datetime))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }
  }

  @Nested
  @DisplayName("Domain Method - localDatetime()")
  class LocalDatetimeTests {

    @Test
    @DisplayName("should convert to local datetime with positive offset")
    void shouldConvertToLocalDatetimeWithPositiveOffset() {
      // Arrange
      Instant utcDatetime = Instant.parse("2024-01-15T12:00:00Z");
      TimezoneOffset timezone = TimezoneOffset.ofHours(3); // UTC+3
      Weather weather = createWeather(utcDatetime, timezone);

      // Act
      ZonedDateTime local = weather.localDatetime();

      // Assert
      assertThat(local.toInstant()).isEqualTo(utcDatetime);
      assertThat(local.getOffset().getTotalSeconds()).isEqualTo(10800); // 3 hours
      assertThat(local.getHour()).isEqualTo(15); // 12:00 + 3:00 = 15:00
    }

    @Test
    @DisplayName("should convert to local datetime with negative offset")
    void shouldConvertToLocalDatetimeWithNegativeOffset() {
      // Arrange
      Instant utcDatetime = Instant.parse("2024-01-15T12:00:00Z");
      TimezoneOffset timezone = TimezoneOffset.ofHours(-5); // UTC-5
      Weather weather = createWeather(utcDatetime, timezone);

      // Act
      ZonedDateTime local = weather.localDatetime();

      // Assert
      assertThat(local.toInstant()).isEqualTo(utcDatetime);
      assertThat(local.getOffset().getTotalSeconds()).isEqualTo(-18000); // -5 hours
      assertThat(local.getHour()).isEqualTo(7); // 12:00 - 5:00 = 07:00
    }

    @Test
    @DisplayName("should convert to local datetime with zero offset")
    void shouldConvertToLocalDatetimeWithZeroOffset() {
      // Arrange
      Instant utcDatetime = Instant.parse("2024-01-15T12:00:00Z");
      TimezoneOffset timezone = TimezoneOffset.ofHours(0); // UTC
      Weather weather = createWeather(utcDatetime, timezone);

      // Act
      ZonedDateTime local = weather.localDatetime();

      // Assert
      assertThat(local.toInstant()).isEqualTo(utcDatetime);
      assertThat(local.getOffset().getTotalSeconds()).isEqualTo(0);
      assertThat(local.getHour()).isEqualTo(12); // 12:00 + 0:00 = 12:00
    }

    @Test
    @DisplayName("should handle maximum timezone offset (UTC+14)")
    void shouldHandleMaxTimezoneOffset() {
      // Arrange
      Instant utcDatetime = Instant.parse("2024-01-15T10:00:00Z");
      TimezoneOffset timezone = TimezoneOffset.ofHours(14); // UTC+14 (Line Islands)
      Weather weather = createWeather(utcDatetime, timezone);

      // Act
      ZonedDateTime local = weather.localDatetime();

      // Assert
      assertThat(local.toInstant()).isEqualTo(utcDatetime);
      assertThat(local.getOffset().getTotalSeconds()).isEqualTo(50400); // 14 hours
      assertThat(local.getHour()).isEqualTo(0); // 10:00 + 14:00 = 24:00 = 00:00 next day
      assertThat(local.getDayOfMonth()).isEqualTo(16); // Next day
    }

    @Test
    @DisplayName("should handle minimum timezone offset (UTC-12)")
    void shouldHandleMinTimezoneOffset() {
      // Arrange
      Instant utcDatetime = Instant.parse("2024-01-15T10:00:00Z");
      TimezoneOffset timezone = TimezoneOffset.ofHours(-12); // UTC-12
      Weather weather = createWeather(utcDatetime, timezone);

      // Act
      ZonedDateTime localDatetime = weather.localDatetime();

      // Assert
      assertThat(localDatetime.toInstant()).isEqualTo(utcDatetime);
      assertThat(localDatetime.getHour()).isEqualTo(22); // 10:00 - 12:00 = -02:00 = 22:00 prev day
      assertThat(localDatetime.getDayOfMonth()).isEqualTo(14);
    }
  }

  @Nested
  @DisplayName("Value Object Integration")
  class ValueObjectIntegrationTests {

    @Test
    @DisplayName("should access all value objects correctly")
    void shouldAccessAllValueObjectsCorrectly() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      // Act
      Weather weather =
          new Weather(
              condition, temperature, wind, visibility, sunTimes, cityName, timezone, datetime);

      // Assert
      assertThat(weather.condition()).isEqualTo(condition);
      assertThat(weather.temperature()).isEqualTo(temperature);
      assertThat(weather.wind()).isEqualTo(wind);
      assertThat(weather.visibility()).isEqualTo(visibility);
      assertThat(weather.sunTimes()).isEqualTo(sunTimes);
      assertThat(weather.cityName()).isEqualTo(cityName);
      assertThat(weather.timezone()).isEqualTo(timezone);
      assertThat(weather.datetime()).isEqualTo(datetime);
    }
  }

  @Nested
  @DisplayName("Comparable Implementation")
  class ComparableTests {

    @Test
    @DisplayName("should order by datetime chronologically")
    void shouldOrderByDatetimeChronologically() {
      // Arrange
      Instant earlier = Instant.parse("2024-01-15T10:00:00Z");
      Instant later = Instant.parse("2024-01-15T14:00:00Z");
      Weather earlierWeather = createWeather(earlier, TimezoneOffset.ofHours(0));
      Weather laterWeather = createWeather(later, TimezoneOffset.ofHours(0));

      // Act
      int result = earlierWeather.compareTo(laterWeather);
      int reverseResult = laterWeather.compareTo(earlierWeather);

      // Assert
      assertThat(result).as("Earlier weather should be less than later weather").isNegative();
      assertThat(reverseResult)
          .as("Later weather should be greater than earlier weather")
          .isPositive();
    }

    @Test
    @DisplayName("should return zero for equal datetimes")
    void shouldReturnZeroForEqualDatetimes() {
      // Arrange
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");
      Weather weather1 = createWeather(datetime, TimezoneOffset.ofHours(0));
      Weather weather2 = createWeather(datetime, TimezoneOffset.ofHours(0));

      // Act
      int result = weather1.compareTo(weather2);

      // Assert
      assertThat(result).as("Weather with same datetime should return 0").isZero();
    }

    @Test
    @DisplayName("should be reflexive (x.compareTo(x) == 0)")
    void shouldBeReflexive() {
      // Arrange
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");
      Weather weather = createWeather(datetime, TimezoneOffset.ofHours(0));

      // Act
      int result = weather.compareTo(weather);

      // Assert
      assertThat(result).as("Weather should equal itself (reflexive)").isZero();
    }

    @Test
    @DisplayName("should be transitive (if a<b and b<c, then a<c)")
    void shouldBeTransitive() {
      // Arrange
      Instant earliest = Instant.parse("2024-01-15T10:00:00Z");
      Instant middle = Instant.parse("2024-01-15T12:00:00Z");
      Instant latest = Instant.parse("2024-01-15T14:00:00Z");

      Weather weatherEarliest = createWeather(earliest, TimezoneOffset.ofHours(0));
      Weather weatherMiddle = createWeather(middle, TimezoneOffset.ofHours(0));
      Weather weatherLatest = createWeather(latest, TimezoneOffset.ofHours(0));

      // Act & Assert - a < b
      assertThat(weatherEarliest.compareTo(weatherMiddle)).as("earliest < middle").isNegative();

      // Act & Assert - b < c
      assertThat(weatherMiddle.compareTo(weatherLatest)).as("middle < latest").isNegative();

      // Act & Assert - a < c (transitivity)
      assertThat(weatherEarliest.compareTo(weatherLatest))
          .as("earliest < latest (transitivity: if a<b and b<c, then a<c)")
          .isNegative();
    }

    @Test
    @DisplayName("should be consistent with equals (compareTo == 0 ⟺ equals)")
    void shouldBeConsistentWithEquals() {
      // Arrange
      Instant datetime1 = Instant.parse("2024-01-15T12:00:00Z");
      Instant datetime2 = Instant.parse("2024-01-15T14:00:00Z");
      Weather weather1 = createWeather(datetime1, TimezoneOffset.ofHours(0));
      Weather weather2 = createWeather(datetime1, TimezoneOffset.ofHours(0));
      Weather different = createWeather(datetime2, TimezoneOffset.ofHours(0));

      // Act & Assert - compareTo == 0 ⟺ equals == true
      assertThat(weather1.compareTo(weather2))
          .as("compareTo should return 0 for equal weather")
          .isZero();
      assertThat(weather1.equals(weather2))
          .as("equals should return true when compareTo returns 0")
          .isTrue();

      // Act & Assert - compareTo != 0 ⟺ equals == false
      assertThat(weather1.compareTo(different))
          .as("compareTo should return non-zero for different weather")
          .isNotZero();
      assertThat(weather1.equals(different))
          .as("equals should return false when compareTo returns non-zero")
          .isFalse();
    }

    @Test
    @DisplayName("should reject null in compareTo")
    void shouldRejectNullInCompareTo() {
      // Arrange
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");
      Weather weather = createWeather(datetime, TimezoneOffset.ofHours(0));

      // Act & Assert
      assertThatThrownBy(() -> weather.compareTo(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Cannot compare to null");
    }

    @Test
    @DisplayName("should sort list correctly")
    void shouldSortListCorrectly() {
      // Arrange
      Instant time1 = Instant.parse("2024-01-15T14:00:00Z");
      Instant time2 = Instant.parse("2024-01-15T10:00:00Z");
      Instant time3 = Instant.parse("2024-01-15T12:00:00Z");

      Weather weather1 = createWeather(time1, TimezoneOffset.ofHours(0));
      Weather weather2 = createWeather(time2, TimezoneOffset.ofHours(0));
      Weather weather3 = createWeather(time3, TimezoneOffset.ofHours(0));

      List<Weather> weatherList = new ArrayList<>(List.of(weather1, weather2, weather3));

      // Act
      Collections.sort(weatherList);

      // Assert
      assertThat(weatherList)
          .as("Weather list should be sorted chronologically by datetime")
          .extracting(Weather::datetime)
          .containsExactly(time2, time3, time1);
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("should be equal when all fields match")
    void shouldBeEqualWhenAllFieldsMatch() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      Weather weather1 =
          new Weather(
              condition, temperature, wind, visibility, sunTimes, cityName, timezone, datetime);
      Weather weather2 =
          new Weather(
              condition, temperature, wind, visibility, sunTimes, cityName, timezone, datetime);

      // Act & Assert
      assertThat(weather1).isEqualTo(weather2);
      assertThat(weather1.hashCode()).isEqualTo(weather2.hashCode());
    }

    @Test
    @DisplayName("should be reflexive (equals itself)")
    void shouldBeReflexive() {
      // Arrange
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");
      Weather weather = createWeather(datetime, TimezoneOffset.ofHours(0));

      // Act & Assert
      assertThat(weather.equals(weather)).as("Weather should equal itself (reflexive)").isTrue();
    }

    @Test
    @DisplayName("should not be equal when condition differs")
    void shouldNotBeEqualWhenConditionDiffers() {
      // Arrange
      WeatherCondition condition1 = new WeatherCondition("Clouds", "scattered clouds");
      WeatherCondition condition2 = new WeatherCondition("Clear", "clear sky");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      String cityName = "London";
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      Weather weather1 =
          new Weather(
              condition1, temperature, wind, visibility, sunTimes, cityName, timezone, datetime);
      Weather weather2 =
          new Weather(
              condition2, temperature, wind, visibility, sunTimes, cityName, timezone, datetime);

      // Act & Assert
      assertThat(weather1).isNotEqualTo(weather2);
    }

    @Test
    @DisplayName("should not be equal when datetime differs")
    void shouldNotBeEqualWhenDatetimeDiffers() {
      // Arrange
      Instant datetime1 = Instant.parse("2024-01-15T12:00:00Z");
      Instant datetime2 = Instant.parse("2024-01-15T14:00:00Z");
      Weather weather1 = createWeather(datetime1, TimezoneOffset.ofHours(0));
      Weather weather2 = createWeather(datetime2, TimezoneOffset.ofHours(0));

      // Act & Assert
      assertThat(weather1).isNotEqualTo(weather2);
    }

    @Test
    @DisplayName("should not be equal when cityName differs")
    void shouldNotBeEqualWhenCityNameDiffers() {
      // Arrange
      WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
      Wind wind = new Wind(5.5, 180);
      Visibility visibility = new Visibility(10000);
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      TimezoneOffset timezone = TimezoneOffset.ofHours(0);
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");

      Weather weather1 =
          new Weather(
              condition, temperature, wind, visibility, sunTimes, "London", timezone, datetime);
      Weather weather2 =
          new Weather(
              condition, temperature, wind, visibility, sunTimes, "Paris", timezone, datetime);

      // Act & Assert
      assertThat(weather1).isNotEqualTo(weather2);
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      // Arrange
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");
      Weather weather = createWeather(datetime, TimezoneOffset.ofHours(0));

      // Act & Assert
      assertThat(weather.equals(null)).as("Weather should not equal null").isFalse();
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      // Arrange
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");
      Weather weather = createWeather(datetime, TimezoneOffset.ofHours(0));

      // Act & Assert
      assertThat(weather.equals("Weather")).as("Weather should not equal different type").isFalse();
    }

    @Test
    @DisplayName("should have equal hash codes when equal")
    void shouldHaveEqualHashCodesWhenEqual() {
      // Arrange
      Instant datetime = Instant.parse("2024-01-15T12:00:00Z");
      Weather weather1 = createWeather(datetime, TimezoneOffset.ofHours(0));
      Weather weather2 = createWeather(datetime, TimezoneOffset.ofHours(0));

      // Act & Assert
      assertThat(weather1).isEqualTo(weather2);
      assertThat(weather1.hashCode()).isEqualTo(weather2.hashCode());
    }
  }

  /**
   * Creates Weather with default values for testing.
   *
   * @param datetime observation timestamp
   * @param timezone timezone offset
   * @return Weather instance with sensible defaults
   */
  private Weather createWeather(Instant datetime, TimezoneOffset timezone) {
    WeatherCondition condition = new WeatherCondition("Clouds", "scattered clouds");
    Temperature temperature = Temperature.fromCelsius(20.0, 18.0);
    Wind wind = new Wind(5.5, 180);
    Visibility visibility = new Visibility(10000);
    Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
    Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
    SunTimes sunTimes = new SunTimes(sunrise, sunset);
    String cityName = "London";

    return new Weather(
        condition, temperature, wind, visibility, sunTimes, cityName, timezone, datetime);
  }
}
