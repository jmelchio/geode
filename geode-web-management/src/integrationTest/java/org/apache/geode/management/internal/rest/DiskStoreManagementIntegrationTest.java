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

package org.apache.geode.management.internal.rest;

import static org.apache.geode.test.junit.assertions.ClusterManagementRealizationResultAssert.assertManagementResult;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import org.apache.geode.management.api.ClusterManagementResult;
import org.apache.geode.management.api.ClusterManagementService;
import org.apache.geode.management.api.RestTemplateClusterManagementServiceTransport;
import org.apache.geode.management.client.ClusterManagementServiceBuilder;
import org.apache.geode.management.configuration.DiskDir;
import org.apache.geode.management.configuration.DiskStore;
import org.apache.geode.util.internal.GeodeJsonMapper;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = {"classpath*:WEB-INF/management-servlet.xml"},
    loader = PlainLocatorContextLoader.class)
@WebAppConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DiskStoreManagementIntegrationTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  // needs to be used together with any BaseLocatorContextLoader
  private LocatorWebContext context;

  private ClusterManagementService client;

  private DiskStore diskStore;
  private DiskDir diskDir;
  private static final ObjectMapper mapper = GeodeJsonMapper.getMapper();

  @Before
  public void before() {
    // needs to be used together with any BaseLocatorContextLoader
    context = new LocatorWebContext(webApplicationContext);
    client = new ClusterManagementServiceBuilder().setTransport(
        new RestTemplateClusterManagementServiceTransport(
            new RestTemplate(context.getRequestFactory())))
        .build();
    diskStore = new DiskStore();
    diskDir = new DiskDir();
  }

  @Test
  @WithMockUser
  public void sanityCheck() {
    diskStore.setName("storeone");
    diskDir.setName("diskdirone");
    diskStore.setDirectories(Collections.singletonList(diskDir));

    assertManagementResult(client.create(diskStore))
        .hasStatusCode(ClusterManagementResult.StatusCode.OK);
    assertManagementResult(client.delete(diskStore))
        .hasStatusCode(ClusterManagementResult.StatusCode.OK);
  }

  @Test
  @WithMockUser
  public void createWithInvalidGroupFails() {
    diskStore.setName("storeone");
    diskDir.setName("diskdirone");
    diskStore.setDirectories(Collections.singletonList(diskDir));
    diskStore.setGroup("cluster");

    assertThatThrownBy(() -> client.create(diskStore))
        .hasMessageContaining("ILLEGAL_ARGUMENT: 'cluster' is a reserved group name");
  }

  @Test
  public void createWithMissingDiskDirFails() {
    diskStore.setName("storeone");

    assertThatThrownBy(() -> client.create(diskStore))
        .hasMessageContaining("ILLEGAL_ARGUMENT: At least one DiskDir element required");
  }

  @Test
  public void createDuplicateDiskStoreFails() {
    diskStore.setName("storeone");
    diskDir.setName("diskdirone");
    diskStore.setDirectories(Collections.singletonList(diskDir));

    // trying to create a duplicate diskstore, reusing existing

    assertManagementResult(client.create(diskStore))
        .hasStatusCode(ClusterManagementResult.StatusCode.OK);

    assertThatThrownBy(() -> client.create(diskStore))
        .hasMessageContaining("ENTITY_EXISTS: DiskStore 'storeone' already exists in group cluster");

    assertManagementResult(client.delete(diskStore))
        .hasStatusCode(ClusterManagementResult.StatusCode.OK);
  }

  @Test
  public void createWithIllegalFails() {
    diskStore.setName("storeone");
    diskDir.setName("diskdirone");
    diskStore.setDirectories(Collections.singletonList(diskDir));
    diskStore.setDiskUsageCriticalPercentage(120.0F);

    assertThatThrownBy(() -> client.create(diskStore))
        .hasMessageContaining("ILLEGAL_ARGUMENT: Disk usage critical percentage must be set to a value between 0-100.  The value 120.0 is invalid");
  }

  @Test
  public void postToIndexRegionEndPoint() {
//    context.perform(post("/v1/regions/products/indexes").content(mapper.writeValueAsString(index)))
//        .andExpect(status().isBadRequest())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("ILLEGAL_ARGUMENT")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers
//                .containsString("Region name in path must match Region name in configuration.")));
//
//    index.setRegionPath(null);
//    context.perform(post("/v1/regions/products/indexes").content(mapper.writeValueAsString(index)))
//        .andExpect(status().isNotFound())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("ENTITY_NOT_FOUND")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers.containsString("Region provided does not exist: products.")));
//
  }

  private void createClusterRegion() {
//    diskStore.setName("region1");
//    diskStore.setType(RegionType.PARTITION);
//    assertManagementResult(client.create(diskStore))
//        .hasStatusCode(ClusterManagementResult.StatusCode.OK);
  }

  @Test
  public void createIndex_succeedsForSpecificRegionAndGroup() {
//    createGroupRegion();
//
//    index.setRegionPath("region1");
//    index.setExpression("i am le tired");
//    index.setName("itworks");
//
//    context.perform(
//        post("/v1/regions/region1/indexes").content(mapper.writeValueAsString(index)))
//        .andExpect(status().isCreated())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("OK")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers
//                .containsString("Successfully updated configuration for group1.")));
//
//    deleteRegion();
  }

  @Test
  public void deleteIndex_succeeds() {
//    createClusterRegion();
//
//    createClusterIndex();
//
//    context.perform(delete("/v1/regions/region1/indexes/index1"))
//        .andExpect(status().isOk())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("OK")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers
//                .containsString("Successfully updated configuration for cluster.")));
//
//    deleteRegion();
  }

  @Test
  public void deleteIndex_succeeds_with_group() {
//    createGroupRegion();
//
//    createGroupIndex();
//
//    context.perform(delete("/v1/regions/region1/indexes/index1").param("group", "group1"))
//        .andExpect(status().isOk())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("OK")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers
//                .containsString("Successfully updated configuration for group1.")));
//
//    deleteRegion();
  }


  @Test
  public void deleteIndex_in_cluster_group_success() {
//    createClusterRegion();
//
//    createClusterIndex();
//
//    context.perform(delete("/v1/regions/region1/indexes/index1"))
//        .andExpect(status().isOk())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("OK")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers
//                .containsString("Successfully updated configuration for cluster")));
//
//    deleteRegion();
  }

  @Test
  public void deleteIndex_Index_in_group_success() {
//    createGroupRegion();
//
//    createGroupIndex();
//
//    context.perform(delete("/v1/regions/region1/indexes/index1"))
//        .andExpect(status().isOk())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("OK")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers
//                .containsString("Successfully updated configuration for group1")));
//
//    deleteRegion();
  }

  @Test
  public void deleteIndex_fails_index_not_found() {
//    createClusterRegion();
//
//    context.perform(delete("/v1/regions/region1/indexes/index1"))
//        .andExpect(status().isNotFound())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("ENTITY_NOT_FOUND")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers
//                .containsString("Index 'index1' does not exist.")));
//
//    deleteRegion();
  }

  @Test
  public void deleteIndex_fails_index_not_found_with_group() {
//    createGroupRegion();
//
//    context.perform(delete("/v1/regions/region1/indexes/index1").param("group", "group1"))
//        .andExpect(status().isNotFound())
//        .andExpect(jsonPath("$.statusCode", Matchers.is("ENTITY_NOT_FOUND")))
//        .andExpect(jsonPath("$.statusMessage",
//            Matchers
//                .containsString("Index 'index1' does not exist.")));
//
//    deleteRegion();
  }

  private void createGroupRegion() {
//    diskStore.setName("region1");
//    diskStore.setType(RegionType.PARTITION);
//    diskStore.setGroup("group1");
//    assertManagementResult(client.create(diskStore))
//        .hasStatusCode(ClusterManagementResult.StatusCode.OK);
  }

  private void deleteRegion() {
//    diskStore.setGroup(null);
//    assertManagementResult(client.delete(diskStore))
//        .hasStatusCode(ClusterManagementResult.StatusCode.OK);
  }

  private void createGroupIndex() {
//    index.setRegionPath("region1");
//    index.setIndexType(IndexType.RANGE);
//    index.setName("index1");
//    index.setExpression("some expression");
//    assertManagementResult(client.create(index))
//        .hasStatusCode(ClusterManagementResult.StatusCode.OK);
  }

  private void createClusterIndex() {
//    index.setRegionPath("region1");
//    index.setIndexType(IndexType.RANGE);
//    index.setName("index1");
//    index.setExpression("some expression");
//    assertManagementResult(client.create(index))
//        .hasStatusCode(ClusterManagementResult.StatusCode.OK);
  }
}
