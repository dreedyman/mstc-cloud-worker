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

import io.minio.*;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dreedy
 */
public class DataService {
    private final String endpoint;
    private final MinioClient minioClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataService.class);

    public DataService(String endpoint,
                       String user,
                       String password) {
        this.endpoint = endpoint;
        minioClient = MinioClient.builder().endpoint(endpoint)
                                 .credentials(user, password)
                                 .build();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public MinioClient getMinioClient() {
        return minioClient;
    }

    public void bucket(String bucket) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            LOGGER.info("Created bucket: " + bucket);
        }
    }

    public List<String> upload(String bucket, File... files) throws Exception {
        bucket(bucket);
        List<String> items = new ArrayList<>();
        for (File file : files) {
            minioClient.uploadObject(UploadObjectArgs.builder()
                                                     .bucket(bucket)
                                                     .object(file.getName())
                                                     .filename(file.getPath())
                                                     .build());
            items.add(endpoint + "/" + bucket + "/" + file.getName());
        }
        return items;
    }

    public File get(File downloadTo, String bucket, String name) throws Exception {
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
        List<File> downloaded = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build());
        for (Result<Item> result : results) {
            Item item = result.get();
            downloaded.add(get(downloadTo, bucket, item.objectName()));
        }
        return downloaded;
    }

    public List<File> download(File downloadTo, URL... urls) throws IOException {
        List<File> files = new ArrayList<>();
        for (URL from : urls) {
            String path = from.getPath();
            int ndx = from.getPath().lastIndexOf("/");
            if (ndx != -1) {
                path = from.getPath().substring(ndx + 1);
            }
            File to = new File(downloadTo, path);
            try (InputStream is = from.openStream();
                 FileOutputStream fos = new FileOutputStream(to)) {
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) { // EOF
                    fos.write(buffer, 0, bytesRead);
                }
            }
            files.add(to);
        }
        return files;
    }
}
