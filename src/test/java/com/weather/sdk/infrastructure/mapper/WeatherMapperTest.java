package com.weather.sdk.infrastructure.mapper;

import static org.assertj.core.api.Assertions.*;

import com.weather.sdk.domain.model.Weather;
import com.weather.sdk.infrastructure.dto.OpenWeatherApiResponse;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link WeatherMapper}.
 *
 * <p>Test strategy: Tests public API only. Private helper methods are tested implicitly through the
 * main {@code map()} method.
 *
 * <p>Coverage targets: Line ≥90%, Branch ≥85%
 *
 * @since 1.0.0
 */
@DisplayName("WeatherMapper")
class WeatherMapperTest {

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should not be instantiable")
    void shouldNotBeInstantiable() throws Exception {
      // Arrange
      var constructor = WeatherMapper.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      // Act & Assert
      assertThatThrownBy(constructor::newInstance)
          .isInstanceOf(InvocationTargetException.class)
          .hasCauseInstanceOf(AssertionError.class)
          .cause()
          .hasMessageContaining("Utility class");
    }
  }

  @Nested
  @DisplayName("Mapping Success")
  class MappingSuccessTests {

    @Test
    @DisplayName("should map complete response to Weather domain object")
    void shouldMapCompleteResponse() {
      // Arrange
      OpenWeatherApiResponse response = createValidResponse();

      // Act
      Weather weather = WeatherMapper.map(response);

      // Assert
      assertThat(weather).isNotNull();
      assertThat(weather.cityName()).isEqualTo("London");

      // WeatherCondition
      assertThat(weather.condition().main()).isEqualTo("Clouds");
      assertThat(weather.condition().description()).isEqualTo("scattered clouds");

      // Temperature
      assertThat(weather.temperature().temp()).isEqualTo(293.15);
      assertThat(weather.temperature().feelsLike()).isEqualTo(291.15);

      // Wind
      assertThat(weather.wind().speed()).isEqualTo(5.5);
      assertThat(weather.wind().direction()).isEqualTo(180);

      // Visibility
      assertThat(weather.visibility().meters()).isEqualTo(10000);

      // SunTimes
      assertThat(weather.sunTimes().sunrise()).isEqualTo(Instant.ofEpochSecond(1675751262));
      assertThat(weather.sunTimes().sunset()).isEqualTo(Instant.ofEpochSecond(1675787560));

      // TimezoneOffset
      assertThat(weather.timezone().offsetSeconds()).isEqualTo(3600);

      // Datetime
      assertThat(weather.datetime()).isEqualTo(Instant.ofEpochSecond(1675744800));
    }

    @Test
    @DisplayName("should map response with null wind direction")
    void shouldMapResponseWithNullWindDirection() {
      // Arrange
      OpenWeatherApiResponse.WeatherDto weatherDto =
          new OpenWeatherApiResponse.WeatherDto("Clear", "clear sky");
      OpenWeatherApiResponse.MainDto mainDto = new OpenWeatherApiResponse.MainDto(288.15, 286.15);
      OpenWeatherApiResponse.WindDto windDto = new OpenWeatherApiResponse.WindDto(3.5, null);
      OpenWeatherApiResponse.SysDto sysDto =
          new OpenWeatherApiResponse.SysDto(1675751262L, 1675787560L);

      OpenWeatherApiResponse response =
          new OpenWeatherApiResponse(
              List.of(weatherDto), mainDto, windDto, 10000, 1675744800L, sysDto, 3600, "Paris");

      // Act
      Weather weather = WeatherMapper.map(response);

      // Assert
      assertThat(weather).isNotNull();
      assertThat(weather.wind().speed()).isEqualTo(3.5);
      assertThat(weather.wind().direction()).isNull();
      assertThat(weather.wind().hasDirection()).isFalse();
    }
  }

  @Nested
  @DisplayName("Mapping Validation")
  class MappingValidationTests {

    @Test
    @DisplayName("should reject null response")
    void shouldRejectNullResponse() {
      // Act & Assert
      assertThatThrownBy(() -> WeatherMapper.map(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("response must not be null");
    }

    @Test
    @DisplayName("should reject null weather list")
    void shouldRejectNullWeatherList() {
      // Arrange
      OpenWeatherApiResponse response =
          new OpenWeatherApiResponse(
              null,
              new OpenWeatherApiResponse.MainDto(293.15, 291.15),
              new OpenWeatherApiResponse.WindDto(5.5, 180),
              10000,
              1675744800L,
              new OpenWeatherApiResponse.SysDto(1675751262L, 1675787560L),
              3600,
              "London");

      // Act & Assert
      assertThatThrownBy(() -> WeatherMapper.map(response))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("weather list must not be null or empty");
    }

    @Test
    @DisplayName("should reject empty weather list")
    void shouldRejectEmptyWeatherList() {
      // Arrange
      OpenWeatherApiResponse response =
          new OpenWeatherApiResponse(
              Collections.emptyList(),
              new OpenWeatherApiResponse.MainDto(293.15, 291.15),
              new OpenWeatherApiResponse.WindDto(5.5, 180),
              10000,
              1675744800L,
              new OpenWeatherApiResponse.SysDto(1675751262L, 1675787560L),
              3600,
              "London");

      // Act & Assert
      assertThatThrownBy(() -> WeatherMapper.map(response))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("weather list must not be null or empty");
    }

    @Test
    @DisplayName("should reject null main")
    void shouldRejectNullMain() {
      // Arrange
      OpenWeatherApiResponse response =
          new OpenWeatherApiResponse(
              List.of(new OpenWeatherApiResponse.WeatherDto("Clouds", "scattered clouds")),
              null,
              new OpenWeatherApiResponse.WindDto(5.5, 180),
              10000,
              1675744800L,
              new OpenWeatherApiResponse.SysDto(1675751262L, 1675787560L),
              3600,
              "London");

      // Act & Assert
      assertThatThrownBy(() -> WeatherMapper.map(response))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("main must not be null");
    }

    @Test
    @DisplayName("should reject null wind")
    void shouldRejectNullWind() {
      // Arrange
      OpenWeatherApiResponse response =
          new OpenWeatherApiResponse(
              List.of(new OpenWeatherApiResponse.WeatherDto("Clouds", "scattered clouds")),
              new OpenWeatherApiResponse.MainDto(293.15, 291.15),
              null,
              10000,
              1675744800L,
              new OpenWeatherApiResponse.SysDto(1675751262L, 1675787560L),
              3600,
              "London");

      // Act & Assert
      assertThatThrownBy(() -> WeatherMapper.map(response))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("wind must not be null");
    }

    @Test
    @DisplayName("should reject null sys")
    void shouldRejectNullSys() {
      // Arrange
      OpenWeatherApiResponse response =
          new OpenWeatherApiResponse(
              List.of(new OpenWeatherApiResponse.WeatherDto("Clouds", "scattered clouds")),
              new OpenWeatherApiResponse.MainDto(293.15, 291.15),
              new OpenWeatherApiResponse.WindDto(5.5, 180),
              10000,
              1675744800L,
              null,
              3600,
              "London");

      // Act & Assert
      assertThatThrownBy(() -> WeatherMapper.map(response))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("sys must not be null");
    }
  }

  private OpenWeatherApiResponse createValidResponse() {
    return new OpenWeatherApiResponse(
        List.of(new OpenWeatherApiResponse.WeatherDto("Clouds", "scattered clouds")),
        new OpenWeatherApiResponse.MainDto(293.15, 291.15),
        new OpenWeatherApiResponse.WindDto(5.5, 180),
        10000,
        1675744800L,
        new OpenWeatherApiResponse.SysDto(1675751262L, 1675787560L),
        3600,
        "London");
  }
}
