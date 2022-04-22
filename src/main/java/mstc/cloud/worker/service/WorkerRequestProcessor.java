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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.job.K8sJob;
import mstc.cloud.worker.job.K8sJobRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dreedy
 */
@Service
@SuppressWarnings("unused")
public class WorkerRequestProcessor {
    @Inject
    private ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(WorkerRequestProcessor.class);

    public String processRequest(Request request) throws Exception {
        K8sJob k8sJob = createJob(request);
        K8sJobRunner jobRunner = new K8sJobRunner();
        logger.info("Submitting job: " + request.getJobName());
        String output = jobRunner.submit(k8sJob);
        logger.info("Result:\n" + output);
        return output;
    }

    public K8sJob createJob(Request request) {
        Map<String, String> env = new HashMap<>();
        env.put("INPUT_BUCKET", request.getInputBucket());
        if (request.getOutputBucket() != null) {
            env.put("OUTPUT_BUCKET", request.getOutputBucket());
        }
        int timeOut = request.getTimeOut() == 0 ? K8sJob.DEFAULT_TIMEOUT_MINUTES : request.getTimeOut();
        return K8sJob.builder()
                     .jobName(request.getJobName())
                     .namespace("mstc-dev")
                     .timeOut(timeOut)
                     .image(request.getImage())
                     .env(env).build();
    }

}
