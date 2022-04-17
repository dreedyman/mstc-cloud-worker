package mstc.cloud.worker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import mstc.cloud.worker.domain.DataItem;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.domain.Response;
import mstc.cloud.worker.service.WorkerRequestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WorkerControllerIntTest {
    @Rule
    public KubernetesServer server = new KubernetesServer();
    /*@Mock
    private WorkerRequestService workerRequestService;*/
    @Autowired
    @InjectMocks
    private WorkerController workerController;
    @LocalServerPort
    private int port;
    @Inject
    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        List<DataItem> items = new ArrayList<>();
        items.add(new DataItem().bucket("green").endpoint("http://locahost:9090").itemNames("black", "blue"));
        Response response = new Response().items(items);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void get() {
    }

    @Test
    public void exec() throws JsonProcessingException {
        Request request = (Request) new Request()
                .image("busybox")
                .jobName("simple")
                .endpoint("http://localhost:9000")
                .bucket("red-rover")
                .itemNames("come", "over");
        ResponseEntity<String> result = workerController.exec(request);
        assertNotNull("Expected non-null", result);
        assertEquals("Expected 200, got: " + result.getStatusCode(),
                     HttpStatus.OK, result.getStatusCode());
        Response response = mapper.readValue(result.getBody(), Response.class);
        assertNotNull("Expected non-null", response);
        assertNotNull("Expected non-null", response.getItems());
        assertEquals("Expected 1 item", 1, response.getItems().size());
    }
}