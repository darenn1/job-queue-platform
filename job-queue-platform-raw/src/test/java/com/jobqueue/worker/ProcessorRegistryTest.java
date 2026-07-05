package com.jobqueue.worker;

import com.jobqueue.domain.Job;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week6")
class ProcessorRegistryTest {

    @Test
    void registerProcessorThenGetProcessorReturnsTheSameInstance() {
        ProcessorRegistry registry = new ProcessorRegistry();
        JobProcessor processor = job -> "ok";

        registry.registerProcessor("send_email", processor);

        assertSame(processor, registry.getProcessor("send_email"));
    }

    @Test
    void getProcessorForUnknownTypeThrowsJobProcessingException() {
        ProcessorRegistry registry = new ProcessorRegistry();

        JobProcessingException ex = assertThrows(JobProcessingException.class,
                () -> registry.getProcessor("unknown_type"));
        assertTrue(ex.getMessage().contains("unknown_type"));
    }

    @Test
    void hasProcessorReflectsRegistrationState() {
        ProcessorRegistry registry = new ProcessorRegistry();
        assertFalse(registry.hasProcessor("resize_image"));

        registry.registerProcessor("resize_image", job -> "resized");

        assertTrue(registry.hasProcessor("resize_image"));
        assertFalse(registry.hasProcessor("generate_report"));
    }

    @Test
    void registerProcessoringNullJobTypeThrows() {
        ProcessorRegistry registry = new ProcessorRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.registerProcessor(null, job -> "ok"));
    }

    @Test
    void registerProcessoringBlankJobTypeThrows() {
        ProcessorRegistry registry = new ProcessorRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.registerProcessor("   ", job -> "ok"));
    }

    @Test
    void registerProcessoringNullProcessorThrows() {
        ProcessorRegistry registry = new ProcessorRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.registerProcessor("send_email", null));
    }

    @Test
    void registerProcessoringTheThreeDefaultProcessorTypesWiresThemAllUp() {
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerProcessor("send_email", new EmailJobProcessor());
        registry.registerProcessor("resize_image", new ResizeImageJobProcessor());
        registry.registerProcessor("generate_report", new GenerateReportJobProcessor());

        assertEquals(3, registry.size());
        assertTrue(registry.getProcessor("send_email") instanceof EmailJobProcessor);
        assertTrue(registry.getProcessor("resize_image") instanceof ResizeImageJobProcessor);
        assertTrue(registry.getProcessor("generate_report") instanceof GenerateReportJobProcessor);
    }

    @Test
    void deterministicProcessorConfigurationsBehaveAsConfigured() {
        JobProcessor alwaysSucceeds = new EmailJobProcessor(0, 0.0);
        JobProcessor alwaysFails = new EmailJobProcessor(0, 1.0);
        Job job = new Job("send_email", "{}", 1);

        assertDoesNotThrow(() -> alwaysSucceeds.process(job));
        assertThrows(JobProcessingException.class, () -> alwaysFails.process(job));
    }
}