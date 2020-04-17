/*
 *
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

package org.apache.geode.tools.pulse.internal.data;

import static java.time.Instant.now;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;

/**
 * A Singleton instance of the memory cache for clusters.
 *
 * @since GemFire version 7.0.Beta 2012-09-23
 */
@Component
public class Repository {
  private static final Logger logger = LogManager.getLogger();
  private static final Locale LOCALE =
      new Locale(PulseConstants.APPLICATION_LANGUAGE, PulseConstants.APPLICATION_COUNTRY);

  private final OAuth2AuthorizedClientService authorizedClientService;
  private final ClusterFactory clusterFactory;
  private final HashMap<String, Cluster> clusterMap = new HashMap<>();
  private Boolean jmxUseLocator;
  private String host;
  private String port;
  private boolean useSSLLocator = false;
  private boolean useSSLManager = false;
  private Properties javaSslProperties;


  private final ResourceBundle resourceBundle =
      ResourceBundle.getBundle(PulseConstants.LOG_MESSAGES_FILE, LOCALE);

  private final PulseConfig pulseConfig = new PulseConfig();

  @Autowired(required = false)
  public Repository() {
    this(null);
  }

  @Autowired(required = false)
  public Repository(OAuth2AuthorizedClientService authorizedClientService) {
    this(authorizedClientService, Cluster::new);
  }

  public Repository(OAuth2AuthorizedClientService authorizedClientService,
      ClusterFactory clusterFactory) {
    this.authorizedClientService = authorizedClientService;
    this.clusterFactory = clusterFactory;
  }

  /**
   * this will return a cluster already connected to the geode jmx manager for the user in the
   * request
   *
   * But for multi-user connections to gemfireJMX, i.e pulse that uses gemfire integrated security,
   * we will need to get the username from the context
   */
  public Cluster getCluster() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }

    if (authentication instanceof OAuth2AuthenticationToken) {
      return getClusterWithAuthenticationToken((OAuth2AuthenticationToken) authentication);
    }

    return getClusterWithUserNameAndPassword(authentication.getName(), null);
  }

  public Cluster getClusterWithUserNameAndPassword(String userName, String password) {
    return getClusterWithCredentials(userName, new String[] {userName, password});
  }

  public Cluster getClusterWithCredentials(String username, Object credentials) {
    synchronized (clusterMap) {
      Cluster cluster = clusterMap.get(username);
      if (cluster == null) {
        logger.info(resourceBundle.getString("LOG_MSG_CREATE_NEW_THREAD") + " : " + username);
        cluster = clusterFactory.create(host, port, username, resourceBundle, this);
        // Assign name to thread created
        cluster.setName(PulseConstants.APP_NAME + "-" + host + ":" + port + ":" + username);
        cluster.connectToGemFire(credentials);
        if (cluster.isConnectedFlag()) {
          clusterMap.put(username, cluster);
        }
      }
      return cluster;
    }
  }

  private Cluster getClusterWithAuthenticationToken(OAuth2AuthenticationToken authentication) {
    OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(authentication);

    if (!isExpired(authorizedClient.getAccessToken())) {
      return getClusterWithAuthorizedClient(authorizedClient);
    }

    OAuth2AuthorizedClient freshClient = handleExpiration(authentication, authorizedClient);

    if (freshClient == null) {
      return null;
    }

    synchronized (clusterMap) {
      Cluster cluster = clusterMap.get(freshClient.getPrincipalName());
      if (cluster != null) {
        cluster.reconnectToGemFire(freshClient.getAccessToken().getTokenValue());
      }
      return cluster;
    }
  }

  private Cluster getClusterWithAuthorizedClient(OAuth2AuthorizedClient authorizedClient) {
    return getClusterWithCredentials(authorizedClient.getPrincipalName(),
        authorizedClient.getAccessToken().getTokenValue());
  }

  public void logoutUser(String username) {
    Cluster cluster = clusterMap.remove(username);
    if (cluster != null) {
      try {
        cluster.setStopUpdates(true);
        cluster.getJMXConnector().close();
      } catch (Exception e) {
        // We're logging out so this can be ignored
      }
    }
  }

  public void removeAllClusters() {
    Iterator<Map.Entry<String, Cluster>> iter = clusterMap.entrySet().iterator();

    while (iter.hasNext()) {
      Map.Entry<String, Cluster> entry = iter.next();
      Cluster c = entry.getValue();
      String clusterKey = entry.getKey();
      c.stopThread();
      iter.remove();
      logger.info("{} : {}", resourceBundle.getString("LOG_MSG_REMOVE_THREAD"), clusterKey);
    }
  }

  public Boolean getJmxUseLocator() {
    return jmxUseLocator;
  }

  public void setJmxUseLocator(Boolean jmxUseLocator) {
    Objects.requireNonNull(jmxUseLocator, "jmxUseLocat == null");
    this.jmxUseLocator = jmxUseLocator;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String jmxHost) {
    host = jmxHost;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String jmxPort) {
    port = jmxPort;
  }

  public boolean isUseSSLLocator() {
    return useSSLLocator;
  }

  public void setUseSSLLocator(boolean useSSLLocator) {
    this.useSSLLocator = useSSLLocator;
  }

  public boolean isUseSSLManager() {
    return useSSLManager;
  }

  public void setUseSSLManager(boolean useSSLManager) {
    this.useSSLManager = useSSLManager;
  }

  public PulseConfig getPulseConfig() {
    return pulseConfig;
  }

  public Properties getJavaSslProperties() {
    return javaSslProperties;
  }

  public void setJavaSslProperties(Properties javaSslProperties) {
    this.javaSslProperties = javaSslProperties;
  }

  public ResourceBundle getResourceBundle() {
    return resourceBundle;
  }

  private OAuth2AuthorizedClient handleExpiration(Authentication authentication,
      OAuth2AuthorizedClient staleClient) {
    OAuth2RefreshToken refreshToken = staleClient.getRefreshToken();

    // If the refresh token is missing or expired, invalidate the current user's authentication
    // and remove the user's cluster from the repository.
    if (refreshToken == null || isExpired(refreshToken)) {
      logoutUser(staleClient.getPrincipalName());
      authentication.setAuthenticated(false);
      return null;
    }

    OAuth2AuthorizedClient freshClient = refreshClient(staleClient);
    authorizedClientService.saveAuthorizedClient(freshClient, authentication);

    // TODO: Find out whether the access token can be null. See the comment in refreshClient().
    if (freshClient.getAccessToken() == null) {
      logoutUser(staleClient.getPrincipalName());
      authentication.setAuthenticated(false);
      return null;
    }

    return freshClient;
  }

  private OAuth2AuthorizedClient getAuthorizedClient(
      OAuth2AuthenticationToken authenticationToken) {
    return authorizedClientService.loadAuthorizedClient(
        authenticationToken.getAuthorizedClientRegistrationId(), authenticationToken.getName());
  }

  private static OAuth2AuthorizedClient refreshClient(OAuth2AuthorizedClient staleClient) {
    OAuth2RefreshTokenGrantRequest refreshRequest = new OAuth2RefreshTokenGrantRequest(
        staleClient.getClientRegistration(),
        staleClient.getAccessToken(),
        staleClient.getRefreshToken());

    OAuth2AccessTokenResponse refreshResponse = new DefaultRefreshTokenTokenResponseClient()
        // As far as I can tell from reading getTokenResponse(), the response will always contain
        // an access token. So I'm not sure how to know whether the refresh request was granted.
        // Maybe it throws an exception if the request was denied?
        .getTokenResponse(refreshRequest);

    return new OAuth2AuthorizedClient(
        staleClient.getClientRegistration(), staleClient.getPrincipalName(),
        refreshResponse.getAccessToken(), refreshResponse.getRefreshToken());
  }

  private static boolean isExpired(AbstractOAuth2Token token) {
    Instant tokenExpiration = token.getExpiresAt();
    return tokenExpiration != null && now().isAfter(tokenExpiration);
  }
}
