package com.weather.sdk.core;

/**
 * Operation mode for WeatherSDK.
 *
 * <p>Defines how the SDK retrieves and updates weather data:
 *
 * <ul>
 *   <li>{@link #ON_DEMAND} - fetches data only when requested
 *   <li>{@link #POLLING} - periodically updates cached data in background
 * </ul>
 *
 * @since 1.0.0
 */
public enum OperationMode {

  /**
   * On-demand mode.
   *
   * <p>Weather data is fetched from API only when {@code getWeather()} is called. Cache is updated
   * only on explicit requests.
   *
   * <p><strong>Use case:</strong> Low-frequency queries, minimal API usage.
   */
  ON_DEMAND,

  /**
   * Polling mode.
   *
   * <p>Weather data is automatically refreshed in background for all cached cities. Provides
   * zero-latency responses for cached data.
   *
   * <p><strong>Use case:</strong> Real-time monitoring, frequent updates.
   *
   * <p><strong>Note:</strong> Requires calling {@code shutdown()} to stop background polling.
   */
  POLLING;

  /**
   * Checks if this mode is ON_DEMAND.
   *
   * @return true if this is ON_DEMAND mode
   * @since 1.0.0
   */
  public boolean isOnDemand() {
    return this == ON_DEMAND;
  }

  /**
   * Checks if this mode is POLLING.
   *
   * @return true if this is POLLING mode
   * @since 1.0.0
   */
  public boolean isPolling() {
    return this == POLLING;
  }
}
