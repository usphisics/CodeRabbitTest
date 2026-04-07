package com.ortb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Infrastructure beans for the DSP client:
 * - {@link RestTemplate} with conservative connect/read timeouts.
 * - A virtual-thread {@link Executor} for non-blocking parallel DSP calls
 *   (leverages Project Loom, enabled globally in application.yml).
 *
 * <p>Per-DSP deadlines are enforced separately in {@code DspClientService}
 * via {@code CompletableFuture.get(timeoutMs)}. The timeouts here are an
 * upper-bound safety net at the HTTP layer.
 */
@Configuration
public class DspClientConfig {

    @Bean
    public RestTemplate dspRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(500);  // 500 ms connect timeout
        factory.setReadTimeout(500);     // 500 ms read timeout
        return new RestTemplate(factory);
    }

    /**
     * Virtual-thread executor for firing DSP bid requests in parallel.
     * Each task runs on a lightweight virtual thread (Java 21 Project Loom).
     */
    @Bean(name = "dspExecutor")
    public Executor dspExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
