/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol, 2012-2016 Ali Tokmen.
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
package org.codehaus.cargo.container.wildfly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.types.FilterChain;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.configuration.entry.DataSource;
import org.codehaus.cargo.container.configuration.script.ScriptCommand;
import org.codehaus.cargo.container.jboss.JBoss7xInstalledLocalDeployer;
import org.codehaus.cargo.container.jboss.JBossPropertySet;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.property.TransactionSupport;
import org.codehaus.cargo.container.wildfly.internal.AbstractWildFlyInstalledLocalContainer;
import org.codehaus.cargo.container.wildfly.internal.AbstractWildFlyStandaloneLocalConfiguration;
import org.codehaus.cargo.container.wildfly.internal.WildFly8xStandaloneLocalConfigurationCapability;
import org.codehaus.cargo.container.wildfly.internal.configuration.factory.WildFly8xCliConfigurationFactory;
import org.codehaus.cargo.container.wildfly.internal.configuration.factory.WildFlyCliConfigurationFactory;
import org.codehaus.cargo.container.wildfly.internal.util.WildFlyLogUtils;
import org.codehaus.cargo.container.wildfly.internal.util.WildFlyModuleUtils;

/**
 * WildFly 8.x standalone local configuration.
 */
public class WildFly8xStandaloneLocalConfiguration
    extends AbstractWildFlyStandaloneLocalConfiguration
{

    /**
     * WildFly container capability.
     */
    private static final ConfigurationCapability CAPABILITY =
        new WildFly8xStandaloneLocalConfigurationCapability();

    /**
     * CLI configuration factory.
     */
    private WildFly8xCliConfigurationFactory factory =
            new WildFly8xCliConfigurationFactory(this);

    /**
     * {@inheritDoc}
     * @see AbstractWildFlyStandaloneLocalConfiguration#AbstractWildFlyStandaloneLocalConfiguration(String)
     */
    public WildFly8xStandaloneLocalConfiguration(String dir)
    {
        super(dir);
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.cargo.container.configuration.Configuration#getCapability()
     */
    public ConfigurationCapability getCapability()
    {
        return CAPABILITY;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.cargo.container.wildfly.internal.WildFlyConfiguration#getConfigurationFactory()
     */
    public WildFlyCliConfigurationFactory getConfigurationFactory()
    {
        return factory;
    }

    /**
     * {@inheritDoc}
     * @see AbstractWildFlyStandaloneLocalConfiguration#doConfigure(LocalContainer)
     */
    @Override
    protected void doConfigure(LocalContainer c) throws Exception
    {
        AbstractWildFlyInstalledLocalContainer container =
                (AbstractWildFlyInstalledLocalContainer) c;
        super.doConfigure(c);

        String configurationXmlFile = "configuration/"
                + getPropertyValue(JBossPropertySet.CONFIGURATION) + ".xml";
        String wildFlyLogLevel = WildFlyLogUtils.getWildFlyLogLevel(
                getPropertyValue(GeneralPropertySet.LOGGING));

        // Configure ports and logging
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='ajp']",
            "port", JBossPropertySet.JBOSS_AJP_PORT);
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='management-http']",
            "port", JBossPropertySet.JBOSS_MANAGEMENT_HTTP_PORT);
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='http']",
            "port", ServletPropertySet.PORT);
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group",
            "port-offset", GeneralPropertySet.PORT_OFFSET);
        addXmlReplacement(
            configurationXmlFile,
            "//server/profile/subsystem/root-logger/level",
            "name", wildFlyLogLevel);

        List<ScriptCommand> configurationScript = new ArrayList<ScriptCommand>();

        // add modules
        for (String classpathElement : container.getExtraClasspath())
        {
            addModuleScript(classpathElement, container, configurationScript);
        }
        for (String classpathElement : container.getSharedClasspath())
        {
            addModuleScript(classpathElement, container, configurationScript);
        }

        container.executeScript(configurationScript);

        configureDataSources(container, configurationXmlFile);

        // deploy deployments
        String deployments;
        String altDeployDir = container.getConfiguration().
            getPropertyValue(JBossPropertySet.ALTERNATIVE_DEPLOYMENT_DIR);
        if (altDeployDir != null && !altDeployDir.equals(""))
        {
            container.getLogger().info("Using non-default deployment target directory " 
                + altDeployDir, this.getClass().getName());
            deployments = getFileHandler().append(getHome(), altDeployDir);
        }
        else
        {
            deployments = getFileHandler().append(getHome(), "deployments");
        }
        getResourceUtils().copyResource(RESOURCE_PATH + "cargocpc.war",
                new File(deployments, "cargocpc.war"));
        JBoss7xInstalledLocalDeployer deployer = new JBoss7xInstalledLocalDeployer(container);
        deployer.deploy(getDeployables());
    }

    /**
     * @param container Installed local container.
     * @param configurationXmlFile Configuration XML file (configuration/standalone....xml).
     * @throws IOException In case of IO error when editing config file.
     */
    private void configureDataSources(InstalledLocalContainer container,
        String configurationXmlFile) throws IOException
    {
        String configurationXml = getFileHandler().append(getHome(), configurationXmlFile);
        String tmpDir = getFileHandler().createUniqueTmpDirectory();
        try
        {
            List<String> driversList = new ArrayList<String>();

            StringBuilder datasources = new StringBuilder();
            StringBuilder drivers = new StringBuilder();

            for (DataSource dataSource : getDataSources())
            {
                String moduleName = WildFlyModuleUtils.getDataSourceDriverModuleName(
                        container, dataSource);

                FilterChain filterChain = createFilterChain();
                getAntUtils().addTokenToFilterChain(filterChain, "moduleName", moduleName);
                getAntUtils().addTokenToFilterChain(filterChain, "driverClass",
                    dataSource.getDriverClass());
                String jndiName = dataSource.getJndiLocation();

                if (!jndiName.startsWith("java:/"))
                {
                    jndiName = "java:/" + jndiName;
                    getLogger().warn("JBoss 7 requires datasource JNDI names to start with "
                        + "java:/, hence changing the given JNDI name to: " + jndiName,
                        this.getClass().getName());
                }

                getAntUtils().addTokenToFilterChain(filterChain, "jndiName", jndiName);
                getAntUtils().addTokenToFilterChain(filterChain, "url", dataSource.getUrl());
                getAntUtils().addTokenToFilterChain(filterChain, "username",
                    dataSource.getUsername());
                getAntUtils().addTokenToFilterChain(filterChain, "password",
                    dataSource.getPassword());

                String xa = "";
                if (TransactionSupport.XA_TRANSACTION.equals(dataSource.getTransactionSupport()))
                {
                    xa = "-xa";
                }

                if (!driversList.contains(dataSource.getDriverClass()))
                {
                    driversList.add(dataSource.getDriverClass());

                    String temporaryDriver = getFileHandler().append(tmpDir, "driver.xml");
                    getResourceUtils().copyResource(
                        RESOURCE_PATH + "wildfly-8/datasource/jboss-driver" + xa + ".xml",
                            temporaryDriver, getFileHandler(), filterChain, "UTF-8");
                    drivers.append("\n");
                    drivers.append(getFileHandler().readTextFile(temporaryDriver, "UTF-8"));
                }

                String temporaryDatasource = getFileHandler().append(tmpDir, "datasource.xml");
                getResourceUtils().copyResource(
                    RESOURCE_PATH + "wildfly-8/datasource/jboss-datasource.xml",
                        temporaryDatasource, getFileHandler(), filterChain, "UTF-8");
                datasources.append("\n");
                datasources.append(getFileHandler().readTextFile(temporaryDatasource, "UTF-8"));
            }

            Map<String, String> replacements = new HashMap<String, String>(1);
            replacements.put("<drivers>", datasources + "<drivers>" + drivers);
            getFileHandler().replaceInFile(configurationXml, replacements, "UTF-8");
        }
        finally
        {
            getFileHandler().delete(tmpDir);
        }
    }
}
