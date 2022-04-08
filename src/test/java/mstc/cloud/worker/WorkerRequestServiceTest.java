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

import io.minio.*;
import io.minio.messages.Item;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.domain.DataItem;
import mstc.cloud.worker.service.DataService;
import mstc.cloud.worker.service.ResponseConsumer;
import mstc.cloud.worker.service.RequestSender;
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
    @Value("${minio.host}")
    private String host;
    @Value("${minio.port}")
    private String port;
    @Value("${minio.password}")
    private String password;
    @Value("${minio.user}")
    private String user;
    @Value("${minio.bucket}")
    private String bucket;
    @Autowired
    private RequestSender requestSender;
    @Autowired
    private ResponseConsumer consumer;
    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private List<String> inputItems;
    private String endpoint;
    private DataService dataService;

    @Before
    public  void setup() throws Exception {
        endpoint = String.format("http://%s:%s", host, port);
        check(new MinIOCheck());
        check(new RabbitMQCheck());
        dataService = new DataService(endpoint, user, password);
        File testFileDir = new File(System.getProperty("test.data.dir"));
        inputItems = dataService.upload(bucket, testFileDir.listFiles());
    }

    @After
    public void cleanup() throws Exception {
        if (dataService.getMinioClient().bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            Iterable<Result<Item>> results = dataService.getMinioClient()
                                                        .listObjects(ListObjectsArgs.builder().bucket(bucket).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                dataService.getMinioClient()
                           .removeObject(RemoveObjectArgs.builder().bucket(bucket).object(item.objectName()).build());
            }
        }
        //dataService.getMinioClient().removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
    }

    @Test
    public void testWorker() throws Exception {
        assertNotNull("Expected sender, not injected", requestSender);
        Request request = new Request("mstc/astros-eap-12.5:0.2.0",
                                      "astros",
                                      dataService.getEndpoint(),
                                      bucket,
                                      inputItems.toArray(new String[0]));
        requestSender.sendAndReceive(request);
        //TestReceiver receiver = new TestReceiver();
    }

    @Test
    public void download() throws Exception {
        File downloadTo = new File(System.getProperty("test.download.dir"));
        downloadTo.mkdirs();
        List<File> downloaded = dataService.getAll(downloadTo, bucket);
        //List<File> downloaded = dataService.download(downloadTo, inputUrls.toArray(new URL[0]));
        assertEquals("expected 2 files", 2, downloaded.size());
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
