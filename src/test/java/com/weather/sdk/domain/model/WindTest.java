package com.weather.sdk.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link Wind} value object.
 *
 * <p>Test strategy: TDD approach with Red-Green-Refactor cycle. Tests are written first to drive
 * the implementation.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("Wind Value Object")
class WindTest {

  /**
   * Tolerance for floating-point comparisons.
   *
   * <p>Set to 1e-9 for consistency with Temperature tests and to protect against floating-point
   * arithmetic issues.
   */
  private static final double EPSILON = 1e-9;

  @Nested
  @DisplayName("Constructor and Validation")
  class ConstructionTests {

    @Test
    @DisplayName("should create wind with valid speed and direction")
    void shouldCreateWindWithValidSpeedAndDirection() {
      // Arrange
      double speed = 5.5;
      Integer direction = 180;

      // Act
      Wind wind = new Wind(speed, direction);

      // Assert
      assertThat(wind.speed())
          .as("Wind speed should be %s m/s", speed)
          .isCloseTo(speed, within(EPSILON));

      assertThat(wind.direction())
          .as("Wind direction should be %s°", direction)
          .isEqualTo(direction);
    }

    @Test
    @DisplayName("should create wind without direction (null)")
    void shouldCreateWindWithoutDirection() {
      // Arrange
      double speed = 3.5;

      // Act
      Wind wind = new Wind(speed, null);

      // Assert
      assertThat(wind.speed())
          .as("Wind speed should match input value")
          .isCloseTo(speed, within(EPSILON));

      assertThat(wind.direction()).as("Wind direction should be null when not provided").isNull();
    }

    @Nested
    @DisplayName("Speed Validation")
    class SpeedValidationTests {

      @Test
      @DisplayName("should reject negative speed")
      void shouldRejectNegativeSpeed() {
        // Arrange
        double negativeSpeed = -5.0;
        Integer validDirection = 180;

        // Act & Assert
        assertThatThrownBy(() -> new Wind(negativeSpeed, validDirection))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind speed cannot be negative")
            .hasMessageContaining("-5.00 m/s");
      }

      @ParameterizedTest(name = "should reject non-finite speed: {0}")
      @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
      @DisplayName("should reject non-finite speed values")
      void shouldRejectNonFiniteSpeed(double invalidSpeed) {
        // Arrange
        Integer direction = 180;

        // Act & Assert
        assertThatThrownBy(() -> new Wind(invalidSpeed, direction))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind speed must be finite");
      }

      @Test
      @DisplayName("should accept zero speed (calm wind)")
      void shouldAcceptZeroSpeed() {
        // Arrange & Act
        Wind calm = new Wind(0.0, null);

        // Assert
        assertThat(calm.speed())
            .as("Calm wind should have speed of 0.0 m/s")
            .isCloseTo(0.0, within(EPSILON));
      }

      @ParameterizedTest(name = "should accept valid speed: {0} m/s")
      @ValueSource(doubles = {0.5, 5.0, 10.0, 25.0, 50.0})
      @DisplayName("should accept typical valid speed values")
      void shouldAcceptValidSpeed(double validSpeed) {
        // Arrange
        Integer direction = 90;

        // Act
        Wind wind = new Wind(validSpeed, direction);

        // Assert
        assertThat(wind.speed())
            .as("Valid speed %.1f m/s should be accepted and stored correctly", validSpeed)
            .isCloseTo(validSpeed, within(EPSILON));
      }
    }

    @Nested
    @DisplayName("Direction Validation")
    class DirectionValidationTests {

      @ParameterizedTest(name = "should reject invalid direction: {0}°")
      @ValueSource(ints = {-1, -45, -90, 361, 400, 720})
      @DisplayName("should reject invalid direction values")
      void shouldRejectInvalidDirection(int invalidDirection) {
        // Arrange
        double validSpeed = 5.0;

        // Act & Assert
        assertThatThrownBy(() -> new Wind(validSpeed, invalidDirection))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind direction must be between")
            .hasMessageContaining("degrees");
      }

      @ParameterizedTest(name = "should accept boundary direction: {0}°")
      @ValueSource(ints = {0, 360})
      @DisplayName("should accept boundary directions (North)")
      void shouldAcceptBoundaryDirections(int boundaryDirection) {
        // Arrange
        double validSpeed = 5.0;

        // Act
        Wind wind = new Wind(validSpeed, boundaryDirection);

        // Assert
        assertThat(wind.direction())
            .as("Boundary direction %d° should be accepted and stored correctly", boundaryDirection)
            .isEqualTo(boundaryDirection);
      }

      @ParameterizedTest(name = "should accept valid direction: {0}°")
      @ValueSource(ints = {45, 90, 135, 180, 225, 270, 315})
      @DisplayName("should accept valid compass directions")
      void shouldAcceptValidDirections(int validDirection) {
        // Arrange
        double validSpeed = 5.0;

        // Act
        Wind wind = new Wind(validSpeed, validDirection);

        // Assert
        assertThat(wind.direction())
            .as("Valid direction %d° should be accepted and stored correctly", validDirection)
            .isEqualTo(validDirection);
      }
    }
  }

  @Nested
  @DisplayName("Domain Behavior")
  class DomainBehavior {

    @Test
    @DisplayName("should return true when direction is present")
    void shouldHaveDirectionWhenProvided() {
      // Arrange
      double validSpeed = 5.0;
      Integer validDirection = 270;

      // Act
      Wind wind = new Wind(validSpeed, validDirection);

      // Assert
      assertThat(wind.hasDirection())
          .as("Wind with direction=%d° should return true for hasDirection()", validDirection)
          .isTrue();
    }

    @Test
    @DisplayName("should return false when direction is null")
    void shouldNotHaveDirectionWhenNull() {
      // Arrange
      double validSpeed = 5.0;

      // Act
      Wind wind = new Wind(validSpeed, null);

      // Assert
      assertThat(wind.hasDirection())
          .as("Wind without direction (null) should return false for hasDirection()")
          .isFalse();
    }

    @Nested
    @DisplayName("Wind Strength Classification")
    class WindStrengthIntegrationTests {

      /**
       * Verifies integration between Wind and WindStrength enum.
       *
       * <p>These are smoke tests to ensure {@code Wind.strength()} correctly delegates to {@code
       * WindStrength.fromSpeed()}. Detailed classification logic is tested in {@link
       * WindStrengthTest}.
       *
       * <p>Coverage: 5 representative executions covering low/mid/high/extreme ranges
       */
      @ParameterizedTest(name = "Wind({0} m/s) should have strength {1}")
      @CsvSource({
        "0.1,   CALM", // Low speed - Beaufort 0
        "5.5,   MODERATE_BREEZE", // Mid speed - Beaufort 4 (boundary)
        "18.0,  GALE", // High speed - Beaufort 8 (commonly referenced)
        "30.0,  VIOLENT_STORM", // Very high - Beaufort 11
        "100.0, HURRICANE" // Extreme - Beaufort 12
      })
      @DisplayName("should correctly delegate to WindStrength classification")
      void shouldDelegateToWindStrength(double speed, WindStrength expected) {

        // Arrange
        Wind wind = new Wind(speed, null);

        // Act
        WindStrength result = wind.strength();

        // Assert
        assertThat(result)
            .as("Wind with speed %.1f m/s should have strength %s", speed, expected)
            .isEqualTo(expected);
      }

      /**
       * Verifies that wind strength classification is independent of direction.
       *
       * <p>The Beaufort Scale is based solely on wind speed, not direction. This test ensures the
       * implementation correctly ignores direction when classifying wind strength, validating an
       * important domain rule.
       *
       * <p>Coverage: 1 execution (validates direction independence)
       */
      @Test
      @DisplayName("strength should not depend on direction")
      void strengthShouldNotDependOnDirection() {

        // Arrange - same speed, different directions
        double speed = 18.0; // GALE (Beaufort 8)
        Wind northWind = new Wind(speed, 0); // North
        Wind southWind = new Wind(speed, 180); // South
        Wind noDirection = new Wind(speed, null);

        // Act
        WindStrength northStrength = northWind.strength();
        WindStrength southStrength = southWind.strength();
        WindStrength noDirectionStrength = noDirection.strength();

        // Assert - all should be equal
        assertThat(northStrength)
            .as("Wind strength should not depend on direction")
            .isEqualTo(WindStrength.GALE)
            .isEqualTo(southStrength)
            .isEqualTo(noDirectionStrength);
      }
    }
  }

  @Nested
  @DisplayName("Comparable Implementation")
  class ComparableTests {

    /**
     * Verifies that Wind objects are compared by speed first (primary sort key).
     *
     * <p>When speeds differ, the Wind with higher speed is considered "greater" regardless of
     * direction. This ensures natural ordering where stronger winds sort after weaker winds.
     *
     * <p>Coverage: 2 executions (bidirectional comparison for symmetry verification)
     */
    @Test
    @DisplayName("should compare by speed first (primary key)")
    void shouldCompareBySpeedFirst() {

      // Arrange - different speeds, different directions
      Wind weaker = new Wind(10.0, 90); // 10 m/s, East
      Wind stronger = new Wind(20.0, 180); // 20 m/s, South

      // Act
      int weakerToStronger = weaker.compareTo(stronger);
      int strongerToWeaker = stronger.compareTo(weaker);

      // Assert - speed determines order (direction ignored)
      assertThat(weakerToStronger).as("Wind(10 m/s) should be less than Wind(20 m/s)").isNegative();

      assertThat(strongerToWeaker)
          .as("Wind(20 m/s) should be greater than Wind(10 m/s)")
          .isPositive();
    }

    /**
     * Verifies that Wind objects with equal speed are compared by direction (secondary key).
     *
     * <p>When speeds are equal, direction determines the ordering:
     *
     * <ul>
     *   <li>null direction is considered "less than" any non-null direction
     *   <li>Non-null directions are compared numerically (0° < 90° < 180°)
     * </ul>
     *
     * <p>Coverage: 4 comparisons (null handling + direction ordering)
     */
    @Test
    @DisplayName("should compare by direction when speeds are equal (secondary key)")
    void shouldCompareByDirectionWhenSpeedEqual() {

      // Arrange - same speed (10 m/s), different directions
      Wind nullDirection = new Wind(10.0, null);
      Wind north = new Wind(10.0, 0); // North (0°)
      Wind east = new Wind(10.0, 90); // East (90°)
      Wind south = new Wind(10.0, 180); // South (180°)

      // Act - null vs non-null
      int nullVsEast = nullDirection.compareTo(east);
      int eastVsNull = east.compareTo(nullDirection);

      // Act - direction value comparisons
      int northVsEast = north.compareTo(east);
      int eastVsSouth = east.compareTo(south);

      // Assert - null < non-null
      assertThat(nullVsEast)
          .as("Wind(10.0, null) should be less than Wind(10.0, 90°)")
          .isNegative();

      assertThat(eastVsNull)
          .as("Wind(10.0, 90°) should be greater than Wind(10.0, null)")
          .isPositive();

      // Assert - direction ordering (0° < 90° < 180°)
      assertThat(northVsEast).as("Wind(10.0, 0°) should be less than Wind(10.0, 90°)").isNegative();

      assertThat(eastVsSouth)
          .as("Wind(10.0, 90°) should be less than Wind(10.0, 180°)")
          .isNegative();
    }

    /**
     * Verifies that compareTo() is consistent with equals().
     *
     * <p>The Comparable contract requires: {@code (x.compareTo(y) == 0) == x.equals(y)}. This test
     * ensures Wind's natural ordering is consistent with equality, critical for correct behavior in
     * sorted collections (TreeSet, TreeMap).
     *
     * <p>Coverage: 3 scenarios (equal non-null, equal null, different)
     */
    @Test
    @DisplayName("compareTo should be consistent with equals")
    void compareToShouldBeConsistentWithEquals() {

      // Arrange - all test objects
      Wind wind1 = new Wind(10.0, 90);
      Wind wind2 = new Wind(10.0, 90);

      Wind wind3 = new Wind(15.0, null);
      Wind wind4 = new Wind(15.0, null);

      Wind wind5 = new Wind(10.0, 90);
      Wind wind6 = new Wind(10.0, 180);

      // Act - all comparisons
      int compareEqual = wind1.compareTo(wind2);
      boolean equalsEqual = wind1.equals(wind2);

      int compareNullEqual = wind3.compareTo(wind4);
      boolean equalsNullEqual = wind3.equals(wind4);

      int compareDifferent = wind5.compareTo(wind6);
      boolean equalsDifferent = wind5.equals(wind6);

      // Assert - consistency for equal objects (non-null)
      assertThat(compareEqual)
          .as("Equal Wind objects (non-null) should have compareTo() == 0")
          .isZero();

      assertThat(equalsEqual)
          .as("equals() should return true when compareTo() == 0 (non-null)")
          .isTrue();

      // Assert - consistency for equal objects (null direction)
      assertThat(compareNullEqual)
          .as("Equal Wind objects (null) should have compareTo() == 0")
          .isZero();

      assertThat(equalsNullEqual)
          .as("equals() should return true when compareTo() == 0 (null)")
          .isTrue();

      // Assert - consistency for different objects
      assertThat(compareDifferent)
          .as("Different Wind objects should have compareTo() != 0")
          .isNotZero();

      assertThat(equalsDifferent)
          .as("equals() should return false when compareTo() != 0")
          .isFalse();
    }

    /**
     * Verifies that compareTo() rejects null argument with NullPointerException.
     *
     * <p>The Comparable contract requires throwing NullPointerException when comparing to null.
     * This ensures proper behavior and prevents silent failures in sorting operations.
     *
     * <p>Coverage: 1 execution (null safety validation)
     */
    @Test
    @DisplayName("should reject null argument in compareTo")
    void shouldRejectNullInCompareTo() {

      // Arrange
      Wind wind = new Wind(10.0, 90);

      // Act & Assert
      assertThatThrownBy(() -> wind.compareTo(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Cannot compare to null");
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    /**
     * Verifies that Wind objects are equal when all fields match.
     *
     * <p>Tests equals() contract: reflexive, symmetric, and hashCode consistency. Since Wind is a
     * Java record, transitivity is guaranteed by the compiler-generated implementation, so we focus
     * on practical scenarios that could fail in custom implementations.
     *
     * <p>Coverage: 3 scenarios (non-null, null direction, reflexive)
     */
    @Test
    @DisplayName("should be equal when all fields match")
    void shouldBeEqualWhenAllFieldsMatch() {

      // Arrange - equal objects with non-null direction
      Wind wind1 = new Wind(10.0, 90);
      Wind wind2 = new Wind(10.0, 90);

      // Arrange - equal objects with null direction
      Wind windNull1 = new Wind(15.0, null);
      Wind windNull2 = new Wind(15.0, null);

      // Act & Assert - Reflexive (equals() contract requirement)
      assertThat(wind1.equals(wind1)).as("Wind should equal itself (reflexive)").isTrue();

      // Act & Assert - Symmetric (equals() contract requirement)
      assertThat(wind1.equals(wind2)).as("wind1.equals(wind2) should be true").isTrue();
      assertThat(wind2.equals(wind1)).as("wind2.equals(wind1) should be true (symmetric)").isTrue();

      // Act & Assert - hashCode consistency
      assertThat(wind1.hashCode())
          .as("Equal objects must have equal hashCodes")
          .isEqualTo(wind2.hashCode());

      // Act & Assert - null direction case
      assertThat(windNull1.equals(windNull2))
          .as("Wind with null direction should equal when speed matches")
          .isTrue();
      assertThat(windNull1.hashCode())
          .as("Equal objects (null direction) must have equal hashCodes")
          .isEqualTo(windNull2.hashCode());
    }

    /**
     * Verifies that Wind objects are NOT equal when fields differ.
     *
     * <p>Tests negative cases for equals() contract, including field differences, null safety, and
     * type safety. This ensures proper behavior in collections and prevents false positives.
     *
     * <p>Coverage: 5 scenarios (speed diff, direction diff, null vs non-null, null safety, type
     * safety)
     */
    @Test
    @DisplayName("should not be equal when fields differ")
    void shouldNotBeEqualWhenFieldsDiffer() {

      // Arrange
      Wind baseline = new Wind(10.0, 90);

      // Field differences
      Wind differentSpeed = new Wind(15.0, 90);
      Wind differentDirection = new Wind(10.0, 180);
      Wind differentBoth = new Wind(15.0, 180);

      // Null vs non-null direction (edge case)
      Wind nullDirection = new Wind(10.0, null);

      // Act & Assert
      // Different speed
      assertThat(baseline.equals(differentSpeed))
          .as("Wind with different speed should NOT be equal")
          .isFalse();

      // Different direction
      assertThat(baseline.equals(differentDirection))
          .as("Wind with different direction should NOT be equal")
          .isFalse();

      // Both fields different
      assertThat(baseline.equals(differentBoth))
          .as("Wind with both fields different should NOT be equal")
          .isFalse();

      // Null vs non-null direction (symmetric)
      assertThat(nullDirection.equals(baseline))
          .as("Wind with null direction should NOT equal Wind with non-null direction")
          .isFalse();

      assertThat(baseline.equals(nullDirection))
          .as("Wind with non-null direction should NOT equal Wind with null direction (symmetric)")
          .isFalse();

      // Null safety
      assertThat(baseline.equals(null)).as("Wind should NOT equal null").isFalse();

      // Type safety - different type categories
      assertThat(baseline.equals("Wind(10.0, 90)"))
          .as("Wind should NOT equal different type (String)")
          .isFalse();

      assertThat(baseline.equals(Integer.valueOf(10)))
          .as("Wind should NOT equal different type (Integer)")
          .isFalse();
    }
  }
}
