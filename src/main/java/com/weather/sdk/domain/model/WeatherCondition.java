package com.weather.sdk.domain.model;

import java.util.Objects;

/**
 * Immutable value object representing weather conditions.
 *
 * <p>Stores the main weather category and detailed description from OpenWeatherMap API.
 *
 * <p><strong>Immutability:</strong> This is a Value Object - once created, it cannot be modified.
 *
 * <p><strong>Validation policy:</strong> Both main and description must be non-null and non-blank.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * WeatherCondition weatherCondition = new WeatherCondition("Clouds", "scattered clouds");
 *
 * System.out.println(weatherCondition.main());         // "Clouds"
 * System.out.println(weatherCondition.description());  // "scattered clouds"
 * }</pre>
 *
 * @param main weather category (e.g., "Clouds", "Rain", "Clear")
 * @param description detailed weather description (e.g., "scattered clouds")
 * @since 1.0.0
 */
public record WeatherCondition(String main, String description) {

  /**
   * Compact constructor with validation.
   *
   * @throws NullPointerException if main or description is null
   * @throws IllegalArgumentException if main or description is blank
   */
  public WeatherCondition {
    Objects.requireNonNull(main, "main must not be null");
    Objects.requireNonNull(description, "description must not be null");
    requireNonBlank(main, "main");
    requireNonBlank(description, "description");
  }

  /**
   * Validates that a string is not blank.
   *
   * @param value the value to validate
   * @param fieldName the field name for error messages
   * @throws IllegalArgumentException if value is blank
   */
  private static void requireNonBlank(String value, String fieldName) {
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
