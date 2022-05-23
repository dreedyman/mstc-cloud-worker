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
import io.fabric8.kubernetes.client.KubernetesClient;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.job.K8sJob;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author dreedy
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkerRequestServiceTest {
    private static final String IN_BUCKET = "worker.request.service.in.bucket";
    private static final String OUT_BUCKET = "worker.request.service.out.bucket";

    @Inject
    private ObjectMapper mapper;
    @Mock
    private WorkerRequestProcessor workerRequestProcessor;
    @Autowired
    @InjectMocks
    private WorkerRequestService sut;

    @Before
    public  void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(workerRequestProcessor.processJob(any(K8sJob.class),
                                               any(KubernetesClient.class),
                                               any(String.class),
                                               any(String.class)))
                .thenReturn("Job Completed");
    }

    @Test
    public void testWorker() throws Exception {
        String image = "mstc/python-test";
        Request request = new Request(image,
                                      "test-job",
                                      5,
                                      IN_BUCKET,
                                      OUT_BUCKET,
                                      Integer.toString( "test-job".hashCode()));
        String output = sut.receiveMessage(createMessage(request));
        assertNotNull(output);
    }

    Message createMessage(Request request) throws JsonProcessingException {
        String requestJson = mapper.writeValueAsString(request);
        return  MessageBuilder
                .withBody(requestJson.getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
    }

}
