package org.eclipse.m2e.pde.target;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eclipse.m2e.pde.target.shared.DependencyDepth;
import org.osgi.framework.Constants;

public class OverridePreconditionChecker {

	/**
	 * Validates preconditions for using override instructions on a bundle.
	 *
	 * @param originalSymbolicName the bundle symbolic name from the original
	 *                             manifest; may be {@code null} if the artifact is
	 *                             not a bundle
	 * @param rootDependecies      the root dependencies of the artifact; must
	 *                             contain exactly one entry
	 * @param instructions         collection of {@link BNDInstructions}; must
	 *                             contain exactly one entry with a symbolic name
	 *                             different from {@code originalSymbolicName}
	 * @param dependencyDepth      the dependency depth; must be
	 *                             {@link DependencyDepth#NONE}
	 * @return an error message if any precondition fails, otherwise {@code null}
	 */
	public static String checkOverridePreconditions(String originalSymbolicName,
			List<MavenTargetDependency> rootDependecies, Collection<BNDInstructions> instructions,
			DependencyDepth dependencyDepth) {
		if (dependencyDepth != DependencyDepth.NONE) {
			return "The dependency depth must be none!";
		}
		if (originalSymbolicName == null) {
			return "The artifact is no bundle.";
		}

		if (rootDependecies.size() != 1) {
			return "The location must contain exactly one root dependency.";
		}

		if (instructions.size() != 1) {
			return "The location must contain exactly one bnd instruction which must contain a symbolic name that differs from the original one.";
		}
		BNDInstructions instruction = instructions.iterator().next();
		if (!isSymbolicNameDefinedAndDiffers(instruction.asProperties(), originalSymbolicName)) {
			return "The symbolic name in the bnd instructions must be defined and differ from the original one.";
		}
		return null;
	}

	private static boolean isSymbolicNameDefinedAndDiffers(Properties properties, String originalSymbolicName) {
		if (properties == null) {
			return false;
		}
		Object definedSymbolicName = properties.get(Constants.BUNDLE_SYMBOLICNAME);
		return definedSymbolicName != null && !definedSymbolicName.equals(originalSymbolicName);
	}

}