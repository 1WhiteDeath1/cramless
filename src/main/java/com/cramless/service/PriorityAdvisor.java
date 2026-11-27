package com.cramless.service;

import com.cramless.model.Student;
import com.cramless.model.Topic;
import com.cramless.util.AppClock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The Smart Priority Advisor: ranks the pending topics of every course and says why. */
public class PriorityAdvisor {
    private final PriorityStrategy strategy;

    public PriorityAdvisor(PriorityStrategy strategy) {
        this.strategy = strategy;
    }

    public PriorityAdvisor() {
        this(new WeightedPriorityStrategy());
    }

    /** Pending topics, best first. On equal scores the nearer exam goes first. */
    public List<Topic> rankTopics(Student s) {
        List<Topic> topics = s.getPendingTopics();
        Map<Topic, Double> scores = new HashMap<>();
        for (Topic t : topics) scores.put(t, strategy.score(t, s.getSettings()));   // score(topic, settings), per topic
        lastScores = scores;
        List<Topic> ranked = new ArrayList<>(topics);
        ranked.sort(Comparator.<Topic>comparingDouble(scores::get).reversed()
                .thenComparingInt(t -> t.getDaysLeft(AppClock.today())));
        return ranked;
    }

    public Topic getTopTopic(Student s) {
        List<Topic> ranked = rankTopics(s);
        return ranked.isEmpty() ? null : ranked.get(0);
    }

    public String explain(Topic t) {
        return strategy.explain(t);
    }

    /** The priority score this topic got in the last ranking. */
    public double scoreOf(Topic t, Student s) {
        Double v = lastScores.get(t);
        return v != null ? v : strategy.score(t, s.getSettings());
    }

    private Map<Topic, Double> lastScores = new HashMap<>();
}
