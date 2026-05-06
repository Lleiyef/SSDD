package es.um.sisdist.backend.Service.metrics;

import io.prometheus.client.servlet.jakarta.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/metrics")
public class PrometheusMetricsServlet extends MetricsServlet {

    private static final long serialVersionUID = 1L;

    static {
        DefaultExports.initialize();
    }
}
