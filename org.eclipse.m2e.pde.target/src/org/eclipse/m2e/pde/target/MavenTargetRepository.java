/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import org.apache.maven.model.Repository;

public class MavenTargetRepository extends Repository {

	public MavenTargetRepository(String id, String url) {
		setId(id);
		setUrl(url);
	}

	public MavenTargetRepository copy() {
		return new MavenTargetRepository(getId(), getUrl());
	}

}
