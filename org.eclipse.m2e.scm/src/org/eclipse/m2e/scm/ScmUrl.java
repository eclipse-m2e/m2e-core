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

package org.eclipse.m2e.scm;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.scm.internal.Messages;


/**
 * An SCM URL wrapper used to adapt 3rd party resources:
 *
 * <pre>
 * scm:{scm_provider}:{scm_provider_specific_part}
 * </pre>
 *
 * @see http://maven.apache.org/scm/scm-url-format.html
 * @see org.eclipse.core.runtime.IAdapterManager
 * @author Eugene Kuleshov
 */
public class ScmUrl {
  private final String scmUrl;

  private final String scmParentUrl;

  private final ScmTag tag;

  public ScmUrl(String scmUrl) {
    this(scmUrl, null);
  }

  public ScmUrl(String scmUrl, String scmParentUrl) {
    this(scmUrl, scmParentUrl, null);
  }

  public ScmUrl(String scmUrl, String scmParentUrl, ScmTag tag) {
    this.scmUrl = scmUrl;
    this.scmParentUrl = scmParentUrl;
    this.tag = tag;
  }

  /**
   * Return SCM url
   */
  public String getUrl() {
    return scmUrl;
  }

  /**
   * Return SCM url for the parent folder
   */
  public String getParentUrl() {
    return scmParentUrl;
  }

  /**
   * Return SCM tag
   */
  public ScmTag getTag() {
    return this.tag;
  }

  /**
   * Return provider-specific part of the SCM url
   *
   * @return
   */
  public String getProviderUrl() {
    try {
      String type = ScmUrl.getType(scmUrl);
      return scmUrl.substring(type.length() + 5);

    } catch(CoreException ex) {
      return null;
    }
  }

  public static synchronized String getType(String url) throws CoreException {
    if(!url.startsWith("scm:")) { //$NON-NLS-1$
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(Messages.ScmUrl_error,
          url), null));
    }
    int n = url.indexOf(":", 4); //$NON-NLS-1$
    if(n == -1) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(Messages.ScmUrl_error,
          url), null));
    }
    return url.substring(4, n);
  }

}
