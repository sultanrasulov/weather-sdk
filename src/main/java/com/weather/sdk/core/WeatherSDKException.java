package com.weather.sdk.core;

/**
 * Base exception for all WeatherSDK errors.
 *
 * <p>This is a checked exception that wraps underlying errors from API requests, network failures,
 * and other SDK operations.
 *
 * <p><strong>Common causes:</strong>
 *
 * <ul>
 *   <li>Invalid API key (401)
 *   <li>City not found (404)
 *   <li>API rate limit exceeded (429)
 *   <li>Network connectivity issues
 *   <li>API server errors (5xx)
 * </ul>
 *
 * @since 1.0.0
 */
public class WeatherSDKException extends Exception {

  /**
   * Constructs a new WeatherSDKException with the specified message.
   *
   * @param message the detail message
   */
  public WeatherSDKException(String message) {
    super(message);
  }

  /**
   * Constructs a new WeatherSDKException with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the cause of this exception
   */
  public WeatherSDKException(String message, Throwable cause) {
    super(message, cause);
  }
}
