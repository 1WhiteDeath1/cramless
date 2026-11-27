package com.cramless.service;

import com.cramless.model.StudySettings;
import com.cramless.model.Topic;

/** A rule for scoring how urgently a topic should be studied (Strategy pattern). */
public interface PriorityStrategy {
    double score(Topic t, StudySettings settings);

    String explain(Topic t);
}
