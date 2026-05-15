package com.sydneyevents.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * Thin wrapper around HttpClient that logs every outgoing request and
 * the matching response (status, size, optionally truncated body).
 */
public class LoggingHttpClient extends HttpClient {

    private static final Logger log = LoggerFactory.getLogger("com.sydneyevents.http");
    private static final int MAX_BODY_LOG = 2000;

    private final HttpClient delegate;

    public LoggingHttpClient(HttpClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        logRequest(request);
        long start = System.nanoTime();
        try {
            HttpResponse<T> resp = delegate.send(request, responseBodyHandler);
            logResponse(request, resp, System.nanoTime() - start);
            return resp;
        } catch (Exception e) {
            log.warn("→ {} {}  FAILED after {} ms: {}",
                    request.method(), request.uri(),
                    Duration.ofNanos(System.nanoTime() - start).toMillis(),
                    e.getMessage());
            throw e;
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        logRequest(request);
        long start = System.nanoTime();
        return delegate.sendAsync(request, responseBodyHandler)
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        log.warn("→ {} {}  FAILED after {} ms: {}",
                                request.method(), request.uri(),
                                Duration.ofNanos(System.nanoTime() - start).toMillis(),
                                err.getMessage());
                    } else {
                        logResponse(request, resp, System.nanoTime() - start);
                    }
                });
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                            HttpResponse.BodyHandler<T> responseBodyHandler,
                                                            HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return sendAsync(request, responseBodyHandler);
    }

    private void logRequest(HttpRequest req) {
        log.info("→ {} {}", req.method(), req.uri());
        if (log.isDebugEnabled()) {
            for (Map.Entry<String, List<String>> h : req.headers().map().entrySet()) {
                log.debug("    {}: {}", h.getKey(), String.join(", ", h.getValue()));
            }
        }
    }

    private void logResponse(HttpRequest req, HttpResponse<?> resp, long elapsedNanos) {
        long ms = Duration.ofNanos(elapsedNanos).toMillis();
        Object body = resp.body();
        String bodyPreview = previewOf(body);
        int size = bodyPreview == null ? -1 : bodyPreview.length();

        log.info("← {} {} → HTTP {} ({} bytes, {} ms)",
                req.method(), req.uri(), resp.statusCode(), size, ms);

        if (bodyPreview != null && log.isDebugEnabled()) {
            String snippet = bodyPreview.length() > MAX_BODY_LOG
                    ? bodyPreview.substring(0, MAX_BODY_LOG) + "…[truncated]"
                    : bodyPreview;
            log.debug("    body: {}", snippet.replaceAll("\\s+", " "));
        }
    }

    private String previewOf(Object body) {
        if (body == null) return null;
        if (body instanceof String s) return s;
        if (body instanceof byte[] bytes) return new String(bytes);
        return body.toString();
    }

    // ── delegate everything else ───────────────────────────────────────────
    @Override public Optional<CookieHandler> cookieHandler() { return delegate.cookieHandler(); }
    @Override public Optional<Duration> connectTimeout() { return delegate.connectTimeout(); }
    @Override public Redirect followRedirects() { return delegate.followRedirects(); }
    @Override public Optional<ProxySelector> proxy() { return delegate.proxy(); }
    @Override public SSLContext sslContext() { return delegate.sslContext(); }
    @Override public SSLParameters sslParameters() { return delegate.sslParameters(); }
    @Override public Optional<Authenticator> authenticator() { return delegate.authenticator(); }
    @Override public Version version() { return delegate.version(); }
    @Override public Optional<Executor> executor() { return delegate.executor(); }
    @Override public WebSocket.Builder newWebSocketBuilder() { return delegate.newWebSocketBuilder(); }
}
