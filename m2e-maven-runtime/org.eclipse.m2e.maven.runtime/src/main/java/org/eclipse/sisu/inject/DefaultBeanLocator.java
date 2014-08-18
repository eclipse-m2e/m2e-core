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
package org.eclipse.sisu.inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Default {@link MutableBeanLocator} that locates qualified beans across a dynamic group of {@link BindingPublisher}s.
 */
@Singleton
@SuppressWarnings( { "rawtypes", "unchecked" } )
public final class DefaultBeanLocator
    implements MutableBeanLocator
{
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final RankedSequence<BindingPublisher> publishers = new RankedSequence<BindingPublisher>();

    private final ConcurrentMap<TypeLiteral, RankedBindings> cachedBindings = Soft.concurrentValues( 256, 8 );

    // reverse mapping; can't use watcher as key since it may not be unique
    private final Map<WatchedBeans, Object> cachedWatchers = Weak.values();

    private final ImplicitBindings implicitBindings = new ImplicitBindings( publishers );

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public Iterable<BeanEntry> locate( final Key key )
    {
        final TypeLiteral type = key.getTypeLiteral();
        RankedBindings bindings = cachedBindings.get( type );
        if ( null == bindings )
        {
            synchronized ( this )
            {
                bindings = new RankedBindings( type, publishers );
                final RankedBindings oldBindings = cachedBindings.putIfAbsent( type, bindings );
                if ( null != oldBindings )
                {
                    bindings = oldBindings;
                }
            }
        }
        final boolean isImplicit = key.getAnnotationType() == null && TypeArguments.isImplicit( type );
        return new LocatedBeans( key, bindings, isImplicit ? implicitBindings : null );
    }

    public synchronized void watch( final Key key, final Mediator mediator, final Object watcher )
    {
        final WatchedBeans beans = new WatchedBeans( key, mediator, watcher );
        for ( final BindingPublisher p : publishers.snapshot() )
        {
            p.subscribe( beans );
        }
        cachedWatchers.put( beans, watcher );
    }

    public synchronized void add( final BindingPublisher publisher, final int rank )
    {
        if ( !publishers.contains( publisher ) )
        {
            Logs.trace( "Add publisher: {}", publisher, null );
            publishers.insert( publisher, rank );
            for ( final RankedBindings bindings : cachedBindings.values() )
            {
                bindings.add( publisher, rank );
            }
            for ( final WatchedBeans beans : cachedWatchers.keySet() )
            {
                publisher.subscribe( beans );
            }
        }
    }

    public synchronized void remove( final BindingPublisher publisher )
    {
        if ( publishers.remove( publisher ) )
        {
            Logs.trace( "Remove publisher: {}", publisher, null );
            // igorf: I question the use of Soft.concurrentValues for cachedBindings.
            // Seems over-engineered and does not correctly implement Map interface.
            // entrySet().iterator().remove() does not modify the map for example
            Map<TypeLiteral, RankedBindings> emptyBindings = new HashMap<TypeLiteral, RankedBindings>();
            Iterator<Map.Entry<TypeLiteral, RankedBindings>> iter = cachedBindings.entrySet().iterator();
            while ( iter.hasNext() )
            {
                final Entry<TypeLiteral, RankedBindings> entry = iter.next();
                final RankedBindings bindings = entry.getValue();
                bindings.remove( publisher );
                if ( bindings.isEmpty() )
                {
                    // iter.remove(); does nothing :-(
                    emptyBindings.put( entry.getKey(), entry.getValue() );
                }
            }
            for ( Map.Entry<TypeLiteral, RankedBindings> entry : emptyBindings.entrySet() )
            {
                cachedBindings.remove( entry.getKey() );
            }
            for ( final WatchedBeans beans : cachedWatchers.keySet() )
            {
                publisher.unsubscribe( beans );
            }
        }
    }

    public synchronized void clear()
    {
        for ( final BindingPublisher p : publishers.snapshot() )
        {
            remove( p );
        }
    }

    public void add( final Injector injector, final int rank )
    {
        add( new InjectorPublisher( injector, new DefaultRankingFunction( rank ) ), rank );
    }

    public void remove( final Injector injector )
    {
        remove( new InjectorPublisher( injector, null ) );
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    /**
     * Automatically publishes any {@link Injector} that contains a binding to this {@link BeanLocator}.
     * 
     * @param injector The injector
     */
    @Inject
    void autoPublish( final Injector injector )
    {
        staticAutoPublish( this, injector );
    }

    @Inject
    static void staticAutoPublish( final MutableBeanLocator locator, final Injector injector )
    {
        final RankingFunction function = injector.getInstance( RankingFunction.class );
        locator.add( new InjectorPublisher( injector, function ), function.maxRank() );
    }
}
