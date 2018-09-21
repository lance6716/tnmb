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

package com.hippo.tnmb.client.ac.data;

import java.util.List;

public class ACForumGroup {

    public String id;
    public String sort;
    public String name;
    public String status;
    public List<ACForum> forums;

    @Override
    public String toString() {
        return "id = " + id + ", sort = " + sort + ", name = " + name + ", status = " + status + ", forums = " + forums;
    }
}
