# Weather SDK

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6.3+-blue.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Code Coverage](https://img.shields.io/badge/Coverage-95%25-brightgreen.svg)]()

A production-ready Java SDK for the [OpenWeatherMap API](https://openweathermap.org/api) with intelligent caching, automatic data refresh, and comprehensive weather data models.

## Features

- 🌡️ **Comprehensive Weather Data** - Temperature, wind, visibility, sun times, and conditions
- 🗄️ **Smart Caching** - LRU cache with TTL (10 minutes, max 10 cities)
- ⚡ **Dual Operation Modes**
    - **ON_DEMAND** - Fetch only when requested (minimal API usage)
    - **POLLING** - Background updates for zero-latency responses
- 🔒 **Thread-Safe** - Safe concurrent access across multiple threads
- 🏗️ **Clean Architecture** - Domain-driven design with immutable value objects
- ✅ **Production-Ready** - 95%+ line coverage, 90%+ branch coverage
- 📦 **Minimal Dependencies** - Only Jackson (JSON) and SLF4J (logging)

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Architecture](#architecture)
- [Testing](#testing)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Requirements

- **Java**: 17 or higher
- **Maven**: 3.6.3 or higher
- **OpenWeatherMap API Key**: [Get one for free](https://openweathermap.org/appid)

## Installation

### Maven
```xml
<dependency>
    <groupId>com.weather</groupId>
    <artifactId>weather-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle
```gradle
implementation 'com.weather:weather-sdk:1.0.0-SNAPSHOT'
```

### Build from Source
```bash
git clone https://github.com/sultanrasulov/weather-sdk.git
cd weather-sdk
mvn clean install
```

## Quick Start

### Basic Usage (ON_DEMAND Mode)
```java
import com.weather.sdk.core.WeatherSDK;
import com.weather.sdk.core.WeatherSDKFactory;
import com.weather.sdk.domain.model.Weather;

public class WeatherApp {
    public static void main(String[] args) {
        WeatherSDK sdk = WeatherSDKFactory.create("your-api-key");
        
        try {
            Weather weather = sdk.getWeather("London");
            
            System.out.println("City: " + weather.cityName());
            System.out.println("Temperature: " + weather.temperature().tempCelsius() + "°C");
            System.out.println("Feels like: " + weather.temperature().feelsLikeCelsius() + "°C");
            System.out.println("Condition: " + weather.condition().description());
            System.out.println("Wind: " + weather.wind().speed() + " m/s");
            
        } catch (WeatherSDKException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            sdk.shutdown();
        }
    }
}
```

### Advanced Usage (POLLING Mode)
```java
import com.weather.sdk.core.OperationMode;

public class WeatherMonitor {
    public static void main(String[] args) {
        WeatherSDK sdk = WeatherSDKFactory.create("your-api-key", OperationMode.POLLING);
        
        try {
            // Initial fetch - populates cache
            Weather london = sdk.getWeather("London");
            Weather paris = sdk.getWeather("Paris");
            
            // Background polling keeps cache fresh automatically
            Thread.sleep(5000);
            
            // Zero-latency response from updated cache
            Weather updated = sdk.getWeather("London");
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sdk.shutdown(); // Critical: stops background polling
        }
    }
}
```

## API Reference

### WeatherSDKFactory

Singleton factory for SDK instance management.
```java
// Create with default mode (ON_DEMAND)
WeatherSDK sdk = WeatherSDKFactory.create("api-key");

// Create with POLLING mode
WeatherSDK sdk = WeatherSDKFactory.create("api-key", OperationMode.POLLING);

// Retrieve existing instance
WeatherSDK sdk = WeatherSDKFactory.getInstance("api-key");

// Destroy instances
WeatherSDKFactory.destroy("api-key");
WeatherSDKFactory.destroyAll();
```

### WeatherSDK

Main SDK interface for weather operations.
```java
// Fetch weather data
Weather weather = sdk.getWeather("London");

// Cache operations
boolean cached = sdk.isCached("London");
boolean fresh = sdk.isFresh("London");
Set<String> cities = sdk.getCachedCities();
sdk.clearCache();

// Lifecycle
sdk.shutdown();
boolean isShutdown = sdk.isShutdown();
```

### Weather Domain Model

Rich domain model with value objects:
```java
Weather weather = sdk.getWeather("London");

// Weather condition
weather.condition().main();           // "Clouds"
weather.condition().description();    // "scattered clouds"

// Temperature (Kelvin, Celsius, Fahrenheit)
weather.temperature().temp();         // Kelvin
weather.temperature().tempCelsius();
weather.temperature().tempFahrenheit();
weather.temperature().feelsLike();
weather.temperature().isFreezing();
weather.temperature().hasSignificantWindChill();

// Wind (with Beaufort scale)
weather.wind().speed();               // m/s
weather.wind().direction();           // degrees (0-360)
weather.wind().strength();            // WindStrength enum
weather.wind().strength().isGale();

// Visibility
weather.visibility().meters();
weather.visibility().kilometers();
weather.visibility().miles();

// Sun times
weather.sunTimes().sunrise();         // Instant
weather.sunTimes().sunset();
weather.sunTimes().daylightDuration(); // Duration
weather.sunTimes().isDaytime(Instant.now());

// Location & time
weather.cityName();
weather.timezone();
weather.datetime();                   // UTC
weather.localDatetime();              // Local time
```

## Configuration

### Cache Settings

Default: 10 cities, 10 minutes TTL, LRU eviction
```java
import com.weather.sdk.cache.WeatherCache;
import java.time.Duration;

WeatherCache cache = new WeatherCache(
    20,                          // 20 cities
    Duration.ofMinutes(5)        // 5 minutes TTL
);
```

### Polling Interval

Default: 10 minutes
```java
import com.weather.sdk.polling.PollingService;

PollingService polling = new PollingService(
    cache,
    client,
    Duration.ofMinutes(5)  // Poll every 5 minutes
);
```

## Error Handling

Comprehensive error handling with `WeatherSDKException`:
```java
try {
    Weather weather = sdk.getWeather("InvalidCity");
} catch (WeatherSDKException e) {
    Throwable cause = e.getCause();
    
    if (cause instanceof OpenWeatherApiException) {
        // Handle API errors (401, 404, 429, 5xx)
        System.err.println("API Error: " + cause.getMessage());
    }
}
```

**Common Error Codes:**

| Code | Description | Solution |
|------|-------------|----------|
| 401 | Invalid API key | Verify OpenWeatherMap API key |
| 404 | City not found | Check city name spelling |
| 429 | Rate limit exceeded | Wait or upgrade API plan |
| 5xx | Server error | Retry with exponential backoff |

## Testing

### Run Tests
```bash
# Unit tests only
mvn test

# All tests (unit + integration)
mvn verify -Pci

# With coverage report
mvn clean verify
# Report: target/jacoco-report/index.html
```

### Code Quality
```bash
# Check formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply

# Full quality check
mvn clean verify -Pci
```

### Coverage Requirements

- **Line Coverage**: 95%
- **Branch Coverage**: 90%
- **Class Coverage**: 90%

## Architecture

### Project Structure
```
weather-sdk/
├── src/main/java/com/weather/sdk/
│   ├── cache/              # LRU cache with TTL
│   ├── core/               # SDK core (Factory, WeatherSDK)
│   ├── domain/model/       # Domain models
│   ├── infrastructure/     # API client, DTOs, mappers
│   └── polling/            # Background polling service
└── src/test/java/          # Comprehensive test suite
```

### Design Patterns

- **Factory Pattern** - `WeatherSDKFactory` for instance management
- **Singleton Pattern** - One SDK instance per API key
- **Value Object Pattern** - Immutable domain models
- **Repository Pattern** - `WeatherCache` for data storage
- **Strategy Pattern** - `OperationMode` (ON_DEMAND vs POLLING)

### Key Design Decisions

1. **Immutable Value Objects** - Thread-safe, predictable
2. **Rich Domain Model** - Business logic encapsulation
3. **Explicit Resource Management** - `shutdown()` required
4. **Case-Insensitive City Names** - Normalized cache keys
5. **UTC-First Timestamps** - UTC storage, local conversion on demand

## Best Practices

### ✅ Resource Management
```java
WeatherSDK sdk = WeatherSDKFactory.create("api-key");
try {
    // Use SDK
} finally {
    sdk.shutdown();
}
```

### ✅ Choose the Right Mode

- **ON_DEMAND**: Infrequent queries, minimal API usage
- **POLLING**: Real-time monitoring, zero-latency requirements

### ✅ Error Handling
```java
try {
    Weather weather = sdk.getWeather(city);
} catch (WeatherSDKException e) {
    logger.error("Failed to fetch weather", e);
    // Show fallback UI or retry
}
```

### ✅ Reuse SDK Instances
```java
// Good - reuse existing
WeatherSDK sdk = WeatherSDKFactory.getInstance("api-key");

// Bad - duplicate instance (throws exception)
WeatherSDK duplicate = WeatherSDKFactory.create("api-key");
```

## Troubleshooting

### Invalid API Key

**Solution**: Verify at [OpenWeatherMap API Keys](https://home.openweathermap.org/api_keys)

### City Not Found

**Solution**: Try variations
```java
sdk.getWeather("London");
sdk.getWeather("London,UK");
sdk.getWeather("London,GB");
```

### Cache Not Working

**Solution**: Check status
```java
System.out.println("Cached: " + sdk.isCached("London"));
System.out.println("Fresh: " + sdk.isFresh("London"));
System.out.println("Cities: " + sdk.getCachedCities());
```

### Memory Leak in POLLING Mode

**Solution**: Always call `shutdown()`
```java
WeatherSDK sdk = WeatherSDKFactory.create("api-key", OperationMode.POLLING);
try {
    // Use SDK
} finally {
    sdk.shutdown(); // Stops background thread
}
```

## Contributing

Contributions welcome! Follow these steps:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Follow Google Java Format
4. Maintain 95%+ coverage
5. Commit changes (`git commit -m 'Add feature'`)
6. Push to branch (`git push origin feature/amazing-feature`)
7. Open Pull Request

### Before Submitting
```bash
mvn spotless:apply
mvn clean verify
```

## License

MIT License - see [LICENSE](LICENSE) file

## Acknowledgments

- [OpenWeatherMap](https://openweathermap.org/) - Weather API provider
- [Jackson](https://github.com/FasterXML/jackson) - JSON processing
- [WireMock](https://wiremock.org/) - HTTP mocking

---

**Made with ☕ by [Sultan](https://github.com/sultanrasulov)**