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

package org.apache.geode.management.internal.rest.controllers;

import static org.apache.geode.management.internal.rest.controllers.AbstractManagementController.MANAGEMENT_API_VERSION;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.dao.ReflectionSaltSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import sun.reflect.Reflection;

import org.apache.geode.management.api.RestApiCommand;

@Controller("cli")
@RequestMapping(MANAGEMENT_API_VERSION)
public class GenericCLIController extends AbstractManagementController{
  private static List<String> controllerNames;

  static {
    controllerNames = Collections.emptyList();
    controllerNames.add("org.apache.geode.management.internal.rest.controllers.GatewayMangementController");
    controllerNames.add("org.apache.geode.management.internal.rest.controllers.MemberManagementController");
    controllerNames.add("org.apache.geode.management.internal.rest.controllers.PdxManagementController");
    controllerNames.add("org.apache.geode.management.internal.rest.controllers.PingManagementController");
    controllerNames.add("org.apache.geode.management.internal.rest.controllers.RegionManagementController");
  }

//  String regex = "(?<=\\p{Ll})(?=\\p{Lu})";
//    System.out.printf("%s -> %s%n", text, Arrays.toString(text.split(regex)));
  {

  }

  @PreAuthorize("@securityService.authorize('CLUSTER', 'READ')")
  @RequestMapping(method = RequestMethod.POST, value = "/cli/command" )
  public ResponseEntity<?> command(@RequestBody(required = true) RestApiCommand restApiCommand) {
//    MemberManagementController memberManagementController = new MemberManagementController();
//    return memberManagementController.listMembers(null, null);
    Method method = mapRequest(restApiCommand);
    try {
      return (ResponseEntity<?>)method.invoke("id");
    } catch (IllegalAccessException | InvocationTargetException e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private Method mapRequest(RestApiCommand restApiCommand){
    return null;
  }

  private Map<String, Method> mapMethods() {
    List<Method> methodList = controllerNames.stream().map(c -> {
      try {
        return Class.forName(c);
      } catch (ClassNotFoundException ignored) {
        return Void.class;
      }
    }).flatMap(c -> Stream.of(c.getMethods())).collect(Collectors.toList());
    return null;
  }
}


