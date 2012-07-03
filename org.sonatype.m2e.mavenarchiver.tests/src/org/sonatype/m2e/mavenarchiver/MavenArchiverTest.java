package org.sonatype.m2e.mavenarchiver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

@SuppressWarnings("restriction")
public class MavenArchiverTest
    extends AbstractMavenProjectTestCase
{
    public void test001_pomProperties()
        throws Exception
    {
        IProject project =
            importProject( "projects/pomproperties/pomproperties-p001/pom.xml", new ResolverConfiguration() );
        waitForJobsToComplete();

        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create( project, monitor );
        ArtifactKey key = facade.getArtifactKey();

        IPath pomPath =
            project.getFolder( "target/classes/META-INF/maven/" + key.getGroupId() + "/" + key.getArtifactId()
                                   + "/pom.xml" ).getFullPath();

        IPath pomPropertiesPath =
            project.getFolder( "target/classes/META-INF/maven/" + key.getGroupId() + "/" + key.getArtifactId()
                                   + "/pom.properties" ).getFullPath();

        workspace.getRoot().getFile( pomPath ).delete( true, monitor );
        workspace.getRoot().getFile( pomPropertiesPath ).delete( true, monitor );
        project.build( IncrementalProjectBuilder.FULL_BUILD, monitor );

        // pom.xml
        assertTrue( pomPath + " is not accessible", workspace.getRoot().getFile( pomPath ).isAccessible() );

        // standard maven properties
        Properties properties = loadProperties( pomPropertiesPath );
        assertEquals( key.getGroupId(), properties.getProperty( "groupId" ) );
        assertEquals( key.getArtifactId(), properties.getProperty( "artifactId" ) );
        assertEquals( key.getVersion(), properties.getProperty( "version" ) );

        // m2e specific properties
        assertEquals( project.getName(), properties.getProperty( "m2e.projectName" ) );
        assertEquals( project.getLocation().toOSString(), properties.getProperty( "m2e.projectLocation" ) );
    }

    public void test002_jarmanifest()
            throws Exception
    {
        IProject project = importProject( "projects/mavenarchiver/mavenarchiver-p001/pom.xml");
        waitForJobsToComplete();
        assertNoErrors(project);
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create( project, monitor );
        ArtifactKey key = facade.getArtifactKey();
        
        IFile manifestFile = project.getFile( "target/classes/META-INF/MANIFEST.MF");
        IPath manifestPath = manifestFile.getFullPath();
        assertTrue( manifestFile + " is not accessible", manifestFile.isAccessible() );

        String manifestContent = getAsString(manifestPath);
        /* We expect something like : 
        Built-By: fbricon
        Build-Jdk: 1.6.0_22
        Specification-Title: mavenarchiver-p001
        Specification-Version: 0.0.1-SNAPSHOT
        Implementation-Title: mavenarchiver-p001
        Implementation-Version: 0.0.1-SNAPSHOT
        Implementation-Vendor-Id: org.sonatype.m2e.mavenarchiver.tests
        Created-By: Maven Integration for Eclipse
        */

        assertTrue("Specification-Version is missing : "+manifestContent, 
        		manifestContent.contains("Specification-Version: "+key.getVersion()));
        assertTrue("Specification-Title is missing : "+manifestContent, 
        		manifestContent.contains("Specification-Title: "+key.getArtifactId()));
        assertTrue("Implementation-Title is missing : "+manifestContent, 
        		manifestContent.contains("Implementation-Title: "+key.getArtifactId()));
        assertTrue("Implementation-Version is missing : "+manifestContent, 
        		manifestContent.contains("Implementation-Version: "+key.getVersion()));
        assertTrue("Created-By is missing", 
        		manifestContent.contains("Created-By: Maven Integration for Eclipse"));
        assertFalse("Classpath: should be missing", manifestContent.contains("Class-Path:"));
        
        manifestFile.delete(true, monitor);
        project.build( IncrementalProjectBuilder.FULL_BUILD, monitor );

        // Check the manifest is recreated
        assertTrue( manifestPath + " is not accessible", workspace.getRoot().getFile( manifestPath ).isAccessible() );
    }

    public void test003_jarmanifest_classpath()
            throws Exception
    {
        IProject[] projects = importProjects( "projects/mavenarchiver/",
				        		new String[]
				        				{
        								"mavenarchiver-p002/pom.xml",
				        				"mavenarchiver-p001/pom.xml"
        								}, 
				        		new ResolverConfiguration());
        waitForJobsToComplete();
        IProject project = projects[0];
        IProject dependency = projects[1];
        
        assertNoErrors(project);
        assertNoErrors(dependency);
        
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create( project, monitor );
        
        IFile manifestFile = project.getFile( "target/classes/META-INF/MANIFEST.MF");
        IPath manifestPath = manifestFile.getFullPath();
        assertTrue( manifestFile + " is not accessible", manifestFile.isAccessible() );

        String manifestContent = getAsString(manifestPath);

        assertFalse("Specification-Version should be missing : "+manifestContent, 
        		manifestContent.contains("Specification-Version"));
        assertFalse("Implementation-Version should be missing : "+manifestContent, 
        		manifestContent.contains("Implementation-Version"));
        assertTrue("Created-By is missing", 
        		manifestContent.contains("Created-By: Maven Integration for Eclipse"));
        assertFalse("Invalid Classpath: "+manifestContent, 
        		manifestContent.contains("Class-Path:"));
                
        copyContent(project, "pom2.xml", "pom.xml", true);
        project.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );
        waitForJobsToComplete();
        
        // Check the manifest is updated
        manifestContent = getAsString(manifestPath);
        assertTrue("Invalid Classpath in manifest : " + manifestContent, 
        		manifestContent.contains("Class-Path: junit-3.8.1.jar mavenarchiver-p001-0.0.1-SNAPSHOT.jar"));

        assertTrue("Created-By is invalid", 
        		manifestContent.contains("Created-By: My beloved IDE"));

    }
    
    @Test
    public void test003_ProvidedManifest() throws Exception 
    {
      IProject project = importProject("projects/mavenarchiver/mavenarchiver-p003/pom.xml");
      waitForJobsToComplete();
      
      IFile manifestFile = project.getFile("src/main/resources/META-INF/MANIFEST.MF");
      assertTrue("The manifest was deleted", manifestFile.exists());
      
      IFile generatedManifestFile = project.getFile("target/classes/META-INF/MANIFEST.MF");
      assertTrue("The generated manifest is missing", generatedManifestFile.exists());
      
      IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create( project, monitor );
      ArtifactKey key = facade.getArtifactKey();
      
      String manifest =getAsString(generatedManifestFile);
      assertTrue("Built-By is invalid :"+manifest, manifest.contains("You know who"));
      assertTrue("Implementation-Title is invalid :"+manifest, manifest.contains("Implementation-Title: "+key.getArtifactId()));
      assertTrue("Invalid Classpath in manifest : " + manifest, manifest.contains("Class-Path: custom.jar"));
    }

    public void testMECLIPSEWTP163_ParentMustBeResolved()
            throws Exception
    {
        IProject[] projects = importProjects( "projects/mavenarchiver/parent-contextsession/", 
        									new String[]{"pom.xml", "child-contextsession/pom.xml"}, 
        									new ResolverConfiguration());
        waitForJobsToComplete();
        IProject parent = projects[0];
        IProject child = projects[1];
        assertNoErrors(parent);
        assertNoErrors(child);
        
        IFile generatedManifestFile = child.getFile("target/classes/META-INF/MANIFEST.MF");
        assertTrue("The generated manifest is missing", generatedManifestFile.exists());
        
        IMavenProjectFacade parentFacade = MavenPlugin.getMavenProjectRegistry().create( parent, monitor );
        String parentUrl = parentFacade.getMavenProject().getModel().getUrl();
        
        String manifest =getAsString(generatedManifestFile);
        assertTrue("Implementation-Url is invalid :"+manifest, manifest.contains("Implementation-URL: "+parentUrl));
    }
    
    private Properties loadProperties( IPath aPath )
        throws CoreException, IOException
    {
        Properties properties = new Properties();
        InputStream contents = workspace.getRoot().getFile( aPath ).getContents();
        try
        {
            properties.load( contents );
        }
        finally
        {
            contents.close();
        }
        return properties;
    }
    
    //Copied from AbstractWTPTestCase
    protected String getAsString(IFile file) throws IOException, CoreException 
    {
	    InputStream ins = null;
	    String content = null;
	    try 
	    {
	      ins = file.getContents();
	      content = IOUtil.toString(ins, 1024);
	    } finally {
	      IOUtil.close(ins);   
	    }
	    return content;
	}      
    
    protected String getAsString(IPath path) throws IOException, CoreException 
    {
    	return getAsString(workspace.getRoot().getFile( path ));
	}    
}
