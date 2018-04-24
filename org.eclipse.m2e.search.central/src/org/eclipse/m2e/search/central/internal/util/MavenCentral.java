/*******************************************************************************
 * Copyright (c) 2018 Sonatype Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.search.central.internal.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.search.central.internal.Constants;
import org.eclipse.m2e.search.central.internal.Messages;
import org.eclipse.m2e.search.central.internal.model.CentralSearchResponse;
import org.eclipse.m2e.search.central.internal.model.MavenCentralQuery;
import org.eclipse.osgi.util.NLS;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Matthew Piggott
 * @since 1.17.0
 */
@SuppressWarnings("restriction")
public class MavenCentral {

  private static final String QUERY = "q"; //$NON-NLS-1$

  private final Gson gson = new GsonBuilder().create();

  public CentralSearchResponse query(MavenCentralQuery query) throws CoreException {
    return get(QUERY, query.toString());
  }

  public CentralSearchResponse getVersionsByGA(MavenCentralQuery query) throws CoreException {
    return get(QUERY, query.toString(), "core", "gav"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private CloseableHttpClient createClient() {
    return HttpClients.createDefault();
  }

  private CentralSearchResponse get(String... params) throws CoreException {
    try (CloseableHttpClient client = createClient()) {
      URI uri = createUri(params);
      HttpGet httpGet = new HttpGet(uri);
      HttpResponse response = client.execute(httpGet);

      if (response.getStatusLine().getStatusCode() != 200) {
        throw new CoreException(new Status(IStatus.ERROR, Constants.PLUGIN_ID,
            NLS.bind(Messages.MavenCentral_UnexpectedResponse, response.getStatusLine().toString())));
      }

      HttpEntity entity = response.getEntity();

      try (Reader reader = new InputStreamReader(entity.getContent())) {
        return gson.fromJson(reader, CentralSearchResponse.class);
      }
    } catch (URISyntaxException | IOException e) {
      throw new CoreException(
          new Status(IStatus.ERROR, Constants.PLUGIN_ID, Messages.MavenCentral_UnexpectedError, e));
    }
  }

  private URI createUri(String... params) throws URISyntaxException {
    String centralUrl = M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getString(MavenPreferenceConstants.P_CENTRAL_SEARCH_URL);

    if (!centralUrl.endsWith("/")) { //$NON-NLS-1$
      centralUrl += '/';
    }
    centralUrl += "solrsearch/select"; //$NON-NLS-1$

    URIBuilder builder = new URIBuilder(centralUrl);
    builder.addParameter("wt", "json"); //$NON-NLS-1$ //$NON-NLS-2$
    builder.addParameter("rows", "20"); //$NON-NLS-1$ //$NON-NLS-2$
    for (int i = 0; i < params.length; i += 2) {
      builder.addParameter(params[i], params[i + 1]);
    }
    return builder.build();
  }
}
