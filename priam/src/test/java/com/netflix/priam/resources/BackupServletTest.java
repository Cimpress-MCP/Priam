/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.priam.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.netflix.priam.ICassandraProcess;
import com.netflix.priam.IConfiguration;
import com.netflix.priam.PriamServer;
import com.netflix.priam.backup.*;
import com.netflix.priam.health.InstanceState;
import com.netflix.priam.identity.IPriamInstanceFactory;
import com.netflix.priam.identity.InstanceIdentity;
import com.netflix.priam.identity.PriamInstance;
import com.netflix.priam.restore.Restore;
import com.netflix.priam.restore.RestoreTokenSelector;
import com.netflix.priam.tuner.ICassandraTuner;
import com.netflix.priam.utils.ITokenManager;
import com.netflix.priam.utils.TokenManager;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class BackupServletTest
{
    private @Mocked PriamServer priamServer;
    private IConfiguration config;
    private @Mocked IBackupFileSystem bkpFs;
    private @Mocked IBackupFileSystem bkpStatusFs;
    private @Mocked Restore restoreObj;
    private @Mocked Provider<AbstractBackupPath> pathProvider;
    private @Mocked
    ICassandraTuner tuner;
    private @Mocked SnapshotBackup snapshotBackup;
    private @Mocked IPriamInstanceFactory factory;
    private @Mocked ICassandraProcess cassProcess;
    private @Mocked BackupStatusMgr bkupStatusMgr;
    private @Mocked IBackupFileSystem backupFileSystem;
    private BackupServlet resource;
    private RestoreServlet restoreResource;
    private BackupVerification backupVerification;
    private static Injector injector;
    private InstanceState instanceState;

    @Before
    public void setUp()
    {
        injector = Guice.createInjector(new BRTestModule());
        config = injector.getInstance(IConfiguration.class);
        instanceState = injector.getInstance(InstanceState.class);
        RestoreTokenSelector tokenSelector = new RestoreTokenSelector(new TokenManager(config), backupFileSystem, factory);
        resource = new BackupServlet(priamServer, config, bkpFs, bkpStatusFs, restoreObj, pathProvider,
            tuner, snapshotBackup, tokenSelector, cassProcess, bkupStatusMgr,backupVerification);

        restoreResource = new RestoreServlet(config, restoreObj, pathProvider,priamServer, tokenSelector, tuner, cassProcess
        		, instanceState);
    }

    @Test
    public void backup() throws Exception
    {
        new Expectations() {{
            snapshotBackup.execute();
        }};

        Response response = resource.backup();
        assertEquals(200, response.getStatus());
        assertEquals("[\"ok\"]", response.getEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMetadata().get("Content-Type").get(0));
    }

    @Test
    public void restore_minimal(@Mocked final InstanceIdentity identity) throws Exception
    {
        final String dateRange = null;
        final String newRegion = null;
        final String newToken = null;
        final String keyspaces = null;

        final String oldRegion = "us-east-1";
        final String oldToken = "1234";

        new Expectations() {
            {
              priamServer.getId(); result = identity; times = 3;
            }
        };

        new Expectations() {

            {
                config.getDC(); result = oldRegion;
                identity.getBackupIdentifier(); result = oldToken;

                config.isRestoreClosestToken(); result = false;

                restoreObj.restore((Date) any, (Date) any); // TODO: test default value

                config.setDC(oldRegion);
                identity.setBackupIdentifier(oldToken);
                tuner.updateAutoBootstrap(config.getYamlLocation(), false);
            }
        };

        expectCassandraStartup();

        Response response = restoreResource.restore(dateRange, newRegion, newToken, keyspaces, null);
        assertEquals(200, response.getStatus());
        assertEquals("[\"ok\"]", response.getEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMetadata().get("Content-Type").get(0));
    }

    @Test
    public void restore_withDateRange(@Mocked final InstanceIdentity identity,
	@Mocked final AbstractBackupPath backupPath) throws Exception
    {
        final String dateRange = "201101010000,20111231259";
        final String newRegion = null;
        final String newToken = null;
        final String keyspaces = null;

        final String oldRegion = "us-east-1";
        final String oldToken = "1234";

        new Expectations() {
            {
              priamServer.getId(); result = identity; times = 3;
            }
        };
        new Expectations() {

            {
                pathProvider.get(); result = backupPath;
                backupPath.parseDate(dateRange.split(",")[0]); result = new DateTime(2011, 01, 01, 00, 00).toDate(); times = 1;
                backupPath.parseDate(dateRange.split(",")[1]); result = new DateTime(2011, 12, 31, 23, 59).toDate(); times = 1;

//                config.getDC(); result = oldRegion;
                identity.getBackupIdentifier(); result = oldToken;

 //               config.isRestoreClosestToken(); result = false;

                restoreObj.restore(
                    new DateTime(2011, 01, 01, 00, 00).toDate(),
                    new DateTime(2011, 12, 31, 23, 59).toDate());

 //               config.setDC(oldRegion);
                identity.setBackupIdentifier(oldToken);
                tuner.updateAutoBootstrap(config.getYamlLocation(), false);
            }
        };

        expectCassandraStartup();

        Response response = restoreResource.restore(dateRange, newRegion, newToken, keyspaces, null);
        assertEquals(200, response.getStatus());
        assertEquals("[\"ok\"]", response.getEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMetadata().get("Content-Type").get(0));
    }

//    @Test
//    public void restore_withRegion() throws Exception
//    {
//        final String dateRange = null;
//        final String newRegion = "us-west-1";
//        final String newToken = null;
//        final String keyspaces = null;
//
//        final String oldRegion = "us-east-1";
//        final String oldToken = "1234";
//        final String appName = "myApp";
//
//        new Expectations() {
//            @NonStrict InstanceIdentity identity;
//            PriamInstance instance;
//            @NonStrict PriamInstance instance1, instance2, instance3;
//
//            {
//                config.getDC(); result = oldRegion;
//                priamServer.getId(); result = identity; times = 3;
//                identity.getInstance(); result = instance; times = 3;
//                instance.getToken(); result = oldToken;
//
//                config.isRestoreClosestToken(); result = false;
//
//                config.setDC(newRegion);
//                instance.getToken(); result = oldToken;
//                config.getAppName(); result = appName;
//                factory.getAllIds(appName); result = ImmutableList.of(instance, instance1, instance2, instance3);
//                instance.getDC();  result = oldRegion;
//                instance.getToken(); result = oldToken;
//                instance1.getDC(); result = oldRegion;
//                instance2.getDC(); result = oldRegion;
//                instance3.getDC(); result = oldRegion;
//                instance1.getToken(); result = "1234";
//                instance2.getToken(); result = "5678";
//                instance3.getToken(); result = "9000";
//                instance.setToken((String) any); // TODO: test mocked closest token
//
//                restoreObj.restore((Date) any, (Date) any); // TODO: test default value
//
//                config.setDC(oldRegion);
//                instance.setToken(oldToken);
//                tuneCassandra.writeAllProperties(false);
//            }
//        };
//
//        expectCassandraStartup();
//
//        Response response = resource.restore(dateRange, newRegion, newToken, keyspaces);
//        assertEquals(200, response.getStatus());
//        assertEquals("[\"ok\"]", response.getEntity());
//        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMetadata().get("Content-Type").get(0));
//    }

    @Test
    public void restore_withToken(@Mocked final InstanceIdentity identity) throws Exception
    {
        final String dateRange = null;
        final String newRegion = null;
        final String newToken = "myNewToken";
        final String keyspaces = null;

        final String oldRegion = "us-east-1";
        final String oldToken = "1234";

        new Expectations() {
            {
              priamServer.getId(); result = identity; times = 3;
            }
        };
        new Expectations() {

            {
                config.getDC(); result = oldRegion;
                identity.getBackupIdentifier(); result = oldToken;
                identity.setBackupIdentifier(newToken);

                //config.isRestoreClosestToken(); result = false;

                restoreObj.restore((Date) any, (Date) any); // TODO: test default value

                config.setDC(oldRegion);
                identity.setBackupIdentifier(oldToken);
                tuner.updateAutoBootstrap(config.getYamlLocation(), false);
            }
        };

        expectCassandraStartup();

        Response response = restoreResource.restore(dateRange, newRegion, newToken, keyspaces, null);
        assertEquals(200, response.getStatus());
        assertEquals("[\"ok\"]", response.getEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMetadata().get("Content-Type").get(0));
    }

    @Test
    public void restore_withKeyspaces(@Mocked final InstanceIdentity identity) throws Exception
    {
        final String dateRange = null;
        final String newRegion = null;
        final String newToken = null;
        final String keyspaces = "keyspace1,keyspace2";

        final String oldRegion = "us-east-1";
        final String oldToken = "1234";

       new Expectations() {
            {
              config.getDC(); result = oldRegion;
              config.isRestoreClosestToken(); result = false;

              List<String> restoreKeyspaces = Lists.newArrayList();
              restoreKeyspaces.clear();
              restoreKeyspaces.addAll(ImmutableList.of("keyspace1", "keyspace2"));

              config.getRestoreKeySpaces(); result = restoreKeyspaces;
              config.setDC(oldRegion);
              priamServer.getId(); result = identity; times = 3;
            }
        };
        new Expectations() {

            {
                identity.getBackupIdentifier(); result = oldToken;

                restoreObj.restore((Date) any, (Date) any); // TODO: test default value

                identity.setBackupIdentifier(oldToken);
                tuner.updateAutoBootstrap(config.getYamlLocation(), false);
            }
        };

        expectCassandraStartup();

        Response response = restoreResource.restore(dateRange, newRegion, newToken, keyspaces, null);
        assertEquals(200, response.getStatus());
        assertEquals("[\"ok\"]", response.getEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMetadata().get("Content-Type").get(0));
    }

    // TODO: this should also set/test newRegion and keyspaces
    @Test
    public void restore_maximal(@Mocked final InstanceIdentity identity,
        @Mocked final PriamInstance instance, @Mocked final PriamInstance instance1,
        @Mocked final PriamInstance instance2, @Mocked final PriamInstance instance3,
        @Mocked final AbstractBackupPath backupPath) throws Exception
    {
        final String dateRange = "201101010000,20111231259";
        final String newRegion = null;
        final String newToken = "5678";
        final String keyspaces = null;

        final String oldRegion = "us-east-1";
        final String oldToken = "1234";
        final String appName = "myApp";

        instance.setDC(oldRegion);
        instance1.setDC(oldRegion);
        instance2.setDC(oldRegion);
        instance3.setDC(oldRegion);
        instance.setToken(oldToken);
        instance1.setToken("1234");
        instance2.setToken("5678");
        instance3.setToken("9000");

        new Expectations() {

            {
                pathProvider.get(); result = backupPath;
                backupPath.parseDate(dateRange.split(",")[0]); result = new DateTime(2011, 01, 01, 00, 00).toDate(); times = 1;
                backupPath.parseDate(dateRange.split(",")[1]); result = new DateTime(2011, 12, 31, 23, 59).toDate(); times = 1;

//                identity.getInstance(); result = instance; times = 5;
//                instance.getToken(); result = oldToken;
//                instance.setToken(newToken);
//
//                instance.getToken(); result = oldToken;
//                factory.getAllIds(appName); result = ImmutableList.of(instance, instance1, instance2, instance3);
//                instance.getDC();  result = oldRegion;
//                instance.getToken(); result = oldToken;
//                instance1.getDC(); result = oldRegion;
//                instance2.getDC(); result = oldRegion;
//                instance3.getDC(); result = oldRegion;
//                instance1.getToken(); result = "1234";
//                instance2.getToken(); result = "5678";
//                instance3.getToken(); result = "9000";
//                instance.setToken((String) any); // TODO: test mocked closest token

                restoreObj.restore(
                    new DateTime(2011, 01, 01, 00, 00).toDate(),
                    new DateTime(2011, 12, 31, 23, 59).toDate());

               // instance.setToken(oldToken);
                tuner.updateAutoBootstrap(config.getYamlLocation(), false);
            }
        };

        expectCassandraStartup();

        Response response = restoreResource.restore(dateRange, newRegion, newToken, keyspaces, null);
        assertEquals(200, response.getStatus());
        assertEquals("[\"ok\"]", response.getEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMetadata().get("Content-Type").get(0));
    }

    // TODO: create CassandraController interface and inject, instead of static util method
    private Expectations expectCassandraStartup() {
        return new Expectations() {{
            config.getCassStartupScript(); result = "/usr/bin/false";
            config.getHeapNewSize(); result = "2G";
            config.getHeapSize(); result = "8G";
            config.getDataFileLocation(); result = "/var/lib/cassandra/data";
            config.getCommitLogLocation(); result = "/var/lib/cassandra/commitlog";
            config.getBackupLocation(); result = "backup";
            config.getCacheLocation(); result = "/var/lib/cassandra/saved_caches";
            config.getJmxPort(); result = 7199;
            config.getMaxDirectMemory(); result = "50G";
        }};
    }
}
