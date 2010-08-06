/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2010 Vincent Massol.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ========================================================================
 */
package org.codehaus.cargo.container.jboss;

import java.net.URL;
import java.net.URLEncoder;

import org.jmock.MockObjectTestCase;
import org.jmock.Mock;
import org.codehaus.cargo.container.RemoteContainer;
import org.codehaus.cargo.container.jboss.internal.HttpURLConnection;
import org.codehaus.cargo.container.jboss.internal.ISimpleHttpFileServer;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.configuration.RuntimeConfiguration;

/**
 * Unit tests for {@link JBossRemoteDeployer}.
 *
 * @version $Id$
 */
public class JBossRemoteDeployerTest extends MockObjectTestCase
{
    public void testCreateJBossRemoteURLForDeploy() throws Throwable
    {
        Mock mockConfiguration = mock(RuntimeConfiguration.class);
        mockConfiguration.stubs().method("getPropertyValue").with(eq("cargo.protocol"))
            .will(returnValue("http"));
        mockConfiguration.stubs().method("getPropertyValue").with(eq("cargo.hostname"))
            .will(returnValue("remotehost"));
        mockConfiguration.stubs().method("getPropertyValue").with(eq("cargo.servlet.port"))
            .will(returnValue("8888"));
        mockConfiguration.stubs().method("getPropertyValue").with(eq("cargo.remote.username"))
            .will(returnValue("john"));
        mockConfiguration.stubs().method("getPropertyValue").with(eq("cargo.remote.password"))
            .will(returnValue("doe"));
        mockConfiguration.stubs().method("getPropertyValue").with(eq("cargo.jboss.remotedeploy.port"))
            .will(returnValue("9999"));
        mockConfiguration.stubs().method("getPropertyValue").with(eq("cargo.jboss.remotedeploy.hostname"))
            .will(returnValue("localhost"));

        Mock mockContainer = mock(RemoteContainer.class);
        mockContainer.stubs().method("getConfiguration")
            .will(returnValue(mockConfiguration.proxy()));

        Mock mockDeployable = mock(Deployable.class);
        mockDeployable.stubs().method("getFile").will(
            returnValue("c:/Something With Space/dummy.war"));

        String mockURL = "http://localhost:9999/Something+With+Space";
        Mock mockHttpFileServer = mock(ISimpleHttpFileServer.class);
        mockHttpFileServer.stubs().method("setLogger");
        mockHttpFileServer.stubs().method("setFile").after("setLogger");
        mockHttpFileServer.stubs().method("setListeningParameters").after("setFile");
        mockHttpFileServer.stubs().method("start").after("setListeningParameters");
        mockHttpFileServer.stubs().method("getURL").after("start").will(returnValue(
            new URL(mockURL)));
        mockHttpFileServer.stubs().method("getCallCount").will(returnValue(0));
        mockHttpFileServer.stubs().method("getCallCount").after("start").will(returnValue(1));
        mockHttpFileServer.stubs().method("stop").after("start");

        Mock mockConnection = mock(HttpURLConnection.class);
        String expectedURLPortion = URLEncoder.encode(mockURL, "UTF-8");
        mockConnection.expects(once()).method("connect").with(stringContains(expectedURLPortion),
            eq("john"), eq("doe"));

        JBossRemoteDeployer deployer = new JBossRemoteDeployer((RemoteContainer) mockContainer.proxy(),
            (HttpURLConnection) mockConnection.proxy(),
            (ISimpleHttpFileServer) mockHttpFileServer.proxy());
        deployer.deploy((Deployable) mockDeployable.proxy());
    }
}
