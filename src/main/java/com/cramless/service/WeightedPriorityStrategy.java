package com.cramless.service;

import com.cramless.model.StudySettings;
import com.cramless.model.Topic;
import com.cramless.util.AppClock;

/**
 * score = 100 * (weightageFactor * weightage + urgencyFactor * urgency + weaknessFactor * weakness)
 * where weightage = min(1, topicWeightage / 5), urgency = 1 / (1 + daysLeft / 7) and weakness is 0..1.
 */
public class WeightedPriorityStrategy implements PriorityStrategy {

    @Override
    public double score(Topic t, StudySettings settings) {
        double weightage = Math.min(1.0, t.getWeightage() / 5.0);
        double urgency = 1.0 / (1.0 + t.getDaysLeft(AppClock.today()) / 7.0);
        double weakness = t.getWeaknessLevel();
        double s = 100.0 * (settings.getWeightageFactor() * weightage
                + settings.getUrgencyFactor() * urgency
                + settings.getWeaknessFactor() * weakness);
        return Math.round(s * 100.0) / 100.0;
    }

    @Override
    public String explain(Topic t) {
        StringBuilder sb = new StringBuilder();
        double w = t.getWeightage();
        sb.append(w >= 3 ? "High weightage" : w >= 1.5 ? "Medium weightage" : "Low weightage");
        sb.append(" (about ").append(Math.round(w * 10) / 10.0).append("% of the grade)");
        int days = t.getDaysLeft(AppClock.today());
        sb.append(", exam or deadline in ").append(days).append(days == 1 ? " day" : " days");
        String note = t.getLatestScoreNote();
        if (note != null) {
            sb.append(", last score: ").append(note);
        } else {
            sb.append(". No scores logged yet, so this uses weightage and time left only");
        }
        return sb.toString();
    }
}
