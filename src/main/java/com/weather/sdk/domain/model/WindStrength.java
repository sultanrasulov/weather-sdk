package com.weather.sdk.domain.model;

/**
 * Wind strength classification based on the official Beaufort Scale.
 *
 * <p>The Beaufort Scale is an empirical measure for describing wind speed based on observed
 * conditions at sea or on land. This enum implements the meteorological standard as defined by WMO
 * (World Meteorological Organization) and national weather services.
 *
 * <p><strong>Official Scale Reference:</strong>
 *
 * <table border="1">
 *   <tr>
 *     <th>Beaufort</th>
 *     <th>Description</th>
 *     <th>Speed (m/s)</th>
 *     <th>Land Observations</th>
 *   </tr>
 *   <tr>
 *     <td>0</td><td>Calm</td><td>0.0 - 0.2</td>
 *     <td>Smoke rises vertically; leaves are still</td>
 *   </tr>
 *   <tr>
 *     <td>1</td><td>Light Air</td><td>0.3 - 1.5</td>
 *     <td>Wind motion visible in smoke</td>
 *   </tr>
 *   <tr>
 *     <td>2</td><td>Light Breeze</td><td>1.6 - 3.3</td>
 *     <td>Wind felt on face; leaves rustle</td>
 *   </tr>
 *   <tr>
 *     <td>3</td><td>Gentle Breeze</td><td>3.4 - 5.4</td>
 *     <td>Leaves and small twigs in constant motion</td>
 *   </tr>
 *   <tr>
 *     <td>4</td><td>Moderate Breeze</td><td>5.5 - 7.9</td>
 *     <td>Dust and loose paper raised</td>
 *   </tr>
 *   <tr>
 *     <td>5</td><td>Fresh Breeze</td><td>8.0 - 10.7</td>
 *     <td>Small trees in leaf begin to sway</td>
 *   </tr>
 *   <tr>
 *     <td>6</td><td>Strong Breeze</td><td>10.8 - 13.8</td>
 *     <td>Large branches in motion</td>
 *   </tr>
 *   <tr>
 *     <td>7</td><td>Near Gale</td><td>13.9 - 17.1</td>
 *     <td>Whole trees in motion</td>
 *   </tr>
 *   <tr>
 *     <td>8</td><td>Gale</td><td>17.2 - 20.7</td>
 *     <td>Twigs broken from trees</td>
 *   </tr>
 *   <tr>
 *     <td>9</td><td>Strong Gale</td><td>20.8 - 24.4</td>
 *     <td>Slight structural damage</td>
 *   </tr>
 *   <tr>
 *     <td>10</td><td>Storm</td><td>24.5 - 28.4</td>
 *     <td>Trees uprooted</td>
 *   </tr>
 *   <tr>
 *     <td>11</td><td>Violent Storm</td><td>28.5 - 32.6</td>
 *     <td>Widespread damage</td>
 *   </tr>
 *   <tr>
 *     <td>12</td><td>Hurricane</td><td>32.7+</td>
 *     <td>Extreme widespread destruction</td>
 *   </tr>
 * </table>
 *
 * <p><strong>References:</strong>
 *
 * <ul>
 *   <li><a href="https://www.rmets.org/metmatters/beaufort-wind-scale">Royal Meteorological
 *       Society</a>
 *   <li><a href="https://www.weather.gov/mfl/beaufort">NOAA Beaufort Wind Scale</a>
 * </ul>
 *
 * @since 1.0.0
 */
public enum WindStrength {
  /**
   * Calm (0.0 - 0.2 m/s). Beaufort 0.
   *
   * <p>Smoke rises vertically; leaves are still.
   */
  CALM(0, 0.0, 0.2),

  /**
   * Light Air (0.3 - 1.5 m/s). Beaufort 1.
   *
   * <p>Wind motion visible in smoke; wind vanes inactive.
   */
  LIGHT_AIR(1, 0.3, 1.5),

  /**
   * Light Breeze (1.6 - 3.3 m/s). Beaufort 2.
   *
   * <p>Wind felt on face; leaves rustle; ordinary wind vane moves.
   */
  LIGHT_BREEZE(2, 1.6, 3.3),

  /**
   * Gentle Breeze (3.4 - 5.4 m/s). Beaufort 3.
   *
   * <p>Leaves and small twigs in constant motion; light flags extend.
   */
  GENTLE_BREEZE(3, 3.4, 5.4),

  /**
   * Moderate Breeze (5.5 - 7.9 m/s). Beaufort 4.
   *
   * <p>Dust and loose paper raised; small branches move.
   */
  MODERATE_BREEZE(4, 5.5, 7.9),

  /**
   * Fresh Breeze (8.0 - 10.7 m/s). Beaufort 5.
   *
   * <p>Small trees in leaf begin to sway; crested wavelets form on inland waters.
   */
  FRESH_BREEZE(5, 8.0, 10.7),

  /**
   * Strong Breeze (10.8 - 13.8 m/s). Beaufort 6.
   *
   * <p>Large branches in motion; whistling heard in overhead wires; umbrellas difficult to use.
   */
  STRONG_BREEZE(6, 10.8, 13.8),

  /**
   * Near Gale (13.9 - 17.1 m/s). Beaufort 7.
   *
   * <p>Whole trees in motion; some difficulty walking against the wind.
   */
  NEAR_GALE(7, 13.9, 17.1),

  /**
   * Gale (17.2 - 20.7 m/s). Beaufort 8.
   *
   * <p>Twigs broken from trees; walking is generally difficult.
   */
  GALE(8, 17.2, 20.7),

  /**
   * Strong Gale (20.8 - 24.4 m/s). Beaufort 9.
   *
   * <p>Slight structural damage (e.g., roof shingles blown off).
   */
  STRONG_GALE(9, 20.8, 24.4),

  /**
   * Storm (24.5 - 28.4 m/s). Beaufort 10.
   *
   * <p>Trees uprooted; considerable structural damage.
   */
  STORM(10, 24.5, 28.4),

  /**
   * Violent Storm (28.5 - 32.6 m/s). Beaufort 11.
   *
   * <p>Widespread damage to structures and vegetation.
   */
  VIOLENT_STORM(11, 28.5, 32.6),

  /**
   * Hurricane Force (32.7+ m/s). Beaufort 12.
   *
   * <p>Extreme widespread destruction; devastation.
   */
  HURRICANE(12, 32.7, Double.MAX_VALUE);

  /**
   * Threshold for handling the gap between CALM (max 0.2) and LIGHT_AIR (min 0.3).
   *
   * <p>Speed values in the range 0.21-0.29 m/s fall in a gap between categories. This threshold
   * (midpoint of the gap) provides a reasonable boundary:
   *
   * <ul>
   *   <li>0.21-0.24 m/s → CALM
   *   <li>0.25-0.29 m/s → LIGHT_AIR
   * </ul>
   *
   * <p>Note: This gap (0.1 m/s) is smaller than typical wind measurement error (±0.5 m/s), so the
   * exact threshold choice has minimal practical impact.
   */
  private static final double CALM_LIGHT_AIR_GAP_THRESHOLD = 0.25;

  private final int beaufortNumber;
  private final double minSpeed;
  private final double maxSpeed;

  /**
   * Constructs a WindStrength category.
   *
   * @param beaufortNumber the Beaufort scale number (0-12)
   * @param minSpeed minimum wind speed in m/s (inclusive)
   * @param maxSpeed maximum wind speed in m/s (exclusive, except for HURRICANE)
   */
  WindStrength(int beaufortNumber, double minSpeed, double maxSpeed) {
    this.beaufortNumber = beaufortNumber;
    this.minSpeed = minSpeed;
    this.maxSpeed = maxSpeed;
  }

  /**
   * Determines the wind strength category for a given speed using the official Beaufort Scale.
   *
   * <p>This method maps wind speed to the appropriate Beaufort category according to WMO standards
   * with official ranges.
   *
   * <p><strong>Gap Handling:</strong> The official Beaufort Scale has a small gap between CALM
   * (0.0-0.2) and LIGHT_AIR (0.3-1.5). Values in this gap (0.21-0.29 m/s) are classified using the
   * midpoint (0.25 m/s) as threshold. Note that this gap is smaller than typical wind measurement
   * error.
   *
   * <p><strong>Examples:</strong>
   *
   * <pre>{@code
   * WindStrength.fromSpeed(0.0);   // Returns CALM
   * WindStrength.fromSpeed(0.22);  // Returns CALM (gap handling)
   * WindStrength.fromSpeed(0.25);  // Returns LIGHT_AIR (gap handling)
   * WindStrength.fromSpeed(0.3);   // Returns LIGHT_AIR
   * WindStrength.fromSpeed(5.5);   // Returns MODERATE_BREEZE
   * WindStrength.fromSpeed(50.0);  // Returns HURRICANE
   * }</pre>
   *
   * @param speed wind speed in m/s (must be >= 0 and finite)
   * @return the appropriate WindStrength category
   * @throws IllegalArgumentException if speed is invalid (negative or non-finite)
   */
  public static WindStrength fromSpeed(double speed) {
    if (!Double.isFinite(speed)) {
      throw new IllegalArgumentException(
          String.format("Wind speed must be finite, got: %s", speed));
    }
    if (speed < 0) {
      throw new IllegalArgumentException(
          String.format("Wind speed cannot be negative, got: %.2f m/s", speed));
    }
    if (speed > CALM.maxSpeed && speed < LIGHT_AIR.minSpeed) {
      return speed < CALM_LIGHT_AIR_GAP_THRESHOLD ? CALM : LIGHT_AIR;
    }
    for (WindStrength strength : values()) {
      if (speed >= strength.minSpeed && speed <= strength.maxSpeed) {
        return strength;
      }
    }
    return HURRICANE;
  }

  /**
   * Returns the Beaufort scale number for this wind strength.
   *
   * @return Beaufort number (0-12)
   */
  public int beaufortNumber() {
    return this.beaufortNumber;
  }

  /**
   * Returns the minimum wind speed for this strength category.
   *
   * @return minimum speed in m/s (inclusive)
   */
  public double minSpeed() {
    return this.minSpeed;
  }

  /**
   * Returns the maximum wind speed for this strength category.
   *
   * @return maximum speed in m/s (exclusive upper bound, except HURRICANE which is inclusive)
   */
  public double maxSpeed() {
    return this.maxSpeed;
  }

  /**
   * Checks if this wind strength is CALM (Beaufort 0).
   *
   * @return true if this is CALM
   */
  public boolean isCalm() {
    return this == CALM;
  }

  /**
   * Checks if this wind strength is LIGHT_AIR (Beaufort 1).
   *
   * @return true if this is LIGHT_AIR
   */
  public boolean isLightAir() {
    return this == LIGHT_AIR;
  }

  /**
   * Checks if this wind strength is LIGHT_BREEZE (Beaufort 2).
   *
   * @return true if this is LIGHT_BREEZE
   */
  public boolean isLightBreeze() {
    return this == LIGHT_BREEZE;
  }

  /**
   * Checks if this wind strength is GENTLE_BREEZE (Beaufort 3).
   *
   * @return true if this is GENTLE_BREEZE
   */
  public boolean isGentleBreeze() {
    return this == GENTLE_BREEZE;
  }

  /**
   * Checks if this wind strength is MODERATE_BREEZE (Beaufort 4).
   *
   * @return true if this is MODERATE_BREEZE
   */
  public boolean isModerateBreeze() {
    return this == MODERATE_BREEZE;
  }

  /**
   * Checks if this wind strength is FRESH_BREEZE (Beaufort 5).
   *
   * @return true if this is FRESH_BREEZE
   */
  public boolean isFreshBreeze() {
    return this == FRESH_BREEZE;
  }

  /**
   * Checks if this wind strength is STRONG_BREEZE (Beaufort 6).
   *
   * @return true if this is STRONG_BREEZE
   */
  public boolean isStrongBreeze() {
    return this == STRONG_BREEZE;
  }

  /**
   * Checks if this wind strength is NEAR_GALE (Beaufort 7).
   *
   * @return true if this is NEAR_GALE
   */
  public boolean isNearGale() {
    return this == NEAR_GALE;
  }

  /**
   * Checks if this wind strength is GALE (Beaufort 8).
   *
   * @return true if this is GALE
   */
  public boolean isGale() {
    return this == GALE;
  }

  /**
   * Checks if this wind strength is STRONG_GALE (Beaufort 9).
   *
   * @return true if this is STRONG_GALE
   */
  public boolean isStrongGale() {
    return this == STRONG_GALE;
  }

  /**
   * Checks if this wind strength is STORM (Beaufort 10).
   *
   * @return true if this is STORM
   */
  public boolean isStorm() {
    return this == STORM;
  }

  /**
   * Checks if this wind strength is VIOLENT_STORM (Beaufort 11).
   *
   * @return true if this is VIOLENT_STORM
   */
  public boolean isViolentStorm() {
    return this == VIOLENT_STORM;
  }

  /**
   * Checks if this wind strength is HURRICANE (Beaufort 12).
   *
   * @return true if this is HURRICANE
   */
  public boolean isHurricane() {
    return this == HURRICANE;
  }
}
