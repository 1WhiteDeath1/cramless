package com.cramless.service;

import com.cramless.model.Course;
import com.cramless.model.ProgressGraph;
import com.cramless.model.Score;
import com.cramless.util.DateRange;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Builds the two graphs of the progress screen: grade over time and study time. */
public class ProgressGraphBuilder {

    /** One point per logged score: the date and the percentage obtained. */
    public ProgressGraph buildGradeGraph(List<Course> courses, DateRange period) {
        ProgressGraph g = new ProgressGraph("Grade over time: " + label(courses), "Score %");
        java.util.List<Score> all = new java.util.ArrayList<>();
        for (Course c : courses) all.addAll(c.getScoresIn(period));
        all.sort(java.util.Comparator.comparing(Score::getDate));
        for (Score s : all) g.addPoint(s.getDate(), Math.round(s.getPercentage() * 10.0) / 10.0);
        return g;
    }

    /** One point per day with recorded study time, in hours. */
    public ProgressGraph buildStudyTimeGraph(List<Course> courses, DateRange period) {
        ProgressGraph g = new ProgressGraph("Study time: " + label(courses), "Hours");
        java.util.Map<LocalDate, Double> perDay = new java.util.TreeMap<>();
        for (Course c : courses) {
            for (Map.Entry<LocalDate, Double> e : c.getStudyTimeByDay(period).entrySet()) {
                perDay.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        for (Map.Entry<LocalDate, Double> e : perDay.entrySet()) {
            g.addPoint(e.getKey(), Math.round(e.getValue() * 100.0) / 100.0);
        }
        return g;
    }

    private String label(List<Course> courses) {
        return courses.size() == 1 ? courses.get(0).getName() : "all selected courses";
    }
}
