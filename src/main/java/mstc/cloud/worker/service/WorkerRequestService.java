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
import mstc.cloud.worker.domain.RequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dreedy
 */
@Service
@SuppressWarnings("unused")
public class WorkerRequestService {
    @Inject
    private ObjectMapper objectMapper;
    @Autowired
    private RequestValidator requestValidator;
    @Autowired
    private WorkerRequestProcessor workerRequestProcessor;
    private static final Logger logger = LoggerFactory.getLogger(WorkerRequestService.class);

    @RabbitListener(queues = "#{workQueue.name}")
    public String receiveMessage(Message message) throws Exception {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String output;
        try {
            Request request = objectMapper.readValue(body, Request.class);
            if (requestValidator.isValidRequest(request)) {
                logger.info("Received\n" + request);
                output = workerRequestProcessor.processRequest(request);
            } else {
                output = "Invalid request "+ String.format("No image: %s", body);
            }
        } catch(JsonProcessingException e) {
            logger.error("Processing JSON", e);
            output = String.format("%s: %s", e.getClass().getName(), e.getMessage());
        } catch(Exception e) {
            logger.error("Error running Job", e);
            output = String.format("%s: %s", e.getClass().getName(), e.getMessage());
        }
        Map<String, String> result = new HashMap<>();
        result.put("result", output);
        return objectMapper.writeValueAsString(result);
    }


}
