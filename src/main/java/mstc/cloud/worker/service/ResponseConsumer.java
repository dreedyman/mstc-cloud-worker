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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * @author dreedy
 */
@Service
@SuppressWarnings("unused")
public class ResponseConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ResponseConsumer.class);

    @RabbitListener(queues = "#{workQueue.name}")
    public void receive(Message message) {
        String correlationId = message.getMessageProperties().getCorrelationId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        logger.info(String.format("Received (queue): %s\n%s\ncorrelation id: %s",
                                  message.getMessageProperties().getConsumerQueue(), body, correlationId));
    }
}
