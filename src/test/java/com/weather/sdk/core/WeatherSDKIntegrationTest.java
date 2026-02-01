package com.weather.sdk.core;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.weather.sdk.cache.WeatherCache;
import com.weather.sdk.domain.model.Weather;
import com.weather.sdk.infrastructure.client.OpenWeatherApiClient;
import com.weather.sdk.infrastructure.exception.OpenWeatherApiException;
import com.weather.sdk.polling.PollingService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive integration tests for WeatherSDK.
 *
 * <p>Tests the complete SDK workflow including API client, mapper, cache, and both operation modes.
 * Uses WireMock to mock OpenWeatherMap API responses.
 *
 * <p><strong>Test coverage:</strong>
 *
 * <ul>
 *   <li>ON_DEMAND mode - fetch on request, cache behavior
 *   <li>POLLING mode - background updates, zero-latency responses
 *   <li>Cache management - capacity, TTL, LRU eviction
 *   <li>Error handling - 404, 401, 429, 5xx, network errors
 *   <li>Factory integration - singleton pattern, lifecycle
 *   <li>Concurrency - thread-safe operations
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("WeatherSDK Integration Tests")
class WeatherSDKIntegrationTest {

  private static final String TEST_API_KEY = "test-api-key";
  private static final int WIREMOCK_PORT = 8089;
  private static final String WIREMOCK_BASE_URL =
      "http://localhost:" + WIREMOCK_PORT + "/data/2.5/weather";

  private static WireMockServer wireMockServer;

  @BeforeAll
  static void startWireMock() {
    wireMockServer = new WireMockServer(WIREMOCK_PORT);
    wireMockServer.start();
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMockServer != null && wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  @AfterEach
  void cleanup() {
    WeatherSDKFactory.destroyAll();
    wireMockServer.resetAll();
  }

  @Nested
  @DisplayName("ON_DEMAND Mode")
  class OnDemandModeTests {

    @Test
    @DisplayName("should fetch weather from API on first request")
    void shouldFetchWeatherFromApi() throws WeatherSDKException {
      // Arrange
      String cityName = "London";

      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act
      Weather weather = sdk.getWeather(cityName);

      // Assert
      assertThat(weather).isNotNull();
      assertThat(weather.cityName()).isEqualTo("London");
      assertThat(weather.condition().main()).isEqualTo("Clouds");
      assertThat(weather.condition().description()).isEqualTo("scattered clouds");
      assertThat(weather.temperature().temp()).isEqualTo(293.15);
      assertThat(weather.temperature().feelsLike()).isEqualTo(291.15);
      assertThat(weather.wind().speed()).isEqualTo(5.5);
      assertThat(weather.wind().direction()).isEqualTo(180);
      assertThat(weather.visibility().meters()).isEqualTo(10000);

      wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/data/2.5/weather")));
    }

    @Test
    @DisplayName("should return cached weather on second request")
    void shouldReturnCachedWeather() throws WeatherSDKException {
      // Arrange
      String cityName = "London";

      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act - First request fetches from API and caches
      Weather firstResult = sdk.getWeather(cityName);

      // Act - Second request should return cached data
      Weather secondResult = sdk.getWeather(cityName);

      // Assert - Second result is identical to first (from cache)
      assertThat(secondResult).isNotNull();
      assertThat(secondResult).isEqualTo(firstResult);
      assertThat(secondResult.cityName()).isEqualTo("London");

      // Assert - API was called only once (cache hit on second request)
      wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/data/2.5/weather")));
    }

    @Test
    @DisplayName("should refetch after cache expiration")
    void shouldRefetchAfterCacheExpiration() throws Exception {
      // Arrange
      String cityName = "London";

      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      // Create SDK with short TTL for testing
      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(100));
      WeatherSDK sdk = new WeatherSDK(OperationMode.ON_DEMAND, client, cache);

      // Act - First request fetches from API
      Weather firstResult = sdk.getWeather(cityName);

      // Assert - First request successful
      assertThat(firstResult).isNotNull();
      assertThat(firstResult.cityName()).isEqualTo("London");

      // Wait for cache to expire
      Thread.sleep(150);

      // Act - Second request should refetch from API (cache expired)
      Weather secondResult = sdk.getWeather(cityName);

      // Assert - Second request successful
      assertThat(secondResult).isNotNull();
      assertThat(secondResult.cityName()).isEqualTo("London");

      // Assert - API was called twice (cache expired)
      wireMockServer.verify(2, getRequestedFor(urlPathEqualTo("/data/2.5/weather")));

      sdk.shutdown();
    }

    @Test
    @DisplayName("should handle multiple cities independently")
    void shouldHandleMultipleCities() throws WeatherSDKException {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(city3, "Rain", "light rain", 285.15, 7.2);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act
      Weather londonWeather = sdk.getWeather(city1);
      Weather parisWeather = sdk.getWeather(city2);
      Weather berlinWeather = sdk.getWeather(city3);

      // Assert - Each city has distinct weather data
      assertThat(londonWeather.cityName()).isEqualTo("London");
      assertThat(londonWeather.condition().main()).isEqualTo("Clouds");

      assertThat(parisWeather.cityName()).isEqualTo("Paris");
      assertThat(parisWeather.condition().main()).isEqualTo("Clear");

      assertThat(berlinWeather.cityName()).isEqualTo("Berlin");
      assertThat(berlinWeather.condition().main()).isEqualTo("Rain");

      // Assert - Independent cache management
      assertThat(sdk.getCachedCities())
          .hasSize(3)
          .containsExactlyInAnyOrder("london", "paris", "berlin");

      // Assert - Each city fetched exactly once
      wireMockServer.verify(3, getRequestedFor(urlPathEqualTo("/data/2.5/weather")));
    }

    @Test
    @DisplayName("should normalize city names (case-insensitive)")
    void shouldNormalizeCityNames() throws WeatherSDKException {
      // Arrange
      stubSuccessfulResponse("London", "Clouds", "scattered clouds", 293.15, 5.5);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act - Request with different case variations
      Weather result1 = sdk.getWeather("LONDON");
      Weather result2 = sdk.getWeather("London");
      Weather result3 = sdk.getWeather("london");
      Weather result4 = sdk.getWeather("LoNdOn");

      // Assert - All variations return same data
      assertThat(result1.cityName()).isEqualTo("London");
      assertThat(result2).isEqualTo(result1);
      assertThat(result3).isEqualTo(result1);
      assertThat(result4).isEqualTo(result1);

      // Assert - Cache uses normalized key (single entry)
      assertThat(sdk.getCachedCities()).hasSize(1).containsExactly("london");

      // Assert - API called only once (all variations hit same cache entry)
      wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/data/2.5/weather")));
    }
  }

  @Nested
  @DisplayName("POLLING Mode")
  class PollingModeTests {

    @Test
    @DisplayName("should start polling automatically in POLLING mode")
    void shouldStartPollingAutomatically() throws Exception {
      // Arrange
      String cityName = "London";

      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMinutes(10));
      PollingService pollingService = new PollingService(cache, client, Duration.ofMillis(100));
      WeatherSDK sdk = new WeatherSDK(OperationMode.POLLING, client, cache);

      // Pre-populate cache
      sdk.getWeather(cityName);

      // Record API calls before polling
      final int callsBeforePolling = wireMockServer.getAllServeEvents().size();

      // Act - Start polling
      pollingService.start();
      Thread.sleep(250); // Wait for 2-3 polling cycles (100ms interval)

      // Assert - Polling is running
      assertThat(pollingService.isRunning()).isTrue();

      // Assert - Cache still fresh
      assertThat(sdk.isFresh(cityName)).isTrue();

      // Assert - API called multiple times (initial + at least 2 polls)
      int callsAfterPolling = wireMockServer.getAllServeEvents().size();
      assertThat(callsAfterPolling).isGreaterThanOrEqualTo(callsBeforePolling + 2);

      // Cleanup
      pollingService.shutdown();
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should update cache in background without blocking")
    void shouldUpdateCacheInBackground() throws Exception {
      // Arrange
      String cityName = "London";

      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(300)); // TTL > polling interval
      PollingService pollingService = new PollingService(cache, client, Duration.ofMillis(100));
      WeatherSDK sdk = new WeatherSDK(OperationMode.POLLING, client, cache);

      // Pre-populate cache
      Weather initialWeather = sdk.getWeather(cityName);
      assertThat(initialWeather).isNotNull();
      assertThat(sdk.isFresh(cityName)).isTrue();

      // Act - Start polling (proactive background refresh)
      pollingService.start();
      assertThat(pollingService.isRunning())
          .as("Polling should run in background without blocking main thread")
          .isTrue();

      // Wait longer than TTL (cache WOULD expire in ON_DEMAND mode)
      Thread.sleep(350);

      // Assert - Cache still fresh (polling prevented expiration)
      assertThat(sdk.isFresh(cityName))
          .as("Cache should remain fresh due to proactive background polling")
          .isTrue();

      // Assert - Zero-latency response (no API call needed)
      int callsBeforeGet = wireMockServer.getAllServeEvents().size();
      Weather cachedWeather = sdk.getWeather(cityName);
      int callsAfterGet = wireMockServer.getAllServeEvents().size();

      assertThat(cachedWeather).isNotNull();
      assertThat(cachedWeather.cityName()).isEqualTo("London");
      assertThat(callsAfterGet)
          .as("getWeather() should return instantly from cache (zero-latency benefit)")
          .isEqualTo(callsBeforeGet);

      // Cleanup
      pollingService.shutdown();
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should provide zero-latency responses with pre-fetched data")
    void shouldProvideZeroLatencyResponses() throws Exception {
      // Arrange
      String cityName = "London";

      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMinutes(10));
      PollingService pollingService = new PollingService(cache, client, Duration.ofMillis(100));
      WeatherSDK sdk = new WeatherSDK(OperationMode.POLLING, client, cache);

      // Pre-populate cache and start polling
      sdk.getWeather(cityName);
      pollingService.start();
      Thread.sleep(150); // Ensure polling has refreshed cache (> 1 interval)

      // Record baseline API calls
      int callsBeforeRequests = wireMockServer.getAllServeEvents().size();

      // Act - Simulate high-frequency access (3 rapid requests)
      long startTime = System.nanoTime();
      Weather result1 = sdk.getWeather(cityName);
      Weather result2 = sdk.getWeather(cityName);
      Weather result3 = sdk.getWeather(cityName);
      long endTime = System.nanoTime();
      long elapsedMillis = (endTime - startTime) / 1_000_000;

      int callsAfterRequests = wireMockServer.getAllServeEvents().size();

      // Assert - All requests returned identical cached data
      assertThat(result1).isNotNull();
      assertThat(result2).isNotNull();
      assertThat(result3).isNotNull();
      assertThat(result1.cityName()).isEqualTo("London");
      assertThat(result2).isEqualTo(result1);
      assertThat(result3).isEqualTo(result1);

      // Assert - Zero-latency: no API calls during rapid requests
      assertThat(callsAfterRequests)
          .as("Multiple rapid getWeather() calls should NOT trigger API requests (all from cache)")
          .isEqualTo(callsBeforeRequests);

      // Assert - Performance: all 3 requests complete in < 10ms
      assertThat(elapsedMillis)
          .as("3 consecutive cache hits should complete in < 10ms (zero-latency benefit)")
          .isLessThan(10);

      // Cleanup
      pollingService.shutdown();
    }

    @Test
    @DisplayName("should poll multiple cached cities independently")
    void shouldPollMultipleCachedCities() throws Exception {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(city3, "Rain", "light rain", 285.15, 7.2);

      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMinutes(10));
      PollingService pollingService = new PollingService(cache, client, Duration.ofMillis(100));
      WeatherSDK sdk = new WeatherSDK(OperationMode.POLLING, client, cache);

      // Pre-populate cache with multiple cities
      sdk.getWeather(city1);
      sdk.getWeather(city2);
      sdk.getWeather(city3);

      assertThat(sdk.getCachedCities()).hasSize(3);

      // Record baseline (3 initial fetches)
      final int callsBeforePolling = wireMockServer.getAllServeEvents().size();

      // Act - Start polling
      pollingService.start();
      Thread.sleep(250); // Allow 2-3 polling cycles (100ms interval)

      int callsAfterPolling = wireMockServer.getAllServeEvents().size();

      // Assert - Polling is active
      assertThat(pollingService.isRunning()).isTrue();

      // Assert - All cities remain fresh (kept alive by polling)
      assertThat(sdk.isFresh(city1)).isTrue();
      assertThat(sdk.isFresh(city2)).isTrue();
      assertThat(sdk.isFresh(city3)).isTrue();

      // Assert - Polling refreshed all cities (at least 1 complete cycle)
      assertThat(callsAfterPolling)
          .as("Polling should refresh all 3 cities in background")
          .isGreaterThanOrEqualTo(callsBeforePolling + 3);

      // Assert - Each city was polled (proves independence)
      wireMockServer.verify(
          getRequestedFor(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", matching("(?i)london")));
      wireMockServer.verify(
          getRequestedFor(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", matching("(?i)paris")));
      wireMockServer.verify(
          getRequestedFor(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", matching("(?i)berlin")));

      // Cleanup
      pollingService.shutdown();
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should handle API errors gracefully without stopping polling")
    void shouldHandleApiErrorsGracefully() throws Exception {
      // Arrange
      String workingCity = "Paris";
      String failingCity = "London";

      stubSuccessfulResponse(workingCity, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(failingCity, "Clouds", "scattered clouds", 293.15, 5.5);

      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMinutes(10));
      WeatherSDK sdk = new WeatherSDK(OperationMode.POLLING, client, cache);

      Weather londonInitial = sdk.getWeather(failingCity);
      sdk.getWeather(workingCity);

      assertThat(sdk.getCachedCities())
          .as("Both cities should be cached initially")
          .hasSize(2)
          .containsExactlyInAnyOrder(failingCity.toLowerCase(), workingCity.toLowerCase());

      wireMockServer.resetMappings();
      stubSuccessfulResponse(workingCity, "Clear", "clear sky", 288.15, 3.5);
      stubServerError(failingCity);

      // Act - Start polling (London fails, Paris succeeds)
      PollingService pollingService = new PollingService(cache, client, Duration.ofMillis(100));
      pollingService.start();
      Thread.sleep(250); // Allow 2-3 polling cycles

      // Assert - Core Resilience: Polling continues despite errors
      assertThat(pollingService.isRunning())
          .as("Polling must continue running despite transient API errors")
          .isTrue();

      // Assert - Error Isolation: Working city unaffected by failing city
      assertThat(sdk.isFresh(workingCity))
          .as("Working city (%s) should remain fresh despite %s failing", workingCity, failingCity)
          .isTrue();

      // Assert - Graceful Degradation: Failing city serves stale but valid data
      assertThat(sdk.isCached(failingCity))
          .as("Failing city (%s) should remain cached", failingCity)
          .isTrue();

      assertThat(sdk.getWeather(failingCity))
          .as("Failing city should return stale cached data (graceful degradation)")
          .isEqualTo(londonInitial);

      // Assert - Fault Tolerance: Both cities attempted (error doesn't break polling loop)
      int workingCityCalls = countRequests(workingCity);
      int failingCityCalls = countRequests(failingCity);

      assertThat(workingCityCalls)
          .as("%s should be polled at least twice (initial + 1-2 cycles)", workingCity)
          .isGreaterThanOrEqualTo(2);

      assertThat(failingCityCalls)
          .as(
              "%s should be attempted at least twice (proves error doesn't stop polling)",
              failingCity)
          .isGreaterThanOrEqualTo(2);

      // Cleanup
      pollingService.shutdown();
      assertThat(pollingService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should stop polling after shutdown")
    void shouldStopPollingAfterShutdown() throws Exception {
      // Arrange
      String cityName = "London";
      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMinutes(10));
      PollingService pollingService = new PollingService(cache, client, Duration.ofMillis(100));
      WeatherSDK sdk = new WeatherSDK(OperationMode.POLLING, client, cache);

      sdk.getWeather(cityName);
      pollingService.start();
      Thread.sleep(150); // Ensure polling is active

      assertThat(pollingService.isRunning()).isTrue();
      final int callsBeforeShutdown = wireMockServer.getAllServeEvents().size();

      // Act
      pollingService.shutdown();

      // Assert - Immediate stop
      assertThat(pollingService.isRunning()).as("Polling should stop immediately").isFalse();

      // Assert - No residual polling
      Thread.sleep(250); // 2-3 cycles worth
      assertThat(wireMockServer.getAllServeEvents().size())
          .as("No API calls after shutdown")
          .isEqualTo(callsBeforeShutdown);

      // Assert - Idempotency
      pollingService.shutdown();
      assertThat(pollingService.isRunning()).isFalse();
    }
  }

  @Nested
  @DisplayName("Cache Management")
  class CacheManagementTests {

    @Test
    @DisplayName("should respect cache capacity limit with LRU eviction")
    void shouldRespectCacheCapacityLimit() throws WeatherSDKException {
      // Arrange
      int capacity = 3;
      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(capacity, Duration.ofMinutes(10));
      WeatherSDK sdk = new WeatherSDK(OperationMode.ON_DEMAND, client, cache);

      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";
      String city4 = "Madrid";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(city3, "Rain", "light rain", 285.15, 7.2);
      stubSuccessfulResponse(city4, "Snow", "light snow", 275.15, 4.0);

      // Act - Fill cache to capacity
      sdk.getWeather(city1);
      sdk.getWeather(city2);
      sdk.getWeather(city3);

      // Assert - Cache at capacity
      assertThat(sdk.getCachedCities())
          .as("Cache should contain exactly %d cities at capacity", capacity)
          .hasSize(capacity)
          .containsExactlyInAnyOrder("london", "paris", "berlin");

      // Act - Exceed capacity (triggers LRU eviction)
      sdk.getWeather(city4);

      // Assert - Capacity enforced
      assertThat(sdk.getCachedCities())
          .as("Cache must not exceed capacity of %d", capacity)
          .hasSize(capacity);

      // Assert - LRU evicted (London oldest)
      assertThat(sdk.getCachedCities())
          .as("LRU city (London) should be evicted")
          .containsExactlyInAnyOrder("paris", "berlin", "madrid")
          .doesNotContain("london");

      // Assert - Eviction is real (re-fetch triggers API call)
      int callsBefore = wireMockServer.getAllServeEvents().size();
      sdk.getWeather(city1); // Re-fetch evicted city

      assertThat(wireMockServer.getAllServeEvents().size())
          .as("Evicted city must trigger new API call (proves true eviction)")
          .isEqualTo(callsBefore + 1);
    }

    @Test
    @DisplayName("should evict least recently used city when capacity exceeded")
    void shouldEvictLeastRecentlyUsedCity() throws WeatherSDKException {
      // Arrange
      int capacity = 3;
      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(capacity, Duration.ofMinutes(10));
      WeatherSDK sdk = new WeatherSDK(OperationMode.ON_DEMAND, client, cache);

      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";
      String city4 = "Madrid";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(city3, "Rain", "light rain", 285.15, 7.2);
      stubSuccessfulResponse(city4, "Snow", "light snow", 275.15, 4.0);

      // Act - Fill cache (insertion order: London, Paris, Berlin)
      sdk.getWeather(city1);
      sdk.getWeather(city2);
      sdk.getWeather(city3);

      // Act - Access London (makes it most recently used)
      sdk.getWeather(city1);

      // Act - Add 4th city (should evict Paris, not London)
      sdk.getWeather(city4);

      // Assert - Paris evicted (was LRU), London retained (was accessed)
      assertThat(sdk.getCachedCities())
          .as("LRU city (Paris) should be evicted, recently accessed city (London) retained")
          .hasSize(capacity)
          .containsExactlyInAnyOrder("london", "berlin", "madrid")
          .doesNotContain("paris");

      // Assert - Paris requires API refetch (proves eviction)
      int callsBefore = wireMockServer.getAllServeEvents().size();
      sdk.getWeather(city2);

      assertThat(wireMockServer.getAllServeEvents().size())
          .as("Evicted city (Paris) must trigger API call")
          .isEqualTo(callsBefore + 1);
    }

    @Test
    @DisplayName("should clear all cached data on demand")
    void shouldClearCacheOnDemand() throws WeatherSDKException {
      // Arrange
      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(city3, "Rain", "light rain", 285.15, 7.2);

      // Populate cache
      sdk.getWeather(city1);
      sdk.getWeather(city2);
      sdk.getWeather(city3);

      assertThat(sdk.getCachedCities())
          .as("Cache should contain 3 cities before clear")
          .hasSize(3)
          .containsExactlyInAnyOrder("london", "paris", "berlin");

      // Act
      sdk.clearCache();

      // Assert - Cache completely emptied
      assertThat(sdk.getCachedCities()).as("Cache should be empty after clear").isEmpty();

      // Assert - All cities require fresh API fetch
      final int callsBefore = wireMockServer.getAllServeEvents().size();
      sdk.getWeather(city1);
      sdk.getWeather(city2);
      sdk.getWeather(city3);

      assertThat(wireMockServer.getAllServeEvents().size())
          .as("All cleared cities must trigger API calls")
          .isEqualTo(callsBefore + 3);
    }

    @Test
    @DisplayName("should return list of all cached cities")
    void shouldGetCachedCitiesList() throws WeatherSDKException {
      // Arrange
      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(city3, "Rain", "light rain", 285.15, 7.2);

      // Act - Empty cache initially
      assertThat(sdk.getCachedCities()).as("Cache should be empty initially").isEmpty();

      // Act - Add cities
      sdk.getWeather(city1);
      sdk.getWeather(city2);
      sdk.getWeather(city3);

      // Assert - All cities listed
      assertThat(sdk.getCachedCities())
          .as("Should return all cached cities in normalized form")
          .hasSize(3)
          .containsExactlyInAnyOrder("london", "paris", "berlin");

      // Assert - Returns unmodifiable set
      assertThatThrownBy(() -> sdk.getCachedCities().add("madrid"))
          .as("Returned set should be unmodifiable")
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should check if city is cached")
    void shouldCheckIfCityIsCached() throws WeatherSDKException {
      // Arrange
      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      String cachedCity = "London";
      String uncachedCity = "Paris";

      stubSuccessfulResponse(cachedCity, "Clouds", "scattered clouds", 293.15, 5.5);

      // Assert - Initial state: nothing cached
      assertThat(sdk.isCached(cachedCity))
          .as("City should not be cached before fetching")
          .isFalse();
      assertThat(sdk.isCached(uncachedCity))
          .as("City should not be cached before fetching")
          .isFalse();

      // Act - Cache one city
      sdk.getWeather(cachedCity);

      // Assert - Cached city
      assertThat(sdk.isCached(cachedCity)).as("City should be cached after fetching").isTrue();

      // Assert - Uncached city (isolation)
      assertThat(sdk.isCached(uncachedCity))
          .as("Non-fetched city should remain uncached")
          .isFalse();

      // Assert - Case insensitive
      assertThat(sdk.isCached("LONDON")).isTrue();
      assertThat(sdk.isCached("london")).isTrue();
    }

    @Test
    @DisplayName("should check if cached city data is fresh")
    void shouldCheckIfCityIsFresh() throws Exception {
      // Arrange
      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(200));
      WeatherSDK sdk = new WeatherSDK(OperationMode.ON_DEMAND, client, cache);

      String city = "London";
      stubSuccessfulResponse(city, "Clouds", "scattered clouds", 293.15, 5.5);

      // Assert - Initial state: not fresh (not cached)
      assertThat(sdk.isFresh(city)).as("City should not be fresh before caching").isFalse();

      // Act - Cache city
      sdk.getWeather(city);

      // Assert - Fresh immediately after caching
      assertThat(sdk.isFresh(city)).as("City should be fresh immediately after caching").isTrue();

      // Wait for TTL expiration
      Thread.sleep(250);

      // Assert - No longer fresh after TTL
      assertThat(sdk.isFresh(city)).as("City should not be fresh after TTL expiration").isFalse();

      // Assert - Case insensitive
      sdk.getWeather(city); // Refresh cache
      assertThat(sdk.isFresh("LONDON")).isTrue();
      assertThat(sdk.isFresh("london")).isTrue();

      sdk.shutdown();
    }
  }

  @Nested
  @DisplayName("API Error Handling")
  class ApiErrorHandlingTests {

    @Test
    @DisplayName("should throw exception for 401 Invalid API Key")
    void shouldThrowExceptionFor401InvalidApiKey() {
      // Arrange
      String cityName = "London";

      stubApiError(cityName, 401, "Invalid API key");

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(cityName))
          .isInstanceOf(WeatherSDKException.class)
          .hasMessageContaining("Failed to fetch weather for city: " + cityName)
          .hasCauseInstanceOf(OpenWeatherApiException.class)
          .cause()
          .hasMessageContaining("Invalid API key");
    }

    @Test
    @DisplayName("should throw exception for 404 City Not Found")
    void shouldThrowExceptionFor404CityNotFound() {
      // Arrange
      String cityName = "NonExistentCity";

      stubApiError(cityName, 404, "city not found");

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(cityName))
          .isInstanceOf(WeatherSDKException.class)
          .hasMessageContaining("Failed to fetch weather for city: " + cityName)
          .hasCauseInstanceOf(OpenWeatherApiException.class)
          .cause()
          .hasMessageContaining("City not found");
    }

    @Test
    @DisplayName("should throw exception for 429 Rate Limit Exceeded")
    void shouldThrowExceptionFor429RateLimit() {
      // Arrange
      String cityName = "London";

      stubApiError(cityName, 429, "rate limit exceeded");

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(cityName))
          .isInstanceOf(WeatherSDKException.class)
          .hasMessageContaining("Failed to fetch weather for city: " + cityName)
          .hasCauseInstanceOf(OpenWeatherApiException.class)
          .cause()
          .hasMessageContaining("rate limit");
    }

    @Test
    @DisplayName("should throw exception for 5xx Server Errors")
    void shouldThrowExceptionFor5xxServerError() {
      // Arrange
      String cityName = "London";

      stubApiError(cityName, 500, "Internal server error");

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(cityName))
          .isInstanceOf(WeatherSDKException.class)
          .hasMessageContaining("Failed to fetch weather for city: " + cityName)
          .hasCauseInstanceOf(OpenWeatherApiException.class)
          .cause()
          .hasMessageContaining("server error");
    }

    @Test
    @DisplayName("should handle network errors")
    void shouldHandleNetworkErrors() {
      // Arrange
      String cityName = "London";

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", matching("(?i)" + cityName))
              .withQueryParam("appid", equalTo(TEST_API_KEY))
              .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(cityName))
          .isInstanceOf(WeatherSDKException.class)
          .hasMessageContaining("Failed to fetch weather for city: " + cityName)
          .hasCauseInstanceOf(OpenWeatherApiException.class)
          .cause()
          .hasMessageContaining("Network error");
    }

    @Test
    @DisplayName("should handle invalid JSON response")
    void shouldHandleInvalidJsonResponse() {
      // Arrange
      String cityName = "London";

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", matching("(?i)" + cityName))
              .withQueryParam("appid", equalTo(TEST_API_KEY))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody("{ invalid json }")));

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert
      assertThatThrownBy(() -> sdk.getWeather(cityName))
          .isInstanceOf(WeatherSDKException.class)
          .hasMessageContaining("Failed to fetch weather for city: " + cityName)
          .hasCauseInstanceOf(OpenWeatherApiException.class)
          .cause()
          .hasMessageContaining("Failed to parse");
    }
  }

  @Nested
  @DisplayName("Factory Integration")
  class FactoryIntegrationTests {

    @Test
    @DisplayName("should create SDK instance via factory")
    void shouldCreateSdkViaFactory() {
      // Act
      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Assert
      assertThat(sdk).isNotNull();
      assertThat(sdk.isShutdown()).isFalse();
    }

    @Test
    @DisplayName("should retrieve existing instance")
    void shouldRetrieveExistingInstance() {
      // Arrange
      WeatherSDK created =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act
      WeatherSDK retrieved = WeatherSDKFactory.getInstance(TEST_API_KEY);

      // Assert
      assertThat(retrieved)
          .as("getInstance() should return existing instance")
          .isNotNull()
          .isSameAs(created);
    }

    @Test
    @DisplayName("should prevent duplicate instances for same API key")
    void shouldPreventDuplicateInstances() {
      // Arrange
      WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert
      assertThatThrownBy(
              () ->
                  WeatherSDKFactory.create(TEST_API_KEY, OperationMode.POLLING, WIREMOCK_BASE_URL))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK instance already exists for API key")
          .hasMessageContaining("Use getInstance()");
    }

    @Test
    @DisplayName("should destroy single instance")
    void shouldDestroySingleInstance() {
      // Arrange
      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      assertThat(WeatherSDKFactory.getInstance(TEST_API_KEY))
          .as("Instance should exist before destroy")
          .isNotNull()
          .isSameAs(sdk);

      // Act
      WeatherSDKFactory.destroy(TEST_API_KEY);

      // Assert - Instance removed from factory
      assertThat(WeatherSDKFactory.getInstance(TEST_API_KEY))
          .as("Instance should be removed after destroy")
          .isNull();

      // Assert - SDK is shutdown
      assertThat(sdk.isShutdown()).as("SDK should be shutdown after destroy").isTrue();
    }

    @Test
    @DisplayName("should destroy all SDK instances")
    void shouldDestroyAllInstances() {
      // Arrange
      String apiKey1 = "api-key-1";
      String apiKey2 = "api-key-2";
      String apiKey3 = "api-key-3";

      WeatherSDK sdk1 =
          WeatherSDKFactory.create(apiKey1, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);
      WeatherSDK sdk2 = WeatherSDKFactory.create(apiKey2, OperationMode.POLLING, WIREMOCK_BASE_URL);
      WeatherSDK sdk3 =
          WeatherSDKFactory.create(apiKey3, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Verify all instances exist
      assertThat(WeatherSDKFactory.getInstance(apiKey1)).isSameAs(sdk1);
      assertThat(WeatherSDKFactory.getInstance(apiKey2)).isSameAs(sdk2);
      assertThat(WeatherSDKFactory.getInstance(apiKey3)).isSameAs(sdk3);

      // Act
      WeatherSDKFactory.destroyAll();

      // Assert - All instances removed
      assertThat(WeatherSDKFactory.getInstance(apiKey1)).isNull();
      assertThat(WeatherSDKFactory.getInstance(apiKey2)).isNull();
      assertThat(WeatherSDKFactory.getInstance(apiKey3)).isNull();

      // Assert - All SDKs shutdown
      assertThat(sdk1.isShutdown()).isTrue();
      assertThat(sdk2.isShutdown()).isTrue();
      assertThat(sdk3.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should handle case-insensitive API keys")
    void shouldHandleCaseInsensitiveApiKeys() {
      // Arrange
      String upperCaseKey = "TEST-API-KEY";
      String lowerCaseKey = "test-api-key";
      String mixedCaseKey = "TeSt-ApI-KeY";

      // Act - Create with uppercase
      WeatherSDK sdk =
          WeatherSDKFactory.create(upperCaseKey, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Assert - All case variations retrieve same instance
      assertThat(WeatherSDKFactory.getInstance(upperCaseKey)).isSameAs(sdk);
      assertThat(WeatherSDKFactory.getInstance(lowerCaseKey)).isSameAs(sdk);
      assertThat(WeatherSDKFactory.getInstance(mixedCaseKey)).isSameAs(sdk);

      // Assert - Cannot create duplicate with different case
      assertThatThrownBy(
              () ->
                  WeatherSDKFactory.create(lowerCaseKey, OperationMode.POLLING, WIREMOCK_BASE_URL))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK instance already exists for API key");
    }
  }

  @Nested
  @DisplayName("Lifecycle Management")
  class LifecycleTests {

    @Test
    @DisplayName("should shutdown cleanly")
    void shouldShutdownCleanly() throws WeatherSDKException {
      // Arrange
      String cityName = "London";
      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      sdk.getWeather(cityName);

      assertThat(sdk.isShutdown()).isFalse();
      assertThat(sdk.isCached(cityName)).isTrue();

      // Act
      sdk.shutdown();

      // Assert
      assertThat(sdk.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should prevent operations after shutdown")
    void shouldPreventOperationsAfterShutdown() {
      // Arrange
      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act
      sdk.shutdown();

      // Assert - All operations should throw IllegalStateException
      assertThatThrownBy(() -> sdk.getWeather("London"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");

      assertThatThrownBy(() -> sdk.isCached("London"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");

      assertThatThrownBy(() -> sdk.isFresh("London"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");

      assertThatThrownBy(sdk::getCachedCities)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");

      assertThatThrownBy(sdk::clearCache)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SDK has been shut down");
    }

    @Test
    @DisplayName("should be idempotent on shutdown")
    void shouldBeIdempotentOnShutdown() {
      // Arrange
      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act - Call shutdown multiple times
      sdk.shutdown();
      sdk.shutdown();
      sdk.shutdown();

      // Assert - Remains shutdown, no exceptions
      assertThat(sdk.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should shutdown successfully with populated cache")
    void shouldShutdownWithPopulatedCache() throws WeatherSDKException {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(city3, "Rain", "light rain", 285.15, 7.2);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      sdk.getWeather(city1);
      sdk.getWeather(city2);
      sdk.getWeather(city3);

      assertThat(sdk.getCachedCities()).hasSize(3);

      // Act
      sdk.shutdown();

      // Assert
      assertThat(sdk.isShutdown()).isTrue();
    }
  }

  @Nested
  @DisplayName("Concurrent Access")
  class ConcurrentAccessTests {

    @Test
    @DisplayName("should handle concurrent getWeather requests")
    void shouldHandleConcurrentGetWeatherRequests() throws Exception {
      // Arrange
      String cityName = "London";
      stubSuccessfulResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      int threadCount = 10;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      List<Weather> results = new CopyOnWriteArrayList<>();
      List<Exception> exceptions = new CopyOnWriteArrayList<>();

      // Act - Execute concurrent requests
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                Weather weather = sdk.getWeather(cityName);
                results.add(weather);
              } catch (Exception e) {
                exceptions.add(e);
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, TimeUnit.SECONDS);
      executor.shutdown();

      // Assert - All requests succeeded
      assertThat(exceptions).as("No exceptions should occur during concurrent requests").isEmpty();

      assertThat(results).as("All threads should receive results").hasSize(threadCount);

      // Assert - All results are identical (cache coherence)
      Weather firstResult = results.get(0);
      assertThat(results)
          .as("All results should be identical (cached)")
          .allMatch(weather -> weather.equals(firstResult));

      // Assert - City cached only once
      assertThat(sdk.getCachedCities())
          .as("City should be cached only once despite concurrent requests")
          .hasSize(1)
          .containsExactly("london");
    }

    @Test
    @DisplayName("should handle concurrent cache operations")
    void shouldHandleConcurrentCacheOperations() throws Exception {
      // Arrange
      String[] cities = {"London", "Paris", "Berlin", "Madrid", "Rome"};

      for (String city : cities) {
        stubSuccessfulResponse(city, "Clear", "clear sky", 293.15, 5.5);
      }

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      int threadCount = 20;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      List<Exception> exceptions = new CopyOnWriteArrayList<>();

      // Act
      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                String city = cities[threadId % cities.length];

                if (threadId % 3 == 0) {
                  sdk.getWeather(city);
                } else if (threadId % 3 == 1) {
                  sdk.isCached(city);
                } else {
                  sdk.isFresh(city);
                }
              } catch (Exception e) {
                exceptions.add(e);
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, TimeUnit.SECONDS);
      executor.shutdown();

      // Assert
      assertThat(exceptions).isEmpty();
      assertThat(sdk.getCachedCities()).isNotEmpty().hasSizeLessThanOrEqualTo(cities.length);
    }
  }

  @Nested
  @DisplayName("Real-World Integration Scenarios")
  class IntegrationScenariosTests {

    @Test
    @DisplayName("should handle typical ON_DEMAND workflow")
    void shouldHandleTypicalOnDemandWorkflow() throws WeatherSDKException {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert
      Weather londonWeather = sdk.getWeather(city1);
      assertThat(londonWeather).isNotNull();
      assertThat(londonWeather.cityName()).isEqualTo("London");
      assertThat(sdk.isCached(city1)).isTrue();
      assertThat(sdk.isFresh(city1)).isTrue();

      Weather londonCached = sdk.getWeather(city1);
      assertThat(londonCached).isEqualTo(londonWeather);

      Weather parisWeather = sdk.getWeather(city2);
      assertThat(parisWeather).isNotNull();
      assertThat(parisWeather.cityName()).isEqualTo("Paris");

      assertThat(sdk.getCachedCities()).hasSize(2).containsExactlyInAnyOrder("london", "paris");

      sdk.clearCache();
      assertThat(sdk.getCachedCities()).isEmpty();

      Weather londonRefresh = sdk.getWeather(city1);
      assertThat(londonRefresh).isNotNull();
      assertThat(londonRefresh.cityName()).isEqualTo("London");

      sdk.shutdown();
      assertThat(sdk.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should handle typical POLLING workflow")
    void shouldHandleTypicalPollingWorkflow() throws Exception {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);

      OpenWeatherApiClient client = new OpenWeatherApiClient(TEST_API_KEY, WIREMOCK_BASE_URL);
      WeatherCache cache = new WeatherCache(10, Duration.ofMinutes(10));
      PollingService pollingService = new PollingService(cache, client, Duration.ofMillis(100));
      WeatherSDK sdk = new WeatherSDK(OperationMode.POLLING, client, cache);

      // Act & Assert
      Weather londonWeather = sdk.getWeather(city1);
      assertThat(londonWeather).isNotNull();
      assertThat(londonWeather.cityName()).isEqualTo("London");
      assertThat(sdk.isCached(city1)).isTrue();
      assertThat(sdk.isFresh(city1)).isTrue();

      pollingService.start();
      assertThat(pollingService.isRunning()).isTrue();

      Thread.sleep(250); // Allow 2-3 polling cycles

      int callsBeforeAccess = wireMockServer.getAllServeEvents().size();
      Weather londonCached = sdk.getWeather(city1);
      assertThat(londonCached).isNotNull();
      assertThat(sdk.isFresh(city1)).isTrue();
      assertThat(wireMockServer.getAllServeEvents().size()).isEqualTo(callsBeforeAccess);

      Weather parisWeather = sdk.getWeather(city2);
      assertThat(parisWeather).isNotNull();
      assertThat(parisWeather.cityName()).isEqualTo("Paris");

      assertThat(sdk.getCachedCities()).hasSize(2).containsExactlyInAnyOrder("london", "paris");

      pollingService.shutdown();
      assertThat(pollingService.isRunning()).isFalse();

      sdk.shutdown();
      assertThat(sdk.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("should switch between multiple cities seamlessly")
    void shouldSwitchBetweenMultipleCities() throws WeatherSDKException {
      // Arrange
      String city1 = "London";
      String city2 = "Paris";
      String city3 = "Berlin";

      stubSuccessfulResponse(city1, "Clouds", "scattered clouds", 293.15, 5.5);
      stubSuccessfulResponse(city2, "Clear", "clear sky", 288.15, 3.5);
      stubSuccessfulResponse(city3, "Rain", "light rain", 285.15, 7.2);

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert - Sequential city switching

      // Switch to London
      Weather londonWeather = sdk.getWeather(city1);
      assertThat(londonWeather).isNotNull();
      assertThat(londonWeather.cityName()).isEqualTo("London");
      assertThat(londonWeather.condition().main()).isEqualTo("Clouds");

      // Switch to Paris
      Weather parisWeather = sdk.getWeather(city2);
      assertThat(parisWeather).isNotNull();
      assertThat(parisWeather.cityName()).isEqualTo("Paris");
      assertThat(parisWeather.condition().main()).isEqualTo("Clear");

      // Switch to Berlin
      Weather berlinWeather = sdk.getWeather(city3);
      assertThat(berlinWeather).isNotNull();
      assertThat(berlinWeather.cityName()).isEqualTo("Berlin");
      assertThat(berlinWeather.condition().main()).isEqualTo("Rain");

      // Switch back to London (cache hit - proves seamless efficiency)
      int callsBeforeSwitch = wireMockServer.getAllServeEvents().size();
      Weather londonCached = sdk.getWeather(city1);
      assertThat(londonCached).isEqualTo(londonWeather);
      assertThat(wireMockServer.getAllServeEvents().size()).isEqualTo(callsBeforeSwitch);

      // All cities cached independently
      assertThat(sdk.getCachedCities())
          .hasSize(3)
          .containsExactlyInAnyOrder("london", "paris", "berlin");
    }

    @Test
    @DisplayName("should recover from transient errors")
    void shouldRecoverFromTransientErrors() throws Exception {
      // Arrange
      String cityName = "London";

      // Initial request fails (transient error)
      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", matching("(?i)" + cityName))
              .withQueryParam("appid", equalTo(TEST_API_KEY))
              .inScenario("Transient Error")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(serverError())
              .willSetStateTo("Error Resolved"));

      // Subsequent request succeeds
      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", matching("(?i)" + cityName))
              .withQueryParam("appid", equalTo(TEST_API_KEY))
              .inScenario("Transient Error")
              .whenScenarioStateIs("Error Resolved")
              .willReturn(
                  okJson(
                      createWeatherResponse(cityName, "Clouds", "scattered clouds", 293.15, 5.5))));

      WeatherSDK sdk =
          WeatherSDKFactory.create(TEST_API_KEY, OperationMode.ON_DEMAND, WIREMOCK_BASE_URL);

      // Act & Assert

      // First request fails
      assertThatThrownBy(() -> sdk.getWeather(cityName))
          .isInstanceOf(WeatherSDKException.class)
          .hasMessageContaining("Failed to fetch weather for city: " + cityName);

      // Retry succeeds (demonstrates recovery)
      Weather weather = sdk.getWeather(cityName);

      // Assert - Complete recovery
      assertThat(weather).isNotNull();
      assertThat(weather.cityName()).isEqualTo("London");
      assertThat(weather.condition().main()).isEqualTo("Clouds");
      assertThat(sdk.isCached(cityName)).isTrue();
    }
  }

  private void stubSuccessfulResponse(
      String city, String main, String description, double temp, double windSpeed) {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/data/2.5/weather"))
            .withQueryParam("q", matching("(?i)" + city))
            .withQueryParam("appid", equalTo(TEST_API_KEY))
            .willReturn(okJson(createWeatherResponse(city, main, description, temp, windSpeed))));
  }

  private void stubServerError(String city) {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/data/2.5/weather"))
            .withQueryParam("q", matching("(?i)" + city))
            .withQueryParam("appid", equalTo(TEST_API_KEY))
            .willReturn(
                serverError()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"cod\": 500, \"message\": \"Internal server error\"}")));
  }

  private int countRequests(String city) {
    return wireMockServer
        .findAll(
            getRequestedFor(urlPathEqualTo("/data/2.5/weather"))
                .withQueryParam("q", matching("(?i)" + city)))
        .size();
  }

  private String createWeatherResponse(
      String city, String main, String description, double temp, double windSpeed) {
    return String.format(
        """
            {
              "weather": [{"main": "%s", "description": "%s"}],
              "main": {"temp": %.2f, "feels_like": %.2f},
              "wind": {"speed": %.1f, "deg": 180},
              "visibility": 10000,
              "dt": 1675744800,
              "sys": {"sunrise": 1675751262, "sunset": 1675787560},
              "timezone": 3600,
              "name": "%s"
            }
            """,
        main, description, temp, temp - 2, windSpeed, city);
  }

  private void stubApiError(String city, int statusCode, String message) {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/data/2.5/weather"))
            .withQueryParam("q", matching("(?i)" + city))
            .withQueryParam("appid", equalTo(TEST_API_KEY))
            .willReturn(
                aResponse()
                    .withStatus(statusCode)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        String.format("{\"cod\": %d, \"message\": \"%s\"}", statusCode, message))));
  }
}
