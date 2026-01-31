package com.weather.sdk.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.weather.sdk.cache.WeatherCache;
import com.weather.sdk.domain.model.Weather;
import com.weather.sdk.infrastructure.client.OpenWeatherApiClient;
import com.weather.sdk.infrastructure.dto.OpenWeatherApiResponse;
import com.weather.sdk.infrastructure.exception.OpenWeatherApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link WeatherSDK}.
 *
 * <p>Test strategy: Mock-based testing with focus on business logic, error handling, and lifecycle
 * management.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("WeatherSDK")
class WeatherSDKTest {

  private static final String TEST_API_KEY = "test-api-key";
  private static final String TEST_CITY = "London";

  private OpenWeatherApiClient mockClient;
  private WeatherCache cache;
  private WeatherSDK sdk;

  @BeforeEach
  void setUp() {
    mockClient = mock(OpenWeatherApiClient.class);
    cache = new WeatherCache(10, Duration.ofMinutes(10));
    sdk = new WeatherSDK(TEST_API_KEY, OperationMode.ON_DEMAND, mockClient, cache);
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should create SDK with valid parameters")
    void shouldCreateWithValidParameters() {
      // Arrange
      OperationMode mode = OperationMode.ON_DEMAND;
      OpenWeatherApiClient client = mock(OpenWeatherApiClient.class);
      WeatherCache cache = new WeatherCache();

      // Act
      WeatherSDK sdk = new WeatherSDK(TEST_API_KEY, mode, client, cache);

      // Assert
      assertThat(sdk).isNotNull();
      assertThat(sdk.isShutdown()).isFalse();
    }

    @Test
    @DisplayName("should reject null API key")
    void shouldRejectNullApiKey() {
      // Arrange
      OperationMode mode = OperationMode.ON_DEMAND;
      OpenWeatherApiClient client = mock(OpenWeatherApiClient.class);
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> new WeatherSDK(null, mode, client, cache))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("apiKey must not be null");
    }

    @Test
    @DisplayName("should reject null mode")
    void shouldRejectNullMode() {
      // Arrange
      String apiKey = "test-key";
      OpenWeatherApiClient client = mock(OpenWeatherApiClient.class);
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> new WeatherSDK(apiKey, null, client, cache))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("mode must not be null");
    }

    @Test
    @DisplayName("should reject null client")
    void shouldRejectNullClient() {
      // Arrange
      String apiKey = "test-key";
      OperationMode mode = OperationMode.ON_DEMAND;
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> new WeatherSDK(apiKey, mode, null, cache))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("client must not be null");
    }

    @Test
    @DisplayName("should reject null cache")
    void shouldRejectNullCache() {
      // Arrange
      String apiKey = "test-key";
      OpenWeatherApiClient client = mock(OpenWeatherApiClient.class);
      OperationMode mode = OperationMode.ON_DEMAND;

      // Act & Assert
      assertThatThrownBy(() -> new WeatherSDK(apiKey, mode, client, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cache must not be null");
    }
  }

  @Nested
  @DisplayName("getWeather()")
  class GetWeatherTests {

    @Test
    @DisplayName("should return weather from cache if fresh")
    void shouldReturnWeatherFromCacheIfFresh() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);

      // First call - fetch from API and cache
      Weather firstResult = sdk.getWeather(TEST_CITY);

      // Act - Second call should use cache
      Weather secondResult = sdk.getWeather(TEST_CITY);

      // Assert
      assertThat(secondResult).isEqualTo(firstResult);
      verify(mockClient, times(1)).getCurrentWeather(TEST_CITY); // Only called once
    }

    @Test
    @DisplayName("should fetch from API if cache is empty")
    void shouldFetchFromApiIfCacheEmpty() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);

      // Act
      Weather result = sdk.getWeather(TEST_CITY);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.cityName()).isEqualTo(TEST_CITY);
      verify(mockClient, times(1)).getCurrentWeather(TEST_CITY);
    }

    @Test
    @DisplayName("should fetch from API if cache is expired")
    void shouldFetchFromApiIfCacheExpired() throws Exception {
      // Arrange
      WeatherCache shortTtlCache = new WeatherCache(10, Duration.ofMillis(100));
      WeatherSDK sdkWithShortTtl =
          new WeatherSDK(TEST_API_KEY, OperationMode.ON_DEMAND, mockClient, shortTtlCache);

      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);

      // First call - populate cache
      sdkWithShortTtl.getWeather(TEST_CITY);

      // Wait for cache to expire
      Thread.sleep(150);

      // Act - Second call should fetch from API (cache expired)
      Weather result = sdkWithShortTtl.getWeather(TEST_CITY);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.cityName()).isEqualTo(TEST_CITY);
      verify(mockClient, times(2)).getCurrentWeather(TEST_CITY); // Called twice
    }

    @Test
    @DisplayName("should update cache after API fetch")
    void shouldUpdateCacheAfterApiFetch() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);

      // Act
      sdk.getWeather(TEST_CITY);

      // Assert
      assertThat(sdk.isCached(TEST_CITY)).isTrue();
      assertThat(sdk.isFresh(TEST_CITY)).isTrue();
    }

    @Test
    @DisplayName("should throw exception when API call fails")
    void shouldThrowExceptionWhenApiCallFails() throws OpenWeatherApiException {
      // Arrange
      when(mockClient.getCurrentWeather(TEST_CITY))
          .thenThrow(new OpenWeatherApiException("API error"));

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(TEST_CITY))
          .isInstanceOf(WeatherSDKException.class)
          .hasMessageContaining("Failed to fetch weather for city: " + TEST_CITY)
          .hasCauseInstanceOf(OpenWeatherApiException.class);
    }

    @Test
    @DisplayName("should reject null city name")
    void shouldRejectNullCityName() {
      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(blankCityName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }

    @Test
    @DisplayName("should throw IllegalStateException when SDK is shut down")
    void shouldThrowExceptionWhenShutdown() {
      // Arrange
      sdk.shutdown();

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(TEST_CITY))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");
    }
  }

  @Nested
  @DisplayName("isCached()")
  class IsCachedTests {

    @Test
    @DisplayName("should return true when city is cached")
    void shouldReturnTrueWhenCached() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);
      sdk.getWeather(TEST_CITY);

      // Act
      boolean result = sdk.isCached(TEST_CITY);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when city is not cached")
    void shouldReturnFalseWhenNotCached() {
      // Act
      boolean result = sdk.isCached(TEST_CITY);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should reject null city name")
    void shouldRejectNullCityName() {
      // Act & Assert
      assertThatThrownBy(() -> sdk.isCached(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Act & Assert
      assertThatThrownBy(() -> sdk.isCached(blankCityName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }

    @Test
    @DisplayName("should throw IllegalStateException when SDK is shut down")
    void shouldThrowExceptionWhenShutdown() {
      // Arrange
      sdk.shutdown();

      // Act & Assert
      assertThatThrownBy(() -> sdk.isCached(TEST_CITY))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");
    }
  }

  @Nested
  @DisplayName("isFresh()")
  class IsFreshTests {

    @Test
    @DisplayName("should return true when fresh")
    void shouldReturnTrueWhenFresh() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);
      sdk.getWeather(TEST_CITY);

      // Act
      boolean result = sdk.isFresh(TEST_CITY);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when cache is expired")
    void shouldReturnFalseWhenExpired() throws Exception {
      // Arrange
      WeatherCache shortTtlCache = new WeatherCache(10, Duration.ofMillis(100));
      WeatherSDK sdkWithShortTtl =
          new WeatherSDK(TEST_API_KEY, OperationMode.ON_DEMAND, mockClient, shortTtlCache);

      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);

      sdkWithShortTtl.getWeather(TEST_CITY);

      // Wait for cache to expire
      Thread.sleep(150);

      // Act
      boolean result = sdkWithShortTtl.isFresh(TEST_CITY);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when not cached")
    void shouldReturnFalseWhenNotCached() {
      // Act
      boolean result = sdk.isFresh(TEST_CITY);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should reject null city name")
    void shouldRejectNullCityName() {
      // Act & Assert
      assertThatThrownBy(() -> sdk.isFresh(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Act & Assert
      assertThatThrownBy(() -> sdk.isFresh(blankCityName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }

    @Test
    @DisplayName("should throw IllegalStateException when SDK is shut down")
    void shouldThrowExceptionWhenShutdown() {
      // Arrange
      sdk.shutdown();

      // Act & Assert
      assertThatThrownBy(() -> sdk.isFresh(TEST_CITY))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");
    }
  }

  @Nested
  @DisplayName("getCachedCities()")
  class GetCachedCitiesTests {

    @Test
    @DisplayName("should return empty set when cache is empty")
    void shouldReturnEmptySetWhenCacheEmpty() {
      // Act
      Set<String> result = sdk.getCachedCities();

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return all cached cities")
    void shouldReturnAllCachedCities() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      when(mockClient.getCurrentWeather(city1)).thenReturn(createMockApiResponse(city1));
      when(mockClient.getCurrentWeather(city2)).thenReturn(createMockApiResponse(city2));
      when(mockClient.getCurrentWeather(city3)).thenReturn(createMockApiResponse(city3));

      sdk.getWeather(city1);
      sdk.getWeather(city2);
      sdk.getWeather(city3);

      // Act
      Set<String> result = sdk.getCachedCities();

      // Assert
      assertThat(result).hasSize(3);
      assertThat(result).containsExactlyInAnyOrder("london", "paris", "berlin");
    }

    @Test
    @DisplayName("should return unmodifiable set")
    void shouldReturnUnmodifiableSet() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);
      sdk.getWeather(TEST_CITY);

      // Act
      Set<String> result = sdk.getCachedCities();

      // Assert
      assertThatThrownBy(() -> result.add("Paris"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when SDK is shut down")
    void shouldThrowExceptionWhenShutdown() {
      // Arrange
      sdk.shutdown();

      // Act & Assert
      assertThatThrownBy(() -> sdk.getCachedCities())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");
    }
  }

  @Nested
  @DisplayName("clearCache()")
  class ClearCacheTests {

    @Test
    @DisplayName("should clear all cached data")
    void shouldClearAllCachedData() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);
      sdk.getWeather(TEST_CITY);
      assertThat(sdk.isCached(TEST_CITY)).isTrue();

      // Act
      sdk.clearCache();

      // Assert
      assertThat(sdk.isCached(TEST_CITY)).isFalse();
      assertThat(sdk.getCachedCities()).isEmpty();
    }

    @Test
    @DisplayName("should throw IllegalStateException when SDK is shut down")
    void shouldThrowExceptionWhenShutdown() {
      // Arrange
      sdk.shutdown();

      // Act & Assert
      assertThatThrownBy(() -> sdk.clearCache())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");
    }
  }

  @Nested
  @DisplayName("shutdown()")
  class ShutdownTests {

    @Test
    @DisplayName("should mark as shutdown")
    void shouldMarkAsShutdown() {
      // Act
      sdk.shutdown();

      // Assert
      assertThat(sdk.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should clear cache on shutdown")
    void shouldClearCacheOnShutdown() throws WeatherSDKException, OpenWeatherApiException {
      // Arrange
      OpenWeatherApiResponse apiResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(TEST_CITY)).thenReturn(apiResponse);
      sdk.getWeather(TEST_CITY);

      // Act
      sdk.shutdown();

      // Assert
      assertThat(cache.getCachedCities()).isEmpty();
    }

    @Test
    @DisplayName("should be idempotent")
    void shouldBeIdempotent() {
      // Act
      sdk.shutdown();
      sdk.shutdown();
      sdk.shutdown();

      // Assert
      assertThat(sdk.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should prevent further operations")
    void shouldPreventFurtherOperations() {
      // Arrange
      sdk.shutdown();

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(TEST_CITY))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");

      assertThatThrownBy(() -> sdk.isCached(TEST_CITY))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");

      assertThatThrownBy(() -> sdk.isFresh(TEST_CITY))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");

      assertThatThrownBy(() -> sdk.getCachedCities())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");

      assertThatThrownBy(() -> sdk.clearCache())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");
    }
  }

  @Nested
  @DisplayName("isShutdown()")
  class IsShutdownTests {

    @Test
    @DisplayName("should return false initially")
    void shouldReturnFalseInitially() {
      // Act
      boolean result = sdk.isShutdown();

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true after shutdown")
    void shouldReturnTrueAfterShutdown() {
      // Arrange
      sdk.shutdown();

      // Act
      boolean result = sdk.isShutdown();

      // Assert
      assertThat(result).isTrue();
    }
  }

  // Helper methods
  private OpenWeatherApiResponse createMockApiResponse(String cityName) {
    return new OpenWeatherApiResponse(
        List.of(new OpenWeatherApiResponse.WeatherDto("Clear", "clear sky")),
        new OpenWeatherApiResponse.MainDto(293.15, 291.15),
        new OpenWeatherApiResponse.WindDto(5.5, 180),
        10000,
        Instant.now().getEpochSecond(),
        new OpenWeatherApiResponse.SysDto(
            Instant.now().minusSeconds(21600).getEpochSecond(),
            Instant.now().plusSeconds(21600).getEpochSecond()),
        3600,
        cityName);
  }
}
