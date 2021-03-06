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
package com.netflix.priam.defaultimpl;

import com.netflix.priam.CompositeConfigSource;
import com.netflix.priam.PropertiesConfigSource;
import com.netflix.priam.SimpleDBConfigSource;
import com.netflix.priam.SystemPropertiesConfigSource;

import javax.inject.Inject;

/**
 * Default {@link com.netflix.priam.IConfigSource} pulling in configs from SimpleDB, local Properties, and System Properties.
 */
public class PriamConfigSource extends CompositeConfigSource {

    @Inject
    public PriamConfigSource(final SimpleDBConfigSource simpleDBConfigSource,
                             final PropertiesConfigSource propertiesConfigSource,
                             final SystemPropertiesConfigSource systemPropertiesConfigSource) {
        // this order was based off PriamConfigurations loading.  W/e loaded last could override, but with Composite, first
        // has the highest priority.
        super(simpleDBConfigSource,
                systemPropertiesConfigSource,
                propertiesConfigSource);
    }
}
