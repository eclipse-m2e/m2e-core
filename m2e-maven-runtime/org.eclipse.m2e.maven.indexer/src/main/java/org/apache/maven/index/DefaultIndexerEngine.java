/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * A default {@link IndexerEngine} implementation.
 *
 * @author Tamas Cservenak
 */
@Singleton
@Named
public class DefaultIndexerEngine
    extends AbstractLogEnabled
    implements IndexerEngine
{

    public void index( IndexingContext context, ArtifactContext ac )
        throws IOException
    {
        // skip artifacts not obeying repository layout (whether m1 or m2)
        if ( ac != null && ac.getGav() != null )
        {
            Document d = ac.createDocument( context );

            if ( d != null )
            {
                context.getIndexWriter().addDocument( d );

                context.updateTimestamp();
            }
        }
    }

    public void update( IndexingContext context, ArtifactContext ac )
        throws IOException
    {
        if ( ac != null && ac.getGav() != null )
        {
            Document d = ac.createDocument( context );

            if ( d != null )
            {
                Document old = getOldDocument( context, ac );

                if ( !equals( d, old ) )
                {
                    IndexWriter w = context.getIndexWriter();

                    w.updateDocument( new Term( ArtifactInfo.UINFO, ac.getArtifactInfo().getUinfo() ), d );

                    updateGroups( context, ac );

                    context.updateTimestamp();
                }
            }
        }
    }

    private boolean equals( Document d1, Document d2 )
    {
        if ( d1 == null && d2 == null )
        {
            return true;
        }
        if ( d1 == null || d2 == null )
        {
            return false;
        }

        Map<String, String> m1 = toMap( d1 );
        Map<String, String> m2 = toMap( d2 );

        m1.remove( MinimalArtifactInfoIndexCreator.FLD_LAST_MODIFIED.getKey() );
        m2.remove( MinimalArtifactInfoIndexCreator.FLD_LAST_MODIFIED.getKey() );

        return m1.equals( m2 );
    }

    private Map<String, String> toMap( Document d )
    {
        final HashMap<String, String> result = new HashMap<>();

        for ( IndexableField f : d.getFields() )
        {
            if ( f.fieldType().stored() )
            {
                result.put( f.name(), f.stringValue() );
            }
        }

        return result;
    }

    private Document getOldDocument( IndexingContext context, ArtifactContext ac )
    {
        try
        {
            IndexSearcher indexSearcher = context.acquireIndexSearcher();
            try
            {
                TopDocs result = indexSearcher
                        .search(new TermQuery(new Term(ArtifactInfo.UINFO, ac.getArtifactInfo().getUinfo())), 2);

                if ( result.totalHits == 1 )
                {
                    return indexSearcher.doc( result.scoreDocs[0].doc );
                }
            }
            finally
            {
                context.releaseIndexSearcher( indexSearcher );
            }
        }
        catch ( IOException e )
        {
        }
        return null;
    }

    private void updateGroups( IndexingContext context, ArtifactContext ac )
        throws IOException
    {
        String rootGroup = ac.getArtifactInfo().getRootGroup();
        Set<String> rootGroups = context.getRootGroups();
        if ( !rootGroups.contains( rootGroup ) )
        {
            rootGroups.add( rootGroup );
            context.setRootGroups( rootGroups );
        }

        Set<String> allGroups = context.getAllGroups();
        if ( !allGroups.contains( ac.getArtifactInfo().getGroupId() ) )
        {
            allGroups.add( ac.getArtifactInfo().getGroupId() );
            context.setAllGroups( allGroups );
        }
    }

    public void remove( IndexingContext context, ArtifactContext ac )
        throws IOException
    {
        if ( ac != null )
        {
            String uinfo = ac.getArtifactInfo().getUinfo();
            // add artifact deletion marker
            Document doc = new Document();
            doc.add( new StringField( ArtifactInfo.DELETED, uinfo, Field.Store.YES ) );
            doc.add( new StringField( ArtifactInfo.LAST_MODIFIED, //
                Long.toString( System.currentTimeMillis() ), Field.Store.YES ) );
            IndexWriter w = context.getIndexWriter();
            w.addDocument( doc );
            w.deleteDocuments( new Term( ArtifactInfo.UINFO, uinfo ) );
            w.commit();
            context.updateTimestamp();
        }
    }

}
