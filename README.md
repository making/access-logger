# access-logger
Access Logger as a HttpExchangeRepository implementation 

```xml
<dependency>
	<groupId>am.ik.access-logger</groupId>
	<artifactId>access-logger</artifactId>
	<version>0.1.5</version>
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
	@Bean
	public AccessLogger accessLogger() {
		return new AccessLogger(httpExchange -> {
			final String uri = httpExchange.getRequest().getUri().getPath();
			return uri != null && !(uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator"));
		});
	}
}
```

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

By deafult, only `request_headers`, `response_headers` and `time_taken` are recordes.
In order to log all attributes, you also need to configure `management.httpexchanges.recording.include` as follows:

```properties
management.httpexchanges.recording.include=request_headers,response_headers,remote_address,principal,response_headers,time_taken
```
