package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DestinationRealtimeService {

    private final Map<Long, List<SseEmitter>> tripEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long tripId) {
        SseEmitter emitter = new SseEmitter(0L);
        tripEmitters.computeIfAbsent(tripId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(tripId, emitter));
        emitter.onTimeout(() -> removeEmitter(tripId, emitter));
        emitter.onError(ignored -> removeEmitter(tripId, emitter));

        return emitter;
    }

    public void publish(Long tripId, Object payload) {
        List<SseEmitter> emitters = tripEmitters.get(tripId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("destinations-updated").data(payload));
            } catch (IOException | IllegalStateException ex) {
                removeEmitter(tripId, emitter);
            }
        }
    }

    private void removeEmitter(Long tripId, SseEmitter emitter) {
        List<SseEmitter> emitters = tripEmitters.get(tripId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            tripEmitters.remove(tripId);
        }
    }
}
