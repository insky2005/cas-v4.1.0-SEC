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
package org.jasig.cas.support.saml.authentication.principal;


import org.jasig.cas.authentication.principal.AbstractWebApplicationService;
import org.jasig.cas.authentication.principal.DefaultResponse;
import org.jasig.cas.authentication.principal.Response;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.support.saml.util.AbstractSaml20ObjectBuilder;
import org.jasig.cas.util.ISOStandardDateFormat;
import org.jdom.Document;
import org.springframework.util.StringUtils;
import org.jasig.cas.support.saml.SamlProtocolConstants;
import org.jasig.cas.support.saml.util.GoogleSaml20ObjectBuilder;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import javax.servlet.http.HttpServletRequest;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;
/**
 * Implementation of a Service that supports Google Accounts (eventually a more
 * generic SAML2 support will come).
 *
 * @author Scott Battaglia
 * @since 3.1
 */
public class GoogleAccountsService extends AbstractWebApplicationService {

    private static final long serialVersionUID = 6678711809842282833L;
    private static final int INFLATED_BYTE_ARRAY_LENGTH = 10000;
    private static final int DEFLATED_BYTE_ARRAY_BUFFER_LENGTH = 1024;
    private static final int SAML_RESPONSE_RANDOM_ID_LENGTH = 20;
    private static final int SAML_RESPONSE_ID_LENGTH = 40;
    private static final int HEX_HIGH_BITS_BITWISE_FLAG = 0x0f;
    private static final int HEX_RIGHT_SHIFT_COEFFICIENT = 4;

    private static final GoogleSaml20ObjectBuilder BUILDER = new GoogleSaml20ObjectBuilder();

    private final String relayState;

    private final PublicKey publicKey;

    private final PrivateKey privateKey;

    private final String requestId;

    private final ServicesManager servicesManager;
    
    /**
     * Instantiates a new google accounts service.
     *
     * @param id the id
     * @param relayState the relay state
     * @param requestId the request id
     * @param privateKey the private key
     * @param publicKey the public key
     * @param servicesManager the services manager
     */
    protected GoogleAccountsService(final String id, final String relayState, final String requestId,
            final PrivateKey privateKey, final PublicKey publicKey, final ServicesManager servicesManager) {
        this(id, id, null, relayState, requestId, privateKey, publicKey, servicesManager);
    }

    /**
     * Instantiates a new google accounts service.
     *
     * @param id the id
     * @param originalUrl the original url
     * @param artifactId the artifact id
     * @param relayState the relay state
     * @param requestId the request id
     * @param privateKey the private key
     * @param publicKey the public key
     * @param servicesManager the services manager
     */
    protected GoogleAccountsService(final String id, final String originalUrl,
            final String artifactId, final String relayState, final String requestId,
            final PrivateKey privateKey, final PublicKey publicKey,
            final ServicesManager servicesManager) {
        super(id, originalUrl, artifactId);
        this.relayState = relayState;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.requestId = requestId;
        this.servicesManager = servicesManager;


    }

    /**
     * Creates the service from request.
     *
     * @param request the request
     * @param privateKey the private key
     * @param publicKey the public key
     * @param servicesManager the services manager
     * @return the google accounts service
     */
    public static GoogleAccountsService createServiceFrom(
            final HttpServletRequest request, final PrivateKey privateKey,
            final PublicKey publicKey, final ServicesManager servicesManager) {
        final String relayState = request.getParameter(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE);

        final String xmlRequest = BUILDER.decodeSamlAuthnRequest(
                request.getParameter(SamlProtocolConstants.PARAMETER_SAML_REQUEST));

        if (!StringUtils.hasText(xmlRequest)) {
            return null;
        }

        final Document document = AbstractSaml20ObjectBuilder.constructDocumentFromXml(xmlRequest);

        if (document == null) {
            return null;
        }

        final Element root = document.getRootElement();
        final String assertionConsumerServiceUrl = root.getAttributeValue("AssertionConsumerServiceURL");
        final String requestId = root.getAttributeValue("ID");

        return new GoogleAccountsService(assertionConsumerServiceUrl,
                relayState, requestId, privateKey, publicKey, servicesManager);
    }

    @Override
    public Response getResponse(final String ticketId) {
        final Map<String, String> parameters = new HashMap<>();
        final String samlResponse = constructSamlResponse();
        final String signedResponse = BUILDER.signSamlResponse(samlResponse,
                this.privateKey, this.publicKey);
        parameters.put(SamlProtocolConstants.PARAMETER_SAML_RESPONSE, signedResponse);
        parameters.put(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE, this.relayState);

        return DefaultResponse.getPostResponse(getOriginalUrl(), parameters);
    }

    /**
     * Return true if the service is already logged out.
     *
     * @return true if the service is already logged out.
     */
    @Override
    public boolean isLoggedOutAlready() {
        return true;
    }

    /**
     * Construct SAML response.
     * <a href="http://bit.ly/1uI8Ggu">See this reference for more info.</a>
     * @return the SAML response
     */
    private String constructSamlResponse() {
        final DateTime currentDateTime = DateTime.parse(new ISOStandardDateFormat().getCurrentDateAndTime());
        final DateTime notBeforeIssueInstant = DateTime.parse("2003-04-17T00:46:02Z");

        final RegisteredService svc = this.servicesManager.findServiceBy(this);
        final String userId = svc.getUsernameAttributeProvider().resolveUsername(getPrincipal(), this);

        final org.opensaml.saml.saml2.core.Response response = BUILDER.newResponse(
                BUILDER.generateSecureRandomId(),
                currentDateTime,
                getId(), this);
        response.setStatus(BUILDER.newStatus(StatusCode.SUCCESS, null));

        final AuthnStatement authnStatement = BUILDER.newAuthnStatement(
                AuthnContext.PASSWORD_AUTHN_CTX, currentDateTime);
        final Assertion assertion = BUILDER.newAssertion(authnStatement,
                "https://www.opensaml.org/IDP",
                notBeforeIssueInstant, BUILDER.generateSecureRandomId());

        final Conditions conditions = BUILDER.newConditions(notBeforeIssueInstant,
                currentDateTime, getId());
        assertion.setConditions(conditions);

        final Subject subject = BUILDER.newSubject(NameID.EMAIL, userId,
                getId(), currentDateTime, this.requestId);
        assertion.setSubject(subject);

        response.getAssertions().add(assertion);

        final StringWriter writer = new StringWriter();
        BUILDER.marshalSamlXmlObject(response, writer);

        final String result = writer.toString();
        logger.debug("Generated Google SAML response: {}", result);
        return result;
    }
}
