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

package org.apache.geode.tools.pulse.internal.security;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("pulse.authentication.default")
public class DefaultSecurityConfig extends WebSecurityConfigurerAdapter {
  @Bean
  public LogoutHandler customLogoutSuccessHandler() {
    return new LogoutHandler("/login.html");
  }

  @Bean
  public ExceptionMappingAuthenticationFailureHandler authenticationFailureHandler() {
    ExceptionMappingAuthenticationFailureHandler exceptionMappingAuthenticationFailureHandler =
        new ExceptionMappingAuthenticationFailureHandler();
    Map<String, String> exceptionMappings = new HashMap<>();
    exceptionMappings.put(BadCredentialsException.class.getName(), "/login.html?error=BAD_CREDS");
    exceptionMappings.put(CredentialsExpiredException.class.getName(),
        "/login.html?error=CRED_EXP");
    exceptionMappings.put(LockedException.class.getName(), "/login.html?error=ACC_LOCKED");
    exceptionMappings.put(DisabledException.class.getName(), "/login.html?error=ACC_DISABLED");
    exceptionMappingAuthenticationFailureHandler.setExceptionMappings(exceptionMappings);
    return exceptionMappingAuthenticationFailureHandler;
  }

  @Autowired
  private LogoutHandler logoutHandler;

  @Autowired
  private ExceptionMappingAuthenticationFailureHandler failureHandler;

  @Override
  protected void configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity.authorizeRequests(authorize -> authorize
        .mvcMatchers("/login.html", "/authenticateUser", "/pulseVersion", "/scripts/**",
            "/images/**", "/css/**", "/properties/**")
        .permitAll()
        // .mvcMatchers("/dataBrowser*", "/getQueryStatisticsGridModel*")
        // .access(
        // "hasRole('CLUSTER:READ') and hasRole('DATA:READ')")
        // .mvcMatchers("/*")
        // .hasRole("CLUSTER:READ")
        .anyRequest().authenticated())
        // .formLogin(form -> form
        // .loginPage("/login.html")
        // .loginProcessingUrl("/login")
        // .failureHandler(failureHandler)
        // .defaultSuccessUrl("/clusterDetail.html", true))
        .oauth2Login(withDefaults())
        .logout(logout -> logout
            .logoutUrl("/clusterLogout")
            .logoutSuccessHandler(logoutHandler))
        .exceptionHandling(exception -> exception
            .accessDeniedPage("/accessDenied.html"))
        .headers(header -> header
            .frameOptions().deny()
            .xssProtection(xss -> xss
                .xssProtectionEnabled(true)
                .block(true))
            .contentTypeOptions())
        .csrf().disable();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder authenticationManagerBuilder)
      throws Exception {
    authenticationManagerBuilder.inMemoryAuthentication()
        .passwordEncoder(NoOpPasswordEncoder.getInstance())
        .withUser("admin")
        .password("admin")
        .roles("CLUSTER:READ", "DATA:READ");
  }

  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    return new InMemoryClientRegistrationRepository(this.uaaClientRegistration());
  }

  @Bean
  public OAuth2AuthorizedClientService authorizedClientService(
      ClientRegistrationRepository clientRegistrationRepository) {
    return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
  }

  @Bean
  public OAuth2AuthorizedClientRepository authorizedClientRepository(
      OAuth2AuthorizedClientService authorizedClientService) {
    return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
  }

  private ClientRegistration uaaClientRegistration() {
    return ClientRegistration.withRegistrationId("uaa")
        .clientId("pulse")
        .clientSecret("pulsesecret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "CLUSTER:READ", "CLUSTER:WRITE", "DATA:READ", "DATA:WRITE")
        .authorizationUri("http://localhost:8080/uaa/oauth/authorize")
        .tokenUri("http://localhost:8080/uaa/oauth/token")
        .userInfoUri("http://localhost:8080/uaa/userinfo")
        .jwkSetUri("http://localhost:8080/uaa/token_keys")
        .clientName("Pulse")
        .userNameAttributeName("user_name")
        .build();
  }
}
