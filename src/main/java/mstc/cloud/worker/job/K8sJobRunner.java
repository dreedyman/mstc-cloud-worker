package mstc.cloud.worker.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author dreedy
 */
public class K8sJobRunner {
    private static final Logger logger = LoggerFactory.getLogger(K8sJobRunner.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ThreadPoolExecutor executor =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public String submit(K8sJob k8sJob) throws JobException {
        Job job = k8sJob.getJob();
        Config config = new ConfigBuilder().build();
        String namespace = k8sJob.getNamespace();
        JobMetrics jobMetrics = new JobMetrics();
        String output;
        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            String jobName = k8sJob.getJobName();
            logJobCreation(jobName, namespace, job);
            jobMetrics.setSubmitted(Instant.now());
            client.batch().v1().jobs().inNamespace(namespace).createOrReplace(job);
            logger.info(String.format("Job \"%s\" is created in namespace %s, timeout of %s minutes, waiting for result...",
                                      jobName,
                                      namespace,
                                      k8sJob.getTimeOut()));
            output = watch(client, job, k8sJob);
        } finally {
            jobMetrics.setReturned(Instant.now());
            logger.info(String.format("Job %s duration: %d ms", k8sJob.getJobName(), jobMetrics.getJobDuration()));
        }
        return output;
    }

    private String watch(KubernetesClient client, Job job, K8sJob k8sJob) throws JobException {
        String namespace = k8sJob.getNamespace();
        PodList podList = client.pods().inNamespace(namespace).withLabel("job-name",
                                                                         k8sJob.getJobName()).list();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        logger.info("Wait for pod to become available...");
        executor.submit(new PodListWaiter(countDownLatch, podList));
        boolean foundPod;
        try {
            foundPod = countDownLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new JobException("Pod was not available after 30 seconds", e);
        }
        logger.info("Count down latch: " + countDownLatch.getCount());
        if (!foundPod || podList.getItems().size() == 0) {
            throw new JobException("No pod found");
        }
        logger.info("Pod availability count: " + podList.getItems().size());
        String name = podList.getItems().get(0).getMetadata().getName();
        logger.info("Watching job: " + name);
        try {
            client.pods().inNamespace(namespace).withName(name)
                  .waitUntilCondition(pod -> pod.getStatus().getPhase().equals("Succeeded") || pod.getStatus()
                                                                                                  .getPhase()
                                                                                                  .equals("Failed"),
                                      k8sJob.getTimeOut(),
                                      TimeUnit.MINUTES);
            return client.batch().v1().jobs().inNamespace(namespace).withName(job.getMetadata().getName()).getLog();
        } finally {
            boolean isDeleted = client.batch().v1().jobs().inNamespace(namespace).withName(name).delete();
            logger.info("Job {} deleted: {}", name, isDeleted);
        }
    }

    private void logJobCreation(String jobName, String namespace, Job job) {
        if (logger.isDebugEnabled()) {
            try {
                logger.debug(String.format("Creating job \"%s\" in namespace %s.%n%s",
                                           jobName,
                                           namespace,
                                           mapper.writerWithDefaultPrettyPrinter().writeValueAsString(job)));
            } catch (JsonProcessingException e) {
                logger.warn(
                        String.format("Caught [JsonProcessingException: %s] " +
                                              "while formatting job \"%s\", continue job submission",
                                      e.getMessage(),
                                      jobName));
            }
        } else {
            logger.info(String.format("Creating job \"%s\" in namespace %s.", jobName, namespace));
        }
    }

    private static class PodListWaiter implements Runnable {
        CountDownLatch countDownLatch;
        PodList podList;

        public PodListWaiter(CountDownLatch countDownLatch, PodList podList) {
            this.countDownLatch = countDownLatch;
            this.podList = podList;
        }

        @Override
        public void run() {
            while(podList.getItems().size() == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            countDownLatch.countDown();
        }
    }
}
