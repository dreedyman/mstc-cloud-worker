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

package mstc.cloud.worker;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.minio.*;
import io.minio.messages.Item;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.service.DataService;
import mstc.cloud.worker.service.RequestSender;
import mstc.cloud.worker.service.WorkerRequestService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author dreedy
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkerRequestServiceTest {
    private final static String HOST = "localhost";
    private final static String PORT = "9000";
    private final static String IN_BUCKET = "test.in.bucket";
    @Autowired
    private RequestSender requestSender;

    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /*@Autowired
    private WorkerRequestService workerRequestService;*/
    private List<String> inputItems;
    private String endpoint;
    @Autowired
    private DataService dataService;
    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    @Before
    public  void setup() throws Exception {
        endpoint = String.format("http://%s:%s", HOST, PORT);
        check(new MinIOCheck());
        check(new RabbitMQCheck());
        File testFileDir = new File(System.getProperty("test.data.dir"));
        inputItems = dataService.upload(IN_BUCKET, testFileDir.listFiles());
        inputItems.add("foo");
        inputItems.add("bar");
    }

    @After
    public void cleanup() throws Exception {
        if (dataService.getMinioClient().bucketExists(BucketExistsArgs.builder().bucket(IN_BUCKET).build())) {
            Iterable<Result<Item>> results = dataService.getMinioClient()
                                                        .listObjects(ListObjectsArgs.builder().bucket(IN_BUCKET).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                dataService.getMinioClient()
                           .removeObject(RemoveObjectArgs.builder().bucket(IN_BUCKET).object(item.objectName()).build());
            }
        }
        //dataService.getMinioClient().removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
    }


    @Test
    public void testWorker() throws Exception {
        assertNotNull("Expected sender, not injected", requestSender);
        //String image = "mstc/astros-eap-12.5:0.3.0";
        String image = "mstc/python-test";
        Request request = new Request(image,
                                      "test-job",
                                      5,
                                      "test.in.bucket",
                                      "test.out.bucket");
        requestSender.sendAndReceive(request);
    }

    interface Check {
        boolean check();
    }

    class MinIOCheck implements Check {
        @Override
        public boolean check() {
            try {
                URL url = new URL(endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.getResponseCode();
                return true;
            } catch (Exception e) {
            }
            return false;
        }
    }

    class RabbitMQCheck implements Check {
        @Override
        public boolean check() {
            try {
                rabbitTemplate.getConnectionFactory().createConnection();
                return true;
            } catch (Exception e) {
            }
            return false;
        }
    }

    private void check(Check checker) throws Exception {
        int retry = 0;
        boolean online = false;
        while (!online) {
            online = checker.check();
            if(!online) {
                retry++;
                if (retry == 5) {
                    throw new TimeoutException(checker.getClass().getSimpleName() + "failed");
                }
                Thread.sleep(1000);
            }
        }
    }
}
