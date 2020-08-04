//jdeps -s httpserver-0.0.1-SNAPSHOT.jar
//jdeps -s --module-path com.ing.httpserver ..\ojdbc8-12.2.0.1.jar
//OJDBC8 is an 'Automatic module', it doesn't contain a Jigsaw module descriptor
module com.ing.httpserver {
  requires java.base;
  requires java.sql;
  requires jdk.httpserver;
  requires transitive oracle.jdbc; // name of Dependency without version number
  
  exports com.ing.httpserver;
}