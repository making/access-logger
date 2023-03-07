package am.ik.accesslogger;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Principal;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Request;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Response;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.util.CollectionUtils;

public class AccessLogger implements HttpExchangeRepository {
	private final Predicate<HttpExchange> filter;

	private final BiConsumer<StringBuilder, HttpExchange> logCustomizer;

	private final Logger log = LoggerFactory.getLogger("accesslog");

	public AccessLogger(Predicate<HttpExchange> filter, BiConsumer<StringBuilder, HttpExchange> logCustomizer) {
		this.filter = filter;
		this.logCustomizer = logCustomizer;
	}

	public AccessLogger(Predicate<HttpExchange> filter) {
		this(filter, (builder, httpExchange) -> {
		});
	}

	public AccessLogger() {
		this(x -> true);
	}

	@Override
	public List<HttpExchange> findAll() {
		return List.of();
	}

	@Override
	public void add(HttpExchange httpExchange) {
		if (!log.isInfoEnabled()) {
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
		if (remoteAddress != null) {
			log.append("remote=").append(remoteAddress).append(" ");
		}
		if (principal != null) {
			log.append("user=\"").append(principal.getName()).append("\" ");
		}
		log.append("ts=\"").append(httpExchange.getTimestamp()).append("\" ");
		log.append("method=").append(request.getMethod()).append(" ");
		log.append("url=").append(uri).append(" ");
		log.append("status=").append(response.getStatus()).append(" ");
		final List<String> referer = headers.get("referer");
		if (!CollectionUtils.isEmpty(referer)) {
			log.append("referer=\"").append(referer.get(0)).append("\" ");
		}
		final List<String> userAgent = headers.get("user-agent");
		if (!CollectionUtils.isEmpty(userAgent)) {
			log.append("ua=\"").append(userAgent.get(0)).append("\" ");
		}
		if (timeTaken != null) {
			log.append("response_time=").append(timeTaken.toMillis()).append(" ");
		}
		this.logCustomizer.accept(log, httpExchange);
		this.log.info(log.toString().trim());
	}
}
