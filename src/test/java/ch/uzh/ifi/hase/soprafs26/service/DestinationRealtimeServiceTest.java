package ch.uzh.ifi.hase.soprafs26.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class DestinationRealtimeServiceTest {

    private DestinationRealtimeService destinationRealtimeService;

    @BeforeEach
    public void setup() {
        destinationRealtimeService = new DestinationRealtimeService();
    }

    @Test
    public void publish_withoutSubscribers_doesNothing() {
        assertDoesNotThrow(() -> destinationRealtimeService.publish(1L, "payload"));
        assertDoesNotThrow(() -> destinationRealtimeService.publish(1L, "custom-event", "payload"));
    }

    @Test
    public void subscribe_registersEmitterForTrip() {
        SseEmitter emitter = destinationRealtimeService.subscribe(1L);

        assertTrue(getTripEmitters().containsKey(1L));
        assertTrue(getTripEmitters().get(1L).contains(emitter));
    }

    @Test
    public void publish_withSubscriber_keepsEmitter_whenSendSucceeds() {
        destinationRealtimeService.subscribe(1L);

        destinationRealtimeService.publish(1L, "payload");

        assertTrue(getTripEmitters().containsKey(1L));
        assertFalse(getTripEmitters().get(1L).isEmpty());
    }

    @Test
    public void publish_removesEmitter_whenSendThrowsIOException() {
        FailingEmitter failingEmitter = new FailingEmitter();
        getTripEmitters().computeIfAbsent(1L, ignored -> new CopyOnWriteArrayList<>()).add(failingEmitter);

        destinationRealtimeService.publish(1L, "custom-event", "payload");

        assertFalse(getTripEmitters().containsKey(1L));
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<SseEmitter>> getTripEmitters() {
        try {
            Field field = DestinationRealtimeService.class.getDeclaredField("tripEmitters");
            field.setAccessible(true);
            return (Map<Long, List<SseEmitter>>) field.get(destinationRealtimeService);
        }
        catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class FailingEmitter extends SseEmitter {
        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            throw new IOException("forced failure");
        }
    }
}