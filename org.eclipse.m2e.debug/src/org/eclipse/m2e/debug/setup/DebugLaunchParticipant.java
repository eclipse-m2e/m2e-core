package org.eclipse.m2e.debug.setup;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.debug.runtime.DebugEventSpy;
import org.eclipse.m2e.internal.launch.IMavenLaunchParticipant;
import org.eclipse.m2e.internal.launch.MavenLaunchUtils;

@SuppressWarnings("restriction")
public class DebugLaunchParticipant implements IMavenLaunchParticipant {

	@Override
	public String getProgramArguments(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) {
		try {
			if (MavenLaunchUtils.getMavenRuntime(configuration) instanceof MavenEmbeddedRuntime) {
				return DebugEventSpy.getEnableDebugProperty();
			}
		} catch (CoreException e) { // assume false
		}
		return null;
	}

	@Override
	public String getVMArguments(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ISourceLookupParticipant> getSourceLookupParticipants(ILaunchConfiguration configuration,
			ILaunch launch, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

}
