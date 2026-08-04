/*******************************************************************************
 * Copyright (c) 2026 Tyce Herrman and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;

import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

import org.junit.Test;


@SuppressWarnings("restriction")
public class PlexusContainerManagerTest extends AbstractMavenProjectTestCase {

  private static final long TIMEOUT_SECONDS = 10;

  @Test
  public void rootedCreationDoesNotBlockNonRootedCreation() throws Exception {
    File rootA = createMavenRoot("root-a");
    File rootB = createMavenRoot("root-b");
    File canonicalRootA = rootA.getCanonicalFile();
    File canonicalRootB = rootB.getCanonicalFile();
    TestContainer containerA = new TestContainer(canonicalRootA);
    TestContainer containerB = new TestContainer(canonicalRootB);
    TestContainer nonRootedContainer = new TestContainer(null);
    CountDownLatch creationStarted = new CountDownLatch(1);
    CountDownLatch releaseCreation = new CountDownLatch(1);
    PlexusContainerManager manager = new PlexusContainerManager((directory, logger, configuration) -> {
      if(canonicalRootA.equals(directory)) {
        creationStarted.countDown();
        await(releaseCreation);
        return containerA;
      }
      if(canonicalRootB.equals(directory)) {
        return containerB;
      }
      if(directory == null) {
        return nonRootedContainer;
      }
      throw new AssertionError("Unexpected root " + directory);
    });
    ExecutorService executor = Executors.newFixedThreadPool(3);
    try {
      Future<IMavenPlexusContainer> rootACreation = executor.submit(() -> manager.aquire(rootA));
      await(creationStarted);

      Future<IMavenPlexusContainer> nonRootedCreation = executor.submit(() -> manager.aquire());
      Future<IMavenPlexusContainer> rootBCreation = executor.submit(() -> manager.aquire(rootB));

      assertSame(nonRootedContainer, nonRootedCreation.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      assertSame(containerB, rootBCreation.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      assertFalse(rootACreation.isDone());

      releaseCreation.countDown();
      assertSame(containerA, rootACreation.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    } finally {
      releaseCreation.countDown();
      shutdown(executor);
      manager.dispose();
      FileUtils.deleteDirectory(rootA);
      FileUtils.deleteDirectory(rootB);
    }
  }

  @Test
  public void concurrentAcquisitionForSameRootCreatesOneContainer() throws Exception {
    File root = createMavenRoot("same-root");
    File canonicalRoot = root.getCanonicalFile();
    TestContainer container = new TestContainer(canonicalRoot);
    AtomicInteger factoryInvocations = new AtomicInteger();
    CountDownLatch creationStarted = new CountDownLatch(1);
    CountDownLatch releaseCreation = new CountDownLatch(1);
    CountDownLatch callersReady = new CountDownLatch(2);
    CountDownLatch startCallers = new CountDownLatch(1);
    PlexusContainerManager manager = new PlexusContainerManager((directory, logger, configuration) -> {
      assertEquals(canonicalRoot, directory);
      factoryInvocations.incrementAndGet();
      creationStarted.countDown();
      await(releaseCreation);
      return container;
    });
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<IMavenPlexusContainer> first = executor.submit(() -> {
        callersReady.countDown();
        await(startCallers);
        return manager.aquire(root);
      });
      Future<IMavenPlexusContainer> second = executor.submit(() -> {
        callersReady.countDown();
        await(startCallers);
        return manager.aquire(root);
      });
      await(callersReady);
      startCallers.countDown();
      await(creationStarted);

      releaseCreation.countDown();
      assertSame(container, first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      assertSame(container, second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      assertEquals(1, factoryInvocations.get());
    } finally {
      startCallers.countDown();
      releaseCreation.countDown();
      shutdown(executor);
      manager.dispose();
      FileUtils.deleteDirectory(root);
    }
  }

  @Test
  public void failedCreationCanBeRetried() throws Exception {
    File root = createMavenRoot("retry");
    Exception failure = new Exception("Expected creation failure");
    TestContainer container = new TestContainer(root.getCanonicalFile());
    AtomicInteger factoryInvocations = new AtomicInteger();
    PlexusContainerManager manager = new PlexusContainerManager((directory, logger, configuration) -> {
      if(factoryInvocations.getAndIncrement() == 0) {
        throw failure;
      }
      return container;
    });
    try {
      try {
        manager.aquire(root);
        fail("Expected container creation to fail");
      } catch(Exception e) {
        assertSame(failure, e);
      }

      assertSame(container, manager.aquire(root));
      assertEquals(2, factoryInvocations.get());
    } finally {
      manager.dispose();
      FileUtils.deleteDirectory(root);
    }
  }

  @Test
  public void cleanupDisposesStaleContainerOnce() throws Exception {
    File root = createMavenRoot("cleanup");
    TestContainer first = new TestContainer(root.getCanonicalFile());
    TestContainer second = new TestContainer(root.getCanonicalFile());
    AtomicInteger factoryInvocations = new AtomicInteger();
    PlexusContainerManager manager = new PlexusContainerManager((directory, logger, configuration) ->
        factoryInvocations.getAndIncrement() == 0 ? first : second);
    try {
      assertSame(first, manager.aquire(root));
      Files.delete(new File(root, IMavenPlexusContainer.MVN_FOLDER).toPath());

      manager.cleanup();
      manager.cleanup();
      first.verifyDisposedOnce();

      Files.createDirectory(new File(root, IMavenPlexusContainer.MVN_FOLDER).toPath());
      assertSame(second, manager.aquire(root));
      assertNotSame(first, second);

      manager.dispose();
      first.verifyDisposedOnce();
      second.verifyDisposedOnce();
    } finally {
      manager.dispose();
      FileUtils.deleteDirectory(root);
    }
  }

  @Test
  public void deactivationHandlesInFlightCreation() throws Exception {
    File root = createMavenRoot("deactivate");
    TestContainer container = new TestContainer(root.getCanonicalFile());
    CountDownLatch creationStarted = new CountDownLatch(1);
    CountDownLatch releaseCreation = new CountDownLatch(1);
    PlexusContainerManager manager = new PlexusContainerManager((directory, logger, configuration) -> {
      creationStarted.countDown();
      await(releaseCreation);
      return container;
    });
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<IMavenPlexusContainer> creation = executor.submit(() -> manager.aquire(root));
      await(creationStarted);

      manager.dispose();
      releaseCreation.countDown();
      assertFutureFailure(creation, IllegalStateException.class);
      container.verifyDisposedOnce();

      try {
        manager.aquire(root);
        fail("Expected acquisition after deactivation to fail");
      } catch(IllegalStateException expected) {
        // expected
      }
    } finally {
      releaseCreation.countDown();
      shutdown(executor);
      manager.dispose();
      FileUtils.deleteDirectory(root);
    }
    container.verifyDisposedOnce();
  }

  private static File createMavenRoot(String name) throws Exception {
    File root = Files.createTempDirectory(PlexusContainerManagerTest.class.getSimpleName() + '-' + name).toFile();
    Files.createDirectory(new File(root, IMavenPlexusContainer.MVN_FOLDER).toPath());
    return root;
  }

  private static void await(CountDownLatch latch) throws InterruptedException {
    assertTrue("Timed out waiting for test coordination", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
  }

  private static void shutdown(ExecutorService executor) throws InterruptedException {
    executor.shutdownNow();
    assertTrue("Executor did not terminate", executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
  }

  private static void assertFutureFailure(Future<?> future, Class<? extends Throwable> expected) throws Exception {
    try {
      future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      fail("Expected future to fail with " + expected.getSimpleName());
    } catch(ExecutionException e) {
      assertTrue("Expected " + expected.getName() + " but got " + e.getCause(), expected.isInstance(e.getCause()));
    }
  }

  private static final class TestContainer implements IMavenPlexusContainer {

    private final Optional<File> mavenDirectory;

    private final ClassWorld classWorld;

    private final PlexusContainer container;

    private final AtomicInteger disposeInvocations = new AtomicInteger();

    TestContainer(File mavenDirectory) throws Exception {
      this.mavenDirectory = Optional.ofNullable(mavenDirectory);
      classWorld = new ClassWorld("plexus.core", PlexusContainerManagerTest.class.getClassLoader());
      var containerRealm = classWorld.getRealm("plexus.core");
      container = (PlexusContainer) Proxy.newProxyInstance(PlexusContainer.class.getClassLoader(),
          new Class<?>[] {PlexusContainer.class}, (proxy, method, arguments) -> switch(method.getName()) {
            case "getContainerRealm" -> containerRealm;
            case "dispose" -> {
              disposeInvocations.incrementAndGet();
              classWorld.close();
              yield null;
            }
            case "toString" -> "TestPlexusContainer";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> arguments != null && proxy == arguments[0];
            default -> throw new AssertionError("Unexpected PlexusContainer method " + method);
          });
    }

    @Override
    public Optional<File> getMavenDirectory() {
      return mavenDirectory;
    }

    @Override
    public PlexusContainer getContainer() {
      return container;
    }

    @Override
    public IComponentLookup getComponentLookup() {
      throw new UnsupportedOperationException();
    }

    void verifyDisposedOnce() {
      assertEquals(1, disposeInvocations.get());
    }

  }

}
