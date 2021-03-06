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
package com.netflix.priam.merics;

/**
 * Created by vinhn on 10/14/16.
 */
public class SnapshotBackupMeasurement implements IMeasurement<Object> {
    private int failure = 0, success = 0;

    @Override
    public MMEASUREMENT_TYPE getType() {
        return MMEASUREMENT_TYPE.SNAPSHOTBACKUP;
    }

    public void incrementFailureCnt(int val) {
        this.failure += val;
    }

    public int getFailureCnt() {
        return this.failure;
    }

    public void incrementSuccessCnt(int val) {
        this.success += val;
    }

    public int getSuccessCnt() {
        return this.success;
    }

    @Override
    public Object getVal() {
        return null;
    }

    @Override
    public void setVal(Object val) {

    }
}
