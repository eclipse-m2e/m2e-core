/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.aether.okhttp;

import io.tesla.aether.client.AetherClient;
import io.tesla.aether.client.AetherClientConfig;
import io.tesla.aether.client.AetherClientProxy;
import io.tesla.aether.client.Response;
import io.tesla.aether.okhttp.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;

import com.squareup.okhttp.OkAuthenticator;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.internal.tls.OkHostnameVerifier;

public class OkHttpAetherClient implements AetherClient {

  private boolean useCache = false;
  private Map<String, String> headers;
  private AetherClientConfig config;
  private OkHttpClient httpClient;
  private SSLSocketFactory sslSocketFactory;

  public OkHttpAetherClient(AetherClientConfig config) {
    this.config = config;

    headers = config.getHeaders();
    
    //
    // If the User-Agent has been overriden in the headers then we will use that
    //
    if (headers != null && !headers.containsKey("User-Agent")) {
      headers.put("User-Agent", config.getUserAgent());
    }
    
    //if (!useCache) {
    //  headers.put("Pragma", "no-cache");
    //}
    
    httpClient = new OkHttpClient();
    httpClient.setProxy(getProxy(config.getProxy()));
    httpClient.setHostnameVerifier(OkHostnameVerifier.INSTANCE);

    if (config.getAuthentication() != null) {
      AetherAuthenticator authenticator = new AetherAuthenticator();
      authenticator.setUsername(config.getAuthentication().getUsername());
      authenticator.setPassword(config.getAuthentication().getPassword());
      httpClient.setAuthenticator(authenticator);
    }

    if (config.getProxy() != null) {
      if (config.getProxy().getAuthentication() != null) {
        AetherAuthenticator authenticator = (AetherAuthenticator) httpClient.getAuthenticator();
        if (authenticator == null) {
          authenticator = new AetherAuthenticator();
        }
        authenticator.setProxyUsername(config.getProxy().getAuthentication().getUsername());
        authenticator.setProxyPassword(config.getProxy().getAuthentication().getPassword());
        httpClient.setAuthenticator(authenticator);
      }
    }
  }

  @Override
  public Response head(String uri) throws IOException {
    HttpURLConnection ohc = httpClient.open(new URL(uri));
    ohc.setRequestMethod("HEAD");
    return new ResponseAdapter(ohc);
  }

  @Override
  public Response get(String uri) throws IOException {
    HttpURLConnection ohc = getConnection(uri, null);
    ohc.setRequestMethod("GET");
    return new ResponseAdapter(ohc);
  }

  @Override
  public Response get(String uri, Map<String, String> requestHeaders) throws IOException {
    HttpURLConnection ohc = getConnection(uri, requestHeaders);
    ohc.setRequestMethod("GET");
    return new ResponseAdapter(ohc);
  }

  @Override
  // i need the response
  public Response put(String uri) throws IOException {
    HttpURLConnection ohc = getConnection(uri, null);
    ohc.setUseCaches(false);
    ohc.setRequestProperty("Content-Type", "application/octet-stream");
    ohc.setRequestMethod("PUT");
    //
    // This is in chunked mode by default and setting this parameter makes the underlying outputstream not a 
    // RetryableOutputStream and fails when authentication is required. Need to make sure this is still efficient
    // where large files don't blow memory.
    //
    //ohc.setFixedLengthStreamingMode((int) file.length());
    ohc.setDoOutput(true);
    return new ResponseAdapter(ohc);
  }

  public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
    this.sslSocketFactory = sslSocketFactory;
  }

  private java.net.Proxy getProxy(AetherClientProxy proxy) {
    java.net.Proxy ohp;
    if (proxy == null) {
      ohp = java.net.Proxy.NO_PROXY;
    } else {
      SocketAddress addr = new InetSocketAddress(proxy.getHost(), proxy.getPort());
      ohp = new java.net.Proxy(java.net.Proxy.Type.HTTP, addr);
    }
    return ohp;
  }

  private void checkForSslSystemProperties() {

    if (sslSocketFactory == null) {
      String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
      String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
      String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType");
      String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
      String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
      String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType");

      SslContextFactory scf = new SslContextFactory();
      if (keyStorePath != null && keyStorePassword != null) {
        scf.setKeyStorePath(keyStorePath);
        scf.setKeyStorePassword(keyStorePassword);
        scf.setKeyStoreType(keyStoreType);
        if (trustStorePath != null && trustStorePassword != null) {
          scf.setTrustStore(trustStorePath);
          scf.setTrustStorePassword(trustStorePassword);
          scf.setTrustStoreType(trustStoreType);
        }
        try {
          sslSocketFactory = scf.getSslContext().getSocketFactory();
        } catch (Exception e) {
          // do nothing
        }
      }
    }
  }

  private HttpURLConnection getConnection(String uri, Map<String, String> requestHeaders) throws IOException {

    checkForSslSystemProperties();

    if (sslSocketFactory != null) {
      httpClient.setSslSocketFactory(sslSocketFactory);
    }

    HttpURLConnection ohc = httpClient.open(new URL(uri));

    // Headers
    if (headers != null) {
      for (String headerName : headers.keySet()) {
        ohc.addRequestProperty(headerName, headers.get(headerName));
      }
    }

    if (requestHeaders != null) {
      for (String headerName : requestHeaders.keySet()) {
        ohc.addRequestProperty(headerName, requestHeaders.get(headerName));
      }
    }

    // Timeouts
    ohc.setConnectTimeout(config.getConnectionTimeout());
    ohc.setReadTimeout(config.getRequestTimeout());

    return ohc;
  }

  class ResponseAdapter implements Response {

    HttpURLConnection conn;

    ResponseAdapter(HttpURLConnection conn) {
      this.conn = conn;
    }

    @Override
    public int getStatusCode() throws IOException {
      return conn.getResponseCode();
    }

    @Override
    public String getStatusMessage() throws IOException {
      return conn.getResponseMessage();
    }

    @Override
    public String getHeader(String name) {
      return conn.getHeaderField(name);
    }

    @Override
    public Map<String, List<String>> getHeaders() {
      return conn.getHeaderFields();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return conn.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return conn.getOutputStream();
    }
  }

  @Override
  public void close() {}

  static class AuthenticateRequestKey {
    private final String host;
    private final int port;
    private final Set<OkAuthenticator.Challenge> challenges;

    AuthenticateRequestKey(URL url, List<OkAuthenticator.Challenge> challenges) {
      this.host = url.getHost();
      this.port = url.getPort();
      this.challenges = new HashSet<OkAuthenticator.Challenge>(challenges);
    }

    @Override
    public int hashCode() {
      int hash = 31;
      hash = hash * 17 + host.hashCode();
      hash = hash * 17 + port;
      hash = hash * 17 + challenges.hashCode();
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof AuthenticateRequestKey)) {
        return false;
      }
      AuthenticateRequestKey other = (AuthenticateRequestKey) obj;
      return host.equals(other.host) && port == other.port && challenges.equals(other.challenges);
    }
  }

  public static class AetherAuthenticator implements OkAuthenticator {

    private String username;
    private String password;
    private String proxyUsername;
    private String proxyPassword;

    private final Set<AuthenticateRequestKey> handled = new HashSet<AuthenticateRequestKey>();

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getProxyUsername() {
      return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
      this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
      return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
      this.proxyPassword = proxyPassword;
    }

    @Override
    public Credential authenticate(Proxy proxy, URL url, List<Challenge> challenges)
        throws IOException {
      if (!handled.add(new AuthenticateRequestKey(url, challenges))) {
        return Credential.basic(username, password);
      }
      return null;
    }

    @Override
    public Credential authenticateProxy(Proxy proxy, URL url, List<Challenge> challenges)
        throws IOException {
      if (!handled.add(new AuthenticateRequestKey(url, challenges))) {
        return Credential.basic(proxyUsername, proxyPassword);
      }
      return null;
    }
  }
}
