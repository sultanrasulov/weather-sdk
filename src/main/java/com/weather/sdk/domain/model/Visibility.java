package com.weather.sdk.domain.model;

import java.util.Objects;

/**
 * Immutable value object representing atmospheric visibility distance.
 *
 * <p>Visibility is the maximum distance at which an object can be clearly discerned under
 * atmospheric conditions. It is measured in meters and is a critical parameter for aviation,
 * maritime operations, and road safety.
 *
 * <p><strong>Physical meaning:</strong> Visibility depends on atmospheric conditions including fog,
 * rain, snow, haze, and air pollution. Clear conditions typically provide 10+ km visibility, while
 * dense fog can reduce it to under 100 meters.
 *
 * <p><strong>Immutability:</strong> This is a Value Object - once created, it cannot be modified.
 *
 * <p><strong>Validation policy:</strong> Visibility must be non-negative (>= 0 meters). There is no
 * upper limit as exceptional atmospheric conditions can produce extremely high visibility values.
 * See ADR-003 for architectural rationale.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Create visibility
 * Visibility good = new Visibility(10000);     // 10 km - excellent
 * Visibility poor = new Visibility(500);       // 500 m - poor
 * Visibility zero = new Visibility(0);         // 0 m - complete opacity
 *
 * // Access value
 * int meters = good.meters();  // 10000
 * }</pre>
 *
 * @param meters visibility distance in meters (must be >= 0)
 * @since 1.0.0
 */
public record Visibility(int meters) implements Comparable<Visibility> {

  /** Minimum valid visibility (0 meters - complete opacity). */
  private static final int MIN_METERS = 0;

  /** Meters per kilometer conversion factor (1 km = 1000 m). */
  private static final double METERS_PER_KILOMETER = 1000.0;

  /**
   * Meters per statute mile conversion factor (1 mi = 1609.344 m).
   *
   * <p>This is the international standard definition: 1 international mile = exactly 1609.344
   * meters. Used in aviation (METAR), maritime, and US weather reports.
   */
  private static final double METERS_PER_MILE = 1609.344;

  /**
   * Compact constructor with validation.
   *
   * <p>Validates that visibility is non-negative. Negative visibility is physically impossible and
   * indicates data corruption or API error.
   *
   * @throws IllegalArgumentException if meters is negative
   */
  public Visibility {
    validateMeters(meters);
  }

  /**
   * Creates Visibility from kilometers.
   *
   * <p>Convenience factory method for creating Visibility from kilometer values, which are more
   * commonly used in weather reports than meters.
   *
   * <p><strong>Conversion:</strong> 1 km = 1000 m. Fractional values are rounded to nearest meter
   * using half-up rounding (e.g., 2.5005 km → 2501 m).
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Visibility good = Visibility.fromKilometers(10.0);  // 10 km = 10000 m
   * Visibility poor = Visibility.fromKilometers(0.5);   // 0.5 km = 500 m
   * }</pre>
   *
   * @param kilometers visibility distance in kilometers (must be >= 0 and finite)
   * @return Visibility instance with value converted to meters
   * @throws IllegalArgumentException if kilometers is negative or non-finite
   * @since 1.0.0
   */
  public static Visibility fromKilometers(double kilometers) {
    if (!Double.isFinite(kilometers)) {
      throw new IllegalArgumentException(
          String.format("Visibility in kilometers must be finite, got: %s", kilometers));
    }
    if (kilometers < 0.0) {
      throw new IllegalArgumentException(
          String.format("Visibility in kilometers cannot be negative, got: %.2f km", kilometers));
    }

    int meters = (int) Math.round(kilometers * METERS_PER_KILOMETER);
    return new Visibility(meters);
  }

  /**
   * Creates Visibility from statute miles.
   *
   * <p>Convenience factory method for creating Visibility from mile values, commonly used in US
   * weather reports and aviation (METAR). Uses international statute mile definition, not nautical
   * miles.
   *
   * <p><strong>Conversion:</strong> 1 statute mile = 1609.344 meters (international standard).
   * Fractional values are rounded to nearest meter using half-up rounding (e.g., 3.106 mi → 4999
   * m).
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Visibility excellent = Visibility.fromMiles(10.0);  // 10 mi → 16093 m
   * Visibility good = Visibility.fromMiles(3.0);        // 3 mi → 4828 m
   * Visibility poor = Visibility.fromMiles(0.5);        // 0.5 mi → 805 m
   * }</pre>
   *
   * @param miles visibility distance in statute miles (must be >= 0 and finite)
   * @return Visibility instance with value converted to meters
   * @throws IllegalArgumentException if miles is negative or non-finite
   * @since 1.0.0
   */
  public static Visibility fromMiles(double miles) {
    if (!Double.isFinite(miles)) {
      throw new IllegalArgumentException(
          String.format("Visibility in miles must be finite, got: %s", miles));
    }
    if (miles < 0.0) {
      throw new IllegalArgumentException(
          String.format("Visibility in miles cannot be negative, got: %.2f mi", miles));
    }

    int meters = (int) Math.round(miles * METERS_PER_MILE);
    return new Visibility(meters);
  }

  /**
   * Returns visibility distance in kilometers.
   *
   * <p>Converts the stored meter value to kilometers for display or comparison purposes. This is a
   * convenience method as kilometers are more commonly used in weather reports than meters.
   *
   * <p><strong>Conversion:</strong> meters ÷ 1000 = kilometers. The result preserves decimal
   * precision without rounding (e.g., 5432 m → 5.432 km).
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Visibility vis1 = new Visibility(10000);
   * double km1 = vis1.kilometers();  // 10.0 km
   *
   * Visibility vis2 = new Visibility(5432);
   * double km2 = vis2.kilometers();  // 5.432 km
   * }</pre>
   *
   * @return visibility distance in kilometers (always >= 0.0)
   * @since 1.0.0
   */
  public double kilometers() {
    return this.meters / METERS_PER_KILOMETER;
  }

  /**
   * Returns visibility distance in statute miles.
   *
   * <p>Converts the stored meter value to statute miles for display or comparison purposes,
   * commonly used in US weather reports and aviation (METAR). Uses international statute mile
   * definition (1 mi = 1609.344 m), not nautical miles (1 nmi = 1852 m).
   *
   * <p><strong>Conversion:</strong> meters ÷ 1609.344 = statute miles. The result preserves decimal
   * precision (e.g., 4828 m → 3.0 mi).
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Visibility vis1 = new Visibility(16093);
   * double mi1 = vis1.miles();  // 10.0 mi
   *
   * Visibility vis2 = new Visibility(4828);
   * double mi2 = vis2.miles();  // 3.0 mi
   *
   * Visibility vis3 = new Visibility(805);
   * double mi3 = vis3.miles();  // 0.5 mi
   * }</pre>
   *
   * @return visibility distance in statute miles (always >= 0.0)
   * @since 1.0.0
   */
  public double miles() {
    return this.meters / METERS_PER_MILE;
  }

  /**
   * Checks if this visibility is better than another.
   *
   * <p>Better visibility means greater distance - a higher meter value indicates clearer
   * atmospheric conditions and improved visual range.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Visibility today = new Visibility(10000);    // 10 km
   * Visibility yesterday = new Visibility(5000); // 5 km
   *
   * if (today.isBetterThan(yesterday)) {
   *     System.out.println("Visibility has improved!");
   * }
   * }</pre>
   *
   * @param other the visibility to compare to (must not be null)
   * @return true if this visibility is better (greater meters value)
   * @throws NullPointerException if other is null
   * @since 1.0.0
   */
  public boolean isBetterThan(Visibility other) {
    Objects.requireNonNull(other, "other must not be null");
    return this.meters > other.meters;
  }

  /**
   * Checks if this visibility is worse than another.
   *
   * <p>Worse visibility means shorter distance - a lower meter value indicates reduced atmospheric
   * clarity and decreased visual range.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Visibility today = new Visibility(2000);     // 2 km
   * Visibility yesterday = new Visibility(8000); // 8 km
   *
   * if (today.isWorseThan(yesterday)) {
   *     System.out.println("Visibility has deteriorated!");
   * }
   * }</pre>
   *
   * @param other the visibility to compare to (must not be null)
   * @return true if this visibility is worse (lower meters value)
   * @throws NullPointerException if other is null
   * @since 1.0.0
   */
  public boolean isWorseThan(Visibility other) {
    Objects.requireNonNull(other, "other must not be null");
    return this.meters < other.meters;
  }

  /**
   * Compares this visibility with another for ordering.
   *
   * <p>Visibility objects are ordered by their meter values in ascending order. Higher visibility
   * (more meters) is considered "greater" in the natural ordering.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * List<Visibility> visibilities = Arrays.asList(
   *     new Visibility(10000),
   *     new Visibility(5000),
   *     new Visibility(1000)
   * );
   *
   * Collections.sort(visibilities);  // [1000m, 5000m, 10000m]
   * }</pre>
   *
   * @param other the visibility to compare to (must not be null)
   * @return negative if this &lt; other, zero if equal, positive if this &gt; other
   * @throws NullPointerException if other is null
   * @since 1.0.0
   */
  @Override
  public int compareTo(Visibility other) {
    Objects.requireNonNull(other, "Cannot compare to null");
    return Integer.compare(this.meters, other.meters);
  }

  /**
   * Validates visibility meters.
   *
   * <p>Visibility must be non-negative (>= 0 meters). Negative values represent physical
   * impossibility and must be rejected at construction time to enforce domain integrity.
   *
   * @param meters visibility distance in meters
   * @throws IllegalArgumentException if meters is negative
   */
  private static void validateMeters(int meters) {
    if (meters < MIN_METERS) {
      throw new IllegalArgumentException(
          String.format(
              "Visibility cannot be negative, got: %d meters (minimum: %d meters)",
              meters, MIN_METERS));
    }
  }
}
