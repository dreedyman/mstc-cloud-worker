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

package mstc.cloud.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Setter;
import mstc.cloud.worker.data.DataService;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.job.K8sJob;
import mstc.cloud.worker.job.JobRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dreedy
 */
@Service
@SuppressWarnings("unused")
public class WorkerRequestProcessor {
    @Autowired
    private JobRunner jobRunner;
    @Setter
    private String namespace;
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private DataService dataService;
    private static final Logger logger = LoggerFactory.getLogger(WorkerRequestProcessor.class);

    public String processRequest(Request request) {
        K8sJob k8sJob = createJob(request);
        processJob(k8sJob, null, request.getOutputBucket(), request.getPrefix());
        return "Job " + k8sJob.getJobNameUnique() + " complete.";
    }

    String processJob(K8sJob k8sJob, KubernetesClient client, String bucket, String prefix) {
        jobRunner.setClient(client);
        logger.info("Submitting job: " + k8sJob.getJobNameUnique());
        String output = jobRunner.submit(k8sJob);
        if (logger.isDebugEnabled()) {
            logger.debug("Result:\n" + output);
        }
        writeLogAndSend(output, k8sJob.getJobNameUnique() + ".log", bucket, prefix);
        return output;
    }

    private void writeLogAndSend(String content, String name, String bucket, String prefix)  {
        String tmpDir = System.getenv("SCRATCH_DIR") == null ?
                System.getProperty("java.io.tmpdir") :
                System.getenv("SCRATCH_DIR");

        File log = new File(tmpDir, name);
        try {
            Files.write(log.toPath(), content.getBytes(StandardCharsets.UTF_8));
            dataService.upload(bucket, prefix, log);
        } catch (Exception e) {
            logger.warn("Problem writing or sending log file", e);
        } finally {
            log.delete();
        }
    }

    public K8sJob createJob(Request request) {
        Map<String, String> env = new HashMap<>();
        env.put("MSTC_JOB", "true");
        env.put("INPUT_BUCKET", request.getInputBucket());
        env.put("OUTPUT_BUCKET", request.getOutputBucket());
        if (request.getPrefix() != null) {
            env.put("FILE_PREFIX", request.getPrefix());
        }
        int timeOut = request.getTimeOut() == 0 ? K8sJob.DEFAULT_TIMEOUT_MINUTES : request.getTimeOut();
        return K8sJob.builder()
                     .jobName(request.getJobName())
                     .namespace(namespace == null ? "mstc-dev" : namespace)
                     .timeOut(timeOut)
                     .image(request.getImage())
                     .env(env).build();
    }

}
