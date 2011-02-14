/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.ning.http.client.BodyConsumer;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Response;
import com.ning.http.client.SimpleAsyncHttpClient;
import com.ning.http.client.ThrowableHandler;
import com.ning.http.client.SimpleAsyncHttpClient.ErrorDocumentBehaviour;
import com.ning.http.client.consumers.OutputStreamBodyConsumer;
import com.ning.http.client.simple.HeaderMap;
import com.ning.http.client.simple.SimpleAHCTransferListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

import org.apache.maven.index.updater.AbstractResourceFetcher;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;
import org.apache.maven.wagon.repository.Repository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.Messages;


/**
 * A resource fetcher using Async HTTP Client.
 * 
 * @author Benjamin Hanzelmann
 */
public class AsyncFetcher extends AbstractResourceFetcher {

  private final AuthenticationInfo authInfo;

  private final ProxyInfo proxyInfo;

  private final String userAgent;

  private final IProgressMonitor monitor;

  private SimpleAsyncHttpClient httpClient;

  private String baseUrl;

  private Map<String, Future<Response>> futures = new HashMap<String, Future<Response>>();

  private Map<String, Streams> streams = new HashMap<String, Streams>();

  public AsyncFetcher(final AuthenticationInfo authInfo, final ProxyInfo proxyInfo, final IProgressMonitor monitor) {
    this.authInfo = authInfo;
    this.proxyInfo = proxyInfo;
    this.monitor = (monitor != null) ? monitor : new NullProgressMonitor();
    this.userAgent = computeUserAgent();
  }

  void cancel(String url) {
    Future<Response> future = futures.remove(url);
    if(future != null) {
      future.cancel(true);
    }
  }

  void closeStream(String url, Throwable exception) {
    Streams s = streams.remove(url);

    if(s == null) {
      return;
    }

    PipedErrorInputStream pis = s.in;
    pis.setError(exception);

    try {
      s.out.close();
    } catch(IOException ex) {
      // we tried
    }
  }

  public void connect(String id, String url) {
    httpClient = createClient(url);
    baseUrl = url.endsWith("/") ? url : (url + '/'); //$NON-NLS-1$
  }

  private SimpleAsyncHttpClient createClient(String url) {
    SimpleAsyncHttpClient.Builder sahcBuilder = new SimpleAsyncHttpClient.Builder();
    
    sahcBuilder.setUserAgent(userAgent);

    sahcBuilder.setConnectionTimeoutInMs(15 * 1000);
    sahcBuilder.setRequestTimeoutInMs(60 * 1000);
    sahcBuilder.setCompressionEnabled(true);
    sahcBuilder.setFollowRedirects(true);
    sahcBuilder.setErrorDocumentBehaviour(ErrorDocumentBehaviour.OMIT);
    sahcBuilder.setListener(new MonitorListener(monitor));

    addAuthInfo(sahcBuilder);
    addProxyInfo(url, sahcBuilder);

    return sahcBuilder.build();
  }

  private String computeUserAgent() {
    String osgiVersion = (String) Platform.getBundle("org.eclipse.osgi").getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION); //$NON-NLS-1$
    String m2eVersion = MavenPlugin.getQualifiedVersion();
    return "m2e/" + osgiVersion + "/" + m2eVersion; //$NON-NLS-1$
  }

  private void addAuthInfo(SimpleAsyncHttpClient.Builder configBuilder) {
    if(authInfo != null && authInfo.getUserName() != null && authInfo.getUserName().length() > 0) {
      configBuilder.setRealmPrincipal(authInfo.getUserName());
      configBuilder.setRealmPassword(authInfo.getPassword());
      configBuilder.setRealmUsePreemptiveAuth(false);
    }
  }

  private void addProxyInfo(String url, SimpleAsyncHttpClient.Builder configBuilder) {
    if(proxyInfo != null) {
      Repository repo = new Repository("id", url); //$NON-NLS-1$
      if(!ProxyUtils.validateNonProxyHosts(proxyInfo, repo.getHost())) {
        if(proxyInfo != null) {
          ProxyServer.Protocol protocol = "https".equalsIgnoreCase(proxyInfo.getType()) ? ProxyServer.Protocol.HTTPS //$NON-NLS-1$
              : ProxyServer.Protocol.HTTP;

          configBuilder.setProxyProtocol(protocol);
          configBuilder.setProxyHost(proxyInfo.getHost());
          configBuilder.setProxyPort(proxyInfo.getPort());
          configBuilder.setProxyPrincipal(proxyInfo.getUserName());
          configBuilder.setProxyPassword(proxyInfo.getPassword());
        }
      }
    }
  }

  public void disconnect() {
    baseUrl = null;
    futures.clear();

    if(httpClient != null) {
      httpClient.close();
    }

    httpClient = null;
  }

  @SuppressWarnings("deprecation")
  public void retrieve(String name, File targetFile) throws IOException, FileNotFoundException {
    InputStream is = retrieve(name);
    try {
      FileUtils.copyStreamToFile(new RawInputStreamFacade(is), targetFile);
    } finally {
      IOUtil.close(is);
    }
  }

  @Override
  public InputStream retrieve(String name) throws IOException, FileNotFoundException {
    final String url = buildUrl(baseUrl, name);

    monitor.subTask(NLS.bind(Messages.AsyncFetcher_task_fetching, url));

    PipedErrorInputStream pis = new PipedErrorInputStream();
    PipedOutputStream pos = new PipedOutputStream(pis);
    BodyConsumer consumer = new OutputStreamBodyConsumer(pos);

    streams.put(url, new Streams(pis, pos));

    Future<Response> future = httpClient.derive().setUrl(url).build().get(consumer, new ErrorPropagator(url));

    futures.put(url, future);

    return pis;
  }

  private static String buildUrl(String baseUrl, String resourceName) {
    String url = baseUrl;

    if(resourceName.startsWith("/")) { //$NON-NLS-1$
      url += resourceName.substring(1);
    } else {
      url += resourceName;
    }

    return url;
  }

  final class ErrorPropagator implements ThrowableHandler {

    private final String url;

    ErrorPropagator(String url) {
      this.url = url;
    }

    public void onThrowable(Throwable t) {
      closeStream(this.url, t);
    }
  }

  static final class PipedErrorInputStream extends PipedInputStream {

    private volatile Throwable error;

    public PipedErrorInputStream() {
      buffer = new byte[1024 * 128];
    }

    public void setError(Throwable t) {
      if(error == null) {
        error = t;
      }
    }

    private void checkError() throws IOException {
      if(error != null) {
        throw (IOException) new IOException(error.getMessage()).initCause(error);
      }
    }

    @Override
    public synchronized int read() throws IOException {
      checkError();
      int b = super.read();
      checkError();
      return b;
    }
  }

  private class MonitorListener implements SimpleAHCTransferListener {
  
    private IProgressMonitor monitor;
  
    public MonitorListener(IProgressMonitor monitor) {
      this.monitor = monitor;
    }
  
    private void checkCancelled(String url) {
      if(monitor.isCanceled()) {
        cancel(url);
      }
    }

    public void onStatus(String url, int code, String text) {
      checkCancelled(url);
      if(code != HttpURLConnection.HTTP_OK) {
        closeStream(url, new IOException(NLS.bind(Messages.AsyncFetcher_error_server, code, text)));
      }
    }

    public void onHeaders(String url, HeaderMap arg1) {
      checkCancelled(url);
    }
  
    public void onBytesReceived(String url, long amount, long current, long total) {
      checkCancelled(url);
      monitor.subTask(NLS.bind(Messages.AsyncFetcher_task_fetching2, url, current * 100 / total));
    }
  
    public void onBytesSent(String arg0, long arg1, long arg2, long arg3) {
      // we only retrieve
    }

    public void onCompleted(String arg0, int arg1, String arg2) {
      monitor.subTask(""); //$NON-NLS-1$
    }
  
  }

  private final class Streams {
    PipedErrorInputStream in;
    PipedOutputStream out;

    public Streams(PipedErrorInputStream pis, PipedOutputStream pos) {
      this.in = pis;
      this.out = pos;
    }
  }

}