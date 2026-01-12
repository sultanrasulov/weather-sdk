package com.weather.sdk.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link Temperature} value object.
 *
 * <p>Test strategy: TDD approach with focus on business logic, edge cases, and validation. Uses
 * parameterized tests to reduce duplication while maintaining high confidence.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("Temperature Value Object")
class TemperatureTest {

  /**
   * Tolerance for floating-point comparisons.
   *
   * <p>Set to 1e-9 (one billionth) which is appropriate for:
   *
   * <ul>
   *   <li>Simple arithmetic operations (no cumulative error)
   *   <li>Double precision floating-point (~15-17 significant digits)
   *   <li>Temperature conversions without iterative calculations
   * </ul>
   *
   * <p><strong>Context:</strong> 1e-9 Kelvin ≈ 0.000001°C, far below human perception threshold
   * (~0.5°C).
   */
  private static final double EPSILON = 1e-9;

  @Nested
  @DisplayName("Constructor and Factory Methods")
  class ConstructionTests {

    // HAPPY PATH - Valid inputs
    @Test
    @DisplayName("should create temperature with valid Kelvin values")
    void shouldCreateWithValidKelvinValues() {
      // Arrange
      double tempKelvin = 293.15; // 20°C
      double feelsLikeKelvin = 291.15; // 18°C

      // Act
      Temperature temperature = new Temperature(tempKelvin, feelsLikeKelvin);

      // Assert
      assertThat(temperature.temp()).isCloseTo(tempKelvin, within(EPSILON));
      assertThat(temperature.feelsLike()).isCloseTo(feelsLikeKelvin, within(EPSILON));
    }

    @Test
    @DisplayName("should accept absolute zero (0K) as valid boundary")
    void shouldAcceptAbsoluteZero() {
      // Arrange & Act
      Temperature temperature = new Temperature(0.0, 0.0);

      // Assert
      assertThat(temperature.temp()).isEqualTo(0.0);
      assertThat(temperature.feelsLike()).isEqualTo(0.0);
    }

    @ParameterizedTest(name = "fromCelsius({0}°C, {1}°C) → {2}K, {3}K")
    @CsvSource({
      "-273.15, -273.15, 0.0, 0.0", // Absolute zero (boundary)
      "-40.0, -40.0, 233.15, 233.15", // Special point: -40°C = -40°F
      "-20.0, -25.0, 253.15, 248.15", // Cold temperature
      "0.0, 0.0, 273.15, 273.15", // Freezing point (boundary)
      "20.0, 18.0, 293.15, 291.15", // Room temperature (typical)
      "25.5, 23.7, 298.65, 296.85", // Fractional values
      "37.0, 35.0, 310.15, 308.15", // Human body temperature
      "100.0, 100.0, 373.15, 373.15" // Boiling point (boundary)
    })
    @DisplayName("fromCelsius() should convert Celsius to Kelvin correctly")
    void fromCelsiusShouldConvertToKelvin(
        double tempCelsius,
        double feelsLikeCelsius,
        double expectedTempKelvin,
        double expectedFeelsLikeKelvin) {

      // Act
      Temperature temperature = Temperature.fromCelsius(tempCelsius, feelsLikeCelsius);

      // Assert
      assertThat(temperature.temp()).isCloseTo(expectedTempKelvin, within(EPSILON));
      assertThat(temperature.feelsLike()).isCloseTo(expectedFeelsLikeKelvin, within(EPSILON));
    }

    @ParameterizedTest(name = "fromFahrenheit({0}°F, {1}°F) → {2}K, {3}K")
    @CsvSource({
      "-459.67, -459.67, 0.0, 0.0", // Absolute zero (-459.67°F = 0K)
      "-40.0, -40.0, 233.15, 233.15", // Special point: -40°F = -40°C = 233.15K
      "32.0, 32.0, 273.15, 273.15", // Freezing point (32°F = 0°C = 273.15K)
      "68.0, 64.4, 293.15, 291.15", // Room temperature (68°F = 20°C)
      "98.6, 96.8, 310.15, 309.15", // Body temperature (98.6°F = 37°C)
      "212.0, 212.0, 373.15, 373.15" // Boiling point (212°F = 100°C = 373.15K)
    })
    @DisplayName("fromFahrenheit() should convert Fahrenheit to Kelvin correctly")
    void fromFahrenheitShouldConvertToKelvin(
        double tempFahrenheit,
        double feelsLikeFahrenheit,
        double expectedTempKelvin,
        double expectedFeelsLikeKelvin) {

      // Act
      Temperature temperature = Temperature.fromFahrenheit(tempFahrenheit, feelsLikeFahrenheit);

      // Assert
      assertThat(temperature.temp()).isCloseTo(expectedTempKelvin, within(EPSILON));
      assertThat(temperature.feelsLike()).isCloseTo(expectedFeelsLikeKelvin, within(EPSILON));
    }

    // VALIDATION - Invalid inputs (Sad Path)
    @Nested
    @DisplayName("Validation")
    class ValidationTests {

      @ParameterizedTest(name = "should reject {0}")
      @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
      @DisplayName("should reject non-finite values for temp")
      void shouldRejectNonFiniteTempValues(double invalidTemp) {
        assertThatThrownBy(() -> new Temperature(invalidTemp, 293.15))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("temp")
            .hasMessageContaining("finite");
      }

      @ParameterizedTest(name = "should reject {0}")
      @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
      @DisplayName("should reject non-finite values for feelsLike")
      void shouldRejectNonFiniteFeelsLikeValues(double invalidFeelsLike) {
        assertThatThrownBy(() -> new Temperature(293.15, invalidFeelsLike))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("feelsLike")
            .hasMessageContaining("finite");
      }

      @ParameterizedTest(name = "should reject {0}K (below absolute zero)")
      @ValueSource(doubles = {-0.01, -1.0, -100.0, -273.16})
      @DisplayName("should reject temperatures below absolute zero for both fields")
      void shouldRejectBelowAbsoluteZero(double invalidKelvin) {
        // Test temp field
        assertThatThrownBy(() -> new Temperature(invalidKelvin, 293.15))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("temp")
            .hasMessageContaining("absolute zero");

        // Test feelsLike field
        assertThatThrownBy(() -> new Temperature(293.15, invalidKelvin))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("feelsLike")
            .hasMessageContaining("absolute zero");
      }

      @ParameterizedTest(name = "fromCelsius() should reject {0}°C (below absolute zero)")
      @ValueSource(doubles = {-273.16, -300.0, -1000.0})
      @DisplayName("fromCelsius() should reject temperatures below absolute zero")
      void fromCelsiusShouldRejectBelowAbsoluteZero(double invalidCelsius) {
        assertThatThrownBy(() -> Temperature.fromCelsius(invalidCelsius, 0.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("absolute zero");
      }

      @ParameterizedTest(name = "fromFahrenheit() should reject {0}°F (below absolute zero)")
      @ValueSource(doubles = {-459.68, -500.0, -1000.0})
      @DisplayName("fromFahrenheit() should reject temperatures below absolute zero")
      void fromFahrenheitShouldRejectBelowAbsoluteZero(double invalidFahrenheit) {
        assertThatThrownBy(() -> Temperature.fromFahrenheit(invalidFahrenheit, 32.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("absolute zero");
      }
    }
  }

  @Nested
  @DisplayName("Temperature Unit Conversions")
  class ConversionTests {

    @ParameterizedTest(name = "{0}K should convert to {1}°C and {2}°F")
    @CsvSource({
      "0.0, -273.15, -459.67", // Absolute zero
      "233.15, -40.0, -40.0", // Special point: -40°C = -40°F
      "253.15, -20.0, -4.0", // Cold temperature
      "273.15, 0.0, 32.0", // Freezing point (water)
      "293.15, 20.0, 68.0", // Room temperature
      "298.15, 25.0, 77.0", // Comfortable temperature
      "310.15, 37.0, 98.6", // Human body temperature
      "373.15, 100.0, 212.0" // Boiling point (water)
    })
    @DisplayName("should convert between Kelvin, Celsius, and Fahrenheit correctly")
    void shouldConvertBetweenUnits(double kelvin, double celsius, double fahrenheit) {
      // Arrange
      Temperature temperature = new Temperature(kelvin, kelvin);

      // Act & Assert - tempCelsius()
      assertThat(temperature.tempCelsius())
          .as("Conversion from %sK to Celsius", kelvin)
          .isCloseTo(celsius, within(EPSILON));

      // Act & Assert - tempFahrenheit()
      assertThat(temperature.tempFahrenheit())
          .as("Conversion from %sK to Fahrenheit", kelvin)
          .isCloseTo(fahrenheit, within(EPSILON));

      // Act & Assert - feelsLikeCelsius()
      assertThat(temperature.feelsLikeCelsius())
          .as("FeelsLike conversion from %sK to Celsius", kelvin)
          .isCloseTo(celsius, within(EPSILON));

      // Act & Assert - feelsLikeFahrenheit()
      assertThat(temperature.feelsLikeFahrenheit())
          .as("FeelsLike conversion from %sK to Fahrenheit", kelvin)
          .isCloseTo(fahrenheit, within(EPSILON));
    }
  }

  @Nested
  @DisplayName("Domain Behavior")
  class DomainBehaviorTests {

    @ParameterizedTest(name = "temp={0}°C should be freezing={1}")
    @CsvSource({
      "-10.0,  true", // Clearly below freezing
      "-0.01,  true", // Just below freezing
      "0.0,    false", // Exactly at freezing point (boundary)
      "0.01,   false", // Just above freezing
      "20.0,   false", // Room temperature
    })
    @DisplayName("isFreezing() should detect temperatures below 0°C correctly")
    void isFreezingShouldDetectCorrectly(double tempCelsius, boolean expectedFreezing) {
      // Arrange
      Temperature temperature = Temperature.fromCelsius(tempCelsius, tempCelsius);

      // Act
      boolean result = temperature.isFreezing();

      // Assert
      assertThat(result)
          .as("Temperature %s°C should %s be freezing", tempCelsius, expectedFreezing ? "" : "NOT")
          .isEqualTo(expectedFreezing);
    }

    @ParameterizedTest(name = "temp={0}°C, feels={1}°C → wind chill: {2}")
    @CsvSource({
      "5.0,  5.0,  false", // 0K   | No difference
      "5.0,  3.0,  false", // 2K   | Below threshold
      "5.0,  2.0,  true", // 3K   | At threshold (boundary)
      "5.0,  0.0,  true", // 5K   | Above threshold
      "-5.0, -8.0, true", // 3K   | Cold weather scenario
      "5.0,  8.0,  false" // -3K  | Heat index (negative test)
    })
    @DisplayName("hasSignificantWindChill() with default threshold (3K)")
    void hasSignificantWindChillWithDefaultThreshold(
        double tempCelsius, double feelsLikeCelsius, boolean expectedWindChill) {

      // Arrange
      Temperature temperature = Temperature.fromCelsius(tempCelsius, feelsLikeCelsius);

      // Act
      boolean result = temperature.hasSignificantWindChill();

      // Assert
      assertThat(result)
          .as(
              "Temperature %.1f°C, feels like %.1f°C (diff: %.1fK) should%s have significant wind chill",
              tempCelsius,
              feelsLikeCelsius,
              tempCelsius - feelsLikeCelsius,
              expectedWindChill ? "" : " NOT")
          .isEqualTo(expectedWindChill);
    }

    @ParameterizedTest(name = "threshold={0}K, temp=5°C, feels=0°C (diff=5K) → {1}")
    @CsvSource({
      "0.0, true", // Zero threshold (edge case) - any diff > 0 detected ← NEW!
      "1.0, true", // Low threshold (1K < 5K)
      "3.0, true", // Default threshold (3K < 5K) - reference point
      "5.0, true", // Exactly at difference (5K = 5K) - boundary
      "6.0, false" // Above difference (6K > 5K) - threshold not met
    })
    @DisplayName("should detect wind chill with custom thresholds")
    void hasSignificantWindChillWithCustomThreshold(
        double thresholdKelvin, boolean expectedWindChill) {

      // Arrange
      // Fixed scenario: temp=5°C, feels=0°C → difference is exactly 5K
      Temperature temperature = Temperature.fromCelsius(5.0, 0.0);

      // Act
      boolean result = temperature.hasSignificantWindChill(thresholdKelvin);

      // Assert
      assertThat(result)
          .as(
              "Wind chill with threshold %.1fK (actual diff: 5.0K) should%s be significant",
              thresholdKelvin, expectedWindChill ? "" : " NOT")
          .isEqualTo(expectedWindChill);
    }

    @ParameterizedTest(name = "temp={0}°C, feels={1}°C → heat index: {2}")
    @CsvSource({
      "30.0, 30.0, false", // No difference (0K) - no heat index
      "30.0, 32.0, false", // 2K difference - below threshold (insignificant)
      "30.0, 33.0, true", // 3K difference - exactly at threshold (boundary)
      "30.0, 35.0, true", // 5K difference - above threshold (significant)
      "30.0, 27.0, false" // Cooler feels like (-3K) - wind chill, not heat index
    })
    @DisplayName("should detect heat index using default threshold (3K)")
    void hasSignificantHeatIndexWithDefaultThreshold(
        double tempCelsius, double feelsLikeCelsius, boolean expectedHeatIndex) {

      // Arrange
      Temperature temperature = Temperature.fromCelsius(tempCelsius, feelsLikeCelsius);

      // Act
      boolean result = temperature.hasSignificantHeatIndex();

      // Assert
      assertThat(result)
          .as(
              "Temperature %.1f°C, feels like %.1f°C (diff: %.1fK) should%s have significant heat index",
              tempCelsius,
              feelsLikeCelsius,
              feelsLikeCelsius - tempCelsius,
              expectedHeatIndex ? "" : " NOT")
          .isEqualTo(expectedHeatIndex);
    }

    @ParameterizedTest(name = "invalid threshold: {0} → should reject")
    @ValueSource(
        doubles = {
          -1.0, // Negative threshold
          -0.01, // Small negative
          Double.NaN, // Not a Number
          Double.POSITIVE_INFINITY, // Positive infinity
          Double.NEGATIVE_INFINITY // Negative infinity
        })
    @DisplayName("should reject invalid threshold values")
    void thresholdValidationShouldReject(double invalidThreshold) {
      // Arrange
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);

      // Act & Assert - hasSignificantWindChill(threshold)
      assertThatThrownBy(() -> temperature.hasSignificantWindChill(invalidThreshold))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("thresholdKelvin");

      // Act & Assert - hasSignificantHeatIndex(threshold)
      assertThatThrownBy(() -> temperature.hasSignificantHeatIndex(invalidThreshold))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("thresholdKelvin");
    }

    @Test
    @DisplayName("should compare temperatures and reject null")
    void comparisonMethodsShouldWork() {
      // Arrange
      Temperature warmer = Temperature.fromCelsius(25.0, 23.0);
      Temperature colder = Temperature.fromCelsius(15.0, 13.0);
      Temperature sameAsWarmer = Temperature.fromCelsius(25.0, 23.0);
      Temperature sameAsColder = Temperature.fromCelsius(15.0, 13.0);

      // Act & Assert - isWarmerThan()
      assertThat(warmer.isWarmerThan(colder)).as("25°C should be warmer than 15°C").isTrue();
      assertThat(colder.isWarmerThan(warmer)).as("15°C should NOT be warmer than 25°C").isFalse();
      assertThat(warmer.isWarmerThan(sameAsWarmer))
          .as("25°C should NOT be warmer than itself")
          .isFalse();

      // Act & Assert - isColderThan()
      assertThat(colder.isColderThan(warmer)).as("15°C should be colder than 25°C").isTrue();
      assertThat(warmer.isColderThan(colder)).as("25°C should NOT be colder than 15°C").isFalse();
      assertThat(colder.isColderThan(sameAsColder))
          .as("15°C should NOT be colder than itself")
          .isFalse();

      // Act & Assert - Null safety
      assertThatThrownBy(() -> warmer.isWarmerThan(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("other must not be null");
      assertThatThrownBy(() -> warmer.isColderThan(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("other must not be null");
    }
  }

  @Nested
  @DisplayName("Equality and Ordering")
  class EqualityAndOrderingTests {

    @Test
    @DisplayName("compareTo() should order by temp first, then by feelsLike")
    void compareToShouldOrderCorrectly() {
      // Arrange - Different temps
      Temperature cold = Temperature.fromCelsius(10.0, 8.0);
      Temperature warm = Temperature.fromCelsius(20.0, 18.0);
      Temperature hot = Temperature.fromCelsius(30.0, 28.0);

      // Arrange - Same temp, different feelsLike
      Temperature temp1 = Temperature.fromCelsius(15.0, 13.0);
      Temperature temp2 = Temperature.fromCelsius(15.0, 14.0);
      Temperature temp3 = Temperature.fromCelsius(15.0, 15.0);

      // Act & Assert - Order by temp (primary key)
      assertThat(cold.compareTo(warm)).as("10°C should be less than 20°C").isNegative();
      assertThat(warm.compareTo(cold)).as("20°C should be greater than 10°C").isPositive();

      // Act & Assert - Transitivity: if 10<20 and 20<30, then 10<30
      assertThat(cold.compareTo(hot)).as("Transitivity: 10°C < 30°C").isNegative();
      assertThat(hot.compareTo(cold)).as("30°C should be greater than 10°C").isPositive();

      // Act & Assert - Order by feelsLike when temp equal (secondary key)
      assertThat(temp1.compareTo(temp2))
          .as("15°C/13°C-feels should be less than 15°C/14°C-feels")
          .isNegative();
      assertThat(temp2.compareTo(temp1))
          .as("15°C/14°C-feels should be greater than 15°C/13°C-feels")
          .isPositive();
      assertThat(temp1.compareTo(temp3))
          .as("15°C/13°C-feels should be less than 15°C/15°C-feels")
          .isNegative();
      assertThat(temp3.compareTo(temp1))
          .as("15°C/15°C-feels should be greater than 15°C/13°C-feels")
          .isPositive();

      // Act & Assert - Reflexivity (self-comparison)
      assertThat(cold.compareTo(cold)).as("Temperature should be equal to itself").isZero();
      assertThat(warm.compareTo(warm)).as("Temperature should be equal to itself").isZero();
    }

    @Test
    @DisplayName("compareTo() should be consistent with equals()")
    void compareToShouldBeConsistentWithEquals() {
      // Arrange - Completely identical temperatures
      Temperature temp1 = Temperature.fromCelsius(20.0, 18.0);
      Temperature temp2 = Temperature.fromCelsius(20.0, 18.0);

      // Arrange - Different temp only
      Temperature differentTemp = Temperature.fromCelsius(25.0, 18.0);

      // Arrange - Different feelsLike only
      Temperature differentFeels = Temperature.fromCelsius(20.0, 19.0);

      // Act & Assert - compareTo() == 0 ⟺ equals() == true
      assertThat(temp1.compareTo(temp2))
          .as("compareTo() should return 0 for equal temperatures")
          .isZero();
      assertThat(temp1.equals(temp2))
          .as("equals() should return true when compareTo() returns 0")
          .isTrue();

      // Act & Assert - compareTo() != 0 ⟺ equals() == false (different temp)
      assertThat(temp1.compareTo(differentTemp))
          .as("compareTo() should return non-zero for different temperatures")
          .isNotZero();
      assertThat(temp1.equals(differentTemp))
          .as("equals() should return false when compareTo() returns non-zero")
          .isFalse();

      // Act & Assert - compareTo() != 0 ⟺ equals() == false (different feelsLike)
      assertThat(temp1.compareTo(differentFeels))
          .as("compareTo() should return non-zero for different feelsLike")
          .isNotZero();
      assertThat(temp1.equals(differentFeels))
          .as("equals() should return false when compareTo() returns non-zero")
          .isFalse();
    }

    @Test
    @DisplayName("equals() should compare both temp and feelsLike fields")
    void equalsShouldCompareAllFields() {
      // Arrange
      Temperature baseline = Temperature.fromCelsius(20.0, 18.0);
      Temperature identical = Temperature.fromCelsius(20.0, 18.0);
      Temperature differentTemp = Temperature.fromCelsius(25.0, 18.0);
      Temperature differentFeels = Temperature.fromCelsius(20.0, 19.0);
      Temperature differentBoth = Temperature.fromCelsius(25.0, 19.0);

      // Act & Assert - Identical → equals + hashCode
      assertThat(baseline.equals(identical)).as("Same values should be equal").isTrue();
      assertThat(baseline.hashCode())
          .as("Equal objects must have equal hashCodes")
          .isEqualTo(identical.hashCode());

      // Act & Assert - Different temp → not equals
      assertThat(baseline.equals(differentTemp)).as("Different temp should NOT be equal").isFalse();

      // Act & Assert - Different feelsLike → not equals
      assertThat(baseline.equals(differentFeels))
          .as("Different feelsLike should NOT be equal")
          .isFalse();

      // Act & Assert - Both different → not equals
      assertThat(baseline.equals(differentBoth))
          .as("Both fields different should NOT be equal")
          .isFalse();

      // Act & Assert - Reflexive
      assertThat(baseline.equals(baseline)).as("Should equal itself (reflexive)").isTrue();

      // Act & Assert - Null safety
      assertThat(baseline.equals(null)).as("Should not equal null").isFalse();

      // Act & Assert - Type safety
      assertThat(baseline.equals("20.0")).as("Should not equal different type").isFalse();
    }

    @Test
    @DisplayName("compareTo() should reject null")
    void compareToShouldRejectNull() {
      // Arrange
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);

      // Act & Assert
      assertThatThrownBy(() -> temperature.compareTo(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Cannot compare to null");
    }
  }

  @Nested
  @DisplayName("String representation")
  class ToStringTests {

    @Test
    @DisplayName("toString() should format with Kelvin and Celsius values")
    void toStringShouldFormatCorrectly() {
      // Arrange
      Temperature temperature = Temperature.fromCelsius(20.0, 18.0);

      // Act
      String result = temperature.toString();

      // Assert - Structure
      assertThat(result).as("toString() should contain class name").contains("Temperature[");

      // Assert - temp field
      assertThat(result).as("toString() should contain temp in Kelvin").contains("temp=293.15K");
      assertThat(result).as("toString() should contain temp in Celsius").contains("20.00°C");

      // Assert - feelsLike field
      assertThat(result)
          .as("toString() should contain feelsLike in Kelvin")
          .contains("feelsLike=291.15K");
      assertThat(result).as("toString() should contain feelsLike in Celsius").contains("18.00°C");

      // Assert - Closing bracket (structure check)
      assertThat(result).as("toString() should end with closing bracket").contains("]");
    }

    @Test
    @DisplayName("toString() should use Locale.ROOT for consistent formatting")
    void toStringShouldUseRootLocale() {
      // Arrange
      Temperature temperature = Temperature.fromCelsius(20.5, 18.7);

      // Act
      String result = temperature.toString();

      // Assert - Should use dot as decimal separator (not comma)
      assertThat(result)
          .as("toString() should use dot as decimal separator (Locale.ROOT)")
          .contains("293.65") // temp in Kelvin: 20.5 + 273.15 = 293.65
          .contains("20.50") // temp in Celsius
          .contains("291.85") // feelsLike in Kelvin: 18.7 + 273.15 = 291.85
          .contains("18.70"); // feelsLike in Celsius

      // Assert - Should NOT use comma as decimal separator
      assertThat(result)
          .as("toString() should NOT use comma as decimal separator")
          .doesNotContain("293,65")
          .doesNotContain("20,50")
          .doesNotContain("291,85")
          .doesNotContain("18,70");
    }
  }
}
