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
 */
package org.apache.geode.internal.cache;

import static org.apache.geode.distributed.ConfigurationProperties.LOCATORS;
import static org.apache.geode.distributed.ConfigurationProperties.NAME;
import static org.apache.geode.test.awaitility.GeodeAwaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.PartitionAttributesFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.test.dunit.AsyncInvocation;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.rules.GfshCommandRule;

public class RepeatedRecoveryDistributedTest implements Serializable {

  @Rule
  public ClusterStartupRule cluster = new ClusterStartupRule();

  @ClassRule
  public static GfshCommandRule gfsh = new GfshCommandRule();

  public static final String DISK_DIRECTORY_BASE_NAME = "/Users/jmelchior/Downloads/gem-3388/";

  @Test
  public void testRecovery() throws Exception {
    // Start the locator
    MemberVM locator = cluster.startLocatorVM(0);

    // Start the servers
    List<MemberVM> servers = new ArrayList<>();
    MemberVM bridgegemfire1 = cluster.startServerVM(1, locator.getPort());
    MemberVM bridgegemfire2 = cluster.startServerVM(2, locator.getPort());
    MemberVM bridgegemfire3 = cluster.startServerVM(3, locator.getPort());
    MemberVM bridgegemfire5 = cluster.startServerVM(5, locator.getPort());
    MemberVM bridgegemfire6 = cluster.startServerVM(6, locator.getPort());
    MemberVM bridgegemfire7 = cluster.startServerVM(7, locator.getPort());

    servers.add(bridgegemfire1);
    servers.add(bridgegemfire2);
    servers.add(bridgegemfire3);
    servers.add(bridgegemfire5);
    servers.add(bridgegemfire6);
    servers.add(bridgegemfire7);

    // Create the disk stores
    String diskStoreName = "diskStore1";
    bridgegemfire1.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_2_bridge3_disk_1"));
    bridgegemfire2.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_3_bridge4_disk_1"));
    bridgegemfire3.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_6_bridge7_disk_1"));
    bridgegemfire5.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_4_bridge5_disk_1"));
    bridgegemfire6.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_1_bridge2_disk_1"));
    bridgegemfire7.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_5_bridge6_disk_1"));

    // Asynchronously create the partitioned regions
    String regionName = "testRegion";
    createPartitionedRegion(servers, regionName, diskStoreName);

    // Verify the statistics
    servers.forEach(server -> server.invoke(() -> verifyPartitionedRegionStats(regionName)));

    // Disconnect the distributed system
    servers.forEach(server -> server.invoke(this::disconnectDistributedSystem));

    // Do offline compaction
    gfsh.connectAndVerify(locator);
    compactDiskStore(diskStoreName, DISK_DIRECTORY_BASE_NAME + "vm_1_bridge2_disk_1");
    compactDiskStore(diskStoreName, DISK_DIRECTORY_BASE_NAME + "vm_2_bridge3_disk_1");
    compactDiskStore(diskStoreName, DISK_DIRECTORY_BASE_NAME + "vm_3_bridge4_disk_1");
    compactDiskStore(diskStoreName, DISK_DIRECTORY_BASE_NAME + "vm_4_bridge5_disk_1");
    compactDiskStore(diskStoreName, DISK_DIRECTORY_BASE_NAME + "vm_5_bridge6_disk_1");
    compactDiskStore(diskStoreName, DISK_DIRECTORY_BASE_NAME + "vm_6_bridge7_disk_1");

    // Re-create cache
    bridgegemfire1.invoke(() -> recreateCache(1, locator.getPort()));
    bridgegemfire2.invoke(() -> recreateCache(2, locator.getPort()));
    bridgegemfire3.invoke(() -> recreateCache(3, locator.getPort()));
    bridgegemfire5.invoke(() -> recreateCache(5, locator.getPort()));
    bridgegemfire6.invoke(() -> recreateCache(6, locator.getPort()));
    bridgegemfire7.invoke(() -> recreateCache(7, locator.getPort()));

    // Re-create the disk stores
    bridgegemfire1.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_6_bridge7_disk_1"));
    bridgegemfire2.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_2_bridge3_disk_1"));
    bridgegemfire3.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_4_bridge5_disk_1"));
    bridgegemfire5.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_5_bridge6_disk_1"));
    bridgegemfire6.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_3_bridge4_disk_1"));
    bridgegemfire7.invoke(() -> createDiskStore(diskStoreName,
        DISK_DIRECTORY_BASE_NAME + "vm_1_bridge2_disk_1"));

    // Asynchronously re-create the partitioned regions
    createPartitionedRegion(servers, regionName, diskStoreName);

    // Re-verify statistics
    servers.forEach(server -> server.invoke(() -> verifyPartitionedRegionStats(regionName)));
    bridgegemfire1.invoke(() -> listBuckets(regionName));
  }

  private void createDiskStore(String diskStoreName, String diskDirectoryName) {
    CacheFactory.getAnyInstance()
        .createDiskStoreFactory()
        .setDiskDirs(new File[] {new File(diskDirectoryName)})
        .create(diskStoreName);
  }

  private void createPartitionedRegion(List<MemberVM> servers, String regionName,
      String diskStoreName)
      throws InterruptedException {
    List<AsyncInvocation> asyncInvocations = servers.stream()
        .map(server -> server.invokeAsync(() -> createRegion(regionName, diskStoreName)))
        .collect(Collectors.toList());

    // Wait for all the AsyncInvocations to complete
    for (AsyncInvocation asyncInvocation : asyncInvocations) {
      asyncInvocation.await();
    }
  }

  private void createRegion(String regionName, String diskStoreName) {
    PartitionAttributesFactory<Object, Object> paf = new PartitionAttributesFactory<>();
    paf.setRedundantCopies(3);
    CacheFactory.getAnyInstance()
        .createRegionFactory(RegionShortcut.PARTITION_PERSISTENT)
        .setDiskStoreName(diskStoreName)
        .setPartitionAttributes(paf.create())
        .create(regionName);
  }

  private void verifyPartitionedRegionStats(String regionName) {
    PartitionedRegion pr = (PartitionedRegion) CacheFactory.getAnyInstance().getRegion(regionName);
    PartitionedRegionStats prStats = pr.getPrStats();
    await().untilAsserted(() -> assertEquals(0, prStats.getLowRedundancyBucketCount()));
  }

  private void listBuckets(String regionName) {
    PartitionedRegion pr = (PartitionedRegion) CacheFactory.getAnyInstance().getRegion(regionName);
    IntStream.range(0, pr.getPartitionAttributes().getTotalNumBuckets()).forEach(bucketID -> {
      try {
        StringBuilder bucketString = new StringBuilder("bucket: " + bucketID);
        pr.getBucketOwnersForValidation(bucketID).forEach(owner -> bucketString.append(" <member: ")
            .append(owner[0]).append(" isPrimary: ").append(owner[1]).append(">"));
        System.out.println(bucketString);
      } catch (ForceReattemptException e) {
        e.printStackTrace();
      }
    });
  }

  private void disconnectDistributedSystem() {
    ClusterStartupRule.getCache().getDistributedSystem().disconnect();
  }

  private void recreateCache(int index, int locatorPort) {
    Properties properties = new Properties();
    properties.put(NAME, "server-" + index);
    properties.setProperty(LOCATORS, "localhost[" + locatorPort + "]");
    new CacheFactory(properties).create();
  }

  private void compactDiskStore(String diskStoreName, String diskDirectoryName) {
    gfsh.executeAndAssertThat("compact offline-disk-store --disk-dirs=" + diskDirectoryName
        + " --name=" + diskStoreName + " --max-oplog-size=1024").statusIsSuccess();
  }
}
