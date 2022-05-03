package mstc.cloud.worker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.service.WorkerRequestProcessor;
import org.junit.After;
import org.junit.Before;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WorkerControllerTest {
    @Mock
    private WorkerRequestProcessor workerRequestProcessor;
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
        Map<String, String> response = new HashMap<>();
        response.put("result", "Happy path");
        String s = mapper.writeValueAsString(response);
        when(workerRequestProcessor.processRequest(any(Request.class))).thenReturn(mapper.writeValueAsString(response));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void get() {
    }

    @Test
    public void exec() throws JsonProcessingException {
        Request request = new Request().inputBucket("red-rover/");
        ResponseEntity<String> result = workerController.exec(request);
        assertNotNull("Expected non-null", result);
        assertEquals("Expected 200, got: " + result.getStatusCode(),
                     HttpStatus.OK, result.getStatusCode());
        Map<String, String> response = mapper.readValue(result.getBody(),new TypeReference<>() {});
        assertNotNull("Expected non-null", response);
        assertEquals("Expected 1 item", 1, response.size());
    }
}