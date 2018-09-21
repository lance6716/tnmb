/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.tnmb.client;

import com.hippo.tnmb.client.data.Site;

public class NMBRequest {

    int method;
    Site site;
    Object[] args;
    NMBClient.Callback callback;

    NMBClient.Task task;

    private boolean mCancel = false;

    public void setMethod(int method) {
        this.method = method;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public void setArgs(Object... args) {
        this.args = args;
    }

    public void setCallback(NMBClient.Callback callback) {
        this.callback = callback;
    }

    public void cancel() {
        if (!mCancel) {
            mCancel = true;
            if (task != null) {
                task.stop();
                task = null;
            }
        }
    }

    public boolean isCancelled() {
        return mCancel;
    }
}
