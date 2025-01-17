/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.data;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.james.GuiceModuleTestRule;
import org.apache.james.user.ldap.DockerLdapSingleton;
import org.apache.james.user.ldap.LdapRepositoryConfiguration;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.github.fge.lambdas.Throwing;
import com.google.inject.Module;

public class DockerLdapRule implements GuiceModuleTestRule {

    @Override
    public Module getModule() {
        return binder -> binder.bind(LdapRepositoryConfiguration.class)
            .toInstance(computeConfiguration(List.of(DockerLdapSingleton.ldapContainer.getLdapHost())));
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return statement;
    }

    private LdapRepositoryConfiguration computeConfiguration(List<String> ldapIps) {
        List<URI> uris = ldapIps.stream()
            .map(Throwing.function(URI::new))
            .collect(Collectors.toUnmodifiableList());
        try {
            return LdapRepositoryConfiguration.builder()
                .ldapHosts(uris)
                .principal("cn=admin,dc=james,dc=org")
                .credentials("mysecretpassword")
                .userBase("ou=People,dc=james,dc=org")
                .userIdAttribute("uid")
                .userObjectClass("inetOrgPerson")
                .build();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        DockerLdapSingleton.ldapContainer.start();
    }

    public void stop() {
        DockerLdapSingleton.ldapContainer.stop();
    }
}
