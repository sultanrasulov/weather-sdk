package com.weather.sdk.infrastructure.exception;

/**
 * Exception thrown when OpenWeatherMap API communication fails.
 *
 * <p>Indicates errors during HTTP communication, invalid responses, or API-specific errors.
 *
 * @since 1.0.0
 */
public class OpenWeatherApiException extends Exception {

  /**
   * Creates exception with message.
   *
   * @param message error description
   */
  public OpenWeatherApiException(String message) {
    super(message);
  }

  /**
   * Creates exception with message and cause.
   *
   * @param message error description
   * @param cause underlying cause
   */
  public OpenWeatherApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
