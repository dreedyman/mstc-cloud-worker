package mstc.cloud.worker.service;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import mstc.cloud.worker.Util;
import mstc.cloud.worker.data.DataService;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.job.K8sJob;
import mstc.cloud.worker.job.JobRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author dreedy
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class WorkerRequestProcessorTest {
    private static final String IN_BUCKET = "worker.request.processor.in.bucket";
    private static final String OUT_BUCKET = "worker.request.processor.out.bucket";
    private File downloadDir;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private DataService dataService;
    @Mock
    private JobRunner jobRunner;
    @Autowired
    @InjectMocks
    private WorkerRequestProcessor sut;
    @Rule
    public KubernetesServer server = new KubernetesServer(true, false);
    private Request request;

    @Before
    public void setup() throws Exception {
        request = new Request("mstc/python-test:latest",
                              "test",
                              0,
                              IN_BUCKET,
                              OUT_BUCKET,
                              Integer.toString("test".hashCode()));
        String downloadDirName = System.getProperty("test.download.dir");
        downloadDir = new File(downloadDirName,
                               WorkerRequestProcessorTest.class.getSimpleName().toLowerCase());
        downloadDir.mkdirs();
        dataService.delete(OUT_BUCKET);
    }

    @After
    public void clean() throws IOException {
        Util.deleteDir(downloadDir);
    }

    @Test
    public void createJob() {
        K8sJob job = sut.createJob(request);
        assertNotNull(job);
        assertEquals(K8sJob.DEFAULT_TIMEOUT_MINUTES, job.getTimeOut());
    }

    @Test
    public void createAndRunJob() throws Exception {
        sut.setNamespace("test");
        K8sJob k8sJob = sut.createJob(request);
        String uid = UUID.randomUUID().toString();

        PodStatus podStatus = new PodStatus();
        podStatus.setPhase("Succeeded");
        Pod jobPod = new PodBuilder()
                .withNewMetadata()
                .withOwnerReferences(
                        new OwnerReferenceBuilder().withApiVersion("batch/v1")
                                                   .withUid(uid)
                                                   .withController(true)
                                                   .withKind("Job")
                                                   .withName(k8sJob.getJobNameUnique())
                                                   .build())
                .withName(k8sJob.getJobNameUnique())
                .addToLabels("job-name", k8sJob.getJobNameUnique())
                .addToLabels("mstc-job", "check")
                .addToLabels("controller-uid", uid)
                .withUid(uid)
                .endMetadata()
                .withStatus(podStatus).build();

        PodList podList = new PodListBuilder()
                .withNewMetadata()
                .endMetadata()
                .withItems(jobPod).build();

        server.expect().withPath("/apis/batch/v1/namespaces/test/jobs")
              .andReturn(200, jobPod).always();

        String path = String.format("/%s?%s&%s&%s",
                                    "api/v1/namespaces/test/pods",
                                    "labelSelector=" + toUrlEncoded("job-name=" + k8sJob.getJobNameUnique()),
                                    "allowWatchBookmarks=true",
                                    "watch=true");

        server.expect().get()
              .withPath(path)
              .andReturn(200, podList).always();

        server.expect().get().withPath("/apis/batch/v1/namespaces/test/jobs/" + k8sJob.getJobNameUnique())
              .andReturn(200, jobPod).once();

        server.expect().get().withPath("/api/v1/namespaces/test/pods").andReturn(200, jobPod).once();

        String uidPath = "/api/v1/namespaces/test/pods?labelSelector=" + toUrlEncoded("controller-uid=" + uid);
        server.expect().get()
              .withPath(uidPath)
              .andReturn(200, podList)
              .once();

        server.expect().get().withPath("/api/v1/namespaces/test/pods/" + k8sJob.getJobNameUnique() + "/log?pretty=false")
              .andReturn(200, "Job complete.")
              .always();
        KubernetesClient client = server.getClient();
        String output = sut.processJob(k8sJob, client, request.getOutputBucket(), request.getPrefix());
        assertNotNull(output);
        assertEquals("Job complete.", output);

        dataService.getAll(downloadDir, OUT_BUCKET);
        assertEquals(1, downloadDir.list().length);
    }

    private static String toUrlEncoded(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException exception) {
            // Ignore
        }
        return null;
    }


}