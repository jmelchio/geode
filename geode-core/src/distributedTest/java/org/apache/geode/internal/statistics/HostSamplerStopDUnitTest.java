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

import java.util.Objects;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.test.dunit.rules.ClientCacheRule;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.rules.MemberStarterRule;
import org.apache.geode.test.junit.rules.VMProvider;

public class HostSamplerStopDUnitTest {
  private MemberVM serverVM1;
  private MemberVM serverVM2;

  @Rule
  public ClusterStartupRule clusterStartupRule = new ClusterStartupRule();

  @Rule
  public ClientCacheRule clientCacheRule = new ClientCacheRule();

  @Before
  public void setup() {
    MemberVM locatorVM = clusterStartupRule.startLocatorVM(0, MemberStarterRule::withAutoStart);
    int locatorPort = locatorVM.getPort();

    serverVM1 = clusterStartupRule.startServerVM(1, locatorPort);
    serverVM2 = clusterStartupRule.startServerVM(2, locatorPort);

    VMProvider.invokeInEveryMember(
        () -> Objects.requireNonNull(ClusterStartupRule.getCache()).createRegionFactory(
            RegionShortcut.REPLICATE).create("region"),
        serverVM1, serverVM2);


  }

  @After
  public void cleanup() {

  }

  @Test
  public void HostStatSamplerStopFlushesStatsToFile() {

  }
}
