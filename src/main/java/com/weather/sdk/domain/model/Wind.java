package com.weather.sdk.domain.model;

/**
 * Immutable value object representing wind speed and direction.
 *
 * <p>Wind speed is measured in meters per second (m/s) following OpenWeatherMap API standard.
 * Direction is optional and measured in meteorological degrees (0-360), where:
 *
 * <ul>
 *   <li>0° / 360° = North
 *   <li>90° = East
 *   <li>180° = South
 *   <li>270° = West
 * </ul>
 *
 * @param speed wind speed in m/s (must be >= 0 and finite)
 * @param direction wind direction in degrees (0-360, or null if not available)
 * @since 1.0.0
 */
public record Wind(double speed, Integer direction) implements Comparable<Wind> {

  /** Minimum valid wind speed (0 m/s - calm). */
  private static final double MIN_SPEED = 0.0;

  /** Minimum valid direction (0 directions - North). */
  private static final int MIN_DIRECTION = 0;

  /** Maximum valid direction (360 directions - North). */
  private static final int MAX_DIRECTION = 360;

  /**
   * Compact constructor with validation.
   *
   * @throws IllegalArgumentException if speed or direction is invalid
   */
  public Wind {
    validateSpeed(speed);
    if (direction != null) {
      validateDirection(direction);
    }
  }

  public boolean hasDirection() {
    return this.direction != null;
  }

  /**
   * Returns the wind strength classification based on the Beaufort Scale.
   *
   * <p>This method classifies the wind speed into one of 13 categories ranging from CALM (Beaufort
   * 0) to HURRICANE (Beaufort 12) according to the official WMO Beaufort Scale.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Wind calm = new Wind(0.1, null);
   * Wind gale = new Wind(18.0, 270);
   *
   * calm.strength();  // Returns WindStrength.CALM (Beaufort 0)
   * gale.strength();  // Returns WindStrength.GALE (Beaufort 8)
   *
   * // Use strength methods
   * if (gale.strength().isGale()) {
   *     alert("Gale warning!");
   * }
   * }</pre>
   *
   * @return wind strength category based on Beaufort Scale
   * @see WindStrength
   * @since 1.0.0
   */
  public WindStrength strength() {
    return WindStrength.fromSpeed(this.speed);
  }

  @Override
  public int compareTo(Wind other) {
    if (other == null) {
      throw new NullPointerException("Cannot compare to null");
    }
    int speedComparison = Double.compare(this.speed, other.speed);
    if (speedComparison != 0) {
      return speedComparison;
    }
    if (this.direction == null && other.direction != null) {
      return -1;
    }
    if (this.direction != null && other.direction == null) {
      return 1;
    }
    if (this.direction != null && other.direction != null) {
      return Integer.compare(this.direction, other.direction);
    }
    return 0;
  }

  /**
   * Validates wind speed.
   *
   * <p>Wind speed must be:
   *
   * <ul>
   *   <li>Finite (not NaN or Infinity)
   *   <li>Non-negative (>= 0 m/s)
   * </ul>
   *
   * @param speed wind speed in m/s
   * @throws IllegalArgumentException if speed is invalid
   */
  private static void validateSpeed(double speed) {
    if (!Double.isFinite(speed)) {
      throw new IllegalArgumentException(
          String.format("Wind speed must be finite, got: %s", speed));
    }
    if (speed < MIN_SPEED) {
      throw new IllegalArgumentException(
          String.format("Wind speed cannot be negative, got: %.2f m/s", speed));
    }
  }

  /**
   * Validates wind direction.
   *
   * <p>Direction must be in range [0, 360] degrees.
   *
   * @param direction wind direction in degrees
   * @throws IllegalArgumentException if direction is out of valid range
   */
  private static void validateDirection(int direction) {
    if (direction < MIN_DIRECTION || direction > MAX_DIRECTION) {
      throw new IllegalArgumentException(
          String.format(
              "Wind direction must be between %d and %d degrees, got: %d",
              MIN_DIRECTION, MAX_DIRECTION, direction));
    }
  }
}
