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
import mstc.cloud.worker.config.WorkerConfig;
import mstc.cloud.worker.domain.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dreedy
 */
@Service
@SuppressWarnings("unused")
public class RequestSender {
    @Inject
    private WorkerConfig workerConfig;
    private final RabbitTemplate rabbitTemplate;
    @Autowired
    private Queue workQueue;
    @Inject
    private ObjectMapper objectMapper;
    private static final AtomicInteger counter = new AtomicInteger();
    private static final Logger logger = LoggerFactory.getLogger(RequestSender.class);

    @Autowired
    public RequestSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(Request request) throws Exception {
        String requestJson = objectMapper.writeValueAsString(request);
        Message message = MessageBuilder
                .withBody(requestJson.getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        Object result = rabbitTemplate.convertSendAndReceive(workerConfig.getExchange(), workQueue.getName(), message);
        System.out.println(result);
    }

    public void sendAndReceive(Request request) throws Exception {
        String requestJson = objectMapper.writeValueAsString(request);
        Message requestMessage = MessageBuilder
                .withBody(requestJson.getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setCorrelationId(UUID.randomUUID().toString())
                .setReplyTo(workQueue.getName())
                .build();

        /*UUID correlationId = UUID.randomUUID();
        MessagePostProcessor messagePostProcessor = message -> {
            MessageProperties messageProperties
                    = message.getMessageProperties();
            messageProperties.setReplyTo(workQueue.getName());
            messageProperties.setCorrelationId(correlationId.toString());
            return message;
        };*/

        //rabbitTemplate.convertAndSend(exchange, routingKey, requestMessage, messagePostProcessor);
        Object result = rabbitTemplate.convertSendAndReceive(workerConfig.getExchange(),
                                                             workQueue.getName(), requestMessage/*, messagePostProcessor*/);
        System.out.println(result);
    }
}
