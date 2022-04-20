package mstc.cloud.worker.service;

import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.job.K8sJob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author dreedy
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkerRequestProcessorTest {
    @Autowired
    private WorkerRequestProcessor workerRequestProcessor;

    @Test
    public void createJob() {
        String image = "busybox";
        WorkerRequestProcessor sut = new WorkerRequestProcessor();
        Request request = new Request(image,
                                      "test",
                                      0,
                                      "http://localhost:9000/in-bucket",
                                      "http://localhost:9000/out-bucket");
        K8sJob job = workerRequestProcessor.createJob(request);
        assertNotNull(job);
        assertEquals(K8sJob.DEFAULT_TIMEOUT_MINUTES, job.getTimeOut());
    }
}