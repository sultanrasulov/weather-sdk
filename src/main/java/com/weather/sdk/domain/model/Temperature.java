package com.weather.sdk.domain.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable value object representing actual and perceived air temperature.
 *
 * <p>Stores temperatures in Kelvin (OpenWeatherMap API standard). Provides conversion helpers to
 * Celsius/Fahrenheit and rich domain behavior such as {@link #isFreezing()} and {@link
 * #hasSignificantWindChill()}.
 *
 * <p><strong>Immutability:</strong> This is a Value Object - once created, it cannot be modified.
 * All conversion methods return primitive values, not new instances.
 *
 * <p><strong>Units:</strong> All temperature values are stored in Kelvin internally. Use conversion
 * methods like {@link #tempCelsius()} or {@link #tempFahrenheit()} to get values in other units.
 *
 * <p><strong>Validation policy:</strong> Values must be finite and >= absolute zero (0K). Stricter
 * application-specific validation should be performed at the application layer.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Create from Kelvin (API format)
 * Temperature temp1 = new Temperature(293.15, 291.15);
 *
 * // Create from Celsius (more convenient)
 * Temperature temp2 = Temperature.fromCelsius(20.0, 18.0);
 *
 * // Use domain methods
 * if (temp2.isFreezing()) {
 *     System.out.println("It's freezing!");
 * }
 *
 * if (temp2.hasSignificantWindChill()) {
 *     System.out.println("Feels colder: " + temp2.feelsLikeCelsius() + "°C");
 * }
 * }</pre>
 *
 * @param temp actual air temperature in Kelvin (must be >= 0K and finite)
 * @param feelsLike perceived temperature in Kelvin (must be >= 0K and finite)
 * @since 1.0.0
 */
public record Temperature(double temp, double feelsLike) implements Comparable<Temperature> {

  /** Absolute zero in Kelvin (0K = -273.15°C). Physical lower limit for temperature. */
  private static final double ABSOLUTE_ZERO_KELVIN = 0.0;

  /**
   * Zero degrees Celsius in Kelvin (0°C = 273.15K).
   *
   * <p>This is the freezing point of water at standard atmospheric pressure and serves as the
   * reference point for Celsius-Kelvin conversions.
   */
  private static final double ZERO_CELSIUS_IN_KELVIN = 273.15;

  /** Celsius to Fahrenheit conversion multiplier (9/5). */
  private static final double CELSIUS_TO_FAHRENHEIT_MULTIPLIER = 9.0 / 5.0;

  /** Celsius to Fahrenheit conversion offset (32°F = 0°C). */
  private static final double CELSIUS_TO_FAHRENHEIT_OFFSET = 32.0;

  /** Freezing point of water (0°C / 273.15K). */
  public static final double FREEZING_POINT_KELVIN = 273.15;

  /**
   * Default threshold for significant perceived temperature difference (3K).
   *
   * <p>This value represents a temperature difference that is universally noticeable to humans and
   * typically requires behavioral or clothing adjustments. The 3K threshold is based on:
   *
   * <ul>
   *   <li>Human temperature perception: detection threshold of 1-2K, comfortable discrimination at
   *       3K
   *   <li>Meteorological standards: NOAA and WMO use 3-5K for "significant" weather changes
   *   <li>Practical experience: 3K difference typically requires additional/fewer clothing layers
   *   <li>Health and safety: 3K wind chill can significantly affect exposed skin comfort and risk
   * </ul>
   *
   * <p>Users can override this default by using methods with explicit threshold parameters.
   *
   * @since 1.0.0
   */
  public static final double DEFAULT_SIGNIFICANCE_THRESHOLD_KELVIN = 3.0;

  /**
   * Compact constructor with validation.
   *
   * <p>Validates that both temperature values are finite and not below absolute zero. This
   * constructor is automatically called by factory methods and direct instantiation.
   *
   * @throws IllegalArgumentException if temperature is below absolute zero or not finite
   */
  public Temperature {
    validateTemperature(temp, "temp");
    validateTemperature(feelsLike, "feelsLike");
  }

  /**
   * Creates Temperature from Celsius values.
   *
   * <p>This factory method is more convenient than using Kelvin directly, as most users think in
   * Celsius or Fahrenheit rather than Kelvin.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Temperature temp = Temperature.fromCelsius(20.0, 18.5);
   * System.out.println(temp.tempCelsius());  // 20.0
   * System.out.println(temp.temp());         // 293.15 (Kelvin)
   * }</pre>
   *
   * @param tempCelsius actual temperature in Celsius
   * @param feelsLikeCelsius perceived temperature in Celsius
   * @return Temperature instance with values converted to Kelvin
   * @throws IllegalArgumentException if values are invalid (below absolute zero or non-finite)
   * @since 1.0.0
   */
  public static Temperature fromCelsius(double tempCelsius, double feelsLikeCelsius) {
    return new Temperature(celsiusToKelvin(tempCelsius), celsiusToKelvin(feelsLikeCelsius));
  }

  /**
   * Creates Temperature from Fahrenheit values.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Temperature temp = Temperature.fromFahrenheit(68.0, 65.3);
   * System.out.println(temp.tempFahrenheit());  // 68.0
   * System.out.println(temp.tempCelsius());     // 20.0
   * }</pre>
   *
   * @param tempFahrenheit actual temperature in Fahrenheit
   * @param feelsLikeFahrenheit perceived temperature in Fahrenheit
   * @return Temperature instance with values converted to Kelvin
   * @throws IllegalArgumentException if values are invalid (below absolute zero or non-finite)
   * @since 1.0.0
   */
  public static Temperature fromFahrenheit(double tempFahrenheit, double feelsLikeFahrenheit) {
    return new Temperature(
        fahrenheitToKelvin(tempFahrenheit), fahrenheitToKelvin(feelsLikeFahrenheit));
  }

  /**
   * Returns actual temperature in Celsius.
   *
   * @return temperature in degrees Celsius
   */
  public double tempCelsius() {
    return this.temp - ZERO_CELSIUS_IN_KELVIN;
  }

  /**
   * Returns actual temperature in Fahrenheit.
   *
   * @return temperature in degrees Fahrenheit
   */
  public double tempFahrenheit() {
    return celsiusToFahrenheit(this.tempCelsius());
  }

  /**
   * Returns perceived temperature in Celsius.
   *
   * <p>The "feels like" temperature accounts for humidity, wind, and other factors that affect how
   * temperature is perceived by the human body.
   *
   * @return perceived temperature in degrees Celsius
   */
  public double feelsLikeCelsius() {
    return this.feelsLike - ZERO_CELSIUS_IN_KELVIN;
  }

  /**
   * Returns perceived temperature in Fahrenheit.
   *
   * <p>The "feels like" temperature accounts for humidity, wind, and other factors that affect how
   * temperature is perceived by the human body.
   *
   * @return perceived temperature in degrees Fahrenheit
   */
  public double feelsLikeFahrenheit() {
    return celsiusToFahrenheit(this.feelsLikeCelsius());
  }

  /**
   * Checks if there's a significant wind chill effect using default threshold (3K).
   *
   * <p>Wind chill occurs when perceived temperature is significantly lower than actual temperature
   * due to wind removing heat from the body surface. A 3K difference is noticeable to humans and
   * typically requires additional clothing.
   *
   * <p>This is equivalent to calling {@code hasSignificantWindChill(3.0)}.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Temperature temp = Temperature.fromCelsius(5.0, 0.0);
   * if (temp.hasSignificantWindChill()) {
   *     System.out.println("Bundle up! Wind chill: " + temp.feelsLikeCelsius() + "°C");
   * }
   * }</pre>
   *
   * @return true if wind chill effect is 3K or greater
   * @see #hasSignificantWindChill(double)
   * @since 1.0.0
   */
  public boolean hasSignificantWindChill() {
    return hasSignificantWindChill(DEFAULT_SIGNIFICANCE_THRESHOLD_KELVIN);
  }

  /**
   * Checks if there's a significant wind chill effect with custom threshold.
   *
   * <p>Wind chill occurs when perceived temperature is significantly lower than actual temperature
   * due to wind removing heat from the body surface.
   *
   * <p><strong>Examples:</strong>
   *
   * <pre>{@code
   * Temperature temp = Temperature.fromCelsius(5.0, 0.0);
   *
   * // More sensitive detection (1K threshold)
   * if (temp.hasSignificantWindChill(1.0)) {
   *     System.out.println("Slight wind chill detected");
   * }
   *
   * // Less sensitive detection (5K threshold)
   * if (temp.hasSignificantWindChill(5.0)) {
   *     System.out.println("Extreme wind chill warning!");
   * }
   * }</pre>
   *
   * @param thresholdKelvin minimum temperature difference to consider significant (must be >= 0)
   * @return true if (actual temp - feels like) >= threshold
   * @throws IllegalArgumentException if threshold is negative or not finite
   * @see #hasSignificantWindChill()
   * @since 1.0.0
   */
  public boolean hasSignificantWindChill(double thresholdKelvin) {
    validateThreshold(thresholdKelvin, "thresholdKelvin");
    return (this.temp - this.feelsLike) >= thresholdKelvin;
  }

  /**
   * Checks if there's a significant heat index effect using default threshold (3K).
   *
   * <p>Heat index occurs when perceived temperature is significantly higher than actual temperature
   * due to humidity preventing effective evaporative cooling from the body.
   *
   * <p>This is equivalent to calling {@code hasSignificantHeatIndex(3.0)}.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Temperature temp = Temperature.fromCelsius(30.0, 35.0);
   * if (temp.hasSignificantHeatIndex()) {
   *     System.out.println("Stay hydrated! Feels like: " + temp.feelsLikeCelsius() + "°C");
   * }
   * }</pre>
   *
   * @return true if heat index effect is 3K or greater
   * @see #hasSignificantHeatIndex(double)
   * @since 1.0.0
   */
  public boolean hasSignificantHeatIndex() {
    return hasSignificantHeatIndex(DEFAULT_SIGNIFICANCE_THRESHOLD_KELVIN);
  }

  /**
   * Checks if there's a significant heat index effect with custom threshold.
   *
   * <p>Heat index occurs when perceived temperature is significantly higher than actual temperature
   * due to humidity preventing effective evaporative cooling from the body.
   *
   * <p><strong>Examples:</strong>
   *
   * <pre>{@code
   * Temperature temp = Temperature.fromCelsius(30.0, 35.0);
   *
   * // More sensitive detection (1K threshold)
   * if (temp.hasSignificantHeatIndex(1.0)) {
   *     System.out.println("Slight heat index detected");
   * }
   *
   * // Less sensitive detection (5K threshold)
   * if (temp.hasSignificantHeatIndex(5.0)) {
   *     System.out.println("Dangerous heat index!");
   * }
   * }</pre>
   *
   * @param thresholdKelvin minimum temperature difference to consider significant (must be >= 0)
   * @return true if (feels like - actual temp) >= threshold
   * @throws IllegalArgumentException if threshold is negative or not finite
   * @see #hasSignificantHeatIndex()
   * @since 1.0.0
   */
  public boolean hasSignificantHeatIndex(double thresholdKelvin) {
    validateThreshold(thresholdKelvin, "thresholdKelvin");
    return (this.feelsLike - this.temp) >= thresholdKelvin;
  }

  /**
   * Checks if temperature is below freezing point (0°C / 273.15K).
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Temperature temp = Temperature.fromCelsius(-5.0, -7.0);
   * if (temp.isFreezing()) {
   *     System.out.println("Water will freeze!");
   * }
   * }</pre>
   *
   * @return true if temperature is below 0°C
   * @since 1.0.0
   */
  public boolean isFreezing() {
    return this.temp < FREEZING_POINT_KELVIN;
  }

  /**
   * Checks if this temperature is warmer than another.
   *
   * <p>Comparison is based on actual temperature, not "feels like" temperature.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Temperature today = Temperature.fromCelsius(20.0, 18.0);
   * Temperature yesterday = Temperature.fromCelsius(15.0, 13.0);
   *
   * if (today.isWarmerThan(yesterday)) {
   *     System.out.println("Temperature is rising!");
   * }
   * }</pre>
   *
   * @param other the temperature to compare to (must not be null)
   * @return true if this temperature is warmer (higher actual temp)
   * @throws NullPointerException if other is null
   * @since 1.0.0
   */
  public boolean isWarmerThan(Temperature other) {
    Objects.requireNonNull(other, "other must not be null");
    return this.temp > other.temp;
  }

  /**
   * Checks if this temperature is colder than another.
   *
   * <p>Comparison is based on actual temperature, not "feels like" temperature.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Temperature today = Temperature.fromCelsius(10.0, 8.0);
   * Temperature yesterday = Temperature.fromCelsius(15.0, 13.0);
   *
   * if (today.isColderThan(yesterday)) {
   *     System.out.println("Temperature is dropping!");
   * }
   * }</pre>
   *
   * @param other the temperature to compare to (must not be null)
   * @return true if this temperature is colder (lower actual temp)
   * @throws NullPointerException if other is null
   * @since 1.0.0
   */
  public boolean isColderThan(Temperature other) {
    Objects.requireNonNull(other, "other must not be null");
    return this.temp < other.temp;
  }

  /**
   * Returns a string representation of this temperature.
   *
   * <p>Format: {@code Temperature[temp=293.15K (20.00°C), feelsLike=291.15K (18.00°C)]}
   *
   * <p>Uses {@link Locale#ROOT} to ensure consistent decimal formatting regardless of system
   * locale.
   *
   * @return formatted string with both Kelvin and Celsius values
   */
  @Override
  public String toString() {
    return String.format(
        Locale.ROOT,
        "Temperature[temp=%.2fK (%.2f°C), feelsLike=%.2fK (%.2f°C)]",
        this.temp,
        tempCelsius(),
        this.feelsLike,
        feelsLikeCelsius());
  }

  /**
   * Compares this temperature with another for ordering.
   *
   * <p>Comparison is performed first by actual temperature (in ascending order), then by feels-like
   * temperature to ensure consistency with {@link #equals(Object)} (i.e., {@code x.compareTo(y) ==
   * 0} implies {@code x.equals(y)}).
   *
   * <p>Higher temperatures are considered "greater" in the sort order. Invalid values (NaN,
   * Infinity) are prevented by validation, so this method never encounters them.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * List<Temperature> temperatures = Arrays.asList(
   *     Temperature.fromCelsius(20.0, 18.0),
   *     Temperature.fromCelsius(15.0, 13.0),
   *     Temperature.fromCelsius(25.0, 23.0)
   * );
   *
   * Collections.sort(temperatures);  // Sorts in ascending order: 15°C, 20°C, 25°C
   * }</pre>
   *
   * @param other the temperature to compare to (must not be null)
   * @return negative if this &lt; other, zero if equal, positive if this &gt; other
   * @throws NullPointerException if other is null
   * @see Comparable
   */
  @Override
  public int compareTo(Temperature other) {
    Objects.requireNonNull(other, "Cannot compare to null");
    int tempComparison = Double.compare(this.temp, other.temp);
    if (tempComparison != 0) {
      return tempComparison;
    }
    return Double.compare(this.feelsLike, other.feelsLike);
  }

  /**
   * Validates a temperature value with business context.
   *
   * <p>Ensures the temperature is:
   *
   * <ul>
   *   <li>Finite (not NaN or Infinity)
   *   <li>Not below absolute zero (0K / -273.15°C)
   * </ul>
   *
   * @param value the temperature value to validate in Kelvin
   * @param fieldName the name of the field (for error messages)
   * @throws IllegalArgumentException if validation fails
   */
  private static void validateTemperature(double value, String fieldName) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid temperature value for %s: must be a finite number, got: %s",
              fieldName, value));
    }
    if (value < ABSOLUTE_ZERO_KELVIN) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid temperature value for %s: cannot be below absolute zero (%.2fK), got: %.2fK (%.2f°C)",
              fieldName, ABSOLUTE_ZERO_KELVIN, value, value - ZERO_CELSIUS_IN_KELVIN));
    }
  }

  /**
   * Validates a threshold parameter.
   *
   * <p>Ensures the threshold is:
   *
   * <ul>
   *   <li>Finite (not NaN or Infinity)
   *   <li>Non-negative (>= 0)
   * </ul>
   *
   * @param threshold the threshold value to validate in Kelvin
   * @param paramName the parameter name (for error messages)
   * @throws IllegalArgumentException if threshold is invalid
   */
  private static void validateThreshold(double threshold, String paramName) {
    if (!Double.isFinite(threshold)) {
      throw new IllegalArgumentException(
          String.format("%s must be finite, got: %s", paramName, threshold));
    }
    if (threshold < 0.0) {
      throw new IllegalArgumentException(
          String.format("%s cannot be negative, got: %.2fK", paramName, threshold));
    }
  }

  /**
   * Converts Celsius to Fahrenheit.
   *
   * @param celsius temperature in Celsius
   * @return temperature in Fahrenheit
   */
  private static double celsiusToFahrenheit(double celsius) {
    return celsius * CELSIUS_TO_FAHRENHEIT_MULTIPLIER + CELSIUS_TO_FAHRENHEIT_OFFSET;
  }

  /**
   * Converts Celsius to Kelvin.
   *
   * @param celsius temperature in Celsius
   * @return temperature in Kelvin
   */
  private static double celsiusToKelvin(double celsius) {
    return celsius + ZERO_CELSIUS_IN_KELVIN;
  }

  /**
   * Converts Fahrenheit to Kelvin.
   *
   * @param fahrenheit temperature in Fahrenheit
   * @return temperature in Kelvin
   */
  private static double fahrenheitToKelvin(double fahrenheit) {
    return ((fahrenheit - CELSIUS_TO_FAHRENHEIT_OFFSET) / CELSIUS_TO_FAHRENHEIT_MULTIPLIER)
        + ZERO_CELSIUS_IN_KELVIN;
  }
}
