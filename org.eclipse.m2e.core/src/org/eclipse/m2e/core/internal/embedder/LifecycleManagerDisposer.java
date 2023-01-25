/********************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.lang.reflect.Field;
import java.util.Map;

import org.eclipse.sisu.bean.BeanManager;
import org.eclipse.sisu.bean.LifecycleManager;
import org.eclipse.sisu.plexus.PlexusLifecycleManager;

import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorldListener;
import org.codehaus.plexus.classworlds.realm.ClassRealm;


public class LifecycleManagerDisposer implements ClassWorldListener {

  private DefaultPlexusContainer container;

  public LifecycleManagerDisposer(DefaultPlexusContainer container) {
    this.container = container;
  }

  @Override
  public void realmCreated(ClassRealm realm) {
  }

  @Override
  public void realmDisposed(ClassRealm realm) {
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=581407
    try {
      BeanManager beanManager = container.lookup(BeanManager.class);
      if(beanManager instanceof PlexusLifecycleManager plexus) {
        Field delegateField = plexus.getClass().getDeclaredField("delegate");
        delegateField.setAccessible(true);
        Object delegate = delegateField.get(plexus);
        if(delegate instanceof LifecycleManager manager) {
          disposeOutdatedEntries(realm, manager);
        }
      }
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  private void disposeOutdatedEntries(ClassRealm realm, LifecycleManager manager)
      throws NoSuchFieldException, IllegalAccessException {
    Field lifecyclesMapField = manager.getClass().getDeclaredField("lifecycles");
    lifecyclesMapField.setAccessible(true);
    Object lifecycles = lifecyclesMapField.get(manager);
    if(lifecycles instanceof Map<?, ?> map) {
      map.keySet().removeIf(key -> key instanceof Class<?> clazz && clazz.getClassLoader() == realm);
    }
  }

}
