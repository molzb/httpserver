package com.ing.httpserver;

import static java.net.HttpURLConnection.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Simple file server, which delivers a file from the classpath<br>
 * Ex. localhost:8082/sample.pdf delivers file /src/main/resources/sample.pdf
 * 
 * @author Bernhard
 */
public class HttpServerFile {
  
  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);
    HttpContext context = server.createContext("/");
    context.setHandler(HttpServerFile::handleRequest);
    server.start();
  }
  
  /** Serve files which are on the classpath */
  private static void handleRequest(HttpExchange t) throws IOException {
    String path = t.getRequestURI().getPath(); // e.g. /db.html
    try (InputStream is = HttpServerFile.class.getResourceAsStream(path); OutputStream os = t.getResponseBody()) {
      byte[] content;
      if (is != null) { // File found
        content = is.readAllBytes();
        t.sendResponseHeaders(HTTP_OK, content.length);
      } else { // File not found
        content = new String("404 - " + path).getBytes();
        t.sendResponseHeaders(HTTP_NOT_FOUND, content.length);
      }
      os.write(content);
    }
  }
}
