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

import lombok.*;

/**
 * @author dreedy
 */
@Data
@NoArgsConstructor
public class Request {
    private String image;
    private String jobName;
    private int timeOut;
    private String inputBucket;
    private String outputBucket;
    private String prefix;

    public Request(String image,
                   String jobName,
                   int timeOut,
                   String inputBucket,
                   String outputBucket,
                   String prefix) {
        this.image = image;
        this.jobName = jobName;
        this.timeOut = timeOut;
        this.inputBucket = inputBucket;
        this.outputBucket = outputBucket;
        this.prefix = prefix;
    }

    public Request image(String image) {
        this.image = image;
        return this;
    }

    public Request jobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    public Request timeOut(int timeOut) {
        this.timeOut = timeOut;
        return this;
    }

    public Request inputBucket(String inputBucket) {
        this.inputBucket = inputBucket;
        return this;
    }

    public Request outputBucket(String outputBucket) {
        this.outputBucket = outputBucket;
        return this;
    }

    public String getOutputBucket() {
        if (outputBucket == null) {
            return inputBucket;
        }
        return outputBucket;
    }



}
