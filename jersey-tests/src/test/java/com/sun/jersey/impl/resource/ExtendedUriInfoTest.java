/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.jersey.impl.resource;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ExtendedUriInfo;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.impl.AbstractResourceTester;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import java.io.IOException;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class ExtendedUriInfoTest extends AbstractResourceTester {

    public ExtendedUriInfoTest(String testName) {
        super(testName);
    }

    @Path("/")
    public static class MatchedMethodResource {
        @Context ExtendedUriInfo eui;

        @GET
        public String get() { 
            assertNull(eui.getMappedThrowable());
            assertNotNull(eui.getMatchedMethod());
            assertEquals("get", eui.getMatchedMethod().getMethod().getName());
            return "GET";
        }

        @GET
        @Path("exception")
        public String getWithException() {
            assertNull(eui.getMappedThrowable());
            assertNotNull(eui.getMatchedMethod());
            assertEquals("getWithException", eui.getMatchedMethod().getMethod().getName());
            throw new WebApplicationException(Response.ok("exception").build());
        }
    }

    public void testMatchedMethodAndThrowable() throws IOException {
        ResourceConfig rc = new DefaultResourceConfig(MatchedMethodResource.class);
        rc.getContainerResponseFilters().add(new ContainerResponseFilter() {
            @Context ExtendedUriInfo eui;
            
            @Override
            public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
                if (eui.getMappedThrowable() != null) {
                    response.getHttpHeaders().putSingle("throwable", eui.getMappedThrowable().getClass().getSimpleName());
                }
                if (eui.getMatchedMethod() != null) {
                    response.getHttpHeaders().putSingle("method", eui.getMatchedMethod().getMethod().getName());
                }
                return response;
            }
        });
        initiateWebApplication(rc);
        WebResource r = resource("/");

        ClientResponse cr = r.get(ClientResponse.class);
        assertNull(cr.getHeaders().getFirst("throwable"));
        assertNotNull(cr.getHeaders().getFirst("method"));
        assertEquals("get", cr.getHeaders().getFirst("method"));

        cr = r.path("exception").get(ClientResponse.class);
        assertNotNull(cr.getHeaders().getFirst("throwable"));
        assertEquals("WebApplicationException", cr.getHeaders().getFirst("throwable"));
        assertNotNull(cr.getHeaders().getFirst("method"));
        assertEquals("getWithException", cr.getHeaders().getFirst("method"));
    }
}