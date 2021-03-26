/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.apache.maven.index;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.index.archetype.AbstractArchetypeDataSource;
import org.apache.maven.index.context.IndexingContext;

/**
 * Trivial implementation of a Nexus indexer-based data source for archetypes.
 *
 * The maven-indexer used to ship such a class, but it is now an abstract helper
 * class that consumers have to extend themselves in order to implement
 * maven-archetype's {@link ArchetypeDataSource} interface. This allows
 * maven-indexer to avoid having a hard dep on maven-archetype at the cost of us
 * having to provide our own concrete implementation.
 */
@Singleton
@Named("nexus")
public class NexusArchetypeDataSource extends AbstractArchetypeDataSource implements ArchetypeDataSource {

	private final NexusIndexer nexusIndexer;

	@Inject
	public NexusArchetypeDataSource(Indexer indexer, NexusIndexer nexusIndexer) {
		super(indexer);
		this.nexusIndexer = nexusIndexer;
	}

	@Override
	protected List<IndexingContext> getIndexingContexts() {
		return new ArrayList<>(nexusIndexer.getIndexingContexts().values());
	}
}