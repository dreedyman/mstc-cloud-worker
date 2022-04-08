/*
 *
 *  * Copyright to the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package mstc.cloud.worker.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author dreedy
 */
@Data
@NoArgsConstructor
public class DataItem {
    private String endpoint;
    private String bucket;
    private final List<String> itemNames = new ArrayList<>();

    public DataItem(String endpoint, String bucket, String... items) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.itemNames.addAll(Arrays.asList(items));
    }

    public DataItem endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public DataItem bucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public DataItem itemName(String itemName) {
        this.itemNames.add(itemName);
        return this;
    }

    public DataItem itemNames(String... itemNames) {
        this.itemNames.addAll(Arrays.asList(itemNames));
        return this;
    }
}
