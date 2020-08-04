package com.ing.httpserver;

import static java.net.HttpURLConnection.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Simple REST-like server, based on HttpServer <br>
 * Ex. localhost:8083/rest shows a Welcome page.<br>
 * Ex. localhost:8083/rest/post echoes the POSTed request body. <br>
 * Ex. localhost:8083/rest/hello?Who responds 'Hello Who'. <br>
 * Ex. localhost:8083/rest/sthElse responds 'Unknown Request' (HTTP status 500)
 * 
 * @author Bernhard
 */
public class HttpServerRest {
  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
    server.createContext("/rest", new RestHandler());
    server.start();
  }
}

class RestHandler implements HttpHandler {
  @Override
  public void handle(HttpExchange t) throws IOException {
    String path = t.getRequestURI().getPath();
    String queryParams = t.getRequestURI().getQuery();
    
    switch (path) {
      case "/rest":
        welcome(t);
        break;
      case "/rest/post":
        post(t);
        break;
      case "/rest/hello":
        hello(t, queryParams);
        break;
      default:
        unknown(t);
    }
  }
  
  private void welcome(HttpExchange t) throws IOException {
    response(t, HTTP_OK, "Welcome! I'm up and running");
  }
  
  /**
   * POST request. Test with JQuery: $.post("post", "Whatever") or $.post("post", { name: "HttpServer", language: "Java" } )
   */
  private void post(HttpExchange t) throws IOException {
    if (t.getRequestMethod().equals("POST")) {
      try (InputStream is = t.getRequestBody()) {
        String requestBody = new String(is.readAllBytes());
        response(t, HTTP_OK, "I have POSTed this: " + requestBody);
      }
    } else {
      response(t, HTTP_BAD_METHOD, "Only POST requests allowed");
    }
  }
  
  private void hello(HttpExchange t, String queryParam) throws IOException {
    response(t, HTTP_OK, "Hello " + queryParam);
  }
  
  private void unknown(HttpExchange t) throws IOException {
    response(t, HTTP_INTERNAL_ERROR, "Unknown Request");
  }
  
  private void response(HttpExchange t, int statusCode, String content) throws IOException {
    try (OutputStream os = t.getResponseBody()) {
      t.getResponseHeaders().add("Content-Type", "text/plain");
      t.sendResponseHeaders(statusCode, content.getBytes().length);
      os.write(content.getBytes());
    }
  }
}
