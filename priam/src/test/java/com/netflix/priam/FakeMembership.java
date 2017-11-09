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

package com.netflix.priam;

import java.util.Collection;
import java.util.List;

import com.netflix.priam.identity.IMembership;
import com.netflix.priam.identity.PriamInstance;

public class FakeMembership implements IMembership
{

    private List<String> instances;

    public FakeMembership(List<String> priamInstances)
    {
        this.instances = priamInstances;
    }
    
    public void setInstances( List<String> priamInstances)
    {
        this.instances = priamInstances;
    }

    @Override
    public boolean isInstanceAlive(PriamInstance instance)
    {
        return instances.contains(instance.getInstanceId());
    }

    @Override
    public int getRacMembershipSize()
    {
        return 3;
    }

    @Override
    public int getRacCount()
    {
        return 3;
    }

    @Override
    public void addACL(Collection<String> listIPs, int from, int to)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeACL(Collection<String> listIPs, int from, int to)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<String> listACL(int from, int to)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
