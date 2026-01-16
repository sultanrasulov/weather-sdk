package com.weather.sdk.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link TimezoneOffset} value object.
 *
 * <p>Test strategy: TDD approach with focus on validation, factory methods, and contract
 * verification. Uses nested test classes for logical grouping.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("TimezoneOffset Value Object")
class TimezoneOffsetTest {

  @Nested
  @DisplayName("Constructor and Validation")
  class ConstructionTests {

    @Test
    @DisplayName("should create with valid offset seconds")
    void shouldCreateWithValidOffsetSeconds() {
      // Arrange
      int offsetSeconds = 3600; // UTC+1

      // Act
      TimezoneOffset offset = new TimezoneOffset(offsetSeconds);

      // Assert
      assertThat(offset.offsetSeconds()).isEqualTo(offsetSeconds);
    }

    @ParameterizedTest(name = "should accept boundary: {0}s")
    @ValueSource(ints = {-43_200, 0, 50_400})
    @DisplayName("should accept boundary offsets")
    void shouldAcceptBoundaryOffsets(int offsetSeconds) {
      // Act
      TimezoneOffset offset = new TimezoneOffset(offsetSeconds);

      // Assert
      assertThat(offset.offsetSeconds()).isEqualTo(offsetSeconds);
    }

    @ParameterizedTest(name = "should reject invalid: {0}s")
    @ValueSource(ints = {-43_201, -50_000, 50_401, 100_000})
    @DisplayName("should reject offsets outside valid range")
    void shouldRejectOffsetsOutsideValidRange(int invalidOffset) {
      // Act & Assert
      assertThatThrownBy(() -> new TimezoneOffset(invalidOffset))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Timezone offset must be between")
          .hasMessageContaining("UTC-12 to UTC+14");
    }
  }

  @Nested
  @DisplayName("Factory Method - ofHours()")
  class OfHoursTests {

    @ParameterizedTest(name = "{0}h → {1}s")
    @CsvSource({"-12, -43200", "-5, -18000", "0, 0", "1, 3600", "9, 32400", "14, 50400"})
    @DisplayName("should create from hours correctly")
    void shouldCreateFromHoursCorrectly(int hours, int expectedSeconds) {
      // Act
      TimezoneOffset offset = TimezoneOffset.ofHours(hours);

      // Assert
      assertThat(offset.offsetSeconds()).isEqualTo(expectedSeconds);
    }

    @ParameterizedTest(name = "should reject invalid hours: {0}")
    @ValueSource(ints = {-13, -20, 15, 20})
    @DisplayName("should reject invalid hours")
    void shouldRejectInvalidHours(int invalidHours) {
      // Act & Assert
      assertThatThrownBy(() -> TimezoneOffset.ofHours(invalidHours))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Timezone offset must be between")
          .hasMessageContaining("UTC-12 to UTC+14");
    }
  }

  @Nested
  @DisplayName("Conversion - toZoneOffset()")
  class ConversionTests {

    @Test
    @DisplayName("should convert to ZoneOffset")
    void shouldConvertToZoneOffset() {
      // Arrange
      TimezoneOffset offset = new TimezoneOffset(3600); // UTC+1

      // Act
      ZoneOffset zoneOffset = offset.toZoneOffset();

      // Assert
      assertThat(zoneOffset.getTotalSeconds()).isEqualTo(3600);
      assertThat(zoneOffset.getId()).isEqualTo("+01:00");
    }

    @ParameterizedTest(name = "{0}s → ZoneOffset")
    @ValueSource(ints = {-43_200, 0, 50_400})
    @DisplayName("should convert boundary offsets to ZoneOffset")
    void shouldConvertBoundaryOffsetsToZoneOffset(int offsetSeconds) {
      // Arrange
      TimezoneOffset offset = new TimezoneOffset(offsetSeconds);

      // Act
      ZoneOffset zoneOffset = offset.toZoneOffset();

      // Assert
      assertThat(zoneOffset.getTotalSeconds()).isEqualTo(offsetSeconds);
    }
  }

  @Nested
  @DisplayName("Comparable Implementation")
  class ComparableTests {

    @Test
    @DisplayName("should order by offset seconds")
    void shouldOrderByOffsetSeconds() {
      // Arrange
      TimezoneOffset west = TimezoneOffset.ofHours(-5); // UTC-5
      TimezoneOffset utc = TimezoneOffset.ofHours(0); // UTC
      TimezoneOffset east = TimezoneOffset.ofHours(9); // UTC+9

      // Act & Assert
      assertThat(west.compareTo(utc)).as("UTC-5 should be less than UTC").isNegative();
      assertThat(east.compareTo(utc)).as("UTC+9 should be greater than UTC").isPositive();
      assertThat(utc.compareTo(utc)).as("UTC should equal itself").isZero();
    }

    @Test
    @DisplayName("should return zero for equal offsets")
    void shouldReturnZeroForEqualOffsets() {
      // Arrange
      TimezoneOffset offset1 = new TimezoneOffset(3600);
      TimezoneOffset offset2 = new TimezoneOffset(3600);

      // Act
      int result = offset1.compareTo(offset2);

      // Assert
      assertThat(result).isZero();
    }

    @Test
    @DisplayName("should be reflexive (x.compareTo(x) == 0)")
    void shouldBeReflexive() {
      // Arrange
      TimezoneOffset offset = new TimezoneOffset(3600);

      // Act
      int result = offset.compareTo(offset);

      // Assert
      assertThat(result).isZero();
    }

    @Test
    @DisplayName("should be consistent with equals (compareTo == 0 ⟺ equals)")
    void shouldBeConsistentWithEquals() {
      // Arrange
      TimezoneOffset offset1 = new TimezoneOffset(3600);
      TimezoneOffset offset2 = new TimezoneOffset(3600);
      TimezoneOffset different = new TimezoneOffset(7200);

      // Act & Assert - compareTo == 0 ⟺ equals == true
      assertThat(offset1.compareTo(offset2)).isZero();
      assertThat(offset1.equals(offset2)).isTrue();

      // Act & Assert - compareTo != 0 ⟺ equals == false
      assertThat(offset1.compareTo(different)).isNotZero();
      assertThat(offset1.equals(different)).isFalse();
    }

    @Test
    @DisplayName("should reject null in compareTo")
    void shouldRejectNullInCompareTo() {
      // Arrange
      TimezoneOffset offset = new TimezoneOffset(3600);

      // Act & Assert
      assertThatThrownBy(() -> offset.compareTo(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Cannot compare to null");
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("should be equal when offset seconds match")
    void shouldBeEqualWhenOffsetSecondsMatch() {
      // Arrange
      TimezoneOffset offset1 = new TimezoneOffset(3600);
      TimezoneOffset offset2 = new TimezoneOffset(3600);

      // Act & Assert
      assertThat(offset1).isEqualTo(offset2);
      assertThat(offset1.hashCode()).isEqualTo(offset2.hashCode());
    }

    @Test
    @DisplayName("should be reflexive (equals itself)")
    void shouldBeReflexive() {
      // Arrange
      TimezoneOffset offset = new TimezoneOffset(3600);

      // Act & Assert
      assertThat(offset).isEqualTo(offset);
    }

    @Test
    @DisplayName("should not be equal when offset seconds differ")
    void shouldNotBeEqualWhenOffsetSecondsDiffer() {
      // Arrange
      TimezoneOffset offset1 = new TimezoneOffset(3600);
      TimezoneOffset offset2 = new TimezoneOffset(7200);

      // Act & Assert
      assertThat(offset1).isNotEqualTo(offset2);
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      // Arrange
      TimezoneOffset offset = new TimezoneOffset(3600);

      // Act & Assert
      assertThat(offset).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      // Arrange
      TimezoneOffset offset = new TimezoneOffset(3600);

      // Act & Assert
      assertThat(offset).isNotEqualTo("3600");
    }

    @Test
    @DisplayName("should have equal hash codes when equal")
    void shouldHaveEqualHashCodesWhenEqual() {
      // Arrange
      TimezoneOffset offset1 = new TimezoneOffset(3600);
      TimezoneOffset offset2 = new TimezoneOffset(3600);

      // Act & Assert
      assertThat(offset1).isEqualTo(offset2);
      assertThat(offset1.hashCode()).isEqualTo(offset2.hashCode());
    }
  }
}
