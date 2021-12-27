package org.eclipse.m2e.debug.runtime;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;

@Named
@Singleton
public class DebugExecutionListener implements MojoExecutionListener {
	// TODO: this class is likely not necessary anymore

	@Override
	public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
		Mojo mojo = event.getMojo();
	}

	@Override
	public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
		// TODO Auto-generated method stub
	}

	@Override
	public void afterExecutionFailure(MojoExecutionEvent event) {
		// TODO Auto-generated method stub
	}

}
