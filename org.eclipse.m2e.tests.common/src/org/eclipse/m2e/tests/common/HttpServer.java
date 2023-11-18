/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.Constraint.Authorization;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.PathResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;


/**
 * A helper for the tests to start an embedded HTTP server powered by Jetty. Create an instance of this class, use its
 * mutators to configure the server and finally call {@link #start()}.
 *
 * @author Benjamin Bentmann
 */
public class HttpServer {

  private Server server;

  private int httpPort;

  private int httpsPort = -1;

  private String keyStoreLocation = "resources/ssl/keystore";

  private String keyStorePassword;

  private String trustStoreLocation;

  private String trustStorePassword;

  private boolean needClientAuth;

  private String proxyUsername;

  private String proxyPassword;

  private boolean redirectToHttps;

  private long latency;

  private final Map<String, String> userPasswords = new HashMap<>();

  private final Map<String, String[]> userRoles = new HashMap<>();

  private final Map<String, String[]> securedRealms = new HashMap<>();

  private final Map<String, File> resourceDirs = new TreeMap<>(Collections.reverseOrder());

  private final Map<String, String[]> resourceFilters = new HashMap<>();

  private final Map<String, String> filterTokens = new HashMap<>();

  private final Collection<String> recordedPatterns = new HashSet<>();

  private final List<String> recordedRequests = new ArrayList<>();

  private final Map<String, Map<String, String>> recordedHeaders = new HashMap<>();

  private String storePassword;

  protected Connector newHttpConnector() {
    HttpConfiguration config = new HttpConfiguration();
    config.setSecurePort(httpsPort);
    ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(config));
    connector.setPort(httpPort);
    return connector;
  }

  protected Connector newHttpsConnector() {
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setKeyManagerPassword(storePassword);
    sslContextFactory.setKeyStorePath(new File(keyStoreLocation).getAbsolutePath());
    sslContextFactory.setKeyStorePassword(keyStorePassword);
    if(trustStoreLocation != null && !"".equals(trustStoreLocation)) {
      sslContextFactory.setTrustStorePath(new File(trustStoreLocation).getAbsolutePath());
    }
    if(trustStorePassword != null && !"".equals(trustStoreLocation)) {
      sslContextFactory.setTrustStorePassword(trustStorePassword);
    }
    sslContextFactory.setNeedClientAuth(needClientAuth);

    ServerConnector connector = new ServerConnector(server,
        new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()));

    connector.setPort(httpsPort);
    return connector;
  }

  /**
   * Sets the port to use for HTTP connections.
   *
   * @param httpPort The port to use, may be {@code 0} to pick a random port (default), if negative the HTTP connector
   *          will be disabled.
   * @return This server, never {@code null}.
   */
  public HttpServer setHttpPort(int httpPort) {
    this.httpPort = httpPort;

    return this;
  }

  /**
   * Gets the port number of the server's HTTP connector.
   *
   * @return The port number of the server's HTTP connector.
   */
  public int getHttpPort() {
    if(httpPort >= 0 && server != null && server.isRunning()) {
      return ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
    }
    return httpPort;
  }

  /**
   * Gets the base URL to the server's HTTP connector, e.g. {@code "http://localhost:8080"}.
   *
   * @return The base URL without trailing slash to the server's HTTP connector, never {@code null}.
   */
  public String getHttpUrl() {
    return "http://localhost:" + getHttpPort();
  }

  /**
   * Sets the port to use for HTTPS connections.
   *
   * @param httpPort The port to use, may be {@code 0} to pick a random port, if negative the HTTPS connector will be
   *          disabled (default).
   * @return This server, never {@code null}.
   */
  public HttpServer setHttpsPort(int httpsPort) {
    this.httpsPort = httpsPort;

    return this;
  }

  /**
   * Gets the port number of the server's HTTPS connector.
   *
   * @return The port number of the server's HTTPS connector.
   */
  public int getHttpsPort() {
    if(httpsPort >= 0 && server != null && server.isRunning()) {
      return ((NetworkConnector) server.getConnectors()[(httpPort < 0) ? 0 : 1]).getLocalPort();
    }
    return httpsPort;
  }

  /**
   * Gets the base URL to the server's HTTPS connector, e.g. {@code "https://localhost:8080"}.
   *
   * @return The base URL without trailing slash to the server's HTTPS connector, never {@code null}.
   */
  public String getHttpsUrl() {
    return "https://localhost:" + getHttpsPort();
  }

  /**
   * Sets the keystore to use for the server certificate on the SSL connector. Also sets the storePassword value to be
   * password, if it has not been set previously.
   *
   * @param path The path to the keystore to use for the server certificate, may be {@code null}.
   * @param password The password for the keystore, may be {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer setKeyStore(String path, String password) {
    keyStoreLocation = path;
    keyStorePassword = password;
    if(storePassword == null) {
      storePassword = keyStorePassword;
    }
    return this;
  }

  /**
   * Sets the truststore to use for validating client credentials via the SSL connector.
   *
   * @param path The path to the truststore to use for the trusted client certificates, may be {@code null}.
   * @param password The password for the truststore, may be {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer setTrustStore(String path, String password) {
    trustStoreLocation = path;
    trustStorePassword = password;
    return this;
  }

  /**
   * Sets the password to use for the SSL connector store.
   *
   * @param password The password for the store, may be {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer setStorePassword(String password) {
    this.storePassword = password;
    return this;
  }

  /**
   * Enables/disables client-side certificate authentication.
   *
   * @param needClientAuth Whether the server should reject clients whose certificate can't be verified via the
   *          truststore.
   * @return This server, never {@code null}.
   */
  public HttpServer setNeedClientAuth(boolean needClientAuth) {
    this.needClientAuth = needClientAuth;
    return this;
  }

  /**
   * Sets the credentials to use for proxy authentication. If either username or password is {@code null}, no proxy
   * authentication is required.
   *
   * @param username The username, may be {@code null}.
   * @param password The password, may be {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer setProxyAuth(String username, String password) {
    this.proxyUsername = username;
    this.proxyPassword = password;

    return this;
  }

  protected Handler newProxyHandler() {
    return new Handler.Abstract() {
      @Override
      public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String auth = request.getHeaders().get(HttpHeader.PROXY_AUTHORIZATION);
        if(auth != null) {
          auth = auth.substring(auth.indexOf(' ') + 1).trim();
          auth = new String(Base64.getDecoder().decode(auth));
        }
        return false;
      }
    };
  }

  /**
   * Enforces redirection from HTTP to HTTPS.
   *
   * @param redirectToHttps {@code true} to redirect any HTTP requests to HTTPS, {@code false} to handle HTTP normally.
   * @return This server, never {@code null}.
   */
  public HttpServer setRedirectToHttps(boolean redirectToHttps) {
    this.redirectToHttps = redirectToHttps;

    return this;
  }

  protected Handler newSslRedirectHandler() {
    return new Handler.Abstract() {
      @Override
      public boolean handle(Request request, Response response, Callback callback) throws Exception {
        int httpsPort = getHttpsPort();
        if(Request.getServerPort(request) != httpsPort) {
          String url = "https://" + Request.getServerName(request) + ":" + httpsPort + request.getHttpURI().getPath();
          response.setStatus(HttpStatus.MOVED_PERMANENTLY_301);
          response.getHeaders().put(HttpHeader.LOCATION, url);
          callback.succeeded();
          return true;
        }
        return false;
      }
    };
  }

  /**
   * Registers a user.
   *
   * @param username The username, must not be {@code null}.
   * @param password The password, must not be {@code null}.
   * @param roles The roles of the user, may be empty or {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer addUser(String username, String password, String... roles) {
    userPasswords.put(username, password);
    userRoles.put(username, (roles == null) ? new String[0] : roles);

    return this;
  }

  /**
   * Sets up a security realm.
   *
   * @param pathSpec The path to secure, e.g. {@code "/files/*"}, must not be {@code null}.
   * @param roles The roles that have access to the realm, may be empty or {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer addSecuredRealm(String pathSpec, String... roles) {
    securedRealms.put(pathSpec, (roles == null) ? new String[0] : roles);

    return this;
  }

  protected SecurityHandler newSecurityHandler() throws IOException {
    SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();

    for(String pathSpec : securedRealms.keySet()) {
      String[] roles = securedRealms.get(pathSpec);
      Constraint constraint = Constraint.from(Authenticator.BASIC_AUTH, Authorization.SPECIFIC_ROLE, roles);
      securityHandler.put(pathSpec, constraint);
    }

    Properties p = new Properties();
    for(String username : userPasswords.keySet()) {
      String password = userPasswords.get(username);
      String[] roles = userRoles.get(username);

      StringBuilder entry = new StringBuilder(password);
      for(String role : roles) {
        entry.append(",");
        entry.append(role);
      }
      p.put(username, entry.toString());
    }

    File propFile = new File("target/users.properties");
    try (FileOutputStream in = new FileOutputStream(propFile)) {
      p.store(in, null);
    }

    HashLoginService userRealm = new HashLoginService("TestRealm",
        new PathResourceFactory().newResource("target/users.properties"));
    server.addBean(userRealm);

    securityHandler.setAuthenticator(new BasicAuthenticator());
    securityHandler.setLoginService(userRealm);

    return securityHandler;
  }

  /**
   * Adds resources to the server. Resources can be filtered upon serving using the tokens set via
   * {@link #setFilterToken(String, String)}. The directory mounted into the server via this method will also be used to
   * store files sent via PUT. Upon requests, the server will try to match the context roots in reverse alphabetical
   * order, thereby giving longer path prefix matches precedence.
   *
   * @param contextRoot The context root to make the resources accessible at, must not be {@code null}.
   * @param baseDirectory The local base directory whose files should be served, must not be {@code null}.
   * @param filteredExtensions A list of extensions for files to filter, e.g. {@code "xml, "properties"}, may be
   *          {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer addResources(String contextRoot, String baseDirectory, String... filteredExtensions) {
    contextRoot = normalizeContextRoot(contextRoot);

    File basedir = new File(baseDirectory).getAbsoluteFile();

    resourceDirs.put(contextRoot, basedir);
    resourceFilters.put(contextRoot, (filteredExtensions == null) ? new String[0] : filteredExtensions);

    return this;
  }

  /**
   * Enables request recording for the specified URI patterns. Recorded requests can be retrieved via
   * {@link #getRecordedRequests()}.
   *
   * @param patterns The regular expressions denoting URIs to monitor, e.g. {@code "/context/.*"}, must not be
   *          {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer enableRecording(String... patterns) {
    Collections.addAll(recordedPatterns, patterns);

    return this;
  }

  /**
   * Gets the sequence of requests that have been issued against context roots for which
   * {@link #enableRecording(String...)} was called. A request is encoded in the form {@code <METHOD> <URI>}, e.g.
   * {@code GET /context/some.jar}.
   *
   * @return The sequence of requests since the server was started, can be empty but never {@code null}.
   */
  public List<String> getRecordedRequests() {
    return recordedRequests;
  }

  /**
   * Gets the headers sent in the most recent request to the specified path.
   *
   * @param uri the path
   * @return the http request headers
   */
  public Map<String, String> getRecordedHeaders(String uri) {
    return recordedHeaders.get(uri);
  }

  /**
   * Sets a token to replace during resource filtering. Upon server start, the following tokens will be defined
   * automatically: <code>@basedir@</code>, <code>@baseurl@</code>, <code>@baseuri@</code>, <code>@port.http@</code> and
   * <code>@port.https@</code>.
   *
   * @param token The token to replace, e.g. <code>@basedir@</code>, must not be {@code null}.
   * @param value The replacement text of the token, may be {@code null}.
   * @return This server, never {@code null}.
   */
  public HttpServer setFilterToken(String token, String value) {
    if(value == null) {
      filterTokens.remove(token);
    } else {
      filterTokens.put(token, value);
    }

    return this;
  }

  protected Handler newResourceHandler() {
    return new ResHandler();
  }

  /**
   * Sets the latency of the server.
   *
   * @param millis The latency in milliseconds, maybe negative for infinite delay.
   * @return This server, never {@code null}.
   */
  public HttpServer setLatency(long millis) {
    this.latency = millis;
    return this;
  }

  protected Handler newSleepHandler(final long millis) {
    return new Handler.Abstract() {
      @Override
      public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if(millis >= 0) {
          try {
            Thread.sleep(millis);
          } catch(InterruptedException e) {
            e.printStackTrace();
          }
        } else {
          synchronized(this) {
            try {
              wait();
            } catch(InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
        return false;
      }
    };
  }

  /**
   * Starts the server. Trying to start an already running server has no effect.
   *
   * @throws Exception If the server could not be started.
   * @return This server, never {@code null}.
   */
  public HttpServer start() throws Exception {
    if(server != null) {
      return this;
    }

    recordedRequests.clear();

    server = new Server();

    List<Connector> connectors = new ArrayList<>();
    if(httpPort >= 0) {
      connectors.add(newHttpConnector());
    }
    if(httpsPort >= 0 && keyStoreLocation != null) {
      connectors.add(newHttpsConnector());
    }

    Handler.Sequence handlerList = new Handler.Sequence();
    if(!recordedPatterns.isEmpty()) {
      handlerList.addHandler(new RecordingHandler());
    }
    if(latency != 0) {
      handlerList.addHandler(newSleepHandler(latency));
    }
    if(redirectToHttps) {
      handlerList.addHandler(newSslRedirectHandler());
    }
    if(proxyUsername != null && proxyPassword != null) {
      handlerList.addHandler(newProxyHandler());
    }
    SecurityHandler security = null;
    if(!securedRealms.isEmpty()) {
      security = newSecurityHandler();
      handlerList.addHandler(security);
    }
    if(!resourceDirs.isEmpty()) {
      if(security != null) {
        security.setHandler(newResourceHandler());
      } else {
        handlerList.addHandler(newResourceHandler());
      }
    }
    handlerList.addHandler(new DefaultHandler());

    server.setHandler(handlerList);
    server.setConnectors(connectors.toArray(new Connector[connectors.size()]));
    server.start();

    waitForConnectors();

    addDefaultFilterTokens();

    return this;
  }

  protected void waitForConnectors() throws Exception {
    // for unknown reasons, the connectors occasionally don't start properly, this tries hard to ensure they are up

    List<Connector> badConnectors = new ArrayList<>(2);

    for(int r = 10; r > 0; r-- ) {
      // wait some seconds for the connectors to come up
      for(int i = 200; i > 0; i-- ) {
        badConnectors.clear();
        for(Connector connector : server.getConnectors()) {
          if(((NetworkConnector) connector).getLocalPort() < 0) {
            badConnectors.add(connector);
          }
        }
        if(badConnectors.isEmpty()) {
          return;
        }
        try {
          Thread.sleep(15);
        } catch(InterruptedException e) {
          return;
        }
      }

      // restart the broken connectors and hope they make it this time
      System.err.println("WARNING: " + badConnectors + " did not start properly, restarting");
      for(Connector connector : badConnectors) {
        connector.stop();
        connector.start();
      }
    }
  }

  protected void addDefaultFilterTokens() {
    if(!filterTokens.containsKey("@basedir@")) {
      filterTokens.put("@basedir@", new File("").getAbsolutePath());
    }
    if(!filterTokens.containsKey("@baseurl@")) {
      String baseurl = "file://" + new File("").toURI().getPath();
      if(baseurl.endsWith("/")) {
        baseurl = baseurl.substring(0, baseurl.length() - 1);
      }
      filterTokens.put("@baseurl@", baseurl);
    }
    if(!filterTokens.containsKey("@baseuri@")) {
      String baseuri = "file://" + new File("").toURI().getRawPath();
      if(baseuri.endsWith("/")) {
        baseuri = baseuri.substring(0, baseuri.length() - 1);
      }
      filterTokens.put("@baseuri@", baseuri);
    }
    if(!filterTokens.containsKey("@port.http@")) {
      filterTokens.put("@port.http@", Integer.toString(getHttpPort()));
    }
    if(!filterTokens.containsKey("@port.https@")) {
      filterTokens.put("@port.https@", Integer.toString(getHttpsPort()));
    }
  }

  /**
   * Stops the server. Stopping an already stopped server has no effect.
   */
  public void stop() {
    if(server != null) {
      try {
        server.stop();
      } catch(Exception e) {
        e.printStackTrace();
      }
      server = null;
    }
  }

  private class ResHandler extends Handler.Abstract {
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
      String uri = request.getHttpURI().getPath();

      for(String contextRoot : resourceDirs.keySet()) {
        String path = URIUtil.decodePath(trimContextRoot(uri, contextRoot));
        if(path != null) {
          File basedir = resourceDirs.get(contextRoot);
          File file = new File(basedir, path);

          HttpMethod requestMethod = HttpMethod.fromString(request.getMethod());
          if(HttpMethod.HEAD == requestMethod) {
            if(file.exists()) {
              response.setStatus(HttpStatus.OK_200);
            } else {
              response.setStatus(HttpStatus.NOT_FOUND_404);
            }
            callback.succeeded();
            return true;
          } else if(HttpMethod.PUT == requestMethod || HttpMethod.POST == requestMethod) {
            file.getParentFile().mkdirs();

            try (var input = Content.Source.asInputStream(request)) {
              Files.copy(input, file.toPath());
            }

            response.setStatus(HttpStatus.CREATED_201);
            callback.succeeded();
            return true;
          } else if(file.isFile()) {

            String filterEncoding = getFilterEncoding(path, resourceFilters.get(contextRoot));
            byte[] bytes;
            if(filterEncoding == null) {
              bytes = Files.readAllBytes(file.toPath());
            } else {
              String text = Files.readString(file.toPath(), Charset.forName(filterEncoding));
              text = filter(text, filterTokens);
              bytes = text.getBytes(filterEncoding);
            }
            response.setStatus(HttpStatus.OK_200);
            response.write(true, ByteBuffer.wrap(bytes), callback);
            return true;
          }
          return false;
        }
      }
      return false;
    }

    private String getExtension(String path) {
      return path.substring(path.lastIndexOf('.') + 1);
    }

    private String getFilterEncoding(String path, String[] filteredExtensions) {
      String ext = getExtension(path);
      if(filteredExtensions != null) {
        for(String filteredExtension : filteredExtensions) {
          if(filteredExtension.startsWith(".")) {
            filteredExtension = filteredExtension.substring(1);
          }
          if(filteredExtension.equalsIgnoreCase(ext)) {
            return "properties".equalsIgnoreCase(ext) ? "ISO-8859-1" : "UTF-8";
          }
        }
      }
      return null;
    }

    private String filter(String str, Map<String, String> tokens) {
      for(String token : tokens.keySet()) {
        str = str.replace(token, tokens.get(token));
      }
      return str;
    }

  }

  private class RecordingHandler extends Handler.Abstract {
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
      String uri = request.getHttpURI().getPath();

      for(String pattern : recordedPatterns) {
        if(uri.matches(pattern)) {
          String req = request.getMethod() + " " + uri;
          recordedRequests.add(req);

          Map<String, String> headers = new HashMap<>();
          recordedHeaders.put(uri, headers);
          for(HttpField field : request.getHeaders()) {
            headers.put(field.getName(), field.getValue());
          }
        }
      }
      return false;
    }
  }

  private static String normalizeContextRoot(String contextRoot) {
    if(contextRoot.endsWith("/")) {
      contextRoot = contextRoot.substring(0, contextRoot.length() - 1);
    }
    if(!contextRoot.startsWith("/")) {
      contextRoot = "/" + contextRoot;
    }
    return contextRoot;
  }

  private static String trimContextRoot(String uri, String contextRoot) {
    if(uri.startsWith(contextRoot)) {
      if(contextRoot.length() == 1) {
        return uri.substring(1);
      } else if(uri.length() > contextRoot.length() && uri.charAt(contextRoot.length()) == '/') {
        return uri.substring(contextRoot.length() + 1);
      }
    }
    return null;
  }

  public void resetRecording() {
    recordedHeaders.clear();
    recordedRequests.clear();
  }
}
