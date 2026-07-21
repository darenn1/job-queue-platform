package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.api.dto.WorkerStatusResponse;
import com.example.job_queue_platform_refined.worker.WorkerPool;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Tag("week7")
class WorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkerPool workerPool;

    @Test
    void status_returnsWhateverWorkerPoolAssembles() throws Exception {
        when(workerPool.getStatus()).thenReturn(new WorkerStatusResponse(4, 2, 7, 100, 3));

        mockMvc.perform(get("/workers/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeWorkers").value(4))
                .andExpect(jsonPath("$.jobsRunning").value(2))
                .andExpect(jsonPath("$.queueDepth").value(7))
                .andExpect(jsonPath("$.totalCompleted").value(100))
                .andExpect(jsonPath("$.totalFailed").value(3));
    }
}