/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
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
package org.jasig.cas.adaptors.trusted.web.flow;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jasig.cas.CentralAuthenticationServiceImpl;
import org.jasig.cas.adaptors.trusted.authentication.handler.support.PrincipalBearingCredentialsAuthenticationHandler;
import org.jasig.cas.adaptors.trusted.authentication.principal.PrincipalBearingPrincipalResolver;
import org.jasig.cas.authentication.AuthenticationHandler;
import org.jasig.cas.authentication.AuthenticationManager;
import org.jasig.cas.authentication.PolicyBasedAuthenticationManager;
import org.jasig.cas.authentication.principal.PrincipalResolver;
import org.jasig.cas.authentication.principal.SimpleWebApplicationServiceImpl;
import org.jasig.cas.logout.LogoutManager;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.ticket.registry.DefaultTicketRegistry;
import org.jasig.cas.ticket.support.NeverExpiresExpirationPolicy;
import org.jasig.cas.util.DefaultUniqueTicketIdGenerator;
import org.jasig.cas.util.UniqueTicketIdGenerator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.test.MockRequestContext;

/**
 * @author Scott Battaglia
 * @since 3.0.0.5
 *
 */
public class PrincipalFromRequestRemoteUserNonInteractiveCredentialsActionTests {

    private PrincipalFromRequestRemoteUserNonInteractiveCredentialsAction action;

    @Before
    public void setUp() throws Exception {
        this.action = new PrincipalFromRequestRemoteUserNonInteractiveCredentialsAction();

        final Map<String, UniqueTicketIdGenerator> idGenerators = new HashMap<>();
        idGenerators.put(SimpleWebApplicationServiceImpl.class.getName(), new DefaultUniqueTicketIdGenerator());


        final AuthenticationManager authenticationManager = new PolicyBasedAuthenticationManager(
                Collections.<AuthenticationHandler, PrincipalResolver>singletonMap(
                        new PrincipalBearingCredentialsAuthenticationHandler(),
                        new PrincipalBearingPrincipalResolver()));
        final CentralAuthenticationServiceImpl centralAuthenticationService = new CentralAuthenticationServiceImpl(
                new DefaultTicketRegistry(), null, authenticationManager, new DefaultUniqueTicketIdGenerator(),
                idGenerators, new NeverExpiresExpirationPolicy(), new NeverExpiresExpirationPolicy(),
                mock(ServicesManager.class), mock(LogoutManager.class));
        this.action.setCentralAuthenticationService(centralAuthenticationService);
    }

    @Test
    public void verifyRemoteUserExists() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteUser("test");

        final MockRequestContext context = new MockRequestContext();
        context.setExternalContext(new ServletExternalContext(
                new MockServletContext(), request, new MockHttpServletResponse()));

        assertEquals("success", this.action.execute(context).getId());
    }

    @Test
    public void verifyRemoteUserDoesntExists() throws Exception {
        final MockRequestContext context = new MockRequestContext();
        context.setExternalContext(new ServletExternalContext(
                new MockServletContext(), new MockHttpServletRequest(), new MockHttpServletResponse()));

        assertEquals("error", this.action.execute(context).getId());
    }
}
