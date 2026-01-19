package com.weather.sdk.infrastructure.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.weather.sdk.infrastructure.dto.OpenWeatherApiResponse;
import com.weather.sdk.infrastructure.exception.OpenWeatherApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for {@link OpenWeatherApiClient}.
 *
 * <p>Test strategy: Behavior-driven testing through public API. Uses WireMock for HTTP mocking to
 * avoid external dependencies and ensure deterministic tests.
 *
 * <p>Coverage targets: Line ≥70%, Branch ≥70%, Behavior 100%
 *
 * @since 1.0.0
 */
@DisplayName("OpenWeatherApiClient")
class OpenWeatherApiClientTest {

  private static final String API_KEY = "test-api-key";
  private static final String TEST_BASE_URL = "http://localhost:8089/data/2.5/weather";

  private static WireMockServer wireMockServer;
  private OpenWeatherApiClient client;

  @BeforeAll
  static void startWireMock() {
    wireMockServer = new WireMockServer(8089);
    wireMockServer.start();
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMockServer != null && wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  @BeforeEach
  void setUp() {
    wireMockServer.resetAll();
    client = new OpenWeatherApiClient(API_KEY, TEST_BASE_URL);
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorTests {

    @Test
    @DisplayName("should create client with valid API key")
    void shouldCreateClientWithValidApiKey() {
      // Act
      OpenWeatherApiClient client = new OpenWeatherApiClient(API_KEY);

      // Assert
      assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("should reject null API key")
    void shouldRejectNullApiKey() {
      // Act & Assert
      assertThatThrownBy(() -> new OpenWeatherApiClient(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("apiKey must not be null");
    }

    @ParameterizedTest(name = "should reject blank API key: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank API key")
    void shouldRejectBlankApiKey(String blankApiKey) {
      // Act & Assert
      assertThatThrownBy(() -> new OpenWeatherApiClient(blankApiKey))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("apiKey must not be blank");
    }
  }

  @Nested
  @DisplayName("Input Validation")
  class InputValidationTests {

    @Test
    @DisplayName("should reject null cityName")
    void shouldRejectNullCityName() {
      // Act & Assert
      assertThatThrownBy(() -> client.getCurrentWeather(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cityName must not be null");
    }

    @ParameterizedTest(name = "should reject blank city name: \"{0}\"")
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", " \t\n "})
    @DisplayName("should reject blank city name")
    void shouldRejectBlankCityName(String blankCityName) {
      // Act & Assert
      assertThatThrownBy(() -> client.getCurrentWeather(blankCityName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cityName must not be blank");
    }
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarioTests {

    @Test
    @DisplayName("should return weather for valid city")
    void shouldReturnWeatherForValidCity() throws OpenWeatherApiException {
      // Arrange
      String cityName = "London";
      String jsonResponse =
          """
          {
            "weather": [{"main": "Clouds", "description": "scattered clouds"}],
            "main": {"temp": 293.15, "feels_like": 291.15},
            "wind": {"speed": 5.5, "deg": 180},
            "visibility": 10000,
            "dt": 1675744800,
            "sys": {"sunrise": 1675751262, "sunset": 1675787560},
            "timezone": 3600,
            "name": "London"
          }
          """;

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(okJson(jsonResponse)));

      // Act
      OpenWeatherApiResponse response = client.getCurrentWeather(cityName);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.name()).isEqualTo("London");
      assertThat(response.weather()).hasSize(1);
      assertThat(response.weather().get(0).main()).isEqualTo("Clouds");
      assertThat(response.weather().get(0).description()).isEqualTo("scattered clouds");
      assertThat(response.main().temp()).isEqualTo(293.15);
      assertThat(response.main().feelsLike()).isEqualTo(291.15);
      assertThat(response.wind().speed()).isEqualTo(5.5);
      assertThat(response.wind().deg()).isEqualTo(180);
      assertThat(response.visibility()).isEqualTo(10000);
      assertThat(response.dt()).isEqualTo(1675744800);
      assertThat(response.timezone()).isEqualTo(3600);
      assertThat(response.sys().sunrise()).isEqualTo(1675751262);
      assertThat(response.sys().sunset()).isEqualTo(1675787560);
    }

    @Test
    @DisplayName("should handle city names with spaces")
    void shouldHandleCityWithSpaces() throws OpenWeatherApiException {
      // Arrange
      String cityName = "New York";
      String jsonResponse =
          """
          {
            "weather": [{"main": "Clear", "description": "clear sky"}],
            "main": {"temp": 288.15, "feels_like": 286.15},
            "wind": {"speed": 3.5, "deg": 90},
            "visibility": 10000,
            "dt": 1675744800,
            "sys": {"sunrise": 1675751262, "sunset": 1675787560},
            "timezone": -18000,
            "name": "New York"
          }
          """;

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(okJson(jsonResponse)));

      // Act
      OpenWeatherApiResponse response = client.getCurrentWeather(cityName);

      // Assert - Focus on what this test verifies
      assertThat(response).isNotNull();
      assertThat(response.name()).isEqualTo("New York");
      assertThat(response.weather()).hasSize(1);
      assertThat(response.weather().get(0).main()).isEqualTo("Clear");
    }

    @Test
    @DisplayName("should handle city names with special characters")
    void shouldHandleCityWithSpecialCharacters() throws OpenWeatherApiException {
      // Arrange
      String cityName = "São Paulo";
      String jsonResponse =
          """
          {
            "weather": [{"main": "Rain", "description": "light rain"}],
            "main": {"temp": 295.15, "feels_like": 294.15},
            "wind": {"speed": 4.2, "deg": 135},
            "visibility": 8000,
            "dt": 1675744800,
            "sys": {"sunrise": 1675751262, "sunset": 1675787560},
            "timezone": -10800,
            "name": "São Paulo"
          }
          """;

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(okJson(jsonResponse)));

      // Act
      OpenWeatherApiResponse response = client.getCurrentWeather(cityName);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.name()).isEqualTo("São Paulo");
      assertThat(response.weather()).hasSize(1);
      assertThat(response.weather().get(0).main()).isEqualTo("Rain");
    }

    @Test
    @DisplayName("should handle non-ASCII city names")
    void shouldHandleNonASCIICityNames() throws OpenWeatherApiException {
      // Arrange
      String cityName = "東京";
      String jsonResponse =
          """
          {
            "weather": [{"main": "Clear", "description": "clear sky"}],
            "main": {"temp": 285.15, "feels_like": 283.15},
            "wind": {"speed": 2.5, "deg": 45},
            "visibility": 10000,
            "dt": 1675744800,
            "sys": {"sunrise": 1675751262, "sunset": 1675787560},
            "timezone": 32400,
            "name": "Tokyo"
          }
          """;

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(okJson(jsonResponse)));

      // Act
      OpenWeatherApiResponse response = client.getCurrentWeather(cityName);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.name()).isEqualTo("Tokyo");
      assertThat(response.weather()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Error Scenarios")
  class ErrorScenarioTests {

    @Test
    @DisplayName("should throw exception for 401 invalid API key")
    void shouldThrowFor401InvalidApiKey() {
      // Arrange
      String cityName = "London";

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(
                  aResponse()
                      .withStatus(401)
                      .withHeader("Content-Type", "application/json")
                      .withBody(
                          """
                      {"cod": 401, "message": "Invalid API key"}
                      """)));

      // Act & Assert
      assertThatThrownBy(() -> client.getCurrentWeather(cityName))
          .isInstanceOf(OpenWeatherApiException.class)
          .hasMessageContaining("Invalid API key");
    }

    @Test
    @DisplayName("should throw exception for 404 city not found")
    void shouldThrowFor404CityNotFound() {
      // Arrange
      String cityName = "NonExistentCity";

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(
                  aResponse()
                      .withStatus(404)
                      .withHeader("Content-Type", "application/json")
                      .withBody(
                          """
                      {"cod": "404", "message": "city not found"}
                      """)));

      // Act & Assert
      assertThatThrownBy(() -> client.getCurrentWeather(cityName))
          .isInstanceOf(OpenWeatherApiException.class)
          .hasMessageContaining("City not found");
    }

    @Test
    @DisplayName("should throw exception for 429 rate limit exceeded")
    void shouldThrowFor429RateLimitExceeded() {
      // Arrange
      String cityName = "London";

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(
                  aResponse()
                      .withStatus(429)
                      .withHeader("Content-Type", "application/json")
                      .withBody(
                          """
                      {"cod": 429, "message": "rate limit exceeded"}
                      """)));

      // Act & Assert
      assertThatThrownBy(() -> client.getCurrentWeather(cityName))
          .isInstanceOf(OpenWeatherApiException.class)
          .hasMessageContaining("rate limit");
    }

    @ParameterizedTest(name = "should throw exception for {0} server error")
    @ValueSource(ints = {500, 502, 503, 504})
    @DisplayName("should throw exception for 5xx server errors")
    void shouldThrowFor5xxServerError(int statusCode) {
      // Arrange
      String cityName = "London";

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(
                  aResponse()
                      .withStatus(statusCode)
                      .withHeader("Content-Type", "application/json")
                      .withBody(
                          """
                      {"cod": %d, "message": "server error"}
                      """
                              .formatted(statusCode))));

      // Act & Assert
      assertThatThrownBy(() -> client.getCurrentWeather(cityName))
          .isInstanceOf(OpenWeatherApiException.class)
          .hasMessageContaining("server error");
    }

    @Test
    @DisplayName("should throw exception for network error")
    void shouldThrowForNetworkError() {
      // Arrange
      String cityName = "London";

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

      // Act & Assert
      assertThatThrownBy(() -> client.getCurrentWeather(cityName))
          .isInstanceOf(OpenWeatherApiException.class)
          .hasMessageContaining("Network error");
    }

    @Test
    @DisplayName("should throw exception for invalid JSON response")
    void shouldThrowForInvalidJsonResponse() {
      // Arrange
      String cityName = "London";

      wireMockServer.stubFor(
          get(urlPathEqualTo("/data/2.5/weather"))
              .withQueryParam("q", equalTo(cityName))
              .withQueryParam("appid", equalTo(API_KEY))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody("{ invalid json }")));

      // Act & Assert
      assertThatThrownBy(() -> client.getCurrentWeather(cityName))
          .isInstanceOf(OpenWeatherApiException.class)
          .hasMessageContaining("Failed to parse");
    }
  }
}
