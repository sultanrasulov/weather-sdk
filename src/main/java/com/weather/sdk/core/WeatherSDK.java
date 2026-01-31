package com.weather.sdk.core;

import com.weather.sdk.cache.WeatherCache;
import com.weather.sdk.domain.model.Weather;
import com.weather.sdk.infrastructure.client.OpenWeatherApiClient;
import com.weather.sdk.infrastructure.dto.OpenWeatherApiResponse;
import com.weather.sdk.infrastructure.exception.OpenWeatherApiException;
import com.weather.sdk.infrastructure.mapper.WeatherMapper;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Main SDK interface for retrieving weather data.
 *
 * <p>Provides weather information for cities with automatic caching and configurable operation
 * modes. Supports both on-demand and polling modes for different use cases.
 *
 * <p><strong>Thread-safety:</strong> This class is thread-safe.
 *
 * <p><strong>Resource management:</strong> Call {@link #shutdown()} when done to release resources,
 * especially in POLLING mode.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Create SDK instance
 * WeatherSDK sdk = WeatherSDKFactory.create("your-api-key");
 *
 * // Get weather data
 * Weather weather = sdk.getWeather("London");
 *
 * // Cleanup
 * sdk.shutdown();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WeatherSDK {

  private final OpenWeatherApiClient client;
  private final WeatherCache cache;
  private volatile boolean isShutdown;

  /**
   * Package-private constructor for WeatherSDKFactory.
   *
   * @param client HTTP client for API requests (must not be null)
   * @param cache weather data cache (must not be null)
   * @throws NullPointerException if any parameter is null
   */
  WeatherSDK(OpenWeatherApiClient client, WeatherCache cache) {
    this.client = Objects.requireNonNull(client, "client must not be null");
    this.cache = Objects.requireNonNull(cache, "cache must not be null");
    this.isShutdown = false;
  }

  /**
   * Retrieves current weather data for the specified city.
   *
   * <p>In ON_DEMAND mode, fetches from API if cache is stale or missing. In POLLING mode, returns
   * pre-fetched data from cache with zero latency.
   *
   * <p>City names are case-insensitive. Results are cached for 10 minutes (default TTL).
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Weather weather = sdk.getWeather("London");
   * System.out.println("Temperature: " + weather.temperature().tempCelsius() + "°C");
   * }</pre>
   *
   * @param cityName name of the city (must not be null or blank)
   * @return current weather data for the city
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   * @throws IllegalStateException if SDK has been shut down
   * @throws WeatherSDKException if API request fails or city is not found
   * @since 1.0.0
   */
  public Weather getWeather(String cityName) throws WeatherSDKException {
    this.ensureNotShutdown();
    this.validateCityName(cityName);

    Optional<Weather> cached = this.cache.get(cityName);
    if (cached.isPresent()) {
      return cached.get();
    }

    return this.fetchFromApi(cityName);
  }

  /**
   * Checks if weather data for the specified city is cached.
   *
   * <p>Returns true even if cached data has expired. Use {@link #isFresh(String)} to check if data
   * is still valid.
   *
   * @param cityName name of the city (must not be null or blank)
   * @return true if city is cached (regardless of expiration)
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   * @throws IllegalStateException if SDK has been shut down
   * @since 1.0.0
   */
  public boolean isCached(String cityName) {
    this.ensureNotShutdown();
    this.validateCityName(cityName);
    return this.cache.contains(cityName);
  }

  /**
   * Checks if cached weather data for the specified city is still fresh (not expired).
   *
   * <p>Returns true only if city is cached AND data age is within TTL (10 minutes by default).
   *
   * @param cityName name of the city (must not be null or blank)
   * @return true if city is cached and data is not expired
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   * @throws IllegalStateException if SDK has been shut down
   * @since 1.0.0
   */
  public boolean isFresh(String cityName) {
    this.ensureNotShutdown();
    this.validateCityName(cityName);
    return this.cache.isFresh(cityName);
  }

  /**
   * Returns all city names currently in cache.
   *
   * <p>Includes cities with both fresh and expired data. City names are returned in their
   * normalized (lowercase) form. The returned set is unmodifiable and represents a snapshot at the
   * time of call.
   *
   * @return unmodifiable set of cached city names
   * @throws IllegalStateException if SDK has been shut down
   * @since 1.0.0
   */
  public Set<String> getCachedCities() {
    this.ensureNotShutdown();
    return this.cache.getCachedCities();
  }

  /**
   * Clears all cached weather data.
   *
   * <p>Removes all entries from cache. Subsequent calls to {@link #getWeather(String)} will fetch
   * fresh data from API.
   *
   * @throws IllegalStateException if SDK has been shut down
   * @since 1.0.0
   */
  public void clearCache() {
    this.ensureNotShutdown();
    this.cache.clear();
  }

  /**
   * Shuts down the SDK and releases all resources.
   *
   * <p>This method clears the cache and stops all background operations. After calling this method,
   * the SDK cannot be used anymore. All subsequent method calls will throw {@link
   * IllegalStateException}.
   *
   * <p>This method is idempotent - calling it multiple times is safe.
   *
   * <p><strong>Note:</strong> In POLLING mode, this also stops the background polling service.
   *
   * @since 1.0.0
   */
  public void shutdown() {
    if (this.isShutdown) {
      return;
    }
    this.isShutdown = true;
    this.cache.clear();
  }

  /**
   * Checks if SDK has been shut down.
   *
   * @return true if SDK has been shut down
   * @since 1.0.0
   */
  public boolean isShutdown() {
    return this.isShutdown;
  }

  /**
   * Ensures SDK has not been shut down.
   *
   * @throws IllegalStateException if SDK has been shut down
   */
  private void ensureNotShutdown() {
    if (this.isShutdown) {
      throw new IllegalStateException("SDK has been shut down");
    }
  }

  /**
   * Validates city name parameter.
   *
   * @param cityName city name to validate
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   */
  private void validateCityName(String cityName) {
    Objects.requireNonNull(cityName, "cityName must not be null");
    if (cityName.isBlank()) {
      throw new IllegalArgumentException("cityName must not be blank");
    }
  }

  /**
   * Fetches weather data from API and updates cache.
   *
   * @param cityName name of the city
   * @return weather data from API
   * @throws WeatherSDKException if API request fails
   */
  private Weather fetchFromApi(String cityName) throws WeatherSDKException {
    try {
      OpenWeatherApiResponse response = this.client.getCurrentWeather(cityName);
      Weather weather = WeatherMapper.map(response);
      this.cache.put(cityName, weather);
      return weather;
    } catch (OpenWeatherApiException e) {
      throw new WeatherSDKException("Failed to fetch weather for city: " + cityName, e);
    }
  }
}
