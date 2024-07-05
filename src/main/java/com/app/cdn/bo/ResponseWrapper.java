package com.app.cdn.bo;

import java.io.InputStream;
import java.net.http.HttpHeaders;

public class ResponseWrapper {

    HttpHeaders headers;

    InputStream body;

    int statusCode;

    public ResponseWrapper(HttpHeaders headers, InputStream body, int statusCode) {
        this.headers = headers;
        this.body = body;
        this.statusCode = statusCode;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public InputStream getBody() {
        return body;
    }

    public void setBody(InputStream body) {
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
