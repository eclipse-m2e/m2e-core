/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index.nexus;

import io.takari.aether.client.AetherClient;
import io.takari.aether.client.AetherClientAuthentication;
import io.takari.aether.client.AetherClientConfig;
import io.takari.aether.client.AetherClientProxy;
import io.takari.aether.client.Response;
import io.takari.aether.okhttp.OkHttpAetherClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.common.io.Closer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.apache.maven.index.updater.AbstractResourceFetcher;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;

import org.eclipse.m2e.core.internal.MavenPluginActivator;


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

  public void connect(String id, String url) throws IOException {
    aetherClient = new OkHttpAetherClient(new AetherClientConfigAdapter(authInfo, proxyInfo, userAgent,
        new HashMap<String, String>()));
    this.baseUrl = url;
  }

  public void disconnect() throws IOException {
    aetherClient.close();
  }

  public void retrieve(String name, File targetFile) throws IOException, FileNotFoundException {

    String url = baseUrl + "/" + name;
    Response response = aetherClient.get(url);

    Closer closer = Closer.create();
    try {
      InputStream is = closer.register(response.getInputStream());
      OutputStream os = closer.register(new BufferedOutputStream(new FileOutputStream(targetFile)));
      final byte[] buffer = new byte[1024 * 1024];
      int n = 0;
      while(-1 != (n = is.read(buffer))) {
        os.write(buffer, 0, n);
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
      }
    } finally {
      closer.close();
    }
  }

  class AetherClientConfigAdapter extends AetherClientConfig {

    int connectionTimeout;

    int requestTimeout;

    AuthenticationInfo authInfo;

    ProxyInfo proxyInfo;

    String userAgent;

    Map<String, String> headers;

    public AetherClientConfigAdapter(AuthenticationInfo authInfo, ProxyInfo proxyInfo, String userAgent,
        Map<String, String> headers) {
      this.authInfo = authInfo;
      this.proxyInfo = proxyInfo;
      this.userAgent = userAgent;
      this.headers = headers;
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
