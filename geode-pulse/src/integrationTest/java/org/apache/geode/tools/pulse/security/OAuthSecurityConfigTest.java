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

package org.apache.geode.tools.pulse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"classpath*:WEB-INF/pulse-servlet.xml"})
@ActiveProfiles({"pulse.authentication.oauth"})
public class OAuthSecurityConfigTest {
  // Defined by Spring because we registered an OAuth client with providerID=uaa
  private static final String SPRING_OAUTH_CLIENT_URL_FOR_UAA =
      "http://localhost/oauth2/authorization/uaa";
  // The URI we specified for authorizing via UAA
  private static final String PULSE_OAUTH_AUTHORIZATION_URL =
      "http://example.com/uaa/oauth/authorize";
  // Built from our redirectUrlTemplate: {baseUrl}/login/oauth2/code/{registrationId}
  // with the registrationId filled in from our providerId (uaa).
  private static final String PULSE_UAA_REDIRECT_URL = "http://localhost/login/oauth2/code/uaa";

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  @Before
  public void setup() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // TODO: Only from login page? Or from any page?
  @Test
  public void loginPageRedirectsUnauthenticatedUserToOAuthClientURL() throws Exception {
    mvc.perform(get("/login.html"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(SPRING_OAUTH_CLIENT_URL_FOR_UAA));
  }

  @Test
  public void oauthClientURLRedirectsToOAuthServerAuthorizationURL() throws Exception {
    Map<String, String> expectedRedirectParams = new HashMap<>();
    expectedRedirectParams.put("response_type", "code");
    expectedRedirectParams.put("client_id", "pulse");
    expectedRedirectParams.put("redirect_uri", PULSE_UAA_REDIRECT_URL);

    mvc.perform(get(SPRING_OAUTH_CLIENT_URL_FOR_UAA))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPath(PULSE_OAUTH_AUTHORIZATION_URL))
        .andExpect(redirectedUrlParams(expectedRedirectParams));
  }

  private static ResultMatcher redirectedUrlPath(String expectedPath) {
    return result -> {
      String redirectedUrlString = result.getResponse().getRedirectedUrl();
      assertThat(redirectedUrlString).isNotNull();
      String actualPath = redirectedUrlString.split("[?#]")[0];
      assertThat(actualPath)
          .as("redirect URL path")
          .startsWith(expectedPath);
    };
  }

  private static ResultMatcher redirectedUrlParams(Map<String, String> expectedParams) {
    return result -> {
      String redirectedUrlString = result.getResponse().getRedirectedUrl();
      assertThat(redirectedUrlString).isNotNull();
      Map<String, String> actualParams = fromUriString(redirectedUrlString)
          .build()
          .getQueryParams()
          .toSingleValueMap();
      assertThat(actualParams)
          .as("redirected URL params")
          .containsAllEntriesOf(expectedParams);
    };
  }
}
