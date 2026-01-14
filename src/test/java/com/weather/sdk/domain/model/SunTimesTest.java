package com.weather.sdk.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link SunTimes} value object.
 *
 * <p>Test strategy: TDD approach with focus on domain logic, boundary conditions, validation, and
 * contract verification. Uses nested test classes for logical grouping.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("SunTimes Value Object")
class SunTimesTest {

  @Nested
  @DisplayName("Constructor and Validation")
  class ConstructionTests {

    @Test
    @DisplayName("should create SunTimes with valid instants")
    void shouldCreateWithValidInstants() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");

      // Act
      SunTimes sunTimes = new SunTimes(sunrise, sunset);

      // Assert
      assertThat(sunTimes.sunrise()).isEqualTo(sunrise);
      assertThat(sunTimes.sunset()).isEqualTo(sunset);
    }

    @Test
    @DisplayName("should accept equal sunrise and sunset (boundary)")
    void shouldAcceptEqualSunriseAndSunset() {
      // Arrange
      Instant sameTime = Instant.parse("2024-01-15T12:00:00Z");

      // Act
      SunTimes sunTimes = new SunTimes(sameTime, sameTime);

      // Assert
      assertThat(sunTimes.sunrise()).isEqualTo(sameTime);
      assertThat(sunTimes.sunset()).isEqualTo(sameTime);
      assertThat(sunTimes.daylightDuration()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("should reject null sunrise")
    void shouldRejectNullSunrise() {
      // Arrange
      Instant validSunset = Instant.parse("2024-01-15T17:45:00Z");

      // Act & Assert
      assertThatThrownBy(() -> new SunTimes(null, validSunset))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("sunrise must not be null");
    }

    @Test
    @DisplayName("should reject null sunset")
    void shouldRejectNullSunset() {
      // Arrange
      Instant validSunrise = Instant.parse("2024-01-15T06:30:00Z");

      // Act & Assert
      assertThatThrownBy(() -> new SunTimes(validSunrise, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("sunset must not be null");
    }

    @Test
    @DisplayName("should reject both null parameters")
    void shouldRejectBothNull() {
      // Act & Assert
      assertThatThrownBy(() -> new SunTimes(null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("should reject sunrise after sunset")
    void shouldRejectSunriseAfterSunset() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T18:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T06:00:00Z");

      // Act & Assert
      assertThatThrownBy(() -> new SunTimes(sunrise, sunset))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sunrise must occur before sunset");
    }
  }

  @Nested
  @DisplayName("Domain Logic - daylightDuration()")
  class DaylightDurationTests {

    @Test
    @DisplayName("should calculate typical daylight duration")
    void shouldCalculateTypicalDaylightDuration() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:30:00Z");
      Instant sunset = Instant.parse("2024-01-15T17:45:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);

      // Act
      Duration result = sunTimes.daylightDuration();

      // Assert
      Duration expected = Duration.ofHours(11).plusMinutes(15);
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("should return zero duration when sunrise equals sunset")
    void shouldReturnZeroWhenSunriseEqualsSunset() {
      // Arrange
      Instant sameTime = Instant.parse("2024-01-15T12:00:00Z");
      SunTimes sunTimes = new SunTimes(sameTime, sameTime);

      // Act
      Duration result = sunTimes.daylightDuration();

      // Assert
      assertThat(result).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("should calculate short winter daylight duration")
    void shouldCalculateWinterDaylight() {
      // Arrange
      Instant sunrise = Instant.parse("2024-12-21T07:30:00Z");
      Instant sunset = Instant.parse("2024-12-21T15:30:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);

      // Act
      Duration result = sunTimes.daylightDuration();

      // Assert
      Duration expected = Duration.ofHours(8);
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("should calculate long summer daylight duration")
    void shouldCalculateSummerDaylight() {
      // Arrange
      Instant sunrise = Instant.parse("2024-06-21T04:30:00Z");
      Instant sunset = Instant.parse("2024-06-21T20:30:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);

      // Act
      Duration result = sunTimes.daylightDuration();

      // Assert
      Duration expected = Duration.ofHours(16);
      assertThat(result).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("Daytime Detection")
  class DaytimeDetectionTests {

    @Test
    @DisplayName("should return true for time between sunrise and sunset")
    void shouldReturnTrueForTimeBetweenSunriseAndSunset() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      Instant noon = Instant.parse("2024-01-15T12:00:00Z");

      // Act
      boolean result = sunTimes.isDaytime(noon);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false for time before sunrise")
    void shouldReturnFalseForTimeBeforeSunrise() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      Instant earlyMorning = Instant.parse("2024-01-15T04:00:00Z");

      // Act
      boolean result = sunTimes.isDaytime(earlyMorning);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false for time after sunset")
    void shouldReturnFalseForTimeAfterSunset() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);
      Instant evening = Instant.parse("2024-01-15T20:00:00Z");

      // Act
      boolean result = sunTimes.isDaytime(evening);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false for exact sunrise time (boundary)")
    void shouldReturnFalseForExactSunriseTime() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);

      // Act
      boolean result = sunTimes.isDaytime(sunrise);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false for exact sunset time (boundary)")
    void shouldReturnFalseForExactSunsetTime() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);

      // Act
      boolean result = sunTimes.isDaytime(sunset);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should reject null instant")
    void shouldRejectNullInstant() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes = new SunTimes(sunrise, sunset);

      // Act & Assert
      assertThatThrownBy(() -> sunTimes.isDaytime(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("instant must not be null");
    }
  }

  @Nested
  @DisplayName("Comparable Implementation")
  class ComparableTests {

    @Test
    @DisplayName("should order by earlier sunrise first")
    void shouldOrderByEarlierSunriseFirst() {
      // Arrange
      SunTimes earlier =
          new SunTimes(
              Instant.parse("2024-01-15T06:00:00Z"), Instant.parse("2024-01-15T18:00:00Z"));
      SunTimes later =
          new SunTimes(
              Instant.parse("2024-01-15T07:00:00Z"), Instant.parse("2024-01-15T19:00:00Z"));

      // Act
      int result = earlier.compareTo(later);

      // Assert
      assertThat(result).isNegative();
    }

    @Test
    @DisplayName("should order by sunset when sunrises are equal")
    void shouldOrderBySunsetWhenSunrisesEqual() {
      // Arrange
      Instant sameSunrise = Instant.parse("2024-01-15T06:00:00Z");
      SunTimes earlierSunset = new SunTimes(sameSunrise, Instant.parse("2024-01-15T17:00:00Z"));
      SunTimes laterSunset = new SunTimes(sameSunrise, Instant.parse("2024-01-15T18:00:00Z"));

      // Act
      int result = earlierSunset.compareTo(laterSunset);

      // Assert
      assertThat(result).isNegative();
    }

    @Test
    @DisplayName("should return zero for equal SunTimes")
    void shouldReturnZeroForEqualSunTimes() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes1 = new SunTimes(sunrise, sunset);
      SunTimes sunTimes2 = new SunTimes(sunrise, sunset);

      // Act
      int result = sunTimes1.compareTo(sunTimes2);

      // Assert
      assertThat(result).isZero();
    }

    @Test
    @DisplayName("should be reflexive (x.compareTo(x) == 0)")
    void shouldBeReflexive() {
      // Arrange
      SunTimes sunTimes =
          new SunTimes(
              Instant.parse("2024-01-15T06:00:00Z"), Instant.parse("2024-01-15T18:00:00Z"));

      // Act
      int result = sunTimes.compareTo(sunTimes);

      // Assert
      assertThat(result).isZero();
    }

    @Test
    @DisplayName("should be consistent with equals (compareTo == 0 ⟺ equals)")
    void shouldBeConsistentWithEquals() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes1 = new SunTimes(sunrise, sunset);
      SunTimes sunTimes2 = new SunTimes(sunrise, sunset);
      SunTimes different =
          new SunTimes(
              Instant.parse("2024-01-15T07:00:00Z"), Instant.parse("2024-01-15T19:00:00Z"));

      // Act & Assert - compareTo == 0 ⟺ equals == true
      assertThat(sunTimes1.compareTo(sunTimes2)).isZero();
      assertThat(sunTimes1.equals(sunTimes2)).isTrue();

      // Act & Assert - compareTo != 0 ⟺ equals == false
      assertThat(sunTimes1.compareTo(different)).isNotZero();
      assertThat(sunTimes1.equals(different)).isFalse();
    }

    @Test
    @DisplayName("should sort list correctly")
    void shouldSortListCorrectly() {
      // Arrange
      SunTimes winter =
          new SunTimes(
              Instant.parse("2024-12-21T07:30:00Z"), Instant.parse("2024-12-21T15:30:00Z"));
      SunTimes spring =
          new SunTimes(
              Instant.parse("2024-03-20T06:00:00Z"), Instant.parse("2024-03-20T18:00:00Z"));
      SunTimes summer =
          new SunTimes(
              Instant.parse("2024-06-21T04:30:00Z"), Instant.parse("2024-06-21T20:30:00Z"));

      List<SunTimes> unsorted = new ArrayList<>(List.of(winter, summer, spring));

      // Act
      Collections.sort(unsorted);

      // Assert
      assertThat(unsorted).containsExactly(spring, summer, winter);
    }

    @Test
    @DisplayName("should reject null in compareTo")
    void shouldRejectNullInCompareTo() {
      // Arrange
      SunTimes sunTimes =
          new SunTimes(
              Instant.parse("2024-01-15T06:00:00Z"), Instant.parse("2024-01-15T18:00:00Z"));

      // Act & Assert
      assertThatThrownBy(() -> sunTimes.compareTo(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Cannot compare to null");
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("should be equal when both fields match")
    void shouldBeEqualWhenBothFieldsMatch() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes1 = new SunTimes(sunrise, sunset);
      SunTimes sunTimes2 = new SunTimes(sunrise, sunset);

      // Act & Assert
      assertThat(sunTimes1).isEqualTo(sunTimes2);
      assertThat(sunTimes1.hashCode()).isEqualTo(sunTimes2.hashCode());
    }

    @Test
    @DisplayName("should be reflexive (equals itself)")
    void shouldBeReflexive() {
      // Arrange
      SunTimes sunTimes =
          new SunTimes(
              Instant.parse("2024-01-15T06:00:00Z"), Instant.parse("2024-01-15T18:00:00Z"));

      // Act & Assert
      assertThat(sunTimes).isEqualTo(sunTimes);
    }

    @Test
    @DisplayName("should not be equal when sunrise differs")
    void shouldNotBeEqualWhenSunriseDiffers() {
      // Arrange
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes1 = new SunTimes(Instant.parse("2024-01-15T06:00:00Z"), sunset);
      SunTimes sunTimes2 = new SunTimes(Instant.parse("2024-01-15T07:00:00Z"), sunset);

      // Act & Assert
      assertThat(sunTimes1).isNotEqualTo(sunTimes2);
    }

    @Test
    @DisplayName("should not be equal when sunset differs")
    void shouldNotBeEqualWhenSunsetDiffers() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      SunTimes sunTimes1 = new SunTimes(sunrise, Instant.parse("2024-01-15T18:00:00Z"));
      SunTimes sunTimes2 = new SunTimes(sunrise, Instant.parse("2024-01-15T19:00:00Z"));

      // Act & Assert
      assertThat(sunTimes1).isNotEqualTo(sunTimes2);
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      // Arrange
      SunTimes sunTimes =
          new SunTimes(
              Instant.parse("2024-01-15T06:00:00Z"), Instant.parse("2024-01-15T18:00:00Z"));

      // Act & Assert
      assertThat(sunTimes).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      // Arrange
      SunTimes sunTimes =
          new SunTimes(
              Instant.parse("2024-01-15T06:00:00Z"), Instant.parse("2024-01-15T18:00:00Z"));

      // Act & Assert
      assertThat(sunTimes).isNotEqualTo("SunTimes");
    }

    @Test
    @DisplayName("should have equal hash codes when equal")
    void shouldHaveEqualHashCodesWhenEqual() {
      // Arrange
      Instant sunrise = Instant.parse("2024-01-15T06:00:00Z");
      Instant sunset = Instant.parse("2024-01-15T18:00:00Z");
      SunTimes sunTimes1 = new SunTimes(sunrise, sunset);
      SunTimes sunTimes2 = new SunTimes(sunrise, sunset);

      // Act & Assert
      assertThat(sunTimes1.hashCode()).isEqualTo(sunTimes2.hashCode());
    }
  }
}
