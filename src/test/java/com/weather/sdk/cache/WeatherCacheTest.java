package com.weather.sdk.cache;

import static org.assertj.core.api.Assertions.*;

import com.weather.sdk.domain.model.SunTimes;
import com.weather.sdk.domain.model.Temperature;
import com.weather.sdk.domain.model.TimezoneOffset;
import com.weather.sdk.domain.model.Visibility;
import com.weather.sdk.domain.model.Weather;
import com.weather.sdk.domain.model.WeatherCondition;
import com.weather.sdk.domain.model.Wind;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link WeatherCache}.
 *
 * <p>Test strategy: TDD approach with focus on cache behavior, eviction policies, and thread
 * safety. Uses nested test classes for logical grouping.
 *
 * <p>Coverage targets: Line ≥95%, Branch ≥90%
 *
 * @since 1.0.0
 */
@DisplayName("WeatherCache")
class WeatherCacheTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create cache with default settings")
    void shouldCreateWithDefaultSettings() {
      // Act
      WeatherCache cache = new WeatherCache();

      // Assert
      assertThat(cache).isNotNull();
      assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("should create cache with custom settings")
    void shouldCreateWithCustomSettings() {
      // Arrange
      int customCapacity = 5;
      Duration customTtl = Duration.ofMinutes(15);

      // Act
      WeatherCache cache = new WeatherCache(customCapacity, customTtl);

      // Assert
      assertThat(cache).isNotNull();
      assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("should reject null TTL")
    void shouldRejectNullTtl() {
      // Arrange
      int validCapacity = 10;

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCache(validCapacity, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("ttl must not be null");
    }

    @Test
    @DisplayName("should reject negative max capacity")
    void shouldRejectNegativeMaxCapacity() {
      // Arrange
      int negativeCapacity = -1;
      Duration validTtl = Duration.ofMinutes(10);

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCache(negativeCapacity, validTtl))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("maxCapacity must be positive");
    }

    @Test
    @DisplayName("should reject zero max capacity")
    void shouldRejectZeroMaxCapacity() {
      // Arrange
      int zeroCapacity = 0;
      Duration validTtl = Duration.ofMinutes(10);

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCache(zeroCapacity, validTtl))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("maxCapacity must be positive");
    }

    @Test
    @DisplayName("should reject negative TTL")
    void shouldRejectNegativeTtl() {
      // Arrange
      int validCapacity = 10;
      Duration negativeTtl = Duration.ofMinutes(-5);

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCache(validCapacity, negativeTtl))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ttl must be positive");
    }

    @Test
    @DisplayName("should reject zero TTL")
    void shouldRejectZeroTtl() {
      // Arrange
      int validCapacity = 10;
      Duration zeroTtl = Duration.ZERO;

      // Act & Assert
      assertThatThrownBy(() -> new WeatherCache(validCapacity, zeroTtl))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ttl must be positive");
    }
  }

  @Nested
  @DisplayName("Put Operation")
  class PutOperationTests {

    @Test
    @DisplayName("should store single entry")
    void shouldStoreSingleEntry() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      String cityName = "London";
      Weather weather = createTestWeather(cityName);

      // Act
      cache.put(cityName, weather);

      // Assert
      assertThat(cache.size()).isEqualTo(1);
      assertThat(cache.contains(cityName)).isTrue();
      assertThat(cache.get(cityName)).isPresent().contains(weather);
    }

    @Test
    @DisplayName("should overwrite existing entry")
    void shouldOverwriteExistingEntry() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      String cityName = "London";
      Weather weather1 = createTestWeather(cityName);
      Weather weather2 = createTestWeather(cityName);

      // Act
      cache.put(cityName, weather1);
      cache.put(cityName, weather2);

      // Assert
      assertThat(cache.size()).isEqualTo(1);
      assertThat(cache.get(cityName)).isPresent().contains(weather2);
    }

    @Test
    @DisplayName("should normalize city name to lowercase")
    void shouldNormalizeCityNameToLowercase() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      Weather weather = createTestWeather("London");

      // Act
      cache.put("LONDON", weather);

      // Assert
      assertThat(cache.contains("london")).isTrue();
      assertThat(cache.contains("London")).isTrue();
      assertThat(cache.contains("LONDON")).isTrue();
      assertThat(cache.get("london")).isPresent();
      assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("should reject null city name")
    void shouldRejectNullCityName() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      Weather weather = createTestWeather("London");

      // Act & Assert
      assertThatThrownBy(() -> cache.put(null, weather))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Arrange
      WeatherCache cache = new WeatherCache();
      Weather weather = createTestWeather("London");

      // Act & Assert
      assertThatThrownBy(() -> cache.put(blankCityName, weather))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }

    @Test
    @DisplayName("should reject null weather")
    void shouldRejectNullWeather() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      String cityName = "London";

      // Act & Assert
      assertThatThrownBy(() -> cache.put(cityName, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("weather must not be null");
    }
  }

  @Nested
  @DisplayName("Get Operation")
  class GetOperationTests {

    @Test
    @DisplayName("should return cached weather")
    void shouldReturnCachedWeather() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      String cityName = "London";
      Weather weather = createTestWeather(cityName);
      cache.put(cityName, weather);

      // Act
      Optional<Weather> result = cache.get(cityName);

      // Assert
      assertThat(result).isPresent().contains(weather);
    }

    @Test
    @DisplayName("should return empty for non-existent city")
    void shouldReturnEmptyForNonExistentCity() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act
      Optional<Weather> result = cache.get("NonExistent");

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty for expired entry")
    void shouldReturnEmptyForExpiredEntry() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(100));
      String cityName = "London";
      Weather weather = createTestWeather(cityName);
      cache.put(cityName, weather);

      // Act
      Thread.sleep(150); // Wait for expiration
      Optional<Weather> result = cache.get(cityName);

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should remove expired entry on access")
    void shouldRemoveExpiredEntryOnAccess() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(100));
      String cityName = "London";
      Weather weather = createTestWeather(cityName);
      cache.put(cityName, weather);

      // Act
      Thread.sleep(150); // Wait for expiration
      cache.get(cityName); // Trigger removal

      // Assert
      assertThat(cache.contains(cityName)).isFalse();
      assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      Weather weather = createTestWeather("London");
      cache.put("london", weather);

      // Act & Assert
      assertThat(cache.get("LONDON")).isPresent().contains(weather);
      assertThat(cache.get("London")).isPresent().contains(weather);
      assertThat(cache.get("london")).isPresent().contains(weather);
    }

    @Test
    @DisplayName("should reject null city name")
    void shouldRejectNullCityName() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> cache.get(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> cache.get(blankCityName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }
  }

  @Nested
  @DisplayName("Contains Operation")
  class ContainsTests {

    @Test
    @DisplayName("should return true for cached city")
    void shouldReturnTrueForCachedCity() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      String cityName = "London";
      Weather weather = createTestWeather(cityName);
      cache.put(cityName, weather);

      // Act
      boolean result = cache.contains(cityName);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false for non-existent city")
    void shouldReturnFalseForNonExistentCity() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act
      boolean result = cache.contains("NonExistent");

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true for expired entry")
    void shouldReturnTrueForExpiredEntry() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(100));
      String cityName = "London";
      Weather weather = createTestWeather(cityName);
      cache.put(cityName, weather);

      // Act
      Thread.sleep(150); // Wait for expiration
      boolean result = cache.contains(cityName);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      Weather weather = createTestWeather("London");
      cache.put("london", weather);

      // Act & Assert
      assertThat(cache.contains("LONDON")).isTrue();
      assertThat(cache.contains("London")).isTrue();
      assertThat(cache.contains("london")).isTrue();
    }

    @Test
    @DisplayName("should reject null city name")
    void shouldRejectNullCityName() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> cache.contains(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> cache.contains(blankCityName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }
  }

  @Nested
  @DisplayName("IsFresh Operation")
  class IsFreshTests {

    @Test
    @DisplayName("should return true for fresh entry")
    void shouldReturnTrueForFreshEntry() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      String cityName = "London";
      Weather weather = createTestWeather(cityName);
      cache.put(cityName, weather);

      // Act
      boolean result = cache.isFresh(cityName);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false for expired entry")
    void shouldReturnFalseForExpiredEntry() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(100));
      String cityName = "London";
      Weather weather = createTestWeather(cityName);
      cache.put(cityName, weather);

      // Act
      Thread.sleep(150); // Wait for expiration
      boolean result = cache.isFresh(cityName);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false for non-existent city")
    void shouldReturnFalseForNonExistentCity() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act
      boolean result = cache.isFresh("NonExistent");

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      Weather weather = createTestWeather("London");
      cache.put("london", weather);

      // Act & Assert
      assertThat(cache.isFresh("LONDON")).isTrue();
      assertThat(cache.isFresh("London")).isTrue();
      assertThat(cache.isFresh("london")).isTrue();
    }

    @Test
    @DisplayName("should reject null city name")
    void shouldRejectNullCityName() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> cache.isFresh(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> cache.isFresh(blankCityName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }
  }

  @Nested
  @DisplayName("Size Operation")
  class SizeTests {

    @Test
    @DisplayName("should return zero for empty cache")
    void shouldReturnZeroForEmptyCache() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act
      int result = cache.size();

      // Assert
      assertThat(result).isZero();
    }

    @Test
    @DisplayName("should return correct size after put")
    void shouldReturnCorrectSizeAfterPut() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThat(cache.size()).isZero();

      cache.put("London", createTestWeather("London"));
      assertThat(cache.size()).isEqualTo(1);

      cache.put("Paris", createTestWeather("Paris"));
      assertThat(cache.size()).isEqualTo(2);

      cache.put("Berlin", createTestWeather("Berlin"));
      assertThat(cache.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("should include both fresh and expired entries")
    void shouldIncludeBothFreshAndExpired() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(100));
      cache.put("London", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));

      // Act
      Thread.sleep(150); // Expire first two entries
      cache.put("Berlin", createTestWeather("Berlin")); // Add fresh entry

      // Assert
      assertThat(cache.size()).isEqualTo(3);
      assertThat(cache.isFresh("London")).isFalse();
      assertThat(cache.isFresh("Paris")).isFalse();
      assertThat(cache.isFresh("Berlin")).isTrue();
    }

    @Test
    @DisplayName("should not exceed max capacity")
    void shouldNotExceedMaxCapacity() {
      // Arrange
      WeatherCache cache = new WeatherCache(3, Duration.ofMinutes(10));

      // Act
      cache.put("London", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));
      cache.put("Berlin", createTestWeather("Berlin"));
      cache.put("Madrid", createTestWeather("Madrid")); // Exceeds capacity

      // Assert
      assertThat(cache.size()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("GetCachedCities Operation")
  class GetCachedCitiesTests {

    @Test
    @DisplayName("should return empty set for empty cache")
    void shouldReturnEmptySetForEmptyCache() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act
      Set<String> result = cache.getCachedCities();

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return all cached city names")
    void shouldReturnAllCachedCityNames() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      cache.put("London", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));
      cache.put("Berlin", createTestWeather("Berlin"));

      // Act
      Set<String> result = cache.getCachedCities();

      // Assert
      assertThat(result).hasSize(3).containsExactlyInAnyOrder("london", "paris", "berlin");
    }

    @Test
    @DisplayName("should return normalized names")
    void shouldReturnNormalizedNames() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      cache.put("LONDON", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));
      cache.put("BeRLiN", createTestWeather("Berlin"));

      // Act
      Set<String> result = cache.getCachedCities();

      // Assert
      assertThat(result).containsExactlyInAnyOrder("london", "paris", "berlin");
    }

    @Test
    @DisplayName("should return unmodifiable set")
    void shouldReturnUnmodifiableSet() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      cache.put("London", createTestWeather("London"));

      // Act
      Set<String> result = cache.getCachedCities();

      // Assert
      assertThatThrownBy(() -> result.add("Paris"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should preserve LRU order")
    void shouldPreserveLRUOrder() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      cache.put("London", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));
      cache.put("Berlin", createTestWeather("Berlin"));

      // Act
      Set<String> result = cache.getCachedCities();

      // Assert
      assertThat(result).containsExactly("london", "paris", "berlin");
    }
  }

  @Nested
  @DisplayName("Remove Operation")
  class RemoveTests {

    @Test
    @DisplayName("should remove existing entry")
    void shouldRemoveExistingEntry() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      String cityName = "London";
      cache.put(cityName, createTestWeather(cityName));

      // Act
      cache.remove(cityName);

      // Assert
      assertThat(cache.contains(cityName)).isFalse();
      assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("should do nothing for non-existent city")
    void shouldDoNothingForNonExistentCity() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      cache.put("London", createTestWeather("London"));

      // Act
      cache.remove("NonExistent");

      // Assert
      assertThat(cache.size()).isEqualTo(1);
      assertThat(cache.contains("London")).isTrue();
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      cache.put("london", createTestWeather("London"));

      // Act
      cache.remove("LONDON");

      // Assert
      assertThat(cache.contains("london")).isFalse();
      assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("should reject null city name")
    void shouldRejectNullCityName() {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> cache.remove(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Arrange
      WeatherCache cache = new WeatherCache();

      // Act & Assert
      assertThatThrownBy(() -> cache.remove(blankCityName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }
  }

  @Nested
  @DisplayName("Clear Operation")
  class ClearTests {

    @Test
    @DisplayName("should remove all entries")
    void shouldRemoveAllEntries() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      cache.put("London", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));
      cache.put("Berlin", createTestWeather("Berlin"));

      // Act
      cache.clear();

      // Assert
      assertThat(cache.contains("London")).isFalse();
      assertThat(cache.contains("Paris")).isFalse();
      assertThat(cache.contains("Berlin")).isFalse();
    }

    @Test
    @DisplayName("should result in zero size")
    void shouldResultInZeroSize() {
      // Arrange
      WeatherCache cache = new WeatherCache();
      cache.put("London", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));
      cache.put("Berlin", createTestWeather("Berlin"));

      // Act
      cache.clear();

      // Assert
      assertThat(cache.size()).isZero();
    }
  }

  @Nested
  @DisplayName("TTL Expiration")
  class TtlExpirationTests {

    @Test
    @DisplayName("should expire after TTL")
    void shouldExpireAfterTTL() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(100));
      String cityName = "London";
      cache.put(cityName, createTestWeather(cityName));

      // Act
      Thread.sleep(150); // Wait for expiration

      // Assert
      assertThat(cache.isFresh(cityName)).isFalse();
      assertThat(cache.get(cityName)).isEmpty();
    }

    @Test
    @DisplayName("should not expire before TTL")
    void shouldNotExpireBeforeTTL() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(200));
      String cityName = "London";
      cache.put(cityName, createTestWeather(cityName));

      // Act
      Thread.sleep(100); // Wait half of TTL

      // Assert
      assertThat(cache.isFresh(cityName)).isTrue();
      assertThat(cache.get(cityName)).isPresent();
    }

    @Test
    @DisplayName("should handle boundary at exact TTL")
    void shouldHandleBoundaryAtExactTTL() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(10, Duration.ofMillis(100));
      String cityName = "London";
      cache.put(cityName, createTestWeather(cityName));

      // Act
      Thread.sleep(100); // Wait exactly TTL

      // Assert - Entry should be expired (age > TTL due to execution time)
      assertThat(cache.isFresh(cityName)).isFalse();
    }
  }

  @Nested
  @DisplayName("LRU Eviction")
  class LruEvictionTests {

    @Test
    @DisplayName("should evict oldest when capacity exceeded")
    void shouldEvictOldestWhenCapacityExceeded() {
      // Arrange
      WeatherCache cache = new WeatherCache(3, Duration.ofMinutes(10));
      cache.put("London", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));
      cache.put("Berlin", createTestWeather("Berlin"));

      // Act
      cache.put("Madrid", createTestWeather("Madrid")); // Exceeds capacity

      // Assert
      assertThat(cache.contains("London")).isFalse(); // Oldest evicted
      assertThat(cache.contains("Paris")).isTrue();
      assertThat(cache.contains("Berlin")).isTrue();
      assertThat(cache.contains("Madrid")).isTrue();
      assertThat(cache.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("should evict least recently used not oldest")
    void shouldEvictLeastRecentlyUsedNotOldest() {
      // Arrange
      WeatherCache cache = new WeatherCache(3, Duration.ofMinutes(10));
      cache.put("London", createTestWeather("London"));
      cache.put("Paris", createTestWeather("Paris"));
      cache.put("Berlin", createTestWeather("Berlin"));

      // Act
      cache.get("London"); // Access oldest - makes it most recently used
      cache.put("Madrid", createTestWeather("Madrid")); // Exceeds capacity

      // Assert
      assertThat(cache.contains("London")).isTrue(); // Oldest but recently accessed
      assertThat(cache.contains("Paris")).isFalse(); // LRU - evicted
      assertThat(cache.contains("Berlin")).isTrue();
      assertThat(cache.contains("Madrid")).isTrue();
      assertThat(cache.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("should update access order on get")
    void shouldUpdateAccessOrderOnGet() {
      // Arrange
      WeatherCache cache = new WeatherCache(3, Duration.ofMinutes(10));
      Weather weather1 = createTestWeather("City1");
      Weather weather2 = createTestWeather("City2");
      Weather weather3 = createTestWeather("City3");
      Weather weather4 = createTestWeather("City4");

      // Act - Fill cache to capacity
      cache.put("City1", weather1);
      cache.put("City2", weather2);
      cache.put("City3", weather3);

      // Access City1 to make it most recently used
      cache.get("City1");

      // Add City4 - should evict City2 (least recently used)
      cache.put("City4", weather4);

      // Assert - Verify get() updated access order
      assertThat(cache.contains("City1")).isTrue(); // Retained (was accessed)
      assertThat(cache.contains("City2")).isFalse(); // Evicted (was LRU)
      assertThat(cache.contains("City3")).isTrue(); // Retained
      assertThat(cache.contains("City4")).isTrue(); // Added
      assertThat(cache.size()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("Thread Safety")
  class ThreadSafetyTests {

    @Test
    @DisplayName("should handle concurrent put operations")
    void shouldHandleConcurrentPuts() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(100, Duration.ofMinutes(10));
      int threadCount = 20;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger successCount = new AtomicInteger(0);

      // Act
      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                String cityName = "City-" + threadId;
                Weather weather = createTestWeather(cityName);
                cache.put(cityName, weather);
                successCount.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, TimeUnit.SECONDS);
      executor.shutdown();

      // Assert
      assertThat(successCount.get()).isEqualTo(threadCount);
      assertThat(cache.size()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("should handle concurrent get operations")
    void shouldHandleConcurrentGets() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache();
      String cityName = "London";
      Weather weather = createTestWeather(cityName);
      cache.put(cityName, weather);

      int threadCount = 10;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger successCount = new AtomicInteger(0);

      // Act
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                Optional<Weather> result = cache.get(cityName);
                if (result.isPresent()) {
                  successCount.incrementAndGet();
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, TimeUnit.SECONDS);
      executor.shutdown();

      // Assert in MAIN thread - CRITICAL!
      assertThat(successCount.get()).isEqualTo(threadCount);
      assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("should handle concurrent put and get")
    void shouldHandleConcurrentPutAndGet() throws InterruptedException {
      // Arrange
      WeatherCache cache = new WeatherCache(100, Duration.ofMinutes(10));
      int threadCount = 20;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      // Act
      for (int i = 0; i < threadCount; i++) {
        int threadId = i;
        executor.submit(
            () -> {
              try {
                if (threadId % 2 == 0) {
                  // Even threads: put
                  String cityName = "City-" + threadId;
                  cache.put(cityName, createTestWeather(cityName));
                } else {
                  // Odd threads: get
                  cache.get("City-" + (threadId - 1));
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(5, TimeUnit.SECONDS);
      executor.shutdown();

      // Assert
      assertThat(cache.size()).isLessThanOrEqualTo(threadCount / 2);
    }
  }

  /**
   * Creates test Weather object with sensible defaults.
   *
   * @param cityName name of the city
   * @return Weather instance for testing
   */
  private Weather createTestWeather(String cityName) {
    Instant now = Instant.now();
    return new Weather(
        new WeatherCondition("Clear", "clear sky"),
        Temperature.fromCelsius(20.0, 18.0),
        new Wind(5.0, 180),
        new Visibility(10000),
        new SunTimes(now.minusSeconds(21600), now.plusSeconds(21600)),
        cityName,
        TimezoneOffset.ofHours(0),
        now);
  }
}
