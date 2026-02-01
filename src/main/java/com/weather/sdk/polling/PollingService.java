package com.weather.sdk.polling;

import com.weather.sdk.cache.WeatherCache;
import com.weather.sdk.domain.model.Weather;
import com.weather.sdk.infrastructure.client.OpenWeatherApiClient;
import com.weather.sdk.infrastructure.dto.OpenWeatherApiResponse;
import com.weather.sdk.infrastructure.exception.OpenWeatherApiException;
import com.weather.sdk.infrastructure.mapper.WeatherMapper;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background polling service for automatic weather data refresh.
 *
 * <p>Periodically updates cached weather data for all cities to provide zero-latency responses in
 * POLLING mode.
 *
 * <p><strong>Thread-safety:</strong> This class is thread-safe.
 *
 * <p><strong>Resource management:</strong> Call {@link #shutdown()} when done to release resources.
 *
 * @since 1.0.0
 */
public final class PollingService {

  private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofMinutes(10);
  private static final int THREAD_POOL_SIZE = 1;

  private final WeatherCache cache;
  private final OpenWeatherApiClient client;
  private final Duration pollingInterval;
  private final ScheduledExecutorService scheduler;
  private volatile boolean isRunning;

  /**
   * Creates polling service with default interval (10 minutes).
   *
   * @param cache weather cache to update
   * @param client API client for fetching data
   * @throws NullPointerException if cache or client is null
   */
  public PollingService(WeatherCache cache, OpenWeatherApiClient client) {
    this(cache, client, DEFAULT_POLLING_INTERVAL);
  }

  /**
   * Creates polling service with custom interval.
   *
   * @param cache weather cache to update
   * @param client API client for fetching data
   * @param pollingInterval interval between polling cycles
   * @throws NullPointerException if any parameter is null
   * @throws IllegalArgumentException if pollingInterval is not positive
   */
  public PollingService(WeatherCache cache, OpenWeatherApiClient client, Duration pollingInterval) {
    this.cache = Objects.requireNonNull(cache, "cache must not be null");
    this.client = Objects.requireNonNull(client, "client must not be null");
    Objects.requireNonNull(pollingInterval, "pollingInterval must not be null");

    if (pollingInterval.isNegative() || pollingInterval.isZero()) {
      throw new IllegalArgumentException(
          String.format("pollingInterval must be positive, got: %s", pollingInterval));
    }

    this.pollingInterval = pollingInterval;
    this.scheduler = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    this.isRunning = false;
  }

  /**
   * Checks if polling service is running.
   *
   * @return true if polling is active
   */
  public boolean isRunning() {
    return this.isRunning;
  }

  /**
   * Starts background polling.
   *
   * <p>Schedules periodic polling task to refresh weather data for all cached cities. Does nothing
   * if polling is already running.
   *
   * @throws IllegalStateException if scheduler has been shut down
   */
  public synchronized void start() {
    if (this.isRunning) {
      return;
    }

    this.scheduler.scheduleAtFixedRate(
        this::pollAllCachedCities,
        0, // initial delay
        this.pollingInterval.toMillis(),
        TimeUnit.MILLISECONDS);

    this.isRunning = true;
  }

  /**
   * Shuts down the polling service.
   *
   * <p>Stops background polling and releases scheduler resources. Waits for currently executing
   * tasks to complete. This method is idempotent - calling it multiple times is safe.
   */
  public synchronized void shutdown() {
    if (!this.isRunning) {
      return;
    }

    this.isRunning = false;
    this.scheduler.shutdown();

    try {
      if (!this.scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
        this.scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      this.scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Polls all cached cities and updates their weather data.
   *
   * <p>Iterates through all cities in cache and fetches fresh weather data. Errors for individual
   * cities are handled gracefully without stopping the entire polling cycle.
   */
  private void pollAllCachedCities() {
    Set<String> cachedCities = this.cache.getCachedCities();

    for (String cityName : cachedCities) {
      try {
        this.pollCity(cityName);
      } catch (Exception e) {
        // Silently handle errors - don't stop polling for other cities
        // Individual city failures should not break the entire polling cycle
      }
    }
  }

  /**
   * Polls weather data for a single city.
   *
   * <p>Fetches fresh weather data from API and updates cache.
   *
   * @param cityName name of the city to poll
   * @throws OpenWeatherApiException if API request fails
   */
  private void pollCity(String cityName) throws OpenWeatherApiException {
    OpenWeatherApiResponse response = this.client.getCurrentWeather(cityName);
    Weather weather = WeatherMapper.map(response);
    this.cache.put(cityName, weather);
  }
}
