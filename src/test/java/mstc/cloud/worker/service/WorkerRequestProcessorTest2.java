package mstc.cloud.worker.service;

import mstc.cloud.worker.Util;
import mstc.cloud.worker.data.DataService;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.job.K8sJob;
import mstc.cloud.worker.job.JobRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author dreedy
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
@SuppressWarnings("unused")
public class WorkerRequestProcessorTest2 {
    private static final String IN_BUCKET = "worker.request.processor.in.bucket";
    private static final String OUT_BUCKET = "worker.request.processor.out.bucket";
    private File downloadDir;
    @Autowired
    private DataService dataService;
    @Mock
    private JobRunner jobRunner;
    @Autowired
    @InjectMocks
    private WorkerRequestProcessor sut;
    private Request request;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(jobRunner.submit(any(K8sJob.class))).thenReturn("Mock Job Completed");
        request = new Request("mstc/python-test:latest",
                              "test",
                              0,
                              IN_BUCKET,
                              OUT_BUCKET);
        String downloadDirName = System.getProperty("test.download.dir");
        downloadDir = new File(downloadDirName,
                               WorkerRequestProcessorTest2.class.getSimpleName().toLowerCase());
        downloadDir.mkdirs();
        Util.check(new Util.MinIOCheck());
    }

    @After
    public void clean() throws IOException {
        Util.deleteDir(downloadDir);
    }

    @Test
    public void createAndRunRequest() throws Exception {
        String output = sut.processRequest(request);
        assertNotNull(output);
        dataService.getAll(downloadDir, OUT_BUCKET);
        assertEquals(1, downloadDir.list().length);
    }

}