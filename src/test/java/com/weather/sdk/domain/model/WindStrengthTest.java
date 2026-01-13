package com.weather.sdk.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link WindStrength} enum.
 *
 * <p>Test strategy: Verify Beaufort Scale classification accuracy, boundary conditions, and gap
 * handling using parameterized tests following TDD best practices.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("WindStrength Enum (Beaufort Scale)")
class WindStrengthTest {

  /**
   * Tolerance for floating-point comparisons.
   *
   * <p>Set to 1e-9 for consistency with other value object tests.
   */
  private static final double EPSILON = 1e-9;

  // ============================================================================
  // FACTORY METHOD TESTS
  // ============================================================================

  @Nested
  @DisplayName("Factory Method - fromSpeed()")
  class FactoryMethodTests {

    /**
     * Verifies classification of all 13 Beaufort Scale categories (0-12).
     *
     * <p>Uses mid-range values from each category to verify core logic. Boundary cases are tested
     * separately in {@code shouldClassifyBoundaryValues}.
     *
     * <p>Coverage: 13 executions, ~60% of fromSpeed() method
     */
    @ParameterizedTest(name = "{0} m/s → {1} (Beaufort {2})")
    @CsvSource({
      // CALM (0.0-0.2 m/s)
      "0.1,   CALM,           0",
      // LIGHT_AIR (0.3-1.5 m/s)
      "0.9,   LIGHT_AIR,      1",
      // LIGHT_BREEZE (1.6-3.3 m/s)
      "2.5,   LIGHT_BREEZE,   2",
      // GENTLE_BREEZE (3.4-5.4 m/s)
      "4.4,   GENTLE_BREEZE,  3",
      // MODERATE_BREEZE (5.5-7.9 m/s)
      "6.7,   MODERATE_BREEZE, 4",
      // FRESH_BREEZE (8.0-10.7 m/s)
      "9.4,   FRESH_BREEZE,   5",
      // STRONG_BREEZE (10.8-13.8 m/s)
      "12.3,  STRONG_BREEZE,  6",
      // NEAR_GALE (13.9-17.1 m/s)
      "15.5,  NEAR_GALE,      7",
      // GALE (17.2-20.7 m/s)
      "19.0,  GALE,           8",
      // STRONG_GALE (20.8-24.4 m/s)
      "22.6,  STRONG_GALE,    9",
      // STORM (24.5-28.4 m/s)
      "26.5,  STORM,         10",
      // VIOLENT_STORM (28.5-32.6 m/s)
      "30.5,  VIOLENT_STORM, 11",
      // HURRICANE (32.7+ m/s)
      "40.0,  HURRICANE,     12"
    })
    @DisplayName("should classify all Beaufort categories correctly")
    void shouldClassifyAllBeaufortCategories(
        double speed, WindStrength expected, int expectedBeaufortNumber) {

      // Act
      WindStrength result = WindStrength.fromSpeed(speed);

      // Assert - verify both enum value and Beaufort number
      assertThat(result).as("Speed %.1f m/s should be %s", speed, expected).isEqualTo(expected);

      assertThat(result.beaufortNumber())
          .as("Beaufort number for %.1f m/s should be %d", speed, expectedBeaufortNumber)
          .isEqualTo(expectedBeaufortNumber);
    }

    /**
     * Verifies handling of the gap between CALM (max 0.2) and LIGHT_AIR (min 0.3).
     *
     * <p>The official Beaufort Scale has a 0.1 m/s gap (0.21-0.29). Implementation uses 0.25 m/s as
     * threshold: values below are classified as CALM, values at/above as LIGHT_AIR.
     *
     * <p>Note: This gap is smaller than typical wind measurement error (±0.5 m/s), so the exact
     * threshold choice has minimal practical impact.
     *
     * <p>Coverage: 6 executions (gap boundaries + official LIGHT_AIR boundary)
     */
    @ParameterizedTest(name = "{0} m/s → {1} (gap handling)")
    @CsvSource({
      // Below threshold → CALM
      "0.21,  CALM", // Just after CALM upper boundary (0.2)
      "0.24,  CALM", // Just before threshold
      // At/above threshold → LIGHT_AIR
      "0.25,  LIGHT_AIR", // Exactly at threshold (critical boundary)
      "0.26,  LIGHT_AIR", // Just after threshold
      "0.29,  LIGHT_AIR", // End of gap
      "0.30,  LIGHT_AIR" // LIGHT_AIR lower boundary (official)
    })
    @DisplayName("should handle gap between CALM and LIGHT_AIR using 0.25 threshold")
    void shouldHandleGapBetweenCalmAndLightAir(double speed, WindStrength expected) {

      // Act
      WindStrength result = WindStrength.fromSpeed(speed);

      // Assert
      assertThat(result)
          .as(
              "Speed %.2f m/s in gap (0.21-0.30) should be classified as %s (threshold: 0.25)",
              speed, expected)
          .isEqualTo(expected);
    }

    /**
     * Verifies rejection of negative wind speed values.
     *
     * <p>Wind speed cannot be negative by physical definition. Implementation must throw {@link
     * IllegalArgumentException} with descriptive error message.
     *
     * <p>Coverage: 1 execution
     */
    @Test
    @DisplayName("should reject negative speed")
    void shouldRejectNegativeSpeed() {
      // Arrange
      double negativeSpeed = -5.0;

      // Act & Assert
      assertThatThrownBy(() -> WindStrength.fromSpeed(negativeSpeed))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Wind speed cannot be negative")
          .hasMessageContaining("-5.00 m/s");
    }

    /**
     * Verifies rejection of non-finite wind speed values (NaN, Infinity).
     *
     * <p>Non-finite values cannot represent physical wind measurements. Implementation must throw
     * {@link IllegalArgumentException} for all non-finite inputs.
     *
     * <p>Coverage: 3 executions (NaN, +Infinity, -Infinity)
     */
    @ParameterizedTest(name = "should reject {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("should reject non-finite speed values")
    void shouldRejectNonFiniteSpeed(double invalidSpeed) {
      // Act & Assert
      assertThatThrownBy(() -> WindStrength.fromSpeed(invalidSpeed))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Wind speed must be finite");
    }

    /**
     * Verifies classification at boundary values for all Beaufort categories.
     *
     * <p>Tests both lower and upper boundaries of each category to ensure correct range handling.
     * This is critical for edge cases where values are exactly at category limits.
     *
     * <p>Coverage: 24 executions (12 categories × 2 boundaries, excluding HURRICANE which has no
     * upper bound)
     */
    @ParameterizedTest(name = "{0} m/s → {1} (boundary: {2})")
    @CsvSource({
      // CALM (0.0-0.2 m/s)
      "0.0,   CALM,           lower",
      "0.2,   CALM,           upper",
      // LIGHT_AIR (0.3-1.5 m/s) - gap handled separately
      "0.3,   LIGHT_AIR,      lower",
      "1.5,   LIGHT_AIR,      upper",
      // LIGHT_BREEZE (1.6-3.3 m/s)
      "1.6,   LIGHT_BREEZE,   lower",
      "3.3,   LIGHT_BREEZE,   upper",
      // GENTLE_BREEZE (3.4-5.4 m/s)
      "3.4,   GENTLE_BREEZE,  lower",
      "5.4,   GENTLE_BREEZE,  upper",
      // MODERATE_BREEZE (5.5-7.9 m/s)
      "5.5,   MODERATE_BREEZE, lower",
      "7.9,   MODERATE_BREEZE, upper",
      // FRESH_BREEZE (8.0-10.7 m/s)
      "8.0,   FRESH_BREEZE,   lower",
      "10.7,  FRESH_BREEZE,   upper",
      // STRONG_BREEZE (10.8-13.8 m/s)
      "10.8,  STRONG_BREEZE,  lower",
      "13.8,  STRONG_BREEZE,  upper",
      // NEAR_GALE (13.9-17.1 m/s)
      "13.9,  NEAR_GALE,      lower",
      "17.1,  NEAR_GALE,      upper",
      // GALE (17.2-20.7 m/s)
      "17.2,  GALE,           lower",
      "20.7,  GALE,           upper",
      // STRONG_GALE (20.8-24.4 m/s)
      "20.8,  STRONG_GALE,    lower",
      "24.4,  STRONG_GALE,    upper",
      // STORM (24.5-28.4 m/s)
      "24.5,  STORM,          lower",
      "28.4,  STORM,          upper",
      // VIOLENT_STORM (28.5-32.6 m/s)
      "28.5,  VIOLENT_STORM,  lower",
      "32.6,  VIOLENT_STORM,  upper",
      // HURRICANE (32.7+ m/s) - only lower boundary
      "32.7,  HURRICANE,      lower",
      "100.0, HURRICANE,      extreme"
    })
    @DisplayName("should classify boundary values correctly")
    void shouldClassifyBoundaryValues(double speed, WindStrength expected, String boundary) {

      // Act
      WindStrength result = WindStrength.fromSpeed(speed);

      // Assert
      assertThat(result)
          .as("Speed %.1f m/s (%s boundary) should be classified as %s", speed, boundary, expected)
          .isEqualTo(expected);
    }

    /**
     * Verifies that all WindStrength enum constants have correct Beaufort properties.
     *
     * <p>Each WindStrength category must have correct Beaufort number, minimum speed, and maximum
     * speed according to the official WMO Beaufort Scale specification.
     *
     * <p>Coverage: 13 executions (one per Beaufort category 0-12)
     */
    @ParameterizedTest(name = "{0}: Beaufort {1}, range [{2}, {3}] m/s")
    @CsvSource({
      // Category,      Beaufort, minSpeed, maxSpeed
      "CALM,           0,        0.0,      0.2",
      "LIGHT_AIR,      1,        0.3,      1.5",
      "LIGHT_BREEZE,   2,        1.6,      3.3",
      "GENTLE_BREEZE,  3,        3.4,      5.4",
      "MODERATE_BREEZE, 4,       5.5,      7.9",
      "FRESH_BREEZE,   5,        8.0,      10.7",
      "STRONG_BREEZE,  6,        10.8,     13.8",
      "NEAR_GALE,      7,        13.9,     17.1",
      "GALE,           8,        17.2,     20.7",
      "STRONG_GALE,    9,        20.8,     24.4",
      "STORM,          10,       24.5,     28.4",
      "VIOLENT_STORM,  11,       28.5,     32.6",
      "HURRICANE,      12,       32.7,     1.7976931348623157E308" // Double.MAX_VALUE
    })
    @DisplayName("should have correct Beaufort properties for all categories")
    void shouldHaveCorrectBeaufortProperties(
        WindStrength category, int expectedBeaufort, double expectedMin, double expectedMax) {

      // Act & Assert - beaufortNumber()
      assertThat(category.beaufortNumber())
          .as("%s should have Beaufort number %d", category, expectedBeaufort)
          .isEqualTo(expectedBeaufort);

      // Act & Assert - minSpeed()
      assertThat(category.minSpeed())
          .as("%s should have minimum speed %.1f m/s", category, expectedMin)
          .isCloseTo(expectedMin, within(EPSILON));

      // Act & Assert - maxSpeed()
      assertThat(category.maxSpeed())
          .as("%s should have maximum speed %.1f m/s", category, expectedMax)
          .isCloseTo(expectedMax, within(EPSILON));
    }

    /**
     * Verifies classification of extremely high wind speeds as HURRICANE.
     *
     * <p>Ensures the implementation correctly handles edge cases beyond normal meteorological
     * ranges, including maximum representable double values.
     *
     * <p>Coverage: 1 execution (multiple assertions for robustness)
     */
    @Test
    @DisplayName("should classify extremely high speed as HURRICANE")
    void shouldHandleExtremelyHighSpeed() {
      // Act & Assert - very high speed (F5 tornado: ~140 m/s, testing ~7x extreme)
      assertThat(WindStrength.fromSpeed(1000.0))
          .as("Speed 1000.0 m/s should be classified as HURRICANE")
          .isEqualTo(WindStrength.HURRICANE);

      // Act & Assert - theoretical maximum (edge case for floating-point operations)
      assertThat(WindStrength.fromSpeed(Double.MAX_VALUE))
          .as("Double.MAX_VALUE should be classified as HURRICANE")
          .isEqualTo(WindStrength.HURRICANE);
    }
  }

  // ============================================================================
  // CONVENIENCE METHODS TESTS
  // ============================================================================

  @Nested
  @DisplayName("Convenience Methods (isXxx)")
  class ConvenienceMethodsTests {

    /**
     * Verifies that convenience methods return true for their corresponding WindStrength values.
     *
     * <p>Each WindStrength enum has a convenience method (e.g., {@code isCalm()}, {@code isGale()})
     * that should return true only for that specific category. This test verifies the positive
     * cases.
     *
     * <p>Coverage: 13 executions (one per WindStrength category)
     */
    @ParameterizedTest(name = "{0}.{1}() should return true")
    @CsvSource({
      "CALM,           isCalm",
      "LIGHT_AIR,      isLightAir",
      "LIGHT_BREEZE,   isLightBreeze",
      "GENTLE_BREEZE,  isGentleBreeze",
      "MODERATE_BREEZE, isModerateBreeze",
      "FRESH_BREEZE,   isFreshBreeze",
      "STRONG_BREEZE,  isStrongBreeze",
      "NEAR_GALE,      isNearGale",
      "GALE,           isGale",
      "STRONG_GALE,    isStrongGale",
      "STORM,          isStorm",
      "VIOLENT_STORM,  isViolentStorm",
      "HURRICANE,      isHurricane"
    })
    @DisplayName("convenience methods should return true for matching strength")
    void convenienceMethodsShouldReturnTrue(WindStrength strength, String methodName) {

      // Act
      boolean result = invokeConvenienceMethod(strength, methodName);

      // Assert
      assertThat(result).as("%s.%s() should return true", strength, methodName).isTrue();
    }

    /**
     * Verifies that convenience methods return false for non-matching WindStrength values.
     *
     * <p>This test ensures that convenience methods correctly return false when called on
     * WindStrength values that don't match the method name. For example, {@code GALE.isCalm()}
     * should return false.
     *
     * <p>Coverage: 13 executions (testing one mismatch per category for representative coverage)
     */
    @ParameterizedTest(name = "{0}.{1}() should return false")
    @CsvSource({
      "CALM,           isGale", // CALM is not GALE
      "LIGHT_AIR,      isCalm", // LIGHT_AIR is not CALM
      "LIGHT_BREEZE,   isHurricane", // LIGHT_BREEZE is not HURRICANE
      "GENTLE_BREEZE,  isStorm", // GENTLE_BREEZE is not STORM
      "MODERATE_BREEZE, isGale", // MODERATE_BREEZE is not GALE
      "FRESH_BREEZE,   isCalm", // FRESH_BREEZE is not CALM
      "STRONG_BREEZE,  isHurricane", // STRONG_BREEZE is not HURRICANE
      "NEAR_GALE,      isGale", // NEAR_GALE is not GALE
      "GALE,           isNearGale", // GALE is not NEAR_GALE
      "STRONG_GALE,    isStorm", // STRONG_GALE is not STORM
      "STORM,          isStrongGale", // STORM is not STRONG_GALE
      "VIOLENT_STORM,  isHurricane", // VIOLENT_STORM is not HURRICANE
      "HURRICANE,      isCalm" // HURRICANE is not CALM
    })
    @DisplayName("convenience methods should return false for non-matching strength")
    void convenienceMethodsShouldReturnFalse(WindStrength strength, String methodName) {

      // Act
      boolean result = invokeConvenienceMethod(strength, methodName);

      // Assert
      assertThat(result).as("%s.%s() should return false", strength, methodName).isFalse();
    }

    /**
     * Helper method to invoke convenience methods by name using reflection.
     *
     * <p>This helper is shared between positive and negative test cases for convenience methods.
     *
     * @param strength the WindStrength instance
     * @param methodName the convenience method name (e.g., "isCalm")
     * @return the result of the method invocation
     * @throws AssertionError if method invocation fails
     */
    private boolean invokeConvenienceMethod(WindStrength strength, String methodName) {
      try {
        var method = WindStrength.class.getMethod(methodName);
        return (boolean) method.invoke(strength);
      } catch (Exception e) {
        throw new AssertionError(
            String.format("Failed to invoke %s() on %s", methodName, strength), e);
      }
    }
  }
}
