package com.example.job_queue_platform_refined.worker;

import com.example.job_queue_platform_refined.domain.Job;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
 
import java.util.List;
 
import static org.junit.jupiter.api.Assertions.*;

@Tag("Week7")
class ProcessorRegistryTest {
 
    private final JobProcessor emailProcessor = new JobProcessor() {
        @Override
        public String getType() {
            return "send_email";
        }
 
        @Override
        public String process(Job job) {
            return "email sent";
        }
    };
 
    @Test
    void getProcessorReturnsTheMatchingProcessorByType() {
        ProcessorRegistry registry = new ProcessorRegistry(List.of(emailProcessor));
 
        JobProcessor found = registry.getProcessor("send_email");
 
        assertSame(emailProcessor, found);
    }
 
    @Test
    void getProcessorThrowsForAnUnregisteredType() {
        ProcessorRegistry registry = new ProcessorRegistry(List.of(emailProcessor));
 
        JobProcessingException ex = assertThrows(JobProcessingException.class,
                () -> registry.getProcessor("nonexistent_type"));
        assertTrue(ex.getMessage().contains("nonexistent_type"));
    }
}
