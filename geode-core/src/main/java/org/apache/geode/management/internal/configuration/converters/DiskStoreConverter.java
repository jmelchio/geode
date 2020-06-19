/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geode.management.internal.configuration.converters;

import java.util.stream.Collectors;

import org.apache.geode.cache.configuration.DiskDirType;
import org.apache.geode.cache.configuration.DiskStoreType;
import org.apache.geode.management.configuration.DiskDir;
import org.apache.geode.management.configuration.DiskStore;

public class DiskStoreConverter extends ConfigurationConverter<DiskStore, DiskStoreType> {
  @Override
  protected DiskStore fromNonNullXmlObject(DiskStoreType xmlObject) {
    DiskStore diskStore = new DiskStore();

    diskStore.setName(xmlObject.getName());
    diskStore.setId(xmlObject.getId());
    diskStore.setCompactionThreshold(xmlObject.getCompactionThreshold());
    diskStore.setDiskUsageCriticalPercentage(xmlObject.getDiskUsageCriticalPercentage());
    diskStore.setDiskUsageWarningPercentage(xmlObject.getDiskUsageWarningPercentage());
    diskStore.setMaxOplogSize(xmlObject.getMaxOplogSize());
    diskStore.setQueueSize(xmlObject.getQueueSize());
    diskStore.setTimeInterval(xmlObject.getTimeInterval());
    diskStore.setWriteBufferSize(xmlObject.getWriteBufferSize());
    diskStore.setDirectories(xmlObject.getDiskDirs().stream().map(diskDirType -> {
      DiskDir diskDir = new DiskDir();
      diskDir.setDirSize(diskDirType.getDirSize());
      diskDir.setName(diskDirType.getContent());
      return diskDir;
    }).collect(Collectors.toList()));

    return diskStore;
  }

  @Override
  protected DiskStoreType fromNonNullConfigObject(DiskStore configObject) {
    DiskStoreType diskStoreType = new DiskStoreType();

    diskStoreType.setName(configObject.getName());
    diskStoreType.setAllowForceCompaction(configObject.isAllowForceCompaction());
    diskStoreType.setAutoCompact(configObject.isAutoCompact());
    diskStoreType.setCompactionThreshold(configObject.getCompactionThreshold());
    diskStoreType.setDiskUsageCriticalPercentage(configObject.getDiskUsageCriticalPercentage());
    diskStoreType.setDiskUsageWarningPercentage(configObject.getDiskUsageWarningPercentage());
    diskStoreType.setMaxOplogSize(configObject.getMaxOplogSize());
    diskStoreType.setQueueSize(configObject.getQueueSize());
    diskStoreType.setTimeInterval(configObject.getTimeInterval());
    diskStoreType.setWriteBufferSize(configObject.getWriteBufferSize());
    diskStoreType.setDiskDirs(configObject.getDirectories().stream().map(diskDir -> {
      DiskDirType diskDirType = new DiskDirType();
      diskDirType.setContent(diskDir.getName());
      diskDirType.setDirSize(diskDir.getDirSize());
      return diskDirType;
    }).collect(Collectors.toList()));

    return diskStoreType;
  }
}
