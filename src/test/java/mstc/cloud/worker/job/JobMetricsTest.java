package mstc.cloud.worker.job;

import org.junit.Test;

import java.io.File;
import java.time.Instant;

import static org.junit.Assert.assertTrue;

/**
 * @author dreedy
 */
public class JobMetricsTest {

    @Test
    public void setJobTiming() throws Exception {
        JobMetrics jobMetrics = new JobMetrics();
        Instant submitted = Instant.now();
        jobMetrics.setSubmitted(submitted);
        Thread.sleep(3);
        Instant returned = Instant.now();
        jobMetrics.setReturned(returned);
        long duration = jobMetrics.getJobDuration();
        assertTrue(jobMetrics.getSubmitted().isBefore(jobMetrics.getReturned()));
        assertTrue(duration > 0);
    }
}