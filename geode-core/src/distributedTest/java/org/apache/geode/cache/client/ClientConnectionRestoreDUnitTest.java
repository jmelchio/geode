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

package org.apache.geode.cache.client;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.PartitionAttributesFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.test.awaitility.GeodeAwaitility;
import org.apache.geode.test.dunit.AsyncInvocation;
import org.apache.geode.test.dunit.SerializableConsumerIF;
import org.apache.geode.test.dunit.rules.ClientVM;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.rules.MemberStarterRule;
import org.apache.geode.test.junit.rules.VMProvider;

public class ClientConnectionRestoreDUnitTest {
  private static final String REGION_REPLICATE_BASENAME = "regionReplicate";
  private static final String REGION_PARTITION_BASENAME = "regionPartition";
  private static final String LOG_PREFIX = "GEM-3617";
  private ClientVM[] clientVMS;
  private int locator0Port;
  private int locator1Port;
  private static final int KEY_SET_SIZE = 10000;
  private static final int READ_TIMEOUT = 10000;

  @Rule
  public ClusterStartupRule clusterStartupRule = new ClusterStartupRule(11);

//  private MemberVM server4;
//  private MemberVM server3;
  private MemberVM server2;
  private MemberVM server1;
  private MemberVM server0;

  @Before
  public void init() throws Exception {
    MemberVM locator0 = clusterStartupRule.startLocatorVM(0, MemberStarterRule::withAutoStart);
    locator0Port = locator0.getPort();
    MemberVM locator1 = clusterStartupRule.startLocatorVM(1, null, locator0Port);
    locator1Port = locator1.getPort();

    server0 = clusterStartupRule.startServerVM(2, locator0Port, locator1Port);
    server1 = clusterStartupRule.startServerVM(3, locator0Port, locator1Port);
    server2 = clusterStartupRule.startServerVM(4, locator0Port, locator1Port);
//    server3 = clusterStartupRule.startServerVM(5, locator0Port, locator1Port);
//    server4 = clusterStartupRule.startServerVM(6, locator0Port, locator1Port);

    int l0Port = locator0Port;
    int l1Port = locator1Port;

    SerializableConsumerIF<ClientCacheFactory> cacheSetup = cf -> {
      cf.addPoolLocator("localhost", l0Port);
      cf.addPoolLocator("localhost", l1Port);
      // cf.setPoolReadTimeout(READ_TIMEOUT);
    };

    Properties clientProps = new Properties();

    ClientVM client0 =
        clusterStartupRule.startClientVM(5, clientProps, cacheSetup);
    ClientVM client1 =
        clusterStartupRule.startClientVM(6, clientProps, cacheSetup);
    ClientVM client2 =
        clusterStartupRule.startClientVM(7, clientProps, cacheSetup);
    ClientVM client3 =
        clusterStartupRule.startClientVM(8, clientProps, cacheSetup);
    ClientVM client4 =
        clusterStartupRule.startClientVM(9, clientProps, cacheSetup);

    clientVMS = new ClientVM[5];
    clientVMS[0] = client0;
    clientVMS[1] = client1;
    clientVMS[2] = client2;
    clientVMS[3] = client3;
    clientVMS[4] = client4;

    // create regions on each server, one of each type for each client
    VMProvider.invokeInEveryMember(() -> {
      InternalCache cache = ClusterStartupRule.getCache();

      IntStream.range(0, 5).forEach(count -> {
        RegionFactory<Integer, Integer> replicateRegionFactory =
            cache.createRegionFactory(RegionShortcut.REPLICATE);
        replicateRegionFactory.create(REGION_REPLICATE_BASENAME + count);

        PartitionAttributesFactory<Integer, Integer> paf = new PartitionAttributesFactory<>();
        paf.setRedundantCopies(1);
        cache.createRegionFactory(RegionShortcut.PARTITION).setPartitionAttributes(paf.create())
            .create(REGION_PARTITION_BASENAME + count);
      });
      System.out
          .println(LOG_PREFIX + ": serverName: " + cache.getInternalDistributedSystem().getName());
    }, server0, server1, server2);

    // on each client create the proxies for the regions they are interested in
    IntStream.range(0, 5).forEach(count -> clientVMS[count].invoke(() -> {
      ClientCache clientCache = ClusterStartupRule.getClientCache();
      clientCache.createClientRegionFactory(ClientRegionShortcut.PROXY)
          .create(REGION_PARTITION_BASENAME + count);
      clientCache.createClientRegionFactory(ClientRegionShortcut.PROXY)
          .create(REGION_REPLICATE_BASENAME + count);
    }));

  }

  @After
  public void cleanup() {

  }

  @Test
  public void serverShutdownDoesNotCauseConnectionIssuesForClientsDuringOperations() {
//    seedBuckets(REGION_PARTITION_BASENAME);
//    seedBuckets(REGION_REPLICATE_BASENAME);

    List<AsyncInvocation> clientRegionInvocations = startPuts(REGION_PARTITION_BASENAME);
    clientRegionInvocations.addAll(startPuts(REGION_REPLICATE_BASENAME));

    long startTS = System.currentTimeMillis();
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    // disconnect server
    String serverName = server2.invoke(() -> {
      InternalDistributedSystem internalDistributedSystem =
          ClusterStartupRule.getCache().getInternalDistributedSystem();
      String sName = internalDistributedSystem.getName();
      internalDistributedSystem.getCache().close();
      internalDistributedSystem.disconnect();
      return sName;
    });

    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    int l0Port = locator0Port;
    int l1Port = locator1Port;

    // re-create the cache on the server
    server2.invoke(() -> {
      Properties properties = new Properties();
      properties.put("name", serverName);
      properties.put("locators", "localhost[" + l0Port + "],localhost[" + l1Port + "]");
      new CacheFactory(properties).create();
    });

    try {
      Thread.sleep(8000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    GeodeAwaitility.await()
        .until(() -> clientRegionInvocations.stream().allMatch(AsyncInvocation::isDone));

    boolean exceptionOccurred = clientRegionInvocations.stream().anyMatch(asyncInvocation -> asyncInvocation.exceptionOccurred());
    if (exceptionOccurred) {
      System.out.println(LOG_PREFIX + " stuff happened");
      dumpThreads();
    }

    for (AsyncInvocation asyncInvocation : clientRegionInvocations) {
      Assertions.assertThatNoException().isThrownBy(asyncInvocation::get);
    }
  }

  private List<AsyncInvocation> startPuts(String regionType) {

    List<AsyncInvocation> invocations = IntStream.range(0, 5).mapToObj(count -> clientVMS[count].invokeAsync(() -> {
      String regionName = regionType + count;
      ClientCache clientCache = ClusterStartupRule.getClientCache();
      System.out.println(LOG_PREFIX + " readTimeout: " + clientCache.getDefaultPool().getReadTimeout());
      Region<Integer, Integer> region = clientCache.getRegion(regionName);
      int key = -1;
      Integer value;
      long run_until = System.currentTimeMillis() + 20000;

      System.out.println(LOG_PREFIX + ": start " + "[client" + count + "]");
      do {
        key = (key + 1) % KEY_SET_SIZE;
        try {
          value = region.get(key);
          if (value == null) {
            value = 0;
          }
          value++;
          region.put(key, value);
        } catch (Throwable unexpected) {
          // Report the unexpected exception and stop doing operations.
          System.out.println(
              LOG_PREFIX + ": exception key: " + key + " [client" + count + "] " + unexpected);
          throw unexpected;
        }
      } while (System.currentTimeMillis() < run_until);
      System.out.println(LOG_PREFIX + ": finished " + "key: " + key + " [client" + count + "]"
          + " [region: " + regionName + "]");
    })).collect(Collectors.toList());

    return invocations;
  }

  private void seedBuckets(String regionType) {
    IntStream.range(0, 5).forEach(count -> clientVMS[count].invoke(() -> {
      String regionName = regionType + count;
      ClientCache clientCache = ClusterStartupRule.getClientCache();
      Region<Integer, Integer> region = clientCache.getRegion(regionName);
      int key = -1;
      Integer value;

      System.out.println(LOG_PREFIX + ": start " + "[client" + count + "]");
      do {
        key = key + 1;
        try {
          value = region.get(key);
          if (value == null) {
            value = 0;
          }
          value++;
          region.put(key, value);
        } catch (Throwable unexpected) {
          // Report the unexpected exception and stop doing operations.
          System.out.println(
              LOG_PREFIX + ": exception key: " + key + " [client" + count + "] " + unexpected);
          throw unexpected;
        }
      } while (key < 113);
    }));

  }

  private void getPutValue(int count, Region<Integer, Integer> region, int key) {
    Integer value;
    try {
      value = region.get(key);
      if (value == null) {
        value = 0;
      }
      value++;
      region.put(key, value);
    } catch (Throwable unexpected) {
      // Report the unexpected exception and stop doing operations.
      System.out.println(
          LOG_PREFIX + ": exception key: " + key + " [client" + count + "] " + unexpected);
      throw unexpected;
    }
  }

  private void dumpThreads() {
    VMProvider.invokeInEveryMember(() -> {
      StringBuilder stringBuilder = new StringBuilder(ClusterStartupRule.getCache().getInternalDistributedSystem().getName()).append("\n");
      final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
      final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
      System.out.println(LOG_PREFIX + " dumping threads");

      Arrays.stream(threadInfos).forEach(threadInfo -> {
        stringBuilder.append("<")
            .append(threadInfo.getThreadName())
            .append(">")
            .append(" tid=")
            .append(threadInfo.getThreadId())
            .append(" Thread State:")
            .append(threadInfo.getThreadState())
            .append("\n");
        Arrays.stream(threadInfo.getStackTrace()).forEach(stackTraceElement -> stringBuilder
            .append("  ")
            .append(stackTraceElement)
            .append("\n"));
        stringBuilder.append("\n");
      });

      System.out.println(stringBuilder);
    }, server0, server1, server2);
  }

}
