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

package org.apache.geode.management.configuration;

import java.util.List;

import org.apache.geode.management.runtime.DiskStoreInfo;

public class DiskStore extends GroupableConfiguration<DiskStoreInfo> {
  public static final String DISK_STORE_CONFIG_ENDPOINT = "/diskstores";

  private String id;
  private String name;
  private String compactionThreshold;
  private String diskUsageCriticalPercentage;
  private String diskUsageWarningPercentage;
  private String maxOplogSize;
  private String queueSize;
  private String timeInterval;
  private String writeBufferSize;
  private List<DiskDir> directories;
  private boolean allowForceCompaction;
  private boolean autoCompact;

  public boolean isAutoCompact() {
    return autoCompact;
  }

  public void setAutoCompact(boolean autoCompact) {
    this.autoCompact = autoCompact;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<DiskDir> getDirectories() {
    return directories;
  }

  public void setDirectories(List<DiskDir> directories) {
    this.directories = directories;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCompactionThreshold() {
    return compactionThreshold;
  }

  public void setCompactionThreshold(String compactionThreshold) {
    this.compactionThreshold = compactionThreshold;
  }

  public String getDiskUsageCriticalPercentage() {
    return diskUsageCriticalPercentage;
  }

  public void setDiskUsageCriticalPercentage(String diskUsageCriticalPercentage) {
    this.diskUsageCriticalPercentage = diskUsageCriticalPercentage;
  }

  public String getDiskUsageWarningPercentage() {
    return diskUsageWarningPercentage;
  }

  public void setDiskUsageWarningPercentage(String diskUsageWarningPercentage) {
    this.diskUsageWarningPercentage = diskUsageWarningPercentage;
  }

  public String getMaxOplogSize() {
    return maxOplogSize;
  }

  public void setMaxOplogSize(String maxOplogSize) {
    this.maxOplogSize = maxOplogSize;
  }

  public String getQueueSize() {
    return queueSize;
  }

  public void setQueueSize(String queueSize) {
    this.queueSize = queueSize;
  }

  public String getTimeInterval() {
    return timeInterval;
  }

  public void setTimeInterval(String timeInterval) {
    this.timeInterval = timeInterval;
  }

  public String getWriteBufferSize() {
    return writeBufferSize;
  }

  public void setWriteBufferSize(String writeBufferSize) {
    this.writeBufferSize = writeBufferSize;
  }

  public boolean isAllowForceCompaction() {
    return allowForceCompaction;
  }

  public void setAllowForceCompaction(boolean allowForceCompaction) {
    this.allowForceCompaction = allowForceCompaction;
  }

  @Override
  public Links getLinks() {
    return new Links(getId(), DISK_STORE_CONFIG_ENDPOINT);
  }
}
