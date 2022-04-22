package mstc.cloud.worker.job;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * @author dreedy
 */
@Builder
public class K8sJob {
    private String image;
    @Getter
    private String namespace;
    private String jobName;
    private String jobNameUnique;
    @Getter
    private int timeOut;
    private Map<String, String> env;
    public static final int DEFAULT_TIMEOUT_MINUTES = 15;

    public String getJobName() {
        if (jobNameUnique == null) {
            jobNameUnique = String.format("%s-%s", jobName, UUID.randomUUID());
        }
        return jobNameUnique;
    }

    private List<EnvVar> getEnvVars() {
        List<EnvVar> envVars = new ArrayList<>();
        for(Map.Entry<String, String> entry : env.entrySet()) {
            EnvVar envVar = new EnvVar();
            envVar.setName(entry.getKey());
            envVar.setValue(entry.getValue());
            envVars.add(envVar);
        }
        return envVars;
    }

    public Job getJob() {
        /*Map<String, Quantity> resourceLimitsMap = new HashMap<>();
        resourceLimitsMap.put("cpu", new Quantity("1000m"));
        resourceLimitsMap.put("memory", new Quantity("1000M"));*/
        return new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                .withName(getJobName())
                .withLabels(Collections.singletonMap("foo", "bar"))
                .endMetadata()
                .withNewSpec()
                .withTtlSecondsAfterFinished(60)
                //.withActiveDeadlineSeconds()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName(jobName)
                .withImage(image)
                .withEnv(getEnvVars())
                .withImagePullPolicy("IfNotPresent")
                /*.withResources(new ResourceRequirementsBuilder()
                                       .withLimits(resourceLimitsMap)
                                       .withRequests(resourceLimitsMap)
                                       .build())*/
                .endContainer()
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }
}
