/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stuart McCulloch (Sonatype, Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.plexus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.sisu.inject.Weak;

/**
 * Utility methods for dealing with Plexus {@link ClassRealm}s.
 */
public final class ClassRealmUtils
{
    // ----------------------------------------------------------------------
    // Static initialization
    // ----------------------------------------------------------------------

    static
    {
        boolean getImportRealmsSupported = true;
        try
        {
            // support both old and new forms of Plexus class realms
            ClassRealm.class.getDeclaredMethod( "getImportRealms" );
        }
        catch ( final Exception e )
        {
            getImportRealmsSupported = false;
        }
        catch ( final LinkageError e )
        {
            getImportRealmsSupported = false;
        }
        GET_IMPORT_REALMS_SUPPORTED = getImportRealmsSupported;
    }

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final boolean GET_IMPORT_REALMS_SUPPORTED;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    private ClassRealmUtils()
    {
        // static utility class, not allowed to create instances
    }

    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private static Map<ClassRealm, Set<String>> namesCache = Weak.concurrentKeys();

    // ----------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------

    /**
     * @return Current context realm
     */
    public static ClassRealm contextRealm()
    {
        for ( ClassLoader tccl = Thread.currentThread().getContextClassLoader(); tccl != null; tccl = tccl.getParent() )
        {
            if ( tccl instanceof ClassRealm )
            {
                return (ClassRealm) tccl;
            }
        }
        return null;
    }

    /**
     * Walks the {@link ClassRealm} import graph to find all realms visible from the given realm.
     * 
     * @param contextRealm The initial realm
     * @return Names of all realms visible from the given realm
     */
    public static Set<String> visibleRealmNames( final ClassRealm contextRealm )
    {
        if ( GET_IMPORT_REALMS_SUPPORTED && null != contextRealm )
        {
            Set<String> names = namesCache.get( contextRealm );
            if ( null == names )
            {
                namesCache.put( contextRealm, names = computeVisibleNames( contextRealm ) );
            }
            return names;
        }
        return null;
    }

    public static void flushCaches( final ClassRealm realm )
    {
        // igorf: static caches are almost never a good idea. consider moving the cache to a component
        namesCache.remove( realm );
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    private static Set<String> computeVisibleNames( final ClassRealm forRealm )
    {
        final Set<String> visibleRealmNames = new HashSet<String>();
        final List<ClassRealm> searchRealms = new ArrayList<ClassRealm>();

        searchRealms.add( forRealm );
        for ( int i = 0; i < searchRealms.size(); i++ )
        {
            final ClassRealm realm = searchRealms.get( i );
            if ( visibleRealmNames.add( realm.toString() ) )
            {
                searchRealms.addAll( realm.getImportRealms() );
                final ClassRealm parent = realm.getParentRealm();
                if ( null != parent )
                {
                    searchRealms.add( parent );
                }
            }
        }
        return visibleRealmNames;
    }
}
