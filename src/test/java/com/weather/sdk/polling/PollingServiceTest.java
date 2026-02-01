package com.weather.sdk.polling;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.weather.sdk.cache.WeatherCache;
import com.weather.sdk.domain.model.Weather;
import com.weather.sdk.infrastructure.client.OpenWeatherApiClient;
import com.weather.sdk.infrastructure.dto.OpenWeatherApiResponse;
import com.weather.sdk.infrastructure.exception.OpenWeatherApiException;
import com.weather.sdk.infrastructure.mapper.WeatherMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link PollingService}.
 *
 * <p>Test strategy: Mock-based testing with focus on lifecycle management, scheduling behavior, and
 * error handling. Uses Thread.sleep() for timing-sensitive tests.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("PollingService")
class PollingServiceTest {

  private static final String TEST_CITY = "London";
  private static final Duration SHORT_INTERVAL = Duration.ofMillis(100);

  private OpenWeatherApiClient mockClient;
  private WeatherCache cache;
  private PollingService pollingService;

  @BeforeEach
  void setUp() {
    mockClient = mock(OpenWeatherApiClient.class);
    cache = new WeatherCache(10, Duration.ofMinutes(10));
  }

  @AfterEach
  void tearDown() {
    if (pollingService != null && pollingService.isRunning()) {
      pollingService.shutdown();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create with default interval")
    void shouldCreateWithDefaultInterval() {
      // Act
      pollingService = new PollingService(cache, mockClient);

      // Assert
      assertThat(pollingService).isNotNull();
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should create with custom interval")
    void shouldCreateWithCustomInterval() {
      // Arrange
      Duration customInterval = Duration.ofMinutes(5);

      // Act
      pollingService = new PollingService(cache, mockClient, customInterval);

      // Assert
      assertThat(pollingService).isNotNull();
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should reject null cache")
    void shouldRejectNullCache() {
      // Act & Assert
      assertThatThrownBy(() -> new PollingService(null, mockClient))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cache must not be null");
    }

    @Test
    @DisplayName("should reject null client")
    void shouldRejectNullClient() {
      // Act & Assert
      assertThatThrownBy(() -> new PollingService(cache, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("client must not be null");
    }

    @Test
    @DisplayName("should reject null polling interval")
    void shouldRejectNullPollingInterval() {
      // Act & Assert
      assertThatThrownBy(() -> new PollingService(cache, mockClient, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("pollingInterval must not be null");
    }

    @Test
    @DisplayName("should reject negative polling interval")
    void shouldRejectNegativePollingInterval() {
      // Arrange
      Duration negativeInterval = Duration.ofMinutes(-5);

      // Act & Assert
      assertThatThrownBy(() -> new PollingService(cache, mockClient, negativeInterval))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("pollingInterval must be positive");
    }

    @Test
    @DisplayName("should reject zero polling interval")
    void shouldRejectZeroPollingInterval() {
      // Arrange
      Duration zeroInterval = Duration.ZERO;

      // Act & Assert
      assertThatThrownBy(() -> new PollingService(cache, mockClient, zeroInterval))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("pollingInterval must be positive");
    }
  }

  @Nested
  @DisplayName("isRunning() Tests")
  class IsRunningTests {

    @Test
    @DisplayName("should return false initially")
    void shouldReturnFalseInitially() {
      // Arrange
      pollingService = new PollingService(cache, mockClient);

      // Act
      boolean result = pollingService.isRunning();

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true after start")
    void shouldReturnTrueAfterStart() {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);

      // Act
      pollingService.start();

      // Assert
      assertThat(pollingService.isRunning()).isTrue();
    }

    @Test
    @DisplayName("should return false after shutdown")
    void shouldReturnFalseAfterShutdown() {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      pollingService.start();
      assertThat(pollingService.isRunning()).isTrue();

      // Act
      pollingService.shutdown();

      // Assert
      assertThat(pollingService.isRunning()).isFalse();
    }
  }

  @Nested
  @DisplayName("start() Tests")
  class StartTests {

    @Test
    @DisplayName("should start polling")
    void shouldStartPolling() {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      assertThat(pollingService.isRunning()).isFalse();

      // Act
      pollingService.start();

      // Assert
      assertThat(pollingService.isRunning()).isTrue();
    }

    @Test
    @DisplayName("should mark as running")
    void shouldMarkAsRunning() {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);

      // Act
      pollingService.start();

      // Assert
      assertThat(pollingService.isRunning()).isTrue();
    }

    @Test
    @DisplayName("should be idempotent")
    void shouldBeIdempotent() {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);

      // Act - Call start() multiple times
      pollingService.start();
      pollingService.start();
      pollingService.start();

      // Assert - Should not throw exception
      assertThat(pollingService.isRunning()).isTrue();
    }

    @Test
    @DisplayName("should schedule with zero initial delay")
    void shouldScheduleWithZeroInitialDelay() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(TEST_CITY, createMockWeather(TEST_CITY));

      when(mockClient.getCurrentWeather(normalizedCity))
          .thenReturn(createMockApiResponse(TEST_CITY));

      // Act
      pollingService.start();
      Thread.sleep(50); // Allow initial poll to execute

      // Assert
      verify(mockClient, atLeastOnce()).getCurrentWeather(normalizedCity);
    }

    @Test
    @DisplayName("should schedule with correct interval")
    void shouldScheduleWithCorrectInterval() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(TEST_CITY, createMockWeather(TEST_CITY));

      when(mockClient.getCurrentWeather(normalizedCity))
          .thenReturn(createMockApiResponse(TEST_CITY));

      // Act
      pollingService.start();
      Thread.sleep(SHORT_INTERVAL.toMillis() * 2 + 50);

      // Assert - Should have polled at least twice (initial + interval)
      verify(mockClient, atLeast(2)).getCurrentWeather(normalizedCity);
    }
  }

  @Nested
  @DisplayName("shutdown() Tests")
  class ShutdownTests {

    @Test
    @DisplayName("should stop polling")
    void shouldStopPolling() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(TEST_CITY, createMockWeather(TEST_CITY));

      when(mockClient.getCurrentWeather(normalizedCity))
          .thenReturn(createMockApiResponse(TEST_CITY));

      pollingService.start();
      Thread.sleep(50);

      int callsBeforeShutdown = mockingDetails(mockClient).getInvocations().size();

      // Act
      pollingService.shutdown();
      Thread.sleep(SHORT_INTERVAL.toMillis() + 50);

      // Assert - No additional polling after shutdown
      int callsAfterShutdown = mockingDetails(mockClient).getInvocations().size();
      assertThat(callsAfterShutdown).isEqualTo(callsBeforeShutdown);
    }

    @Test
    @DisplayName("should mark as not running")
    void shouldMarkAsNotRunning() {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      pollingService.start();
      assertThat(pollingService.isRunning()).isTrue();

      // Act
      pollingService.shutdown();

      // Assert
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should be idempotent")
    void shouldBeIdempotent() {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      pollingService.start();

      // Act - Call shutdown() multiple times
      pollingService.shutdown();
      pollingService.shutdown();
      pollingService.shutdown();

      // Assert - Should not throw exception
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should shutdown scheduler")
    void shouldShutdownScheduler() throws Exception {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      pollingService.start();

      // Act
      pollingService.shutdown();

      // Assert - scheduler is shut down
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should wait for termination")
    void shouldWaitForTermination() throws Exception {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      pollingService.start();

      // Act
      pollingService.shutdown();

      // Assert - shutdown completes without hanging
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should force shutdown if termination times out")
    void shouldForceShutdownIfTerminationTimesOut() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, Duration.ofMillis(50));
      cache.put(TEST_CITY, createMockWeather(TEST_CITY));

      // Mock hangs indefinitely
      when(mockClient.getCurrentWeather(normalizedCity))
          .thenAnswer(
              invocation -> {
                Thread.sleep(60000); // Sleep longer than shutdown timeout (30s)
                return createMockApiResponse(TEST_CITY);
              });

      pollingService.start();
      Thread.sleep(100); // Ensure polling task starts

      // Act
      pollingService.shutdown(); // Should force shutdown after 30s timeout

      // Assert - shutdown completes (doesn't hang forever)
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should handle InterruptedException")
    void shouldHandleInterruptedException() throws Exception {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      pollingService.start();

      Thread shutdownThread =
          new Thread(
              () -> {
                pollingService.shutdown();
              });

      // Act
      shutdownThread.start();
      Thread.sleep(10); // Give shutdown a moment to start waiting
      shutdownThread.interrupt(); // Interrupt during awaitTermination
      shutdownThread.join(1000); // Wait for shutdown to complete

      // Assert - shutdown completes despite interruption
      assertThat(pollingService.isRunning()).isFalse();
      assertThat(shutdownThread.isInterrupted()).isTrue();
    }

    @Test
    @DisplayName("should restore interrupt status")
    void shouldRestoreInterruptStatus() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(TEST_CITY, createMockWeather(TEST_CITY));

      // Mock with delay to keep task running during shutdown
      when(mockClient.getCurrentWeather(normalizedCity))
          .thenAnswer(
              invocation -> {
                Thread.sleep(200);
                return createMockApiResponse(TEST_CITY);
              });

      pollingService.start();
      Thread.sleep(50); // Let polling task start

      AtomicBoolean interruptStatusRestored = new AtomicBoolean(false);

      Thread shutdownThread =
          new Thread(
              () -> {
                pollingService.shutdown();
                interruptStatusRestored.set(Thread.currentThread().isInterrupted());
              });

      // Act
      shutdownThread.start();
      Thread.sleep(100); // Give shutdown time to enter awaitTermination
      shutdownThread.interrupt();
      shutdownThread.join(2000);

      // Assert
      assertThat(interruptStatusRestored.get()).isTrue();
      assertThat(pollingService.isRunning()).isFalse();
    }
  }

  @Nested
  @DisplayName("Polling Behavior Tests")
  class PollingBehaviorTests {

    @Test
    @DisplayName("should poll all cached cities")
    void shouldPollAllCachedCities() throws Exception {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(city1, createMockWeather(city1));
      cache.put(city2, createMockWeather(city2));
      cache.put(city3, createMockWeather(city3));

      when(mockClient.getCurrentWeather(anyString())).thenReturn(createMockApiResponse(city1));

      // Act
      pollingService.start();
      Thread.sleep(50);

      // Assert - All cities should be polled
      verify(mockClient, atLeastOnce()).getCurrentWeather(city1.toLowerCase());
      verify(mockClient, atLeastOnce()).getCurrentWeather(city2.toLowerCase());
      verify(mockClient, atLeastOnce()).getCurrentWeather(city3.toLowerCase());
    }

    @Test
    @DisplayName("should update cache after polling")
    void shouldUpdateCacheAfterPolling() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);

      Weather initialWeather = createMockWeather(TEST_CITY);
      cache.put(TEST_CITY, initialWeather);

      OpenWeatherApiResponse freshResponse = createMockApiResponse(TEST_CITY);
      when(mockClient.getCurrentWeather(normalizedCity)).thenReturn(freshResponse);

      // Act
      pollingService.start();
      Thread.sleep(50);

      // Assert
      assertThat(cache.isFresh(TEST_CITY)).isTrue();
      verify(mockClient, atLeastOnce()).getCurrentWeather(normalizedCity);
    }

    @Test
    @DisplayName("should poll multiple cities")
    void shouldPollMultipleCities() throws Exception {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(city1, createMockWeather(city1));
      cache.put(city2, createMockWeather(city2));
      cache.put(city3, createMockWeather(city3));

      when(mockClient.getCurrentWeather(city1.toLowerCase()))
          .thenReturn(createMockApiResponse(city1));
      when(mockClient.getCurrentWeather(city2.toLowerCase()))
          .thenReturn(createMockApiResponse(city2));
      when(mockClient.getCurrentWeather(city3.toLowerCase()))
          .thenReturn(createMockApiResponse(city3));

      // Act
      pollingService.start();
      Thread.sleep(50);

      // Assert
      verify(mockClient).getCurrentWeather(city1.toLowerCase());
      verify(mockClient).getCurrentWeather(city2.toLowerCase());
      verify(mockClient).getCurrentWeather(city3.toLowerCase());
    }

    @Test
    @DisplayName("should handle API error gracefully")
    void shouldHandleApiErrorGracefully() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(TEST_CITY, createMockWeather(TEST_CITY));

      when(mockClient.getCurrentWeather(normalizedCity))
          .thenThrow(new OpenWeatherApiException("API error"));

      // Act
      pollingService.start();
      Thread.sleep(50);

      // Assert - Should not crash, polling continues
      assertThat(pollingService.isRunning()).isTrue();
      verify(mockClient, atLeastOnce()).getCurrentWeather(normalizedCity);
    }

    @Test
    @DisplayName("should continue polling after single city error")
    void shouldContinuePollingAfterSingleCityError() throws Exception {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";

      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(city1, createMockWeather(city1));
      cache.put(city2, createMockWeather(city2));

      when(mockClient.getCurrentWeather(city1.toLowerCase()))
          .thenThrow(new OpenWeatherApiException("API error"));
      when(mockClient.getCurrentWeather(city2.toLowerCase()))
          .thenReturn(createMockApiResponse(city2));

      // Act
      pollingService.start();
      Thread.sleep(50);

      // Assert - Paris should still be polled despite London error
      verify(mockClient, atLeastOnce()).getCurrentWeather(city1.toLowerCase());
      verify(mockClient, atLeastOnce()).getCurrentWeather(city2.toLowerCase());
      assertThat(pollingService.isRunning()).isTrue();
    }

    @Test
    @DisplayName("should handle empty cache")
    void shouldHandleEmptyCache() throws Exception {
      // Arrange
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      // Cache is empty - no cities added

      // Act
      pollingService.start();
      Thread.sleep(50);

      // Assert - Should not crash, no API calls made
      assertThat(pollingService.isRunning()).isTrue();
      verify(mockClient, never()).getCurrentWeather(anyString());
    }

    @Test
    @DisplayName("should poll periodically")
    void shouldPollPeriodically() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(TEST_CITY, createMockWeather(TEST_CITY));

      when(mockClient.getCurrentWeather(normalizedCity))
          .thenReturn(createMockApiResponse(TEST_CITY));

      // Act
      pollingService.start();
      Thread.sleep(SHORT_INTERVAL.toMillis() * 3 + 50);

      // Assert - Should poll at least 3 times (initial + 2 intervals)
      verify(mockClient, atLeast(3)).getCurrentWeather(normalizedCity);
    }

    @Test
    @DisplayName("should update cache in background")
    void shouldUpdateCacheInBackground() throws Exception {
      // Arrange
      String normalizedCity = TEST_CITY.toLowerCase();
      pollingService = new PollingService(cache, mockClient, SHORT_INTERVAL);
      cache.put(TEST_CITY, createMockWeather(TEST_CITY));

      when(mockClient.getCurrentWeather(normalizedCity))
          .thenReturn(createMockApiResponse(TEST_CITY));

      // Act
      pollingService.start();

      // Main thread is not blocked
      assertThat(pollingService.isRunning()).isTrue();

      // Wait for background update
      Thread.sleep(SHORT_INTERVAL.toMillis() + 50);

      // Assert - Cache updated in background
      assertThat(cache.isFresh(TEST_CITY)).isTrue();
      verify(mockClient, atLeast(1)).getCurrentWeather(normalizedCity);
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

  private Weather createMockWeather(String cityName) {
    return WeatherMapper.map(createMockApiResponse(cityName));
  }
}
