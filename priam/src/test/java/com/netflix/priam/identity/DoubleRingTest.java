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

package com.netflix.priam.identity;

import java.util.List;

import com.netflix.priam.identity.token.TokenRetrieverBase;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;

public class DoubleRingTest extends InstanceTestUtils
{

    @Test
    public void testDouble() throws Exception
    {
        createInstances();
        int originalSize = factory.getAllIds(config.getAppName()).size();
        new DoubleRing(config, factory, tokenManager).doubleSlots();
        List<PriamInstance> doubled = factory.getAllIds(config.getAppName());
        factory.sort(doubled);

        assertEquals(originalSize * 2, doubled.size());
        validate(doubled);
    }

    private void validate(List<PriamInstance> doubled)
    {
        List<String> validator = Lists.newArrayList();
        for (int i = 0; i < doubled.size(); i++)
        {
            validator.add(tokenManager.createToken(i, doubled.size(), config.getDC()));
            
        }
        
        for (int i = 0; i < doubled.size(); i++)
        {
            PriamInstance ins = doubled.get(i);
            assertEquals(validator.get(i), ins.getToken());
            int id = ins.getId() - tokenManager.regionOffset(config.getDC());
            System.out.println(ins);
            if (0 != id % 2)
                assertEquals(ins.getInstanceId(), TokenRetrieverBase.DUMMY_INSTANCE_ID);
        }
    }

    @Test
    public void testBR() throws Exception
    {
        createInstances();
        int intialSize = factory.getAllIds(config.getAppName()).size();
        DoubleRing ring = new DoubleRing(config, factory, tokenManager);
        ring.backup();
        ring.doubleSlots();
        assertEquals(intialSize * 2, factory.getAllIds(config.getAppName()).size());
        ring.restore();
        assertEquals(intialSize, factory.getAllIds(config.getAppName()).size());
    }
}
