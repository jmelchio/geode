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

package org.apache.geode.management.api;

import java.util.Map;

public class RestApiCommand {
  private String action;
  private String target;
  private Map<String, String> paramaters;

  public RestApiCommand() {}

  public RestApiCommand(String action, String target, Map<String, String> parameters) {
    this.action = action;
    this.target = target;
    this.paramaters = parameters;
  }

  public String getAction() {
    return action;
  }

  public RestApiCommand setAction(String action) {
    this.action = action;
    return this;
  }

  public String getTarget() {
    return target;
  }

  public RestApiCommand setTarget(String target) {
    this.target = target;
    return this;
  }

  public Map<String, String> getParamaters() {
    return paramaters;
  }

  public RestApiCommand setParamaters(Map<String, String> paramaters) {
    this.paramaters = paramaters;
    return this;
  }
}
