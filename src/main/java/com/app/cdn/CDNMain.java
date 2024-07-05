package com.app.cdn;

import com.app.cdn.bo.ResponseWrapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CDNMain {

    public static final int SERVER_PORT = 8001;
    public static final String TARGET_URL = "https://images.unsplash.com";
    public static Map<String, ResponseWrapper> cache = new ConcurrentHashMap<>();

    public static ExecutorService executorService = Executors.newFixedThreadPool(100);

    public static void main(String[] args) throws IOException {

        CDNMain app = new CDNMain();

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
        httpServer.createContext("/", exchange -> {
            executorService.submit(() -> {
                try {
                    app.handleRequest(exchange);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
        httpServer.setExecutor(executorService);

        httpServer.start();
        System.out.println("Server started on port " + SERVER_PORT);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String requestUrl = exchange.getRequestURI().toString();
        String targetUrl = TARGET_URL + requestUrl;


        String fileName = getFileNameFromUrl(requestUrl);

        if (cache.containsKey(fileName)) {

            System.out.println("serving from cache for "+ fileName);

            redirectResponse(exchange, cache.get(fileName).getStatusCode(), cache.get(fileName).getBody());
        } else {

            HttpClient httpClient = HttpClient.newBuilder()
                                              .version(HttpClient.Version.HTTP_1_1)
                                              .connectTimeout(Duration.ofSeconds(10))
                                              .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                                                 .uri(URI.create(targetUrl))
                                                 .build();

            try {
                HttpResponse<InputStream> response = httpClient.send(httpRequest,
                                                                     HttpResponse.BodyHandlers.ofInputStream());

                InputStream body = response.body();

                // Set response headers and status code
                HttpHeaders headers = response.headers();
                exchange.getResponseHeaders().putAll(headers.map());
                int statusCode = response.statusCode();

                cache.put(fileName, new ResponseWrapper(headers, body, statusCode));

                redirectResponse(exchange, statusCode, body);
            } catch (InterruptedException e) {
                exchange.sendResponseHeaders(500, 0); // Internal server error
            } finally {
                exchange.close();
            }
        }

        // Send the request and handle the response

    }

    private static void redirectResponse(HttpExchange exchange, int statusCode, InputStream responseBody) throws
                                                                                                          IOException {
        exchange.sendResponseHeaders(statusCode, 0);

        // Write response body to client
        try (OutputStream respStream = exchange.getResponseBody()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = responseBody.read(buffer)) != -1) {
                respStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private static String getFileNameFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1); // Extract filename from URL
    }
}
