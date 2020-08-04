package com.ing.httpserver;

import static java.lang.String.*;
import static java.net.HttpURLConnection.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import oracle.jdbc.driver.OracleConnection;

/**
 * HttpServer for a simple web-based SQL frontend<br>
 * Start with: java HttpServerDb [jdbcUrl|jdbcUser|jdbcPw], e.g. HttpServerDb jdbc:oracle:thin:@host myUser myPw<br>
 * Frontend: localhost:8084/db/db.html <br>
 * DB Endpoint: localhost:8084/db/select.json (+POST: e.g. {SELECT * FROM tbl})
 * 
 * @author Bernhard
 */
public class HttpServerDb {
  protected static String jdbcUrl, jdbcUser, jdbcPw;
  
  public static void main(String[] args) throws IOException {
    jdbcUrl = args[0];
    jdbcUser = args[1];
    jdbcPw = args[2];
    HttpServer server = HttpServer.create(new InetSocketAddress(8084), 0);
    server.createContext("/db", new DatabaseHttpHandler());
    server.start();
  }
}

class DatabaseHttpHandler implements HttpHandler {
  private Map<String, String> contentTypes = //
      Map.of("html", "text/html; charset=utf-8", "js", "application/javascript; charset=utf-8", "json",
             "application/json; charset=utf-8", "css", "text/css", "png", "image/png");
  private Connection dbConn;
  
  @Override
  public void handle(HttpExchange t) throws IOException {
    String path = t.getRequestURI().getPath();
    if (path.equals("/db/select.json"))
      selectFromDb(t);
    else
      serveFile(t);
  }
  
  private void serveFile(HttpExchange t) throws IOException {
    String path = t.getRequestURI().getPath(); // e.g. /db/select.json...
    String file = path.substring(3); // remove '/db' from URL
    try (InputStream is = getClass().getResourceAsStream(file)) {
      if (is == null) {
        fileNotFound(t);
      } else {
        response(t, HTTP_OK, is.readAllBytes());
      }
    }
  }
  
  private void fileNotFound(HttpExchange t) throws IOException {
    response(t, HTTP_NOT_FOUND, "File not found: " + t.getRequestURI());
  }
  
  private void selectFromDb(HttpExchange t) throws IOException {
    try (InputStream is = t.getRequestBody()) {
      String sql = new String(is.readAllBytes());
      if (sql.toLowerCase().startsWith("select")) {
        List<Map<String, String>> rows = getRowsFromDatabase(sql);
        response(t, HTTP_OK, toJson(rows));
        getDatabaseConnection(); // prepare Connection for the next time
      } else {
        response(t, HTTP_BAD_REQUEST, "Only SELECT is allowed");
      }
    } catch (SQLException e) {
      response(t, HTTP_BAD_REQUEST, e.getMessage());
    }
  }
  
  private List<Map<String, String>> getRowsFromDatabase(String sql) throws SQLException {
    List<Map<String, String>> rows = new ArrayList<>();
    try (Connection conn = getDatabaseConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
      ResultSetMetaData metaData = rs.getMetaData(); // column names from SELECT
      while (rs.next()) { // rows from SELECT
        Map<String, String> row = new HashMap<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
          row.put(metaData.getColumnName(i), rs.getString(i));
        }
        rows.add(row);
      }
    } catch (SQLException sqle) {
      throw sqle;
    }
    
    return rows;
  }
  
  private Connection getDatabaseConnection() throws SQLException {
    if (dbConn != null && !dbConn.isClosed()) { // get prepared connection
      return dbConn;
    }
    dbConn = DriverManager.getConnection(HttpServerDb.jdbcUrl, HttpServerDb.jdbcUser, HttpServerDb.jdbcPw);
    ((OracleConnection) dbConn).setDefaultRowPrefetch(100);
    return dbConn;
  }
  
  /**
   * Converts List<Map...> (e.g. rows from the database) to JSON
   * 
   * @param rows
   *          e.g. List|Map| {"col1","Val1", "col2","Val2"},{"col1","Val99","col2","Val100"}
   * @return JSON string, e.g. [{"col1":"Val1", "col2":"Val2"},{"col1":"Val99","col2":"Val100"}]
   */
  private String toJson(List<Map<String, String>> rows) {
    String jsonField = "\"%s\":\"%s\"";
    List<String> jsonRows = new ArrayList<>();
    for (Map<String, String> row : rows) {
      Set<String> keySet = row.keySet();
      List<String> jsonRow = new ArrayList<>();
      keySet.forEach(key -> jsonRow.add(format(jsonField, key, row.get(key)))); // "col1":"Val1"
      jsonRows.add("{" + String.join(",", jsonRow) + "}"); // {"col1":"Val1", "col2":"Val2"}
    }
    
    return "[" + String.join(",\n", jsonRows) + "]"; // [{"col1":"Val1", ...},{...},{...}]
  }
  
  private void response(HttpExchange t, int statusCode, String content) throws IOException {
    response(t, statusCode, content.getBytes());
  }
  
  private void response(HttpExchange t, int statusCode, byte[] content) throws IOException {
    String path = t.getRequestURI().getPath();
    String suffix = path.substring(path.lastIndexOf('.') + 1);
    t.getResponseHeaders().add("Content-Type", getContentTypeFor(suffix));
    
    t.sendResponseHeaders(statusCode, content.length);
    try (OutputStream os = t.getResponseBody()) {
      os.write(content);
    }
  }
  
  private String getContentTypeFor(String suffix) {
    String contentType = contentTypes.get(suffix);
    return contentType == null ? "text/plain; charset=utf-8" : contentType;
  }
}
