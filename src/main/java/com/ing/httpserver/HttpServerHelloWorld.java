package com.ing.httpserver;

import static java.net.HttpURLConnection.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * HttpServer, which always responds with "Hello World"<br>
 * Ex. localhost:8081/whatever -> "Hello World"
 */
public class HttpServerHelloWorld {
  
  /** Start server */
  public static void main(String[] args) throws IOException {
    // RFC 2616
    HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
    HttpContext context = server.createContext("/");
    context.setHandler(HttpServerHelloWorld::handleRequest);
    server.start();
  }
  
  /** Always respond with 'Hello World' */
  private static void handleRequest(HttpExchange t) throws IOException {
    byte[] helloWorld = "Hello World".getBytes();
    try (OutputStream os = t.getResponseBody()) {
      t.sendResponseHeaders(HTTP_OK, helloWorld.length);
      os.write(helloWorld);
    }
  }
}
