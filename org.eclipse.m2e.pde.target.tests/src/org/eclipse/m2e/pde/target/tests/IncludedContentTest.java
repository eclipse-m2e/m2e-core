package org.eclipse.m2e.pde.target.tests;

import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.junit.Test;

public class IncludedContentTest extends AbstractMavenTargetTest {
	@Test
	public void testIncludeProvided() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="provided" includeSource="false" missingManifest="ignore" type="Maven">
							<dependencies>
								<dependency>
									<groupId>org.osgi</groupId>
									<artifactId>org.osgi.test.common</artifactId>
									<version>1.3.0</version>
									<type>jar</type>
								</dependency>
							</dependencies>
						</location>
						""");
		assertStatusOk(getTargetStatus(target));
		TargetBundle[] allBundles = target.getBundles();
		for (TargetBundle targetBundle : allBundles) {
			System.out.println(targetBundle);
		}
	}
}
