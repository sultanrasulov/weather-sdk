package com.weather.sdk.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link OperationMode} enum.
 *
 * <p>Test strategy: Verify enum constants, convenience methods, and enum contract compliance.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("OperationMode Enum")
class OperationModeTest {

  @Nested
  @DisplayName("Enum Constants")
  class EnumConstantsTests {

    @Test
    @DisplayName("should have ON_DEMAND constant")
    void shouldHaveOnDemandConstant() {
      // Act
      OperationMode mode = OperationMode.ON_DEMAND;

      // Assert
      assertThat(mode).isNotNull();
      assertThat(mode.name()).isEqualTo("ON_DEMAND");
    }

    @Test
    @DisplayName("should have POLLING constant")
    void shouldHavePollingConstant() {
      // Act
      OperationMode mode = OperationMode.POLLING;

      // Assert
      assertThat(mode).isNotNull();
      assertThat(mode.name()).isEqualTo("POLLING");
    }

    @Test
    @DisplayName("should have exactly two constants")
    void shouldHaveTwoConstants() {
      // Act
      OperationMode[] values = OperationMode.values();

      // Assert
      assertThat(values).hasSize(2);
      assertThat(values).containsExactly(OperationMode.ON_DEMAND, OperationMode.POLLING);
    }
  }

  @Nested
  @DisplayName("isOnDemand() Method")
  class IsOnDemandTests {

    @Test
    @DisplayName("should return true for ON_DEMAND")
    void isOnDemandShouldReturnTrueForOnDemand() {
      // Arrange
      OperationMode mode = OperationMode.ON_DEMAND;

      // Act
      boolean result = mode.isOnDemand();

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false for POLLING")
    void isOnDemandShouldReturnFalseForPolling() {
      // Arrange
      OperationMode mode = OperationMode.POLLING;

      // Act
      boolean result = mode.isOnDemand();

      // Assert
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("isPolling() Method")
  class IsPollingTests {

    @Test
    @DisplayName("should return true for POLLING")
    void isPollingShouldReturnTrueForPolling() {
      // Arrange
      OperationMode mode = OperationMode.POLLING;

      // Act
      boolean result = mode.isPolling();

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false for ON_DEMAND")
    void isPollingShouldReturnFalseForOnDemand() {
      // Arrange
      OperationMode mode = OperationMode.ON_DEMAND;

      // Act
      boolean result = mode.isPolling();

      // Assert
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Enum Contract")
  class EnumContractTests {

    @Test
    @DisplayName("should support values() method")
    void shouldSupportValuesMethod() {
      // Act
      OperationMode[] values = OperationMode.values();

      // Assert
      assertThat(values).isNotNull();
      assertThat(values).hasSize(2);
      assertThat(values).contains(OperationMode.ON_DEMAND, OperationMode.POLLING);
    }

    @Test
    @DisplayName("should support valueOf() method")
    void shouldSupportValueOfMethod() {
      // Act
      OperationMode onDemand = OperationMode.valueOf("ON_DEMAND");
      OperationMode polling = OperationMode.valueOf("POLLING");

      // Assert
      assertThat(onDemand).isEqualTo(OperationMode.ON_DEMAND);
      assertThat(polling).isEqualTo(OperationMode.POLLING);
    }

    @Test
    @DisplayName("valueOf() should reject invalid name")
    void valueOfShouldRejectInvalidName() {
      // Act & Assert
      assertThatThrownBy(() -> OperationMode.valueOf("INVALID"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("valueOf() should reject null")
    void valueOfShouldRejectNull() {
      // Act & Assert
      assertThatThrownBy(() -> OperationMode.valueOf(null))
          .isInstanceOf(NullPointerException.class);
    }
  }
}
