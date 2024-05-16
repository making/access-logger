# access-logger
Access Logger as a HttpExchangeRepository implementation 

```xml
<dependency>
	<groupId>am.ik.access-logger</groupId>
	<artifactId>access-logger</artifactId>
	<version>0.3.0</version>
</dependency>
<!-- Spring Boot Actuator is also required -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```


## Example

```java
import am.ik.accesslogger.AccessLogger;
import am.ik.accesslogger.AccessLoggerBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	@Bean
	public AccessLogger accessLogger() {
		return AccessLoggerBuilder.accessLogger().filter(httpExchange -> {
			final String uri = httpExchange.getRequest().getUri().getPath();
			return uri != null && !(uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator"));
		}).build();
	}

}
```

> [!NOTE]
> `AccessLoggerBuilder` is available since 0.3.0 

If Spring Security is enabled,


```java
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AccessLogger accessLogger, HttpExchangesProperties properties)
			throws Exception {
		return http
				// ....
				.addFilterAfter(
						new HttpExchangesFilter(accessLogger,
								properties.getRecording().getInclude()),
						SecurityContextHolderAwareRequestFilter.class)
				.build();
	}
```

By deafult, only `request_headers`, `response_headers` and `time_taken` are recorded.
In order to log all attributes, you also need to configure `management.httpexchanges.recording.include` as follows:

```properties
management.httpexchanges.recording.include=request_headers,response_headers,remote_address,principal,time_taken
```

## How to customize

```java
AccessLogger accessLogger = AccessLoggerBuilder.accessLogger()
	// Change the log level
	.level(Level.INFO) // Default: INFO
	// Change the logger name
	.loggerName("accesslog") // Default: accessLog
	// Enable structured logging via SLF4J key-value API
	.addKeyValues(true) // Default: false
	// Make log messages empty if structured logging is enabled
	.emptyLogMessage(true) // Default: false
	// Customize log message
	.logCustomizer((builder, exchange) -> {})
	// Customize logging Event
	.loggingEventBuilderCustomizer((builder, exchange) -> builder)
	// Filter the log message depending on the http request
	.filter(httpExchange -> {
		final String uri = httpExchange.getRequest().getUri().getPath();
		return uri != null && !(uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator"));
	})
	.build();
```