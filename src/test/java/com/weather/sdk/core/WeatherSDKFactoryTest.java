package com.weather.sdk.core;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link WeatherSDKFactory}.
 *
 * <p>Test strategy: Test singleton behavior, factory methods, validation, and lifecycle management.
 * Cleanup after each test to ensure isolation.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("WeatherSDKFactory")
class WeatherSDKFactoryTest {

  private static final String TEST_API_KEY = "test-api-key";

  @AfterEach
  void tearDown() {
    WeatherSDKFactory.destroyAll();
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should not be instantiable")
    void shouldNotBeInstantiable() throws Exception {
      // Arrange
      var constructor = WeatherSDKFactory.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      // Act & Assert
      assertThatThrownBy(constructor::newInstance)
          .isInstanceOf(InvocationTargetException.class)
          .hasCauseInstanceOf(AssertionError.class)
          .cause()
          .hasMessageContaining("Utility class");
    }
  }

  @Nested
  @DisplayName("create(String apiKey)")
  class CreateWithDefaultsTests {

    @Test
    @DisplayName("should create SDK with default mode (ON_DEMAND)")
    void shouldCreateSdkWithDefaultMode() {
      // Act
      WeatherSDK sdk = WeatherSDKFactory.create(TEST_API_KEY);

      // Assert
      assertThat(sdk).isNotNull();
      assertThat(sdk.isShutdown()).isFalse();
    }

    @Test
    @DisplayName("should reject null API key")
    void shouldRejectNullApiKey() {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.create(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("apiKey must not be null");
    }

    @ParameterizedTest(name = "should reject blank API key: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank API key")
    void shouldRejectBlankApiKey(String blankApiKey) {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.create(blankApiKey))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("apiKey must not be blank");
    }

    @Test
    @DisplayName("should throw exception when instance already exists")
    void shouldThrowExceptionWhenInstanceAlreadyExists() {
      // Arrange
      WeatherSDKFactory.create(TEST_API_KEY);

      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.create(TEST_API_KEY))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK instance already exists for API key")
          .hasMessageContaining("Use getInstance()");
    }
  }

  @Nested
  @DisplayName("create(String apiKey, OperationMode mode)")
  class CreateWithModeTests {

    @Test
    @DisplayName("should create SDK with custom mode")
    void shouldCreateSdkWithCustomMode() {
      // Act
      WeatherSDK sdk = WeatherSDKFactory.create(TEST_API_KEY, OperationMode.POLLING);

      // Assert
      assertThat(sdk).isNotNull();
      assertThat(sdk.isShutdown()).isFalse();
    }

    @Test
    @DisplayName("should create SDK with ON_DEMAND mode")
    void shouldCreateSdkWithOnDemandMode() {
      // Act
      WeatherSDK sdk = WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND);

      // Assert
      assertThat(sdk).isNotNull();
      assertThat(sdk.isShutdown()).isFalse();
    }

    @Test
    @DisplayName("should create SDK with POLLING mode")
    void shouldCreateSdkWithPollingMode() {
      // Act
      WeatherSDK sdk = WeatherSDKFactory.create(TEST_API_KEY, OperationMode.POLLING);

      // Assert
      assertThat(sdk).isNotNull();
      assertThat(sdk.isShutdown()).isFalse();
    }

    @Test
    @DisplayName("should reject null API key")
    void shouldRejectNullApiKey() {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.create(null, OperationMode.ON_DEMAND))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("apiKey must not be null");
    }

    @ParameterizedTest(name = "should reject blank API key: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank API key")
    void shouldRejectBlankApiKey(String blankApiKey) {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.create(blankApiKey, OperationMode.ON_DEMAND))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("apiKey must not be blank");
    }

    @Test
    @DisplayName("should reject null mode")
    void shouldRejectNullMode() {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.create(TEST_API_KEY, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("mode must not be null");
    }

    @Test
    @DisplayName("should throw exception when instance already exists")
    void shouldThrowExceptionWhenInstanceAlreadyExists() {
      // Arrange
      WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND);

      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.create(TEST_API_KEY, OperationMode.POLLING))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK instance already exists for API key")
          .hasMessageContaining("Use getInstance()");
    }

    @Test
    @DisplayName("should normalize API key to lowercase")
    void shouldNormalizeApiKeyToLowercase() {
      // Arrange
      String upperCaseKey = "TEST-API-KEY";
      String lowerCaseKey = "test-api-key";
      String mixedCaseKey = "TeSt-ApI-KeY";

      // Act
      WeatherSDK sdk = WeatherSDKFactory.create(upperCaseKey, OperationMode.ON_DEMAND);

      // Assert - All case variations should retrieve the same instance (singleton)
      assertThat(WeatherSDKFactory.getInstance(upperCaseKey)).isSameAs(sdk);
      assertThat(WeatherSDKFactory.getInstance(lowerCaseKey)).isSameAs(sdk);
      assertThat(WeatherSDKFactory.getInstance(mixedCaseKey)).isSameAs(sdk);

      // Assert - Cannot create duplicate with different case
      assertThatThrownBy(() -> WeatherSDKFactory.create(lowerCaseKey, OperationMode.ON_DEMAND))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK instance already exists for API key");
    }
  }

  @Nested
  @DisplayName("getInstance(String apiKey)")
  class GetInstanceTests {

    @Test
    @DisplayName("should return existing instance")
    void shouldReturnExistingInstance() {
      // Arrange
      WeatherSDK created = WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND);

      // Act
      WeatherSDK retrieved = WeatherSDKFactory.getInstance(TEST_API_KEY);

      // Assert
      assertThat(retrieved).isSameAs(created);
    }

    @Test
    @DisplayName("should return null when instance not found")
    void shouldReturnNullWhenInstanceNotFound() {
      // Act
      WeatherSDK result = WeatherSDKFactory.getInstance("non-existent-key");

      // Assert
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("should reject null API key")
    void shouldRejectNullApiKey() {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.getInstance(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("apiKey must not be null");
    }

    @ParameterizedTest(name = "should reject blank API key: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank API key")
    void shouldRejectBlankApiKey(String blankApiKey) {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.getInstance(blankApiKey))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("apiKey must not be blank");
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
      // Arrange
      WeatherSDK sdk = WeatherSDKFactory.create("test-api-key", OperationMode.ON_DEMAND);

      // Act & Assert
      assertThat(WeatherSDKFactory.getInstance("TEST-API-KEY")).isSameAs(sdk);
      assertThat(WeatherSDKFactory.getInstance("Test-Api-Key")).isSameAs(sdk);
      assertThat(WeatherSDKFactory.getInstance("test-api-key")).isSameAs(sdk);
    }
  }

  @Nested
  @DisplayName("destroy(String apiKey)")
  class DestroyTests {

    @Test
    @DisplayName("should destroy existing instance")
    void shouldDestroyExistingInstance() {
      // Arrange
      WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND);
      assertThat(WeatherSDKFactory.getInstance(TEST_API_KEY)).isNotNull();

      // Act
      WeatherSDKFactory.destroy(TEST_API_KEY);

      // Assert
      assertThat(WeatherSDKFactory.getInstance(TEST_API_KEY)).isNull();
    }

    @Test
    @DisplayName("should shutdown SDK on destroy")
    void shouldShutdownSdkOnDestroy() {
      // Arrange
      WeatherSDK sdk = WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND);
      assertThat(sdk.isShutdown()).isFalse();

      // Act
      WeatherSDKFactory.destroy(TEST_API_KEY);

      // Assert
      assertThat(sdk.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should do nothing when instance not found")
    void shouldDoNothingWhenInstanceNotFound() {
      // Act & Assert - Should not throw exception
      assertThatNoException().isThrownBy(() -> WeatherSDKFactory.destroy("non-existent-key"));
    }

    @Test
    @DisplayName("should reject null API key")
    void shouldRejectNullApiKey() {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.destroy(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("apiKey must not be null");
    }

    @ParameterizedTest(name = "should reject blank API key: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank API key")
    void shouldRejectBlankApiKey(String blankApiKey) {
      // Act & Assert
      assertThatThrownBy(() -> WeatherSDKFactory.destroy(blankApiKey))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("apiKey must not be blank");
    }
  }

  @Nested
  @DisplayName("destroyAll()")
  class DestroyAllTests {

    @Test
    @DisplayName("should destroy all instances")
    void shouldDestroyAllInstances() {
      // Arrange
      WeatherSDKFactory.create("api-key-1", OperationMode.ON_DEMAND);
      WeatherSDKFactory.create("api-key-2", OperationMode.ON_DEMAND);
      WeatherSDKFactory.create("api-key-3", OperationMode.POLLING);

      assertThat(WeatherSDKFactory.getInstance("api-key-1")).isNotNull();
      assertThat(WeatherSDKFactory.getInstance("api-key-2")).isNotNull();
      assertThat(WeatherSDKFactory.getInstance("api-key-3")).isNotNull();

      // Act
      WeatherSDKFactory.destroyAll();

      // Assert
      assertThat(WeatherSDKFactory.getInstance("api-key-1")).isNull();
      assertThat(WeatherSDKFactory.getInstance("api-key-2")).isNull();
      assertThat(WeatherSDKFactory.getInstance("api-key-3")).isNull();
    }

    @Test
    @DisplayName("should shutdown all SDKs on destroyAll")
    void shouldShutdownAllSdksOnDestroyAll() {
      // Arrange
      WeatherSDK sdk1 = WeatherSDKFactory.create("api-key-1", OperationMode.ON_DEMAND);
      WeatherSDK sdk2 = WeatherSDKFactory.create("api-key-2", OperationMode.ON_DEMAND);
      WeatherSDK sdk3 = WeatherSDKFactory.create("api-key-3", OperationMode.POLLING);

      assertThat(sdk1.isShutdown()).isFalse();
      assertThat(sdk2.isShutdown()).isFalse();
      assertThat(sdk3.isShutdown()).isFalse();

      // Act
      WeatherSDKFactory.destroyAll();

      // Assert
      assertThat(sdk1.isShutdown()).isTrue();
      assertThat(sdk2.isShutdown()).isTrue();
      assertThat(sdk3.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should be idempotent")
    void shouldBeIdempotent() {
      // Arrange
      WeatherSDKFactory.create("api-key-1", OperationMode.ON_DEMAND);
      WeatherSDKFactory.create("api-key-2", OperationMode.POLLING);

      // Act - Call multiple times
      WeatherSDKFactory.destroyAll();
      WeatherSDKFactory.destroyAll();
      WeatherSDKFactory.destroyAll();

      // Assert - Should not throw exception
      assertThat(WeatherSDKFactory.getInstance("api-key-1")).isNull();
      assertThat(WeatherSDKFactory.getInstance("api-key-2")).isNull();
    }
  }
}
