package com.fitmentor.domain.feedback;

import java.util.ArrayList;
import java.util.List;

public class FeedbackSubject {
    private final List<FeedbackObserver> observers = new ArrayList<>();

    public void subscribe(FeedbackObserver observer) { observers.add(observer); }

    public void publish(FeedbackEvent event) {
        observers.forEach(observer -> observer.update(event));
    }
}
