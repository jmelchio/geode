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

package org.apache.geode.management.internal.rest.config;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;

@Configuration
public class AppConfig {

  @Bean
  public Jackson2ObjectMapperFactoryBean objectMapperFactory() {
    Jackson2ObjectMapperFactoryBean mapperFactoryBean = new Jackson2ObjectMapperFactoryBean();

    mapperFactoryBean.setObjectMapper(new ObjectMapper());
    mapperFactoryBean.setFailOnEmptyBeans(true);
    mapperFactoryBean.setSimpleDateFormat("MM/dd/yyyy");
    mapperFactoryBean.setFeaturesToEnable(JsonParser.Feature.ALLOW_COMMENTS,
        JsonParser.Feature.ALLOW_SINGLE_QUOTES, MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL,
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapperFactoryBean.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    return mapperFactoryBean;
  }

  @Bean
  public ContentNegotiationManagerFactoryBean mvcContentNegotiationManager() {
    ContentNegotiationManagerFactoryBean contentNegotiationManager =
        new ContentNegotiationManagerFactoryBean();

    contentNegotiationManager.setFavorPathExtension(true);
    contentNegotiationManager.setFavorParameter(true);
    contentNegotiationManager.setIgnoreAcceptHeader(false);
    contentNegotiationManager.setUseJaf(false);
    contentNegotiationManager.setDefaultContentType(MediaType.APPLICATION_JSON);
    Properties mediaTypes = new Properties();
    mediaTypes.put("json", MediaType.APPLICATION_JSON_VALUE);
    contentNegotiationManager.setMediaTypes(mediaTypes);

    return contentNegotiationManager;
  }
}
