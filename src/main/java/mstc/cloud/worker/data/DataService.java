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

package mstc.cloud.worker.data;

import io.minio.*;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dreedy
 */
@Service
public class DataService {
    @Value("${MINIO_SERVICE_HOST:localhost}")
    private String host;
    @Value("${MINIO_SERVICE_PORT:9000}")
    private String port;
    @Value("${MINIO_ROOT_USER:minioadmin}")
    private String user;
    @Value("${MINIO_ROOT_PASSWORD:minioadmin}")
    private String password;
    private String endpoint;
    private MinioClient minioClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataService.class);

    public String getEndpoint() {
        return endpoint;
    }

    public MinioClient getMinioClient() {
        if (minioClient == null) {
            LOGGER.info("host: " + host +", port: " + port);
            endpoint = String.format("http://%s:%s", host, port);
            minioClient = MinioClient.builder().endpoint(endpoint)
                                     .credentials(user, password)
                                     .build();
        }
        return minioClient;
    }

    public void bucket(String bucket) throws Exception {
        getMinioClient();
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            LOGGER.info("Created bucket: " + bucket);
        }
    }

    public List<String> upload(String bucket, File... files) throws Exception {
        return upload(bucket, null, files);
    }

    public List<String> upload(String bucket, String prefix, File... files) throws Exception {
        getMinioClient();
        bucket(bucket);
        List<String> items = new ArrayList<>();
        for (File file : files) {
            String objectName = prefix == null ? file.getName() : prefix + "-" + file.getName();
            LOGGER.info("Uploading " + objectName + " to bucket " + bucket);
            minioClient.uploadObject(UploadObjectArgs.builder()
                                                     .bucket(bucket)
                                                     .object(objectName)
                                                     .filename(file.getPath())
                                                     .build());
            items.add(endpoint + "/" + bucket + "/" + objectName);
        }
        return items;
    }

    public File get(File downloadTo, String bucket, String name) throws Exception {
        getMinioClient();
        File to = new File(downloadTo, name);
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(name).build());
             FileOutputStream fos = new FileOutputStream(to)) {
            int bufferSize = 4096;
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) { // EOF
                fos.write(buffer, 0, bytesRead);
            }
        }
        return to;
    }

    public List<File> getAll(File downloadTo, String bucket) throws Exception {
        getMinioClient();
        List<File> downloaded = new ArrayList<>();
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                downloaded.add(get(downloadTo, bucket, item.objectName()));
            }
        }
        return downloaded;
    }

    public void delete(String bucket) throws Exception {
        getMinioClient();
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                minioClient.removeObject(RemoveObjectArgs.builder()
                                                         .bucket(bucket)
                                                         .object(item.objectName())
                                                         .build());
            }
            minioClient.removeBucket(RemoveBucketArgs.builder()
                                                     .bucket(bucket)
                                                     .build());
        }
    }

}
