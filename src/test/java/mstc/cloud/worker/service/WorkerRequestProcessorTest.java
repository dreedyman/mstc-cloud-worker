package mstc.cloud.worker.service;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import junit.framework.AssertionFailedError;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.job.K8sJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author dreedy
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkerRequestProcessorTest {
    @Autowired
    private WorkerRequestProcessor sut;
    @Rule
    public KubernetesServer server = new KubernetesServer(true, false);
    private Request request;

    @Before
    public void setup() {
        request = new Request("mstc/python-test:latest",
                              "test",
                              0,
                              "in-bucket",
                              "out-bucket");
    }

    @Test
    public void createJob() throws Exception {
        K8sJob job = sut.createJob(request);
        assertNotNull(job);
        assertEquals(K8sJob.DEFAULT_TIMEOUT_MINUTES, job.getTimeOut());
    }

    @Test
    public void createAndRunJob() throws Exception {
        K8sJob job = sut.createJob(request);

        server.expect().withPath("/apis/batch/v1/namespaces/test/jobs")
              .andReturn(200, new JobBuilder()
                      .withNewMetadata()
                      .withName(job.getJobNameUnique())
                      .addToLabels("job-name", job.getJobNameUnique())
                      .addToLabels("mstc-job", "check")
                      .endMetadata()
                      .build()).always();

        /*server.expect().withPath("/apis/batch/v1/namespaces/test/jobs")
              .andReturn(200, new JobListBuilder().build()).once();*/

        server.expect().get()
              .withPath("/api/v1/namespaces/test/pods?labelSelector=" + toUrlEncoded("job-name=" + job.getJobNameUnique()))
              .andReturn(200, new PodListBuilder().withItems(new PodBuilder().build()).build()).always();
        KubernetesClient client = server.getClient();
        sut.setNamespace("test");
        sut.processRequest(request, client);
    }

    private static String toUrlEncoded(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException exception) {
            // Ignore
        }
        return null;
    }

    @Test
    public void example() throws InterruptedException {
        KubernetesClient client = server.getClient();

        final CountDownLatch deleteLatch = new CountDownLatch(1);
        final CountDownLatch closeLatch = new CountDownLatch(1);

        //CREATE
        client.pods().inNamespace("ns1").create(new PodBuilder().withNewMetadata().withName("pod1").endMetadata().build());

        //READ
        PodList podList = client.pods().inNamespace("ns1").list();
        assertNotNull(podList);
        assertEquals(1, podList.getItems().size());

        //WATCH
        Watch watch = client.pods().inNamespace("ns1").withName("pod1").watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Watcher.Action action, Pod resource) {
                switch (action) {
                    case DELETED:
                        deleteLatch.countDown();
                        break;
                    default:
                        throw new AssertionFailedError(action.toString().concat(" isn't recognised."));
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                closeLatch.countDown();
            }
        });

        //DELETE
        client.pods().inNamespace("ns1").withName("pod1").delete();

        //READ AGAIN
        podList = client.pods().inNamespace("ns1").list();
        assertNotNull(podList);
        assertEquals(0, podList.getItems().size());

        assertTrue(deleteLatch.await(1, TimeUnit.MINUTES));
        watch.close();
        assertTrue(closeLatch.await(1, TimeUnit.MINUTES));
    }
}