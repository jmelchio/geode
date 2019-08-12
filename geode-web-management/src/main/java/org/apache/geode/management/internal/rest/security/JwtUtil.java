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

package org.apache.geode.management.internal.rest.security;

public class JwtUtil {
  // private static String USER_NAME = "userName";
  // private static String USER_ID = "userId";
  // private static String ROLE = "role";
  //
  // @Value("${jwt.secret}")
  // private String secret;
  //
  // /**
  // * Tries to parse specified String as a JWT token. If successful, returns User object with
  // * username, id and role prefilled (extracted from token).
  // * If unsuccessful (token is invalid or not containing all required user properties), simply
  // * returns null.
  // *
  // * @param token the JWT token to parse
  // * @return the User object extracted from specified token or null if a token is invalid.
  // */
  // public Properties parseToken(String token) {
  // try {
  // Claims body = Jwts.parser()
  // .setSigningKey(secret)
  // .parseClaimsJws(token)
  // .getBody();
  //
  // Properties userProperties = new Properties();
  // userProperties.setProperty(USER_NAME, body.getSubject());
  // userProperties.setProperty(USER_ID, (String) body.get(USER_ID));
  // userProperties.setProperty(ROLE, (String) body.get(ROLE));
  //
  // return userProperties;
  //
  // } catch (JwtException | ClassCastException e) {
  // return null;
  // }
  // }
  //
  // /**
  // * Generates a JWT token containing username as subject, and userId and role as additional
  // claims.
  // * These properties are taken from the specified
  // * User object. Tokens validity is infinite.
  // *
  // * @param userProperties the user for which the token will be generated
  // * @return the JWT token
  // */
  // public String generateToken(Properties userProperties) {
  // Claims claims = Jwts.claims().setSubject(userProperties.getProperty(USER_NAME));
  // claims.put(USER_ID, userProperties.getProperty(USER_ID) + "");
  // claims.put(ROLE, userProperties.getProperty(ROLE));
  //
  // return Jwts.builder()
  // .setClaims(claims)
  // .signWith(SignatureAlgorithm.HS512, secret)
  // .compact();
  // }
}
