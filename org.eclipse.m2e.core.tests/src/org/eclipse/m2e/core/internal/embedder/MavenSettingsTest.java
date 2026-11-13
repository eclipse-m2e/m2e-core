/*******************************************************************************
 * Copyright (c) 2026 Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.SettingsBuilder;

import org.eclipse.m2e.core.embedder.MavenSettingsLocations;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class MavenSettingsTest {

  private static final long TIMEOUT_SECONDS = 10;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void settingsLookupDoesNotInvertContainerLockOrder() throws Exception {
    // Model the container monitor with an interruptible lock so a regression can
    // be cancelled rather than leaving permanently deadlocked test threads.
    ReentrantLock containerLock = new ReentrantLock();
    CountDownLatch containerLocked = new CountDownLatch(1);
    CountDownLatch lookupStarted = new CountDownLatch(1);
    AtomicInteger builds = new AtomicInteger();
    SettingsBuilder builder = countingBuilder(builds);
    MavenImpl maven = new MavenImpl() {
      @Override
      public <T> T lookup(Class<T> type) throws CoreException {
        lookupStarted.countDown();
        try {
          containerLock.lockInterruptibly();
          try {
            return type.cast(builder);
          } finally {
            containerLock.unlock();
          }
        } catch(InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new CoreException(Status.error("Interrupted test lookup", e));
        }
      }
    };
    MavenSettingsLocations locations = new MavenSettingsLocations(null, null);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    Future<Settings> containerCreation = null;
    Future<Settings> settingsRead = null;
    try {
      containerCreation = executor.submit(() -> {
        containerLock.lockInterruptibly();
        try {
          containerLocked.countDown();
          await(lookupStarted);
          return maven.getSettings(locations);
        } finally {
          containerLock.unlock();
        }
      });
      await(containerLocked);
      settingsRead = executor.submit(() -> maven.getSettings(locations));

      // Before the fix, settingsRead holds MavenSettings while waiting for the
      // container lock, and containerCreation holds that lock while requesting settings.
      Settings settings = containerCreation.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      assertSame(settings, settingsRead.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      assertEquals("Concurrent initial reads must build settings once", 1, builds.get());
    } finally {
      if(settingsRead != null) {
        settingsRead.cancel(true);
      }
      if(containerCreation != null) {
        containerCreation.cancel(true);
      }
      executor.shutdownNow();
      assertTrue("Test workers did not terminate", executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
  }

  @Test
  public void unchangedSettingsAreCached() throws Exception {
    AtomicInteger builds = new AtomicInteger();
    MavenImpl maven = maven(countingBuilder(builds));
    File user = temporaryFolder.newFile("settings.xml");
    writeSettings(user, "first");
    MavenSettingsLocations locations = new MavenSettingsLocations(null, user);

    Settings first = maven.getSettings(locations);
    assertEquals("first", first.getLocalRepository());
    assertSame(first, maven.getSettings(locations));
    assertEquals(1, builds.get());
  }

  @Test
  public void cachedSettingsDoNotRequireAnotherComponentLookup() throws Exception {
    AtomicBoolean rejectLookup = new AtomicBoolean();
    SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
    MavenImpl maven = new MavenImpl() {
      @Override
      public <T> T lookup(Class<T> type) throws CoreException {
        if(rejectLookup.get()) {
          throw new CoreException(Status.error("Unexpected lookup for cached settings"));
        }
        return type.cast(builder);
      }
    };
    MavenSettingsLocations locations = new MavenSettingsLocations(null, null);
    Settings cached = maven.getSettings(locations);

    rejectLookup.set(true);
    assertSame(cached, maven.getSettings(locations));
  }

  @Test
  public void fileMetadataIsRecheckedAfterComponentLookup() throws Exception {
    AtomicInteger builds = new AtomicInteger();
    SettingsBuilder builder = countingBuilder(builds);
    CountDownLatch lookupStarted = new CountDownLatch(1);
    CountDownLatch releaseLookup = new CountDownLatch(1);
    MavenImpl maven = new MavenImpl() {
      @Override
      public <T> T lookup(Class<T> type) throws CoreException {
        lookupStarted.countDown();
        try {
          await(releaseLookup);
        } catch(InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new CoreException(Status.error("Interrupted test lookup", e));
        }
        return type.cast(builder);
      }
    };
    File user = temporaryFolder.newFile("settings.xml");
    writeSettings(user, "first");
    MavenSettingsLocations locations = new MavenSettingsLocations(null, user);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Settings> read = executor.submit(() -> maven.getSettings(locations));
      await(lookupStarted);
      writeSettings(user, "changed-during-lookup");
      releaseLookup.countDown();

      Settings settings = read.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      assertEquals("changed-during-lookup", settings.getLocalRepository());
      assertSame(settings, maven.getSettings(locations));
      assertEquals("Metadata captured before lookup must not cause an unnecessary rebuild", 1, builds.get());
    } finally {
      releaseLookup.countDown();
      executor.shutdownNow();
      assertTrue("Test worker did not terminate", executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
  }

  @Test
  public void settingsReloadWhenUserFileLengthChanges() throws Exception {
    AtomicInteger builds = new AtomicInteger();
    MavenImpl maven = maven(countingBuilder(builds));
    File user = temporaryFolder.newFile("settings.xml");
    writeSettings(user, "first");
    MavenSettingsLocations locations = new MavenSettingsLocations(null, user);
    Settings first = maven.getSettings(locations);
    FileTime originalTimestamp = Files.getLastModifiedTime(user.toPath());

    writeSettings(user, "a-longer-repository");
    Files.setLastModifiedTime(user.toPath(), originalTimestamp);
    Settings second = maven.getSettings(locations);
    assertNotSame(first, second);
    assertEquals("a-longer-repository", second.getLocalRepository());
    assertSame(second, maven.getSettings(locations));
    assertEquals(2, builds.get());
  }

  @Test
  public void settingsReloadWhenGlobalFileTimestampChanges() throws Exception {
    AtomicInteger builds = new AtomicInteger();
    MavenImpl maven = maven(countingBuilder(builds));
    File global = temporaryFolder.newFile("settings.xml");
    writeSettings(global, "first");
    MavenSettingsLocations locations = new MavenSettingsLocations(global, null);
    Settings first = maven.getSettings(locations);
    long originalTimestamp = global.lastModified();
    long originalLength = global.length();

    writeSettings(global, "other");
    Files.setLastModifiedTime(global.toPath(), FileTime.fromMillis(originalTimestamp + 2000));
    assertEquals(originalLength, global.length());
    assertTrue("Settings timestamp must change", originalTimestamp != global.lastModified());
    Settings second = maven.getSettings(locations);
    assertNotSame(first, second);
    assertEquals("other", second.getLocalRepository());
    assertSame(second, maven.getSettings(locations));
    assertEquals(2, builds.get());
  }

  @Test
  public void settingsReloadAfterFileDeletionAndRecreation() throws Exception {
    AtomicInteger builds = new AtomicInteger();
    MavenImpl maven = maven(countingBuilder(builds));
    File user = temporaryFolder.newFile("settings.xml");
    writeSettings(user, "first");
    MavenSettingsLocations locations = new MavenSettingsLocations(null, user);
    Settings first = maven.getSettings(locations);

    Files.delete(user.toPath());
    Settings missing = maven.getSettings(locations);
    assertNotSame(first, missing);
    assertSame(missing, maven.getSettings(locations));
    writeSettings(user, "recreated");
    Settings recreated = maven.getSettings(locations);
    assertEquals("recreated", recreated.getLocalRepository());
    assertEquals(3, builds.get());
  }

  @Test
  public void malformedSettingsFallbackIsCachedUntilFileChanges() throws Exception {
    AtomicInteger builds = new AtomicInteger();
    MavenImpl maven = maven(countingBuilder(builds));
    File user = temporaryFolder.newFile("settings.xml");
    Files.writeString(user.toPath(), "<settings><broken>");
    MavenSettingsLocations locations = new MavenSettingsLocations(null, user);

    Settings fallback = maven.getSettings(locations);
    assertSame(fallback, maven.getSettings(locations));
    assertEquals(1, builds.get());
    writeSettings(user, "repaired");
    assertEquals("repaired", maven.getSettings(locations).getLocalRepository());
    assertEquals(2, builds.get());
  }

  private static MavenImpl maven(SettingsBuilder builder) {
    return new MavenImpl() {
      @Override
      public <T> T lookup(Class<T> type) {
        return type.cast(builder);
      }
    };
  }

  private static SettingsBuilder countingBuilder(AtomicInteger builds) {
    SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
    return request -> {
      builds.incrementAndGet();
      return builder.build(request);
    };
  }

  private static void writeSettings(File file, String repository) throws Exception {
    Files.writeString(file.toPath(), "<settings><localRepository>" + repository + "</localRepository></settings>");
  }

  private static void await(CountDownLatch latch) throws InterruptedException {
    assertTrue("Timed out waiting for test coordination", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
  }
}
