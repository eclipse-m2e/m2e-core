/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Christoph Läubrich - #549 - Improve conflict handling of lifecycle mappings
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;


/**
 * Exception thrown when multiple metadata source for either {@link PluginExecutionMetadata} or
 * {@link LifecycleMappingMetadata} are found for the same plugin/packaging.
 */
public abstract class DuplicateMappingException extends RuntimeException {
  private static final String DESCRIPTION_UNKNOWN_SOURCE = "unknown source";

  private static final long serialVersionUID = -7303637464019592307L;

  private final LifecycleMappingMetadataSource[] sources;


  protected DuplicateMappingException(LifecycleMappingMetadataSource... sources) {
    this.sources = sources;
  }

  /* (non-Javadoc)
   * @see java.lang.Throwable#getMessage()
   */
  public String getMessage() {
    // sources might be either bundle, artifact or "default", "workspace" or MavenProject (all should provide proper toString() implementations)
    return "Mapping defined in "
        + Arrays.stream(sources).map(s -> s.getSource()).map(s -> s == null ? DESCRIPTION_UNKNOWN_SOURCE : s.toString())
            .collect(Collectors.joining("' and '", "'", "'"));
  }

  /**
   * @return Returns the sources.
   */
  public LifecycleMappingMetadataSource[] getConflictingSources() {
    return this.sources;
  }


}
