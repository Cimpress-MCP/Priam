/**
 * Copyright 2017 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.priam.identity.config;

import org.codehaus.jettison.json.JSONException;

/**
 * Looks at local (system) properties for metadata about the running 'instance'.
 * Typically, this is used for locally-deployed testing.
 */
public class LocalInstanceDataRetriever implements InstanceDataRetriever {
    private static final String PREFIX = "Priam.localInstance.";

    public String getRac() {
        return System.getProperty(PREFIX + "availabilityZone", "");
    }

    public String getPublicHostname() {
        return System.getProperty(PREFIX + "publicHostname", "");
    }

    public String getPublicIP() {
        return System.getProperty(PREFIX + "publicIp", "");
    }

    @Override
    public String getPrivateIP() {
        return System.getProperty(PREFIX + "privateIp", "");
    }

    public String getInstanceId() {
        return System.getProperty(PREFIX + "instanceId", "");
    }

    public String getInstanceType() {
        return System.getProperty(PREFIX + "instanceType", "");
    }

    @Override
    public String getMac() {
        return System.getProperty(PREFIX + "networkinterface", "");
    }

    @Override
    public String getVpcId() {
        return System.getProperty(PREFIX + "vpcid", "");
    }

    @Override
    public String getAWSAccountId() throws JSONException {
        return System.getProperty(PREFIX + "awsacctid", "");
    }

    @Override
    public String getAvailabilityZone() throws JSONException {
        return System.getProperty(PREFIX + "availabilityzone", "");
    }

    @Override
    public String getRegion() throws JSONException {
        return System.getProperty(PREFIX + "region", "");
    }
}