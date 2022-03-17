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
 *
 */

package org.apache.geode.internal.statistics;

import java.io.Serializable;

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;

public final class StatTestFunctions implements Serializable {
  public static class HangFunction implements Function<Object> {
    public static final String SUCCESS_OUTPUT = "nothingToReturn";
    public static final String FAIL_OUTPUT = "interrupted";
    public static final long HANG_TIME_MILLIS = 120000;

    @Override
    public void execute(FunctionContext<Object> context) {
      try {
        Thread.currentThread().wait(HANG_TIME_MILLIS);
      } catch (InterruptedException e) {
        context.getResultSender().lastResult(FAIL_OUTPUT);
      }
      context.getResultSender().lastResult(SUCCESS_OUTPUT);
    }

    @Override
    public String getId() {
      return "hangAWhile";
    }

    @Override
    public boolean optimizeForWrite() {
      return true;
    }
  }

}
