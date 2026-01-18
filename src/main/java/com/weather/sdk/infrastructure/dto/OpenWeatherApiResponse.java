package com.weather.sdk.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Dto for OpenWeatherMap Current Weather API response.
 *
 * <p>Maps JSON response from OpenWeatherMap API to Java objects. Only fields required by the SDK
 * are mapped.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherApiResponse(
    @JsonProperty("weather") List<WeatherDto> weather,
    @JsonProperty("main") MainDto main,
    @JsonProperty("wind") WindDto wind,
    @JsonProperty("visibility") int visibility,
    @JsonProperty("dt") long dt,
    @JsonProperty("sys") SysDto sys,
    @JsonProperty("timezone") int timezone,
    @JsonProperty("name") String name) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record WeatherDto(
      @JsonProperty("main") String main, @JsonProperty("description") String description) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MainDto(
      @JsonProperty("temp") double temp, @JsonProperty("feels_like") double feelsLike) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record WindDto(@JsonProperty("speed") double speed, @JsonProperty("deg") Integer deg) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SysDto(
      @JsonProperty("sunrise") long sunrise, @JsonProperty("sunset") long sunset) {}
}
