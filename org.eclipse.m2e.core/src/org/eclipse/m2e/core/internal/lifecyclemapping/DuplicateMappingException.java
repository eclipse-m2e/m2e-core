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

package org.eclipse.m2e.core.internal.lifecyclemapping;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;


/**
 * Exception thrown when multiple metadata source for either {@link PluginExecutionMetadata} or
 * {@link LifecycleMappingMetadata} are found for the same plugin/packaging.
 */
public class DuplicateMappingException extends RuntimeException {
  private static final String DESCRIPTION_UNKNOWN_SOURCE = "unknown source";

  private static final long serialVersionUID = 6916144930019743563L;

  private final LifecycleMappingMetadataSource source1, source2;

  public DuplicateMappingException(LifecycleMappingMetadataSource source1, LifecycleMappingMetadataSource source2) {
    this.source1 = source1;
    this.source2 = source2;
  }

  /* (non-Javadoc)
   * @see java.lang.Throwable#getMessage()
   */
  public String getMessage() {
    // sources might be either bundle, artifact or "default", "workspace" or MavenProject (all should provide proper toString() implementations)
    String source1Description = source1 != null ? source1.getSource().toString() : DESCRIPTION_UNKNOWN_SOURCE;
    String source2Description = source2 != null ? source2.getSource().toString() : DESCRIPTION_UNKNOWN_SOURCE;

    return "Mapping defined in '" + source1Description + "' and '" + source2Description + "'";
  }

}
