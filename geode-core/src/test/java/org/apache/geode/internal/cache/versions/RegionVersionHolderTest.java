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
package org.apache.geode.internal.cache.versions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.internal.cache.persistence.DiskStoreID;

public class RegionVersionHolderTest {
  @Test
  public void setRegionSynchronizeScheduledCanSetSyncScheduledOrDone() {
    DiskStoreID server = new DiskStoreID(0, 0);
    RegionVersionHolder holder = new RegionVersionHolder(server);

    holder.setRegionSynchronizeScheduled();
    assertThat(holder.setRegionSynchronizeScheduledOrDoneIfNot()).isFalse();
  }

  @Test
  public void setRegionSynchronizeScheduledOrDoneIfNotReturnsTrueIfSyncScheduledNotSet() {
    DiskStoreID server = new DiskStoreID(0, 0);
    RegionVersionHolder holder = new RegionVersionHolder(server);

    assertThat(holder.setRegionSynchronizeScheduledOrDoneIfNot()).isTrue();
    assertThat(holder.setRegionSynchronizeScheduledOrDoneIfNot()).isFalse();
  }
}
