---
layout: default
title: CAS - Configuring SSO Session Cookie
---

# SSO Session Cookie
A ticket-granting cookie is an HTTP cookie set by CAS upon the establishment of a single sign-on session. This cookie maintains login state for the client, and while it is valid, the client can present it to CAS in lieu of primary credentials. Services can opt out of single sign-on through the `renew` parameter. See the [CAS Protocol](../protocol/CAS-Protocol.html) for more info.

## Configuration
The generation of the ticket-granting cookie is controlled by the file `cas-server-webapp\src\main\webapp\WEB-INF\spring-configuration\ticketGrantingTicketCookieGenerator.xml`

{% highlight xml %}
<bean id="ticketGrantingTicketCookieGenerator" 
    class="org.jasig.cas.web.support.CookieRetrievingCookieGenerator"
    c:casCookieValueManager-ref="cookieValueManager"
    p:cookieSecure="true"
    p:cookieMaxAge="-1"
    p:cookieName="TGC"
    p:cookiePath="/cas" />

<bean id="cookieCipherExecutor" class="org.jasig.cas.util.DefaultCipherExecutor"
    c:secretKeyEncryption="${tgc.encryption.key}"
    c:secretKeySigning="${tgc.signing.key}" />

<bean id="cookieValueManager" class="org.jasig.cas.web.support.DefaultCasCookieValueManager"
      c:cipherExecutor-ref="cookieCipherExecutor" />

{% endhighlight %}

The cookie has the following properties:

1. It is marked as secure.
2. Depending on container support, the cookie would be marked as http-only automatically.
3. The cookie value is encrypted and signed via secret keys that need to be generated upon deployment.

## Cookie Value Encryption

The cookie value is linked to the active ticket-granting ticket, the remote IP address that initiated the request 
as well as the user agent that submitted the request. The final cookie value is then encrypted and signed
using `AES_128_CBC_HMAC_SHA_256` and `HMAC_SHA512` respectively.

The secret keys are defined in the `cas.properties` file. While sample data is provided
for initial deployments, these keys **MUST** be regenerated per your specific environment. Each key
is a JSON Web Token with a defined length per the algorithm used for encryption and signing. 
You may [use the following tool](https://github.com/mitreid-connect/json-web-key-generator)
to generate your own JSON Web Tokens.

{% highlight properties %}
# CAS SSO Cookie Generation & Security
# See https://github.com/mitreid-connect/json-web-key-generator
#
# Do note that the following settings MUST be generated per deployment.
#
# Defaults at spring-configuration/ticketGrantingTicketCookieGenerator.xml
# The encryption secret key. By default, must be a octet string of size 256.
tgc.encryption.key=1PbwSbnHeinpkZOSZjuSJ8yYpUrInm5aaV18J2Ar4rM

# The signing secret key. By default, must be a octet string of size 512.
tgc.signing.key=szxK-5_eJjs-aUj-64MpUZ-GPPzGLhYPLGl0wrYjYNVAGva2P0lLe6UGKGM7k8dWxsOVGutZWgvmY3l5oVPO3w

{% endhighlight %}

## Turning Off Cookie Value Encryption
if you wish to disable the signing and encryption of the cookie, in the
configuration xml file, use the following beans instead of those provided by default:

{% highlight xml %}
<bean id="cookieCipherExecutor" class="org.jasig.cas.util.NoOpCipherExecutor" />

<bean id="cookieValueManager" class="org.jasig.cas.web.support.NoOpCookieValueManager"
      c:cipherExecutor-ref="cookieCipherExecutor" />

{% endhighlight %}

## Cookie Generation for Renewed Authentications

By default, forced authentication requests that challenge the user for credentials
either via the [`renew` request parameter](../protocol/CAS-Protocol.html)
or via [the service-specific setting](Service-Management.html) of
the CAS service registry will always generate the ticket-granting cookie
nonetheless. What this means is, logging in to a non-SSO-participating application
via CAS nonetheless creates a valid CAS single sign-on session that will be honored on a
subsequent attempt to authenticate to a SSO-participating application.

Plausibly, a CAS adopter may want this behavior to be different, such that logging in to a non-SSO-participating application
via CAS either does not create a CAS SSO session and the SSO session it creates is not honored for authenticating subsequently
to an SSO-participating application. This might better match user expectations.

The controlling of this behavior is done via the `cas.properties` file:

{% highlight properties %}
##
# Single Sign-On Session
#
# Indicates whether an SSO session should be created for renewed authentication requests.
# create.sso.renewed.authn=true
{% endhighlight %}


