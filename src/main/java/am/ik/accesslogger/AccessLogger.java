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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.jilt.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Principal;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Request;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Response;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

public class AccessLogger implements HttpExchangeRepository {

	private final Predicate<HttpExchange> filter;

	@Nullable
	private final BiConsumer<StringBuilder, HttpExchange> logCustomizer;

	private final Level level;

	private final boolean addKeyValues;

	private final boolean emptyLogMessage;

	@Nullable
	private final BiFunction<LoggingEventBuilder, HttpExchange, LoggingEventBuilder> loggingEventBuilderCustomizer;

	private final Logger log;

	@Builder
	public AccessLogger(@Nullable Predicate<HttpExchange> filter,
			@Nullable BiConsumer<StringBuilder, HttpExchange> logCustomizer, @Nullable String loggerName,
			@Nullable Level level, boolean addKeyValues, boolean emptyLogMessage,
			@Nullable BiFunction<LoggingEventBuilder, HttpExchange, LoggingEventBuilder> loggingEventBuilderCustomizer) {
		if (emptyLogMessage && !addKeyValues) {
			throw new IllegalArgumentException("'emptyLogMessage' can be true only when 'addKeyValues' is true.");
		}
		this.filter = Objects.requireNonNullElseGet(filter, () -> __ -> true);
		this.logCustomizer = logCustomizer;
		this.log = LoggerFactory.getLogger(Objects.requireNonNullElse(loggerName, "accesslog"));
		this.level = Objects.requireNonNullElse(level, Level.INFO);
		this.addKeyValues = addKeyValues;
		this.emptyLogMessage = emptyLogMessage;
		this.loggingEventBuilderCustomizer = loggingEventBuilderCustomizer;
	}

	public AccessLogger(@Nullable Predicate<HttpExchange> filter) {
		this(filter, null, null, null, false, false, null);
	}

	public AccessLogger() {
		this(null);
	}

	@Override
	public List<HttpExchange> findAll() {
		return List.of();
	}

	@Override
	public void add(HttpExchange httpExchange) {
		if (!log.isEnabledForLevel(this.level)) {
			return;
		}
		final Request request = httpExchange.getRequest();
		final URI uri = request.getUri();
		if (!filter.test(httpExchange)) {
			return;
		}
		final Response response = httpExchange.getResponse();
		final Principal principal = httpExchange.getPrincipal();
		final Duration timeTaken = httpExchange.getTimeTaken();
		final Map<String, List<String>> headers = request.getHeaders();
		final StringBuilder log = new StringBuilder();
		final String remoteAddress = request.getRemoteAddress();
		LoggingEventBuilder eventBuilder = this.log.atLevel(this.level);
		if (remoteAddress != null) {
			if (!this.emptyLogMessage) {
				log.append("remote=").append(remoteAddress).append(" ");
			}
			if (this.addKeyValues) {
				eventBuilder = eventBuilder.addKeyValue("remote", remoteAddress);
			}
		}
		if (principal != null) {
			String user = principal.getName();
			if (!this.emptyLogMessage) {
				log.append("user=\"").append(user).append("\" ");
			}
			if (this.addKeyValues) {
				eventBuilder = eventBuilder.addKeyValue("user", user);
			}
		}
		if (!this.emptyLogMessage) {
			log.append("ts=\"").append(httpExchange.getTimestamp()).append("\" ");
			log.append("method=").append(request.getMethod()).append(" ");
			log.append("url=\"").append(uri).append("\" ");
			log.append("response_code=").append(response.getStatus()).append(" ");
		}
		if (this.addKeyValues) {
			eventBuilder = eventBuilder.addKeyValue("ts", httpExchange.getTimestamp())
				.addKeyValue("method", request.getMethod())
				.addKeyValue("url", uri)
				.addKeyValue("response_code", response.getStatus());
		}

		final List<String> referer = headers.get("referer");
		if (!CollectionUtils.isEmpty(referer)) {
			String referer0 = referer.get(0);
			if (!this.emptyLogMessage) {
				log.append("referer=\"").append(referer0).append("\" ");
			}
			if (this.addKeyValues) {
				eventBuilder = eventBuilder.addKeyValue("referer", referer0);
			}
		}
		final List<String> userAgent = headers.get("user-agent");
		if (!CollectionUtils.isEmpty(userAgent)) {
			String userAgent0 = userAgent.get(0);
			if (!this.emptyLogMessage) {
				log.append("user_agent=\"").append(userAgent0).append("\" ");
			}
			if (this.addKeyValues) {
				eventBuilder = eventBuilder.addKeyValue("user_agent", userAgent0);
			}
		}
		if (timeTaken != null) {
			long duration = timeTaken.toMillis();
			if (!this.emptyLogMessage) {
				log.append("duration=").append(duration).append(" ");
			}
			if (this.addKeyValues) {
				eventBuilder = eventBuilder.addKeyValue("duration", duration);
			}
		}
		if (this.logCustomizer != null) {
			this.logCustomizer.accept(log, httpExchange);
		}
		if (this.loggingEventBuilderCustomizer != null) {
			eventBuilder = this.loggingEventBuilderCustomizer.apply(eventBuilder, httpExchange);
		}
		eventBuilder.log(log.toString().trim());
	}

}