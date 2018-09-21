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

import com.hippo.tnmb.client.ac.ACUrl;
import com.hippo.tnmb.client.data.Site;

public class NMBUrl {

    public static String getPostListUrl(Site site, String forum, int page) {
        switch (site.getId()) {
            case Site.AC:
                return ACUrl.getPostListUrl(forum, page);
            default:
                throw new IllegalStateException("Unknown site " + site);
        }
    }

    public static String getPostUrl(Site site, String id, int page) {
        switch (site.getId()) {
            case Site.AC:
                return ACUrl.getPostUrl(id, page);
            default:
                throw new IllegalStateException("Unknown site " + site);
        }
    }

    public static String getReferenceUrl(Site site, String id) {
        switch (site.getId()) {
            case Site.AC:
                return ACUrl.getReferenceUrl(id);
            default:
                throw new IllegalStateException("Unknown site " + site);
        }
    }

    public static String getBrowsablePostUrl(Site site, String id, int page) {
        switch (site.getId()) {
            case Site.AC:
                return ACUrl.getBrowsablePostUrl(id, page);
            default:
                throw new IllegalStateException("Unknown site " + site);
        }
    }
}
