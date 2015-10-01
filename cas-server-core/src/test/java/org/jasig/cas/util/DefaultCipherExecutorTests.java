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

package org.jasig.cas.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for {@link org.jasig.cas.util.DefaultCipherExecutor}.
 *
 * @author Misagh Moayyed
 * @since 4.1
 */
public class DefaultCipherExecutorTests {

    @Test
    public void checkEncryptionWithDefaultSettings() {
        final CipherExecutor cipherExecutor = new DefaultCipherExecutor("1PbwSbnHeinpkZOSZjuSJ8yYpUrInm5aaV18J2Ar4rM",
                "szxK-5_eJjs-aUj-64MpUZ-GPPzGLhYPLGl0wrYjYNVAGva2P0lLe6UGKGM7k8dWxsOVGutZWgvmY3l5oVPO3w");
        assertEquals(cipherExecutor.decode(cipherExecutor.encode("CAS Test")), "CAS Test");
    }
}
