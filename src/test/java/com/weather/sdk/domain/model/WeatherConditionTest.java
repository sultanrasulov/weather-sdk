package com.weather.sdk.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link WeatherCondition} value object.
 *
 * <p>Test strategy: TDD approach with focus on validation and equality contracts. Uses
 * parameterized tests to reduce duplication for blank string validation.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("WeatherCondition Value Object")
class WeatherConditionTest {

  @Nested
  @DisplayName("Constructor and Validation")
  class ConstructionTests {

    @Test
    @DisplayName("should create WeatherCondition with valid main and description")
    void shouldCreateWithValidMainAndDescription() {
      // Arrange
      String main = "Clouds";
      String description = "scattered clouds";

      // Act
      WeatherCondition weatherCondition = new WeatherCondition(main, description);

      // Assert
      assertThat(weatherCondition.main()).isEqualTo(main);
      assertThat(weatherCondition.description()).isEqualTo(description);
    }

    @Test
    @DisplayName("should reject null main")
    void shouldRejectNullMain() {
      // Arrange
      String validDescription = "scattered clouds";

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCondition(null, validDescription))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("main must not be null");
    }

    @Test
    @DisplayName("should reject null description")
    void shouldRejectNullDescription() {
      // Arrange
      String validMain = "Clouds";

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCondition(validMain, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("description must not be null");
    }

    @Test
    @DisplayName("should reject both null parameters")
    void shouldRejectBothNull() {
      // Act & Assert
      assertThatThrownBy(() -> new WeatherCondition(null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("must not be null");
    }

    @ParameterizedTest(name = "should reject blank main: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank main")
    void shouldRejectBlankMain(String invalidMain) {
      // Arrange
      String validDescription = "scattered clouds";

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCondition(invalidMain, validDescription))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("main must not be blank");
    }

    @ParameterizedTest(name = "should reject blank description: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank description")
    void shouldRejectBlankDescription(String invalidDescription) {
      // Arrange
      String validMain = "Clouds";

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCondition(validMain, invalidDescription))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("description must not be blank");
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("should be equal when both fields match")
    void shouldBeEqualWhenBothFieldsMatch() {
      // Arrange
      String main = "Clouds";
      String description = "scattered clouds";
      WeatherCondition weatherCondition1 = new WeatherCondition(main, description);
      WeatherCondition weatherCondition2 = new WeatherCondition(main, description);

      // Act & Assert
      assertThat(weatherCondition1).isEqualTo(weatherCondition2);
      assertThat(weatherCondition1.hashCode()).isEqualTo(weatherCondition2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when main differs")
    void shouldNotBeEqualWhenMainDiffers() {
      // Arrange
      String description = "scattered clouds";
      WeatherCondition weatherCondition1 = new WeatherCondition("Clouds", description);
      WeatherCondition weatherCondition2 = new WeatherCondition("Rain", description);

      // Act & Assert
      assertThat(weatherCondition1).isNotEqualTo(weatherCondition2);
    }

    @Test
    @DisplayName("should not be equal when description differs")
    void shouldNotBeEqualWhenDescriptionDiffers() {
      // Arrange
      String main = "Clouds";
      WeatherCondition weatherCondition1 = new WeatherCondition(main, "scattered clouds");
      WeatherCondition weatherCondition2 = new WeatherCondition(main, "broken clouds");

      // Act & Assert
      assertThat(weatherCondition1).isNotEqualTo(weatherCondition2);
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      // Arrange
      WeatherCondition weatherCondition = new WeatherCondition("Clouds", "scattered clouds");

      // Act & Assert
      assertThat(weatherCondition).isNotEqualTo(null);
    }
  }
}
