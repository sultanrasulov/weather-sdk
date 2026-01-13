package com.weather.sdk.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link Visibility} value object.
 *
 * <p>Test strategy: TDD approach with Red-Green-Refactor cycle. Tests are written first to drive
 * the implementation.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("Visibility Value Object")
class VisibilityTest {

  /**
   * Tolerance for floating-point comparisons.
   *
   * <p>Set to 1e-9 for consistency with other value object tests (Temperature, Wind).
   */
  private static final double EPSILON = 1e-9;

  /**
   * Tolerance for miles conversion.
   *
   * <p>Miles use a non-integer conversion factor (1609.344), which introduces floating-point
   * rounding errors. A tolerance of 0.001 mi (≈1.6 meters) is appropriate as:
   *
   * <ul>
   *   <li>It's smaller than typical weather measurement precision (±100m for visibility)
   *   <li>It's larger than accumulated floating-point errors (~1e-4 to 1e-5 mi)
   *   <li>1.6 meters is negligible for visibility measurements (0.016% at 10km)
   * </ul>
   */
  private static final double MILES_EPSILON = 1e-3;

  @Nested
  @DisplayName("Constructor and Validation")
  class ConstructionTests {

    // HAPPY PATH - Valid inputs

    @Test
    @DisplayName("should create visibility with valid meters")
    void shouldCreateWithValidMeters() {

      // Arrange
      int meters = 5000;

      // Act
      Visibility visibility = new Visibility(meters);

      // Assert
      assertThat(visibility.meters())
          .as("Visibility meters should match input value")
          .isEqualTo(meters);
    }

    /**
     * Verifies that Visibility accepts zero meters as a valid boundary condition.
     *
     * <p>Zero visibility represents complete atmospheric opacity - a state that occurs in extreme
     * weather conditions such as dense fog, blizzards, or severe storms. This is the physical
     * minimum and a critical boundary case that must be supported.
     *
     * <p><strong>Why this boundary matters:</strong> Zero is a common source of off-by-one errors.
     * Many implementations incorrectly reject zero (using {@code meters > 0}), when the correct
     * validation rule is {@code meters >= 0} (inclusive lower boundary).
     *
     * <p><strong>Validation implications:</strong> This test ensures the validation rule is {@code
     * meters >= 0} (inclusive), not {@code meters > 0} (exclusive).
     *
     * <p>Coverage: 1 execution (lower boundary)
     */
    @Test
    @DisplayName("should accept zero visibility (boundary)")
    void shouldAcceptZeroVisibility() {

      // Arrange
      int zeroMeters = 0;

      // Act
      Visibility visibility = new Visibility(zeroMeters);

      // Assert
      assertThat(visibility.meters())
          .as("Zero visibility should be accepted as valid lower boundary")
          .isEqualTo(zeroMeters);
    }

    /**
     * Verifies that Visibility accepts typical real-world visibility values.
     *
     * <p>Tests representative values from poor to exceptional conditions, ensuring the value object
     * correctly stores various realistic measurements without artificial restrictions.
     *
     * <p><strong>Test data rationale:</strong>
     *
     * <ul>
     *   <li>100m - Very poor visibility (dense fog, heavy rain)
     *   <li>500m - Poor visibility (fog, heavy precipitation)
     *   <li>1000m - Moderate visibility (light fog, haze) - ICAO boundary
     *   <li>5000m - Good visibility (clear conditions) - ICAO boundary
     *   <li>10000m - Excellent visibility (standard clear day) - ICAO boundary
     *   <li>50000m - Exceptional visibility (rare, high altitude or very dry air)
     * </ul>
     *
     * <p>These values include ICAO visibility category boundaries (1km, 5km, 10km) to ensure
     * compatibility with future classification logic.
     *
     * <p>Coverage: 6 executions (representative sampling from poor to exceptional)
     */
    @ParameterizedTest(name = "should accept {0} meters")
    @ValueSource(ints = {100, 500, 1000, 5000, 10000, 50000})
    @DisplayName("should accept typical visibility values")
    void shouldAcceptTypicalValues(int meters) {

      // Act
      Visibility visibility = new Visibility(meters);

      // Assert
      assertThat(visibility.meters())
          .as("Visibility should accept typical value: %d meters", meters)
          .isEqualTo(meters);
    }

    /**
     * Verifies that Visibility accepts high visibility values without imposing arbitrary limits.
     *
     * <p>Very high visibility (>10km) can occur in exceptional atmospheric conditions such as high
     * altitude locations, extremely dry air, or after heavy precipitation that has cleansed the
     * atmosphere. The OpenWeatherMap API may return such values, and the value object must handle
     * them correctly.
     *
     * <p><strong>Test data rationale:</strong>
     *
     * <ul>
     *   <li>10000m (10km) - Standard "excellent" visibility threshold (ICAO reference)
     *   <li>50000m (50km) - Exceptional visibility (documented in mountain/desert regions)
     *   <li>100000m (100km) - Extreme visibility (high-altitude observations, rare but valid)
     * </ul>
     *
     * <p><strong>Why no maximum limit:</strong> Unlike temperature or wind speed which have
     * practical physical maximums, visibility has no hard upper bound. Imposing an artificial
     * maximum would reject valid API responses and reduce forward compatibility. See ADR-003 for
     * detailed architectural rationale.
     *
     * <p><strong>Real-world examples:</strong> Visibility >50km documented in Antarctic research
     * stations, Atacama Desert, and high-altitude observatories. These are rare but legitimate
     * meteorological conditions.
     *
     * <p>Coverage: 3 executions (excellent, exceptional, extreme)
     */
    @ParameterizedTest(name = "should accept high value: {0} meters")
    @ValueSource(ints = {10000, 50000, 100000})
    @DisplayName("should accept high visibility values without arbitrary limits")
    void shouldAcceptHighValues(int meters) {

      // Act
      Visibility visibility = new Visibility(meters);

      // Assert
      assertThat(visibility.meters())
          .as(
              "High visibility value (%d meters) should be accepted without arbitrary upper limit",
              meters)
          .isEqualTo(meters);
    }

    // VALIDATION - Invalid inputs (Sad Path)

    /**
     * Verifies that Visibility rejects negative meter values.
     *
     * <p>Negative visibility is physically impossible - it represents a measurement error, data
     * corruption, or API malfunction. The value object must fail fast with a clear error message to
     * prevent invalid state from propagating through the system.
     *
     * <p><strong>Why this validation matters:</strong>
     *
     * <ul>
     *   <li><strong>Physical impossibility:</strong> Visibility is a distance measurement and
     *       cannot be negative by definition
     *   <li><strong>Data integrity:</strong> Negative values indicate upstream problems that must
     *       be caught immediately
     *   <li><strong>Fail-fast principle:</strong> Invalid domain objects should never be created
     * </ul>
     *
     * <p><strong>Test data selection:</strong>
     *
     * <ul>
     *   <li>-1 - Just below zero (boundary condition, common off-by-one error)
     *   <li>-100 - Moderate negative (typical error magnitude)
     *   <li>-1000 - Large negative (significant data corruption or calculation error)
     * </ul>
     *
     * <p>Coverage: 3 executions (boundary, moderate, large negative values)
     */
    @ParameterizedTest(name = "should reject negative meters: {0}")
    @ValueSource(ints = {-1, -100, -1000})
    @DisplayName("should reject negative meters (physically impossible)")
    void shouldRejectNegativeMeters(int negativeMeters) {

      // Act & Assert
      assertThatThrownBy(() -> new Visibility(negativeMeters))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Visibility cannot be negative")
          .hasMessageContaining(String.valueOf(negativeMeters));
    }
  }

  @Nested
  @DisplayName("Factory Method - fromKilometers()")
  class FromKilometersTests {

    @ParameterizedTest(name = "fromKilometers({0}km) → {1}m")
    @CsvSource({
      "0.0,     0", // Zero boundary
      "0.5,     500", // Fractional < 1
      "1.0,     1000", // Exactly 1 km
      "5.0,     5000", // Typical
      "10.0,    10000", // Excellent
      "50.0,    50000" // Exceptional
    })
    @DisplayName("should convert kilometers to meters correctly")
    void shouldConvertKilometersToMeters(double kilometers, int expectedMeters) {
      // Act
      Visibility visibility = Visibility.fromKilometers(kilometers);

      // Assert
      assertThat(visibility.meters())
          .as("fromKilometers(%.1f) should convert to %d meters", kilometers, expectedMeters)
          .isEqualTo(expectedMeters);
    }

    @ParameterizedTest(name = "{0}km rounds to {1}m")
    @CsvSource({
      "2.5005,  2501", // Round up (half-up rounding)
      "2.4995,  2500" // Round down
    })
    @DisplayName("should round fractional kilometers correctly using half-up rounding")
    void shouldRoundKilometersCorrectly(double kilometers, int expectedMeters) {
      // Act
      Visibility visibility = Visibility.fromKilometers(kilometers);

      // Assert
      assertThat(visibility.meters())
          .as("fromKilometers(%.4f) should round to %d meters", kilometers, expectedMeters)
          .isEqualTo(expectedMeters);
    }

    @ParameterizedTest(name = "should reject non-finite kilometers: {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("should reject non-finite kilometers")
    void shouldRejectNonFiniteKilometers(double invalidKilometers) {
      // Act & Assert
      assertThatThrownBy(() -> Visibility.fromKilometers(invalidKilometers))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Visibility in kilometers must be finite");
    }

    @ParameterizedTest(name = "should reject negative: {0}km")
    @ValueSource(doubles = {-0.01, -1.0, -10.0})
    @DisplayName("should reject negative kilometer values")
    void shouldRejectNegativeKilometers(double negativeKilometers) {
      // Act & Assert
      assertThatThrownBy(() -> Visibility.fromKilometers(negativeKilometers))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Visibility in kilometers cannot be negative")
          .hasMessageContaining(String.format("%.2f km", negativeKilometers));
    }
  }

  @Nested
  @DisplayName("Factory Method - fromMiles()")
  class FromMilesTests {

    @ParameterizedTest(name = "fromMiles({0}mi) → {1}m")
    @CsvSource({
      "0.0,     0", // Zero boundary
      "0.5,     805", // 0.5 SM (fog conditions)
      "3.0,     4828", // 3 SM (METAR threshold)
      "10.0,    16093", // Excellent visibility
      "50.0,    80467" // Exceptional visibility
    })
    @DisplayName("should convert miles to meters correctly")
    void shouldConvertMilesToMeters(double miles, int expectedMeters) {
      // Act
      Visibility visibility = Visibility.fromMiles(miles);

      // Assert
      assertThat(visibility.meters())
          .as("fromMiles(%.1f) should convert to %d meters", miles, expectedMeters)
          .isEqualTo(expectedMeters);
    }

    @ParameterizedTest(name = "{0}mi rounds to {1}m")
    @CsvSource({"3.106,   4999", "5.0004,  8047"})
    @DisplayName("should round miles correctly using half-up rounding")
    void shouldRoundMilesCorrectly(double miles, int expectedMeters) {
      // Act
      Visibility visibility = Visibility.fromMiles(miles);

      // Assert
      assertThat(visibility.meters())
          .as("fromMiles(%.4f) should round to %d meters", miles, expectedMeters)
          .isEqualTo(expectedMeters);
    }

    @ParameterizedTest(name = "should reject non-finite miles: {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("should reject non-finite miles")
    void shouldRejectNonFiniteMiles(double invalidMiles) {
      // Act & Assert
      assertThatThrownBy(() -> Visibility.fromMiles(invalidMiles))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Visibility in miles must be finite");
    }

    @ParameterizedTest(name = "should reject negative: {0}mi")
    @ValueSource(doubles = {-0.01, -1.0, -10.0})
    @DisplayName("should reject negative mile values")
    void shouldRejectNegativeMiles(double negativeMiles) {
      // Act & Assert
      assertThatThrownBy(() -> Visibility.fromMiles(negativeMiles))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Visibility in miles cannot be negative")
          .hasMessageContaining(String.format("%.2f mi", negativeMiles));
    }
  }

  @Nested
  @DisplayName("Unit Conversions")
  class ConversionTests {

    @ParameterizedTest(name = "{0}m → {1}km")
    @CsvSource({
      "0,      0.0", // Zero
      "500,    0.5", // Fractional result
      "5432,   5.432", // Preserves decimal precision
      "10000,  10.0" // Excellent visibility
    })
    @DisplayName("should convert meters to kilometers correctly")
    void shouldConvertMetersToKilometers(int meters, double expectedKilometers) {
      // Arrange
      Visibility visibility = new Visibility(meters);

      // Act
      double result = visibility.kilometers();

      // Assert
      assertThat(result)
          .as("%d meters should convert to %.3f kilometers", meters, expectedKilometers)
          .isCloseTo(expectedKilometers, within(EPSILON));
    }

    @ParameterizedTest(name = "{0}m → {1}mi")
    @CsvSource({
      "0,      0.0", // Zero
      "805,    0.5", // Poor visibility (fog)
      "4828,   3.0", // METAR threshold
      "16093,  10.0" // Excellent visibility
    })
    @DisplayName("should convert meters to miles correctly")
    void shouldConvertMetersToMiles(int meters, double expectedMiles) {
      // Arrange
      Visibility visibility = new Visibility(meters);

      // Act
      double result = visibility.miles();

      // Assert
      assertThat(result)
          .as("%d meters should convert to %.1f miles", meters, expectedMiles)
          .isCloseTo(expectedMiles, within(MILES_EPSILON));
    }

    @Test
    @DisplayName("should preserve value in kilometers round-trip conversion")
    void shouldPreserveValueInKilometersRoundTrip() {
      double originalKilometers = 10.0;

      Visibility visibility = Visibility.fromKilometers(originalKilometers);
      double resultKilometers = visibility.kilometers();

      assertThat(resultKilometers)
          .as(
              "Round-trip: fromKilometers(%.1f km) → kilometers() should preserve value",
              originalKilometers)
          .isCloseTo(originalKilometers, within(EPSILON));
    }
  }

  @Nested
  @DisplayName("Comparison Methods")
  class ComparisonMethodsTests {

    @Test
    @DisplayName("isBetterThan() should return true when visibility is higher")
    void shouldReturnTrueWhenBetterThan() {
      // Arrange
      Visibility higher = new Visibility(10000);
      Visibility lower = new Visibility(5000);

      // Act
      boolean result = higher.isBetterThan(lower);

      // Assert
      assertThat(result).as("10000m should be better than 5000m").isTrue();
    }

    @Test
    @DisplayName("isBetterThan() should return false when visibility is equal or lower")
    void shouldReturnFalseWhenNotBetter() {
      // Arrange
      Visibility vis1 = new Visibility(5000);
      Visibility vis2 = new Visibility(10000);
      Visibility vis3 = new Visibility(5000);

      // Act & Assert - Lower visibility
      assertThat(vis1.isBetterThan(vis2))
          .as("Visibility of 5000m should NOT be better than 10000m")
          .isFalse();

      // Act & Assert - Equal visibility
      assertThat(vis1.isBetterThan(vis3))
          .as("Visibility of 5000m should NOT be better than itself (5000m)")
          .isFalse();
    }

    @Test
    @DisplayName("isBetterThan() should reject null")
    void shouldRejectNullInIsBetterThan() {
      // Arrange
      Visibility visibility = new Visibility(5000);

      // Act & Assert
      assertThatThrownBy(() -> visibility.isBetterThan(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("other must not be null");
    }

    @Test
    @DisplayName("isWorseThan() should return true when visibility is lower")
    void shouldReturnTrueWhenWorseThan() {
      // Arrange
      Visibility worse = new Visibility(2000);
      Visibility better = new Visibility(8000);

      // Act
      boolean result = worse.isWorseThan(better);

      // Assert
      assertThat(result).as("Visibility of 2000m should be worse than 8000m").isTrue();
    }

    @Test
    @DisplayName("isWorseThan() should return false when visibility is equal or higher")
    void shouldReturnFalseWhenNotWorse() {
      // Arrange
      Visibility vis1 = new Visibility(8000);
      Visibility vis2 = new Visibility(2000);
      Visibility vis3 = new Visibility(8000);

      // Act & Assert - Higher visibility
      assertThat(vis1.isWorseThan(vis2))
          .as("Visibility of 8000m should NOT be worse than 2000m")
          .isFalse();

      // Act & Assert - Equal visibility
      assertThat(vis1.isWorseThan(vis3))
          .as("Visibility of 8000m should NOT be worse than itself (8000m)")
          .isFalse();
    }

    @Test
    @DisplayName("isWorseThan() should reject null")
    void shouldRejectNullInIsWorseThan() {
      // Arrange
      Visibility visibility = new Visibility(5000);

      // Act & Assert
      assertThatThrownBy(() -> visibility.isWorseThan(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("other must not be null");
    }
  }

  @Nested
  @DisplayName("Comparable Implementation")
  class ComparableTests {

    @Test
    @DisplayName("should order by meters in ascending order")
    void shouldOrderByMetersAscending() {
      // Arrange
      Visibility low = new Visibility(1000);
      Visibility medium = new Visibility(5000);
      Visibility high = new Visibility(10000);

      // Act & Assert - Low < Medium
      assertThat(low.compareTo(medium)).as("1000m should be less than 5000m").isNegative();

      // Act & Assert - Medium < High
      assertThat(medium.compareTo(high)).as("5000m should be less than 10000m").isNegative();

      // Act & Assert - High > Low
      assertThat(high.compareTo(low)).as("10000m should be greater than 1000m").isPositive();
    }

    @Test
    @DisplayName("compareTo() should return zero when visibility values are equal")
    void shouldReturnZeroWhenEqual() {
      // Arrange
      Visibility vis1 = new Visibility(5000);
      Visibility vis2 = new Visibility(5000);

      // Act
      int result = vis1.compareTo(vis2);

      // Assert
      assertThat(result).as("compareTo() should return 0 for equal visibility (5000m)").isZero();
    }

    @Test
    @DisplayName("compareTo() should be reflexive (x.compareTo(x) == 0)")
    void compareToShouldBeReflexive() {
      // Arrange
      Visibility visibility = new Visibility(5000);

      // Act
      int result = visibility.compareTo(visibility);

      // Assert
      assertThat(result).as("compareTo() should return 0 when comparing to itself").isZero();
    }

    @Test
    @DisplayName("compareTo() should be consistent with equals()")
    void compareToShouldBeConsistentWithEquals() {
      // Arrange
      Visibility vis1 = new Visibility(5000);
      Visibility vis2 = new Visibility(5000);
      Visibility vis3 = new Visibility(10000);

      // Act & Assert - Equal objects
      assertThat(vis1.compareTo(vis2))
          .as("compareTo() should return 0 for equal visibility")
          .isZero();
      assertThat(vis1.equals(vis2))
          .as("equals() should return true when compareTo() returns 0")
          .isTrue();

      // Act & Assert - Different objects
      assertThat(vis1.compareTo(vis3))
          .as("compareTo() should return non-zero for different visibility")
          .isNotZero();
      assertThat(vis1.equals(vis3))
          .as("equals() should return false when compareTo() returns non-zero")
          .isFalse();
    }

    @Test
    @DisplayName("should sort visibility list correctly")
    void shouldSortListCorrectly() {
      // Arrange
      List<Visibility> visibilities =
          Arrays.asList(new Visibility(10000), new Visibility(5000), new Visibility(1000));

      // Act
      Collections.sort(visibilities);

      // Assert
      assertThat(visibilities)
          .as("List should be sorted in ascending order by meters")
          .extracting(Visibility::meters)
          .containsExactly(1000, 5000, 10000);
    }

    @Test
    @DisplayName("compareTo() should reject null")
    void shouldRejectNullInCompareTo() {
      // Arrange
      Visibility visibility = new Visibility(5000);

      // Act & Assert
      assertThatThrownBy(() -> visibility.compareTo(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Cannot compare to null");
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("should be equal when meters match")
    void shouldBeEqualWhenMetersMatch() {
      // Arrange
      Visibility vis1 = new Visibility(5000);
      Visibility vis2 = new Visibility(5000);

      // Act & Assert
      assertThat(vis1)
          .as("Visibility objects with same meters (5000) should be equal")
          .isEqualTo(vis2);
    }

    @Test
    @DisplayName("equals() should be reflexive (x.equals(x) == true)")
    void equalsShouldBeReflexive() {
      // Arrange
      Visibility visibility = new Visibility(5000);

      // Act & Assert
      assertThat(visibility.equals(visibility))
          .as("Visibility should equal itself (reflexive)")
          .isTrue();
    }

    @Test
    @DisplayName("should not be equal when meters differ")
    void shouldNotBeEqualWhenMetersDiffer() {
      // Arrange
      Visibility vis1 = new Visibility(5000);
      Visibility vis2 = new Visibility(10000);

      // Act & Assert
      assertThat(vis1)
          .as("Visibility objects with different meters (5000 vs 10000) should not be equal")
          .isNotEqualTo(vis2);
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      // Arrange
      Visibility visibility = new Visibility(5000);

      // Act & Assert
      assertThat(visibility.equals(null)).as("Visibility should not equal null").isFalse();
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      // Arrange
      Visibility visibility = new Visibility(5000);
      String differentType = "5000";

      // Act & Assert
      assertThat(visibility.equals(differentType))
          .as("Visibility should not equal different type (String)")
          .isFalse();
    }

    @Test
    @DisplayName("equal objects must have equal hashCodes")
    void equalObjectsMustHaveEqualHashCodes() {
      // Arrange
      Visibility vis1 = new Visibility(5000);
      Visibility vis2 = new Visibility(5000);

      // Act & Assert
      assertThat(vis1.equals(vis2)).as("vis1 and vis2 should be equal").isTrue();

      assertThat(vis1.hashCode())
          .as("Equal objects must have equal hashCodes")
          .isEqualTo(vis2.hashCode());
    }
  }
}
