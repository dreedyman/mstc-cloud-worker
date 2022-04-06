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

package mstc.cloud.worker.job;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;

import java.util.Collections;
import java.util.UUID;

/**
 * @author dreedy
 */
public class K8sJob {
    private String image;
    private String namespace;
    private String name;

    public K8sJob image(String image) {
        this.image = image;
        return this;
    }

    public K8sJob name(String name) {
        this.name = name;
        return this;
    }

    public K8sJob namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getImage() {
        return image;
    }

    public String getNamespace() {
        return namespace == null ? "default" : namespace;
    }

    public Job createJob() {
        String jobName = String.format("%s-%s", name, UUID.randomUUID());
        return new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                .withName(jobName)
                .withLabels(Collections.singletonMap("label1", "maximum-length-of-63-characters"))
                .withAnnotations(Collections.singletonMap("annotation1", "some-very-long-annotation"))
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName(name)
                //.withImage("perl")
                .withImage(image)
                .withArgs("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)")
                .endContainer()
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }
}
