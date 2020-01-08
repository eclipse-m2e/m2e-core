/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.index.nexus;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.apache.maven.index.updater.AbstractResourceFetcher;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;

import io.takari.aether.client.AetherClient;
import io.takari.aether.client.AetherClientAuthentication;
import io.takari.aether.client.AetherClientConfig;
import io.takari.aether.client.AetherClientProxy;
import io.takari.aether.client.Response;
import io.takari.aether.okhttp.OkHttpAetherClient;


public class AetherClientResourceFetcher extends AbstractResourceFetcher {

  private AetherClient aetherClient;

  private final AuthenticationInfo authInfo;

  private final ProxyInfo proxyInfo;

  private final String userAgent;

  private final IProgressMonitor monitor;

  private String baseUrl;

  public AetherClientResourceFetcher(final AuthenticationInfo authInfo, final ProxyInfo proxyInfo,
      final IProgressMonitor monitor) {
    this.authInfo = authInfo;
    this.proxyInfo = proxyInfo;
    this.monitor = (monitor != null) ? monitor : new NullProgressMonitor();
    this.userAgent = MavenPluginActivator.getUserAgent();
  }

  public void connect(String id, String url) {
    this.baseUrl = url;
    aetherClient = new OkHttpAetherClient(
        new AetherClientConfigAdapter(baseUrl, authInfo, proxyInfo, userAgent,
        new HashMap<String, String>()));
  }

  public void disconnect() throws IOException {
    aetherClient.close();
  }

  @Deprecated
  public void retrieve(String name, File targetFile) throws IOException, FileNotFoundException {
    String url = baseUrl + "/" + name;
    try (Response response = aetherClient.get(url);
        InputStream is = response.getInputStream();
        OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile))) {
      final byte[] buffer = new byte[1024 * 1024];
      int n = 0;
      while(-1 != (n = is.read(buffer))) {
        os.write(buffer, 0, n);
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
      }
    }
  }

  class AetherClientConfigAdapter extends AetherClientConfig {
    private final Logger log = LoggerFactory.getLogger(AetherClientConfigAdapter.class);

    int connectionTimeout;

    int requestTimeout;

    AuthenticationInfo authInfo;

    ProxyInfo proxyInfo;

    String userAgent;

    String baseUrl;

    Map<String, String> headers;

    public AetherClientConfigAdapter(String baseUrl, AuthenticationInfo authInfo, ProxyInfo proxyInfo, String userAgent,
        Map<String, String> headers) {
      this.baseUrl = baseUrl;
      this.authInfo = authInfo;
      this.proxyInfo = proxyInfo;
      this.userAgent = userAgent;
      this.headers = headers;

      try {
        // ensure JVM's trust & key stores are used
        setSslSocketFactory(SSLContext.getDefault().getSocketFactory());
      } catch(NoSuchAlgorithmException ex) {
        log.warn(Messages.AetherClientConfigAdapter_error_sslContext);
      }
    }

    public String getUserAgent() {
      return userAgent;
    }

    public int getConnectionTimeout() {
      return connectionTimeout;
    }

    public int getRequestTimeout() {
      return requestTimeout;
    }

    public AetherClientProxy getProxy() {

      if(proxyInfo == null) {
        return null;
      }
      //Bug 512006 don't return the proxy for nonProxyHosts 
      try {
        if(ProxyUtils.validateNonProxyHosts(proxyInfo, new URL(baseUrl).getHost())) {
          return null;
        }
      } catch(MalformedURLException ignore) {
      }

      return new AetherClientProxy() {

        public String getHost() {
          return proxyInfo.getHost();
        }

        public int getPort() {
          return proxyInfo.getPort();
        }

        public AetherClientAuthentication getAuthentication() {

          if(proxyInfo != null && proxyInfo.getUserName() != null && proxyInfo.getPassword() != null) {
            return new AetherClientAuthentication(proxyInfo.getUserName(), proxyInfo.getPassword());
          }
          return null;
        }
      };
    }

    public AetherClientAuthentication getAuthentication() {

      if(authInfo != null) {
        return new AetherClientAuthentication(authInfo.getUserName(), authInfo.getPassword());
      }
      return null;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }
  }
}
