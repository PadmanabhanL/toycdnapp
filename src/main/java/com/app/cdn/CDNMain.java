package com.app.cdn;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CDNMain {

    public static final int SERVER_PORT = 8001;
    public static final String TARGET_URL = "https://images.unsplash.com";
    private static final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
        httpServer.createContext("/", exchange -> {
            handleRequest(exchange);
        });

        httpServer.start();
        System.out.println("Server started on port " + SERVER_PORT);
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        String requestUrl = exchange.getRequestURI().toString();

        byte[] cachedResponse = cache.get(requestUrl);
        if (cachedResponse != null) {
            System.out.println("Serving from cache: " + requestUrl);
            sendResponse(exchange, cachedResponse);
            return;
        }

        String targetUrl = TARGET_URL + requestUrl;

        HttpClient httpClient = HttpClient.newBuilder()
                                          .version(HttpClient.Version.HTTP_1_1)
                                          .connectTimeout(Duration.ofSeconds(10))
                                          .build();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                                             .uri(URI.create(targetUrl))
                                             .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            byte[] responseBody = readInputStream(response.body());
            cache.put(requestUrl, responseBody);


            sendResponse(exchange, responseBody);
        } catch (InterruptedException e) {
            exchange.sendResponseHeaders(500, 0); // Internal server error
        } finally {
            exchange.close();
        }
    }

    private static void sendResponse(HttpExchange exchange, byte[] responseBody) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
        exchange.sendResponseHeaders(200, responseBody.length);

        try (OutputStream responseStream = exchange.getResponseBody()) {
            responseStream.write(responseBody);
        }
    }

    private static byte[] readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}
