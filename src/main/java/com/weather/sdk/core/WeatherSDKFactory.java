package com.weather.sdk.core;

import com.weather.sdk.cache.WeatherCache;
import com.weather.sdk.infrastructure.client.OpenWeatherApiClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and managing WeatherSDK instances.
 *
 * <p>Ensures that only one SDK instance exists per API key (singleton pattern). Provides factory
 * methods for creating instances with default or custom configurations.
 *
 * <p><strong>Thread-safety:</strong> This class is thread-safe.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Create SDK with defaults (ON_DEMAND mode)
 * WeatherSDK sdk = WeatherSDKFactory.create("your-api-key");
 *
 * // Create SDK with custom mode
 * WeatherSDK sdk = WeatherSDKFactory.create("your-api-key", OperationMode.POLLING);
 *
 * // Get existing instance
 * WeatherSDK sdk = WeatherSDKFactory.getInstance("your-api-key");
 *
 * // Cleanup
 * WeatherSDKFactory.destroy("your-api-key");
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WeatherSDKFactory {

  private static final int DEFAULT_CACHE_CAPACITY = 10;
  private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(10);
  private static final Map<String, WeatherSDK> instances = new ConcurrentHashMap<>();

  /** Private constructor to prevent instantiation. */
  private WeatherSDKFactory() {
    throw new AssertionError("Utility class - do not instantiate");
  }

  /**
   * Retrieves an existing WeatherSDK instance for the specified API key.
   *
   * <p>Returns null if no instance exists for this API key. Use {@link #create(String)} to create a
   * new instance.
   *
   * @param apiKey OpenWeatherMap API key (must not be null or blank)
   * @return existing WeatherSDK instance, or null if not found
   * @throws NullPointerException if apiKey is null
   * @throws IllegalArgumentException if apiKey is blank
   * @since 1.0.0
   */
  public static synchronized WeatherSDK getInstance(String apiKey) {
    validateApiKey(apiKey);
    String normalizedKey = normalizeApiKey(apiKey);
    return instances.get(normalizedKey);
  }

  /**
   * Creates a new WeatherSDK instance with default settings.
   *
   * <p><strong>Default operation mode:</strong> ON_DEMAND
   *
   * <p><strong>Default cache settings:</strong>
   *
   * <ul>
   *   <li>Capacity: 10 cities maximum
   *   <li>TTL: 10 minutes
   * </ul>
   *
   * <p><strong>API endpoint:</strong> OpenWeatherMap production API
   *
   * <p>If an instance with the same API key already exists, throws {@link IllegalStateException}.
   * Use {@link #getInstance(String)} to retrieve existing instances.
   *
   * @param apiKey OpenWeatherMap API key (must not be null or blank)
   * @return new WeatherSDK instance
   * @throws NullPointerException if apiKey is null
   * @throws IllegalArgumentException if apiKey is blank
   * @throws IllegalStateException if instance already exists for this API key
   * @since 1.0.0
   */
  public static WeatherSDK create(String apiKey) {
    return create(apiKey, OperationMode.ON_DEMAND);
  }

  /**
   * Creates a new WeatherSDK instance with custom operation mode.
   *
   * <p><strong>Default cache settings:</strong>
   *
   * <ul>
   *   <li>Capacity: 10 cities maximum
   *   <li>TTL: 10 minutes
   * </ul>
   *
   * <p><strong>API endpoint:</strong> OpenWeatherMap production API
   *
   * <p>If an instance with the same API key already exists, throws {@link IllegalStateException}.
   * Use {@link #getInstance(String)} to retrieve existing instances.
   *
   * @param apiKey OpenWeatherMap API key (must not be null or blank)
   * @param mode operation mode (must not be null)
   * @return new WeatherSDK instance
   * @throws NullPointerException if apiKey or mode is null
   * @throws IllegalArgumentException if apiKey is blank
   * @throws IllegalStateException if instance already exists for this API key
   * @since 1.0.0
   */
  public static synchronized WeatherSDK create(String apiKey, OperationMode mode) {
    return create(apiKey, mode, OpenWeatherApiClient.DEFAULT_BASE_URL);
  }

  /**
   * Creates a new WeatherSDK instance with custom base URL.
   *
   * <p><strong>Package-private method for integration testing.</strong> Allows injection of custom
   * API endpoint for end-to-end testing without external dependencies on OpenWeatherMap API.
   *
   * <p><strong>Supported test frameworks:</strong>
   *
   * <ul>
   *   <li>WireMock - HTTP mocking
   *   <li>MockServer - HTTP mock server
   *   <li>Custom test endpoints
   * </ul>
   *
   * <p><strong>Default cache settings:</strong>
   *
   * <ul>
   *   <li>Capacity: 10 cities maximum
   *   <li>TTL: 10 minutes
   * </ul>
   *
   * <p>If an instance with the same API key already exists, throws {@link IllegalStateException}.
   * Use {@link #getInstance(String)} to retrieve existing instances.
   *
   * @param apiKey OpenWeatherMap API key (must not be null or blank)
   * @param mode operation mode (must not be null)
   * @param baseUrl custom API base URL (must not be null)
   * @return new WeatherSDK instance
   * @throws NullPointerException if apiKey, mode, or baseUrl is null
   * @throws IllegalArgumentException if apiKey is blank
   * @throws IllegalStateException if instance already exists for this API key
   * @since 1.0.0
   */
  static synchronized WeatherSDK create(String apiKey, OperationMode mode, String baseUrl) {
    validateApiKey(apiKey);
    Objects.requireNonNull(mode, "mode must not be null");
    Objects.requireNonNull(baseUrl, "baseUrl must not be null");

    String normalizedKey = normalizeApiKey(apiKey);

    if (instances.containsKey(normalizedKey)) {
      throw new IllegalStateException(
          "SDK instance already exists for API key. Use getInstance() to retrieve it.");
    }

    OpenWeatherApiClient client = new OpenWeatherApiClient(apiKey, baseUrl);
    WeatherCache cache = new WeatherCache(DEFAULT_CACHE_CAPACITY, DEFAULT_CACHE_TTL);
    WeatherSDK sdk = new WeatherSDK(mode, client, cache);

    instances.put(normalizedKey, sdk);
    return sdk;
  }

  /**
   * Destroys the WeatherSDK instance for the specified API key.
   *
   * <p>Shuts down the SDK instance and removes it from the factory registry. Does nothing if no
   * instance exists for this API key.
   *
   * @param apiKey OpenWeatherMap API key (must not be null or blank)
   * @throws NullPointerException if apiKey is null
   * @throws IllegalArgumentException if apiKey is blank
   * @since 1.0.0
   */
  public static synchronized void destroy(String apiKey) {
    validateApiKey(apiKey);
    String normalizedKey = normalizeApiKey(apiKey);
    WeatherSDK sdk = instances.remove(normalizedKey);
    if (sdk != null) {
      sdk.shutdown();
    }
  }

  /**
   * Destroys all WeatherSDK instances managed by this factory.
   *
   * <p>Shuts down all SDK instances and clears the factory registry. This method is idempotent -
   * calling it multiple times is safe.
   *
   * @since 1.0.0
   */
  public static synchronized void destroyAll() {
    instances.values().forEach(WeatherSDK::shutdown);
    instances.clear();
  }

  /**
   * Validates API key parameter.
   *
   * @param apiKey API key to validate
   * @throws NullPointerException if apiKey is null
   * @throws IllegalArgumentException if apiKey is blank
   */
  private static void validateApiKey(String apiKey) {
    Objects.requireNonNull(apiKey, "apiKey must not be null");
    if (apiKey.isBlank()) {
      throw new IllegalArgumentException("apiKey must not be blank");
    }
  }

  /**
   * Normalizes API key to lowercase for case-insensitive lookups.
   *
   * @param apiKey API key to normalize
   * @return normalized API key in lowercase
   */
  private static String normalizeApiKey(String apiKey) {
    return apiKey.toLowerCase(Locale.ROOT);
  }
}
