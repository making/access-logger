/*
 * Copyright (C) 2023-2024 Toshiaki Maki <makingx@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package am.ik.accesslogger;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.event.Level;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Principal;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class AccessLoggerTest {

	private BasicJsonTester json = new BasicJsonTester(getClass());

	@Test
	void addDefault(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger().build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"))),
				new HttpExchange.Response(200, Map.of()), null, null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("accesslog");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("INFO");
		assertThat(content).extractingJsonPathStringValue("@.message")
				.isEqualToIgnoringNewLines(
						"""
								remote=127.0.0.1 ts="2024-05-16T00:00:00Z" method=GET url="https://example.com" response_code=200 user_agent="mock" duration=1000
								""");
	}

	@Test
	void addCustomizeLog(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger()
				.logCustomizer((builder, exchange) -> builder.append("x_request_id=")
						.append(exchange.getRequest().getHeaders().get("x-request-id").get(0)))
				.build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"), "x-request-id", List.of("xyz"))),
				new HttpExchange.Response(200, Map.of()), null, null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("accesslog");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("INFO");
		assertThat(content).extractingJsonPathStringValue("@.message")
				.isEqualToIgnoringNewLines(
						"""
								remote=127.0.0.1 ts="2024-05-16T00:00:00Z" method=GET url="https://example.com" response_code=200 user_agent="mock" duration=1000 x_request_id=xyz
								""");
	}

	@Test
	void addDebugLevel(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger().level(Level.DEBUG).build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"))),
				new HttpExchange.Response(200, Map.of()), null, null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("accesslog");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("DEBUG");
		assertThat(content).extractingJsonPathStringValue("@.message")
				.isEqualToIgnoringNewLines(
						"""
								remote=127.0.0.1 ts="2024-05-16T00:00:00Z" method=GET url="https://example.com" response_code=200 user_agent="mock" duration=1000
								""");
	}

	@Test
	void addDifferentLoggerName(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger().loggerName("ACCESS_LOG").build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"))),
				new HttpExchange.Response(200, Map.of()), null, null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("ACCESS_LOG");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("INFO");
		assertThat(content).extractingJsonPathStringValue("@.message")
				.isEqualToIgnoringNewLines(
						"""
								remote=127.0.0.1 ts="2024-05-16T00:00:00Z" method=GET url="https://example.com" response_code=200 user_agent="mock" duration=1000
								""");
	}

	@Test
	void addKeyValues(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger().addKeyValues(true).build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"))),
				new HttpExchange.Response(200, Map.of()), null, null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("accesslog");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("INFO");
		assertThat(content).extractingJsonPathStringValue("@.message")
				.isEqualToIgnoringNewLines(
						"""
								remote=127.0.0.1 ts="2024-05-16T00:00:00Z" method=GET url="https://example.com" response_code=200 user_agent="mock" duration=1000
								""");
		assertThat(content).extractingJsonPathStringValue("@.remote").isEqualTo("127.0.0.1");
		assertThat(content).extractingJsonPathStringValue("@.ts").isEqualTo("2024-05-16T00:00:00Z");
		assertThat(content).extractingJsonPathStringValue("@.method").isEqualTo("GET");
		assertThat(content).extractingJsonPathStringValue("@.url").isEqualTo("https://example.com");
		assertThat(content).extractingJsonPathStringValue("@.response_code").isEqualTo("200");
		assertThat(content).extractingJsonPathStringValue("@.user_agent").isEqualTo("mock");
		assertThat(content).extractingJsonPathStringValue("@.duration").isEqualTo("1000");
	}

	@Test
	void addKeyValuesWithReferer(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger().addKeyValues(true).build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"), "referer", List.of("https://google.com"))),
				new HttpExchange.Response(200, Map.of()), null, null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("accesslog");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("INFO");
		assertThat(content).extractingJsonPathStringValue("@.message")
				.isEqualToIgnoringNewLines(
						"""
								remote=127.0.0.1 ts="2024-05-16T00:00:00Z" method=GET url="https://example.com" response_code=200 referer="https://google.com" user_agent="mock" duration=1000
								""");
		assertThat(content).extractingJsonPathStringValue("@.remote").isEqualTo("127.0.0.1");
		assertThat(content).extractingJsonPathStringValue("@.ts").isEqualTo("2024-05-16T00:00:00Z");
		assertThat(content).extractingJsonPathStringValue("@.method").isEqualTo("GET");
		assertThat(content).extractingJsonPathStringValue("@.url").isEqualTo("https://example.com");
		assertThat(content).extractingJsonPathStringValue("@.response_code").isEqualTo("200");
		assertThat(content).extractingJsonPathStringValue("@.user_agent").isEqualTo("mock");
		assertThat(content).extractingJsonPathStringValue("@.duration").isEqualTo("1000");
		assertThat(content).extractingJsonPathStringValue("@.referer").isEqualTo("https://google.com");
	}

	@Test
	void addKeyValuesWithPrincipal(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger().addKeyValues(true).build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"))),
				new HttpExchange.Response(200, Map.of()), new Principal("admin"), null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("accesslog");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("INFO");
		assertThat(content).extractingJsonPathStringValue("@.message")
				.isEqualToIgnoringNewLines(
						"""
								remote=127.0.0.1 user="admin" ts="2024-05-16T00:00:00Z" method=GET url="https://example.com" response_code=200 user_agent="mock" duration=1000
								""");
		assertThat(content).extractingJsonPathStringValue("@.remote").isEqualTo("127.0.0.1");
		assertThat(content).extractingJsonPathStringValue("@.ts").isEqualTo("2024-05-16T00:00:00Z");
		assertThat(content).extractingJsonPathStringValue("@.method").isEqualTo("GET");
		assertThat(content).extractingJsonPathStringValue("@.url").isEqualTo("https://example.com");
		assertThat(content).extractingJsonPathStringValue("@.response_code").isEqualTo("200");
		assertThat(content).extractingJsonPathStringValue("@.user_agent").isEqualTo("mock");
		assertThat(content).extractingJsonPathStringValue("@.duration").isEqualTo("1000");
		assertThat(content).extractingJsonPathStringValue("@.user").isEqualTo("admin");
	}

	@Test
	void addKeyValuesEmptyLogMessage(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger().addKeyValues(true).emptyLogMessage(true).build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"))),
				new HttpExchange.Response(200, Map.of()), null, null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("accesslog");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("INFO");
		assertThat(content).extractingJsonPathStringValue("@.message").isEmpty();
		assertThat(content).extractingJsonPathStringValue("@.remote").isEqualTo("127.0.0.1");
		assertThat(content).extractingJsonPathStringValue("@.ts").isEqualTo("2024-05-16T00:00:00Z");
		assertThat(content).extractingJsonPathStringValue("@.method").isEqualTo("GET");
		assertThat(content).extractingJsonPathStringValue("@.url").isEqualTo("https://example.com");
		assertThat(content).extractingJsonPathStringValue("@.response_code").isEqualTo("200");
		assertThat(content).extractingJsonPathStringValue("@.user_agent").isEqualTo("mock");
		assertThat(content).extractingJsonPathStringValue("@.duration").isEqualTo("1000");
	}

	@Test
	void addEmptyLogMessageWithoutKeyValues() {
		assertThatThrownBy(() -> {
			AccessLoggerBuilder.accessLogger().emptyLogMessage(true).build();
		}).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("'emptyLogMessage' can be true only when 'addKeyValues' is true.");
	}

	@Test
	void addCustomizeLogEventBuilder(CapturedOutput output) {
		AccessLogger accessLogger = AccessLoggerBuilder.accessLogger()
				.addKeyValues(true)
				.emptyLogMessage(true)
				.loggingEventBuilderCustomizer((builder, exchange) -> builder.addKeyValue("x-request-id",
						exchange.getRequest().getHeaders().get("x-request-id").get(0)))
				.build();
		HttpExchange httpExchange = new HttpExchange(Instant.parse("2024-05-16T00:00:00Z"),
				new HttpExchange.Request(URI.create("https://example.com"), "127.0.0.1", "GET",
						Map.of("user-agent", List.of("mock"), "x-request-id", List.of("xyz"))),
				new HttpExchange.Response(200, Map.of()), null, null, Duration.ofSeconds(1));
		accessLogger.add(httpExchange);
		JsonContent<Object> content = json.from(output.getOut());
		assertThat(content).extractingJsonPathStringValue("@.['log.logger']").isEqualTo("accesslog");
		assertThat(content).extractingJsonPathStringValue("@.['log.level']").isEqualTo("INFO");
		assertThat(content).extractingJsonPathStringValue("@.message").isEmpty();
		assertThat(content).extractingJsonPathStringValue("@.remote").isEqualTo("127.0.0.1");
		assertThat(content).extractingJsonPathStringValue("@.ts").isEqualTo("2024-05-16T00:00:00Z");
		assertThat(content).extractingJsonPathStringValue("@.method").isEqualTo("GET");
		assertThat(content).extractingJsonPathStringValue("@.url").isEqualTo("https://example.com");
		assertThat(content).extractingJsonPathStringValue("@.response_code").isEqualTo("200");
		assertThat(content).extractingJsonPathStringValue("@.user_agent").isEqualTo("mock");
		assertThat(content).extractingJsonPathStringValue("@.duration").isEqualTo("1000");
		assertThat(content).extractingJsonPathStringValue("@.['x-request-id']").isEqualTo("xyz");
	}

}