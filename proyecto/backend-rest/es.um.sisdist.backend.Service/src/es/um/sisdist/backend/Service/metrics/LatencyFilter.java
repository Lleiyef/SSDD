package es.um.sisdist.backend.Service.metrics;

import io.prometheus.client.Histogram;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LatencyFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Histogram requestLatency = Histogram.build()
        .name("http_server_requests_seconds")
        .help("Latencia de peticiones HTTP al backend REST")
        .labelNames("uri", "status")
        .register();

    private static final String START_TIME_KEY = "LatencyFilter.startTime";

    @Override
    public void filter(ContainerRequestContext req) {
        req.setProperty(START_TIME_KEY, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext resp) {
        Long start = (Long) req.getProperty(START_TIME_KEY);
        if (start == null) return;
        double elapsedSeconds = (System.nanoTime() - start) / 1e9;
        String path = req.getUriInfo().getPath();
        String uri = path.startsWith("/") ? "/jaxrs" + path : "/jaxrs/" + path;
        requestLatency.labels(uri, String.valueOf(resp.getStatus())).observe(elapsedSeconds);
    }
}
