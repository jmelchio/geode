/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geode.tools.pulse.internal.context;

import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_AEQ_LISTENER;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_CLIENT_NAME;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_CLUSTER_NAME;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_GEMFIRE_VERSION;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_MEMBER_ID;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_MEMBER_NAME;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_PHYSICAL_HOST_NAME;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_REGION_NAME;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_REGION_PATH;
import static org.apache.geode.tools.pulse.controllers.PulseControllerJUnitTest.TEST_REGION_TYPE;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import org.apache.geode.tools.pulse.internal.data.Cluster;
import org.apache.geode.tools.pulse.internal.data.Repository;

@Configuration
@Profile("test")
public class TestContext {

  @Bean
  @Primary
  public Repository repository() {
    return spy(Repository.class);
  }

  @Bean
  @Primary
  public Cluster cluster() {
    Cluster cluster = spy(Cluster.class);

    Cluster.Region region = new Cluster.Region();
    region.setName(TEST_REGION_NAME);
    region.setFullPath(TEST_REGION_PATH);
    region.setRegionType(TEST_REGION_TYPE);
    region.setMemberCount(1);
    region.setMemberName(new ArrayList<String>() {
      {
        add(TEST_MEMBER_NAME);
      }
    });

    region.setPutsRate(12.31D);
    region.setGetsRate(27.99D);
    Cluster.RegionOnMember regionOnMember = new Cluster.RegionOnMember();
    regionOnMember.setRegionFullPath(TEST_REGION_PATH);
    regionOnMember.setMemberName(TEST_MEMBER_NAME);
    region.setRegionOnMembers(new ArrayList<Cluster.RegionOnMember>() {
      {
        add(regionOnMember);
      }
    });
    cluster.addClusterRegion(TEST_REGION_PATH, region);

    Cluster.Member member = new Cluster.Member();
    member.setId(TEST_MEMBER_ID);
    member.setName(TEST_MEMBER_NAME);
    member.setUptime(1L);
    member.setHost(TEST_PHYSICAL_HOST_NAME);
    member.setGemfireVersion(TEST_GEMFIRE_VERSION);
    member.setCpuUsage(55.77123D);

    member.setMemberRegions(new HashMap<String, Cluster.Region>() {
      {
        put(TEST_REGION_NAME, region);
      }
    });

    Cluster.AsyncEventQueue aeq = new Cluster.AsyncEventQueue();
    aeq.setAsyncEventListener(TEST_AEQ_LISTENER);
    member.setAsyncEventQueueList(new ArrayList<Cluster.AsyncEventQueue>() {
      {
        add(aeq);
      }
    });

    Cluster.Client client = new Cluster.Client();
    client.setId("100");
    client.setName(TEST_CLIENT_NAME);
    client.setUptime(1L);
    member.setMemberClientsHMap(new HashMap<String, Cluster.Client>() {
      {
        put(TEST_CLIENT_NAME, client);
      }
    });

    cluster.setMembersHMap(new HashMap<String, Cluster.Member>() {
      {
        put(TEST_MEMBER_NAME, member);
      }
    });
    cluster.setPhysicalToMember(new HashMap<String, List<Cluster.Member>>() {
      {
        put(TEST_PHYSICAL_HOST_NAME, new ArrayList<Cluster.Member>() {
          {
            add(member);
          }
        });
      }
    });
    cluster.setServerName(TEST_CLUSTER_NAME);
    cluster.setMemoryUsageTrend(new CircularFifoBuffer() {
      {
        add(1);
        add(2);
        add(3);
      }
    });
    cluster.setWritePerSecTrend(new CircularFifoBuffer() {
      {
        add(1.29);
        add(2.3);
        add(3.0);
      }
    });
    cluster.setThroughoutReadsTrend(new CircularFifoBuffer() {
      {
        add(1);
        add(2);
        add(3);
      }
    });
    cluster.setThroughoutWritesTrend(new CircularFifoBuffer() {
      {
        add(4);
        add(5);
        add(6);
      }
    });

    return cluster;
  }
}
