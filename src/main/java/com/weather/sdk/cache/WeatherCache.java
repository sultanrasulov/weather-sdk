package com.weather.sdk.cache;

import com.weather.sdk.domain.model.Weather;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Thread-safe cache for weather data with LRU eviction and TTL expiration.
 *
 * <p>Stores weather information for cities with automatic eviction when capacity is exceeded or
 * data becomes stale. City names are normalized to lowercase for case-insensitive lookups.
 *
 * <p><strong>Eviction policy:</strong>
 *
 * <ul>
 *   <li>LRU (Least Recently Used) when capacity is exceeded
 *   <li>TTL (Time-To-Live) - entries expire after configured duration
 * </ul>
 *
 * <p><strong>Thread-safety:</strong> Safe for concurrent access.
 *
 * @since 1.0.0
 */
public final class WeatherCache {

  private static final int DEFAULT_MAX_CAPACITY = 10;
  private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

  private final int maxCapacity;
  private final Duration ttl;
  private final Map<String, WeatherCacheEntry> cache;

  /** Creates cache with default settings (capacity=10, TTL=10 minutes). */
  public WeatherCache() {
    this(DEFAULT_MAX_CAPACITY, DEFAULT_TTL);
  }

  /**
   * Creates cache with custom settings.
   *
   * @param maxCapacity maximum number of cities to cache (must be > 0)
   * @param ttl time-to-live for cached entries (must be positive)
   * @throws IllegalArgumentException if maxCapacity <= 0 or ttl is not positive
   * @throws NullPointerException if ttl is null
   */
  public WeatherCache(int maxCapacity, Duration ttl) {
    Objects.requireNonNull(ttl, "ttl must not be null");

    if (maxCapacity <= 0) {
      throw new IllegalArgumentException(
          String.format("maxCapacity must be positive, got: %d", maxCapacity));
    }

    if (ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException(String.format("ttl must be positive, got: %s", ttl));
    }

    this.maxCapacity = maxCapacity;
    this.ttl = ttl;

    // Calculate initial capacity to avoid rehashing
    int initialCapacity = (int) Math.ceil(maxCapacity / 0.75f) + 1;

    this.cache =
        new LinkedHashMap<>(initialCapacity, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, WeatherCacheEntry> eldest) {
            return size() > maxCapacity;
          }
        };
  }

  /**
   * Stores weather data for the specified city.
   *
   * <p>City name is normalized to lowercase for case-insensitive lookups. If capacity is exceeded,
   * least recently used entry is automatically evicted.
   *
   * @param cityName name of the city (must not be null or blank)
   * @param weather weather data to cache (must not be null)
   * @throws NullPointerException if cityName or weather is null
   * @throws IllegalArgumentException if cityName is blank
   */
  public synchronized void put(String cityName, Weather weather) {
    Objects.requireNonNull(cityName, "cityName must not be null");
    Objects.requireNonNull(weather, "weather must not be null");

    if (cityName.isBlank()) {
      throw new IllegalArgumentException("cityName must not be blank");
    }

    String normalizedName = this.normalizeCityName(cityName);
    WeatherCacheEntry entry = new WeatherCacheEntry(weather, Instant.now());
    this.cache.put(normalizedName, entry);
  }

  /**
   * Retrieves cached weather data for the specified city.
   *
   * <p>Returns empty if city is not cached or data has expired (older than TTL). City name lookup
   * is case-insensitive.
   *
   * @param cityName name of the city (must not be null or blank)
   * @return cached weather data, or empty if not found or expired
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   */
  public synchronized Optional<Weather> get(String cityName) {
    Objects.requireNonNull(cityName, "cityName must not be null");

    if (cityName.isBlank()) {
      throw new IllegalArgumentException("cityName must not be blank");
    }

    String normalizedName = this.normalizeCityName(cityName);
    WeatherCacheEntry entry = this.cache.get(normalizedName);

    if (entry == null) {
      return Optional.empty();
    }

    if (this.isExpired(entry)) {
      this.cache.remove(normalizedName);
      return Optional.empty();
    }

    return Optional.of(entry.weather());
  }

  /**
   * Checks if cache contains weather data for the specified city.
   *
   * <p>Returns true even if cached data has expired. Use {@link #isFresh(String)} to check if data
   * is still valid. City name lookup is case-insensitive.
   *
   * @param cityName name of the city (must not be null or blank)
   * @return true if city is in cache (regardless of expiration)
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   */
  public synchronized boolean contains(String cityName) {
    Objects.requireNonNull(cityName, "cityName must not be null");

    if (cityName.isBlank()) {
      throw new IllegalArgumentException("cityName must not be blank");
    }

    String normalizedName = normalizeCityName(cityName);
    return this.cache.containsKey(normalizedName);
  }

  /**
   * Checks if cached weather data for the specified city is still fresh (not expired).
   *
   * <p>Returns true only if city is cached AND data age is within TTL. City name lookup is
   * case-insensitive.
   *
   * @param cityName name of the city (must not be null or blank)
   * @return true if city is cached and data is not expired
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   */
  public synchronized boolean isFresh(String cityName) {
    Objects.requireNonNull(cityName, "cityName must not be null");

    if (cityName.isBlank()) {
      throw new IllegalArgumentException("cityName must not be blank");
    }

    String normalizedName = normalizeCityName(cityName);
    WeatherCacheEntry entry = this.cache.get(normalizedName);

    return entry != null && !this.isExpired(entry);
  }

  /**
   * Returns the current number of cached entries.
   *
   * <p>Includes both fresh and expired entries. Expired entries are only removed when accessed via
   * {@link #get(String)}.
   *
   * @return number of entries currently in cache
   */
  public synchronized int size() {
    return this.cache.size();
  }

  /**
   * Returns all city names currently in cache.
   *
   * <p>Includes cities with both fresh and expired data. City names are returned in their
   * normalized (lowercase) form. The returned set is unmodifiable and represents a snapshot at the
   * time of call.
   *
   * @return unmodifiable set of cached city names in LRU order
   */
  public synchronized Set<String> getCachedCities() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(this.cache.keySet()));
  }

  /**
   * Removes weather data for the specified city from cache.
   *
   * <p>Does nothing if city is not cached. City name lookup is case-insensitive.
   *
   * @param cityName name of the city (must not be null or blank)
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   */
  public synchronized void remove(String cityName) {
    Objects.requireNonNull(cityName, "cityName must not be null");

    if (cityName.isBlank()) {
      throw new IllegalArgumentException("cityName must not be blank");
    }

    String normalizedName = this.normalizeCityName(cityName);
    this.cache.remove(normalizedName);
  }

  /** Removes all entries from cache. */
  public synchronized void clear() {
    this.cache.clear();
  }

  /**
   * Normalizes city name to lowercase for case-insensitive comparisons.
   *
   * @param cityName city name to normalize
   * @return normalized city name in lowercase
   */
  private String normalizeCityName(String cityName) {
    return cityName.toLowerCase(Locale.ROOT);
  }

  /**
   * Checks if cache entry has expired based on TTL.
   *
   * @param entry cache entry to check
   * @return true if entry age exceeds TTL
   */
  private boolean isExpired(WeatherCacheEntry entry) {
    Duration age = Duration.between(entry.cachedAt(), Instant.now());
    return age.compareTo(this.ttl) > 0;
  }

  /** Immutable cache entry holding weather data and timestamp. */
  private record WeatherCacheEntry(Weather weather, Instant cachedAt) {}
}
