package com.weather.sdk.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.sdk.infrastructure.dto.OpenWeatherApiResponse;
import com.weather.sdk.infrastructure.exception.OpenWeatherApiException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * HTTP client for OpenWeatherMap Current Weather API.
 *
 * <p>Provides methods to retrieve current weather data for a given city using the OpenWeatherMap
 * API. Handles HTTP communication, response parsing, and error handling.
 *
 * @since 1.0.0
 */
public final class OpenWeatherApiClient {

  private static final String DEFAULT_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final String baseUrl;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  /**
   * Creates a new OpenWeatherApiClient with the specified API key.
   *
   * <p>Uses the default OpenWeatherMap API base URL.
   *
   * @param apiKey OpenWeatherMap API key (must not be null or blank)
   * @throws NullPointerException if apiKey is null
   * @throws IllegalArgumentException if apiKey is blank
   */
  public OpenWeatherApiClient(String apiKey) {
    this(apiKey, DEFAULT_BASE_URL);
  }

  /**
   * Creates a new OpenWeatherApiClient with custom base URL.
   *
   * <p>Package-private constructor for testing. Allows injection of custom base URL to support
   * mocking with WireMock or other test frameworks.
   *
   * @param apiKey OpenWeatherMap API key (must not be null or blank)
   * @param baseUrl custom API base URL (must not be null)
   * @throws NullPointerException if apiKey or baseUrl is null
   * @throws IllegalArgumentException if apiKey is blank
   */
  OpenWeatherApiClient(String apiKey, String baseUrl) {
    Objects.requireNonNull(apiKey, "apiKey must not be null");
    Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    if (apiKey.isBlank()) {
      throw new IllegalArgumentException("apiKey must not be blank");
    }

    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Retrieves current weather data for the specified city.
   *
   * <p>Makes a synchronous HTTP request to OpenWeatherMap API and returns parsed weather data.
   *
   * @param cityName name of the city (must not be null or blank)
   * @return parsed weather data
   * @throws NullPointerException if cityName is null
   * @throws IllegalArgumentException if cityName is blank
   * @throws OpenWeatherApiException if API request fails
   * @since 1.0.0
   */
  public OpenWeatherApiResponse getCurrentWeather(String cityName) throws OpenWeatherApiException {
    Objects.requireNonNull(cityName, "cityName must not be null");
    if (cityName.isBlank()) {
      throw new IllegalArgumentException("cityName must not be blank");
    }

    String url = buildUrl(cityName);
    HttpRequest request = buildRequest(url);
    HttpResponse<String> response = executeRequest(request);
    validateResponse(response);
    return parseResponse(response.body());
  }

  /**
   * Builds URL for current weather API request.
   *
   * <p>City name is URL-encoded to handle special characters, spaces, and non-ASCII symbols.
   *
   * @param cityName name of the city (will be URL-encoded)
   * @return complete API URL with query parameters
   */
  private String buildUrl(String cityName) {
    String encodedCityName = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
    return this.baseUrl + "?q=" + encodedCityName + "&appid=" + this.apiKey;
  }

  /**
   * Builds HTTP request for the given URL.
   *
   * @param url API endpoint URL
   * @return configured HttpRequest
   */
  private HttpRequest buildRequest(String url) {
    return HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Accept", "application/json")
        .timeout(REQUEST_TIMEOUT)
        .GET()
        .build();
  }

  /**
   * Executes HTTP request and returns response.
   *
   * @param request HTTP request to execute
   * @return HTTP response
   * @throws OpenWeatherApiException if request execution fails
   */
  private HttpResponse<String> executeRequest(HttpRequest request) throws OpenWeatherApiException {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new OpenWeatherApiException("Network error during API request", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OpenWeatherApiException("Request was interrupted", e);
    }
  }

  /**
   * Validates HTTP response status code.
   *
   * @param response HTTP response to validate
   * @throws OpenWeatherApiException if response indicates an error
   */
  private void validateResponse(HttpResponse<String> response) throws OpenWeatherApiException {
    int statusCode = response.statusCode();

    if (statusCode >= 200 && statusCode < 300) {
      return; // Success
    }

    String errorMessage =
        switch (statusCode) {
          case 401 -> "Invalid API key. Please check your OpenWeatherMap API key.";
          case 404 -> "City not found. Please check the city name.";
          case 429 -> "API rate limit exceeded. Please try again later.";
          case 500, 502, 503, 504 -> "OpenWeatherMap API server error. Please try again later.";
          default -> "API request failed with status code: " + statusCode;
        };

    // Include response body if available for additional context
    String responseBody = response.body();
    if (responseBody != null && !responseBody.isBlank()) {
      errorMessage += " | Response: " + responseBody;
    }

    throw new OpenWeatherApiException(errorMessage);
  }

  /**
   * Parses JSON response body to DTO.
   *
   * @param responseBody JSON response body
   * @return parsed response DTO
   * @throws OpenWeatherApiException if JSON parsing fails
   */
  private OpenWeatherApiResponse parseResponse(String responseBody) throws OpenWeatherApiException {
    try {
      return objectMapper.readValue(responseBody, OpenWeatherApiResponse.class);
    } catch (IOException e) {
      throw new OpenWeatherApiException("Failed to parse API response: " + e.getMessage(), e);
    }
  }
}
