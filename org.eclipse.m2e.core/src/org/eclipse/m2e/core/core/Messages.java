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

package org.eclipse.m2e.core.core;

import java.util.MissingResourceException;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

//mkleint: this class looks like not following the default eclipse way of i18n and resides in public packages

public class Messages {
  private static final String BUNDLE_NAME = IMavenConstants.PLUGIN_ID + ".messages"; //$NON-NLS-1$

  private static final UResourceBundle RESOURCE_BUNDLE = UResourceBundle.getBundleInstance(BUNDLE_NAME,
      ULocale.getDefault(), Messages.class.getClassLoader());

  private Messages() {
  }

  public static String getString(String key) {
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch(MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  public static String getString( String key, Object[] args ) {
    try {
      return MessageFormat.format(
        RESOURCE_BUNDLE.getString( key ), args );
    } catch( MissingResourceException e ) {
      return '!' + key + '!';
    }
  }

  public static String getString( String key, Object arg ) {
    return getString( key, new Object[]{ arg } );
  }

  public static String getString( String key, int arg ) {
    return getString( key, new Object[]{ String.valueOf(arg) } );
  }
}
