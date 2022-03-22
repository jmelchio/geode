/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.apache.geode.internal.statistics;

import static org.apache.geode.cache.execute.FunctionService.onServers;
import static org.apache.geode.distributed.ConfigurationProperties.LOG_FILE;
import static org.apache.geode.distributed.ConfigurationProperties.LOG_LEVEL;
import static org.apache.geode.distributed.ConfigurationProperties.SERIALIZABLE_OBJECT_FILTER;
import static org.apache.geode.distributed.ConfigurationProperties.STATISTIC_ARCHIVE_FILE;
import static org.apache.geode.test.awaitility.GeodeAwaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.rules.ClientCacheRule;
import org.apache.geode.test.junit.rules.MemberStarterRule;
import org.apache.geode.test.junit.rules.VMProvider;

public class HostSamplerStopDUnitTest {
  private static final String TEST_STATS_FILE_NAME = "testStats.gfs";
  private static Function<Object> hangFunction;
  private MemberVM serverVM1;

  @Rule
  public ClusterStartupRule clusterStartupRule = new ClusterStartupRule().withLogFile();

  @Rule
  public ClientCacheRule clientCacheRule = new ClientCacheRule();

  @Before
  public void setup() {
    MemberVM locatorVM = clusterStartupRule.startLocatorVM(0, MemberStarterRule::withAutoStart);
    int locatorPort = locatorVM.getPort();

    Properties props = new Properties();
    props.setProperty(STATISTIC_ARCHIVE_FILE, TEST_STATS_FILE_NAME);
    props.setProperty(SERIALIZABLE_OBJECT_FILTER, "org.apache.geode.internal.statistics.*");
    props.setProperty(LOG_FILE, "system.log");
    props.setProperty(LOG_LEVEL, "info");

    serverVM1 = clusterStartupRule.startServerVM(1, props, locatorPort);

    VMProvider.invokeInEveryMember(
        () -> Objects.requireNonNull(ClusterStartupRule.getCache()).createRegionFactory(
            RegionShortcut.REPLICATE).create("region"),
        serverVM1);

    VMProvider.invokeInEveryMember(() -> {
      InternalCache cache = ClusterStartupRule.getCache();
      InternalDistributedSystem system = cache.getInternalDistributedSystem();
      GemFireStatSampler sampler = system.getStatSampler();
      assertThat(sampler.isSamplingEnabled()).isTrue();
      assertThat(sampler.getSampleRate()).isEqualTo(1000);
      assertThat(sampler.isAlive()).isTrue();
      assertThat(sampler.getArchiveFileName().getName()).isEqualTo(TEST_STATS_FILE_NAME);
      assertThat(sampler.getArchiveFileName()).exists();
      assertThat(sampler.getArchiveFileName()).isWritable();
      System.out.println("joris: " + sampler.getArchiveFileName().getAbsolutePath());
    }, serverVM1);

    VMProvider.invokeInEveryMember(() -> await("waiting for sample collector to exist")
        .until(() -> ClusterStartupRule.getCache()
            .getInternalDistributedSystem().getStatSampler().getSampleCollector() != null),
        serverVM1);

    clientCacheRule
        .withPoolSubscription(true)
        .withLocatorConnection(locatorPort);
  }

  @After
  public void cleanup() {

  }

  @Test
  public void HostStatSamplerStopFlushesStatsToFile() throws Exception {
    ClientCache clientCache = clientCacheRule.createCache();
    hangFunction = new StatTestFunctions.HangFunction();

    CompletableFuture<ResultCollector> rc = CompletableFuture
        .supplyAsync(() -> onServers(clientCache.getDefaultPool()).execute(hangFunction));

    VMProvider.invokeInEveryMember(() -> {
      SampleCollector sampleCollector = ClusterStartupRule.getCache().getInternalDistributedSystem()
          .getStatSampler().getSampleCollector();
      assertThat(sampleCollector.currentHandlersForTesting())
          .hasAtLeastOneElementOfType(SampleHandler.class);
      for (SampleHandler sampleHandler : sampleCollector.currentHandlersForTesting()) {
        System.out.println("joris:" + sampleHandler.toString());
      }
    }, serverVM1);

    ResultCollector results = rc.join();
    System.out.println("joris - results: " + results.getResult());
  }

}
