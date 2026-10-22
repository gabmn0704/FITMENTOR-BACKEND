package com.fitmentor.domain.feedback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer pattern, subject side: one per live session. Observers (the STOMP publisher,
 * tests, future persistence) subscribe once and receive every published event.
 */
public class FeedbackSubject {

    private final List<FeedbackObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(FeedbackObserver observer) {
        observers.add(observer);
    }

    public void publish(SessionEvent event) {
        for (FeedbackObserver observer : observers) {
            observer.update(event);
        }
    }

    public int observerCount() {
        return observers.size();
    }
}
