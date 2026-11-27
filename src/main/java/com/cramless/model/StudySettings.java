package com.cramless.model;

/** The adjustable weights of the Smart Priority Advisor. */
public class StudySettings {
    private double weightageFactor = 0.4;
    private double urgencyFactor = 0.4;
    private double weaknessFactor = 0.2;
    private double weakScoreThreshold = 60;

    public double getWeightageFactor() { return weightageFactor; }
    public double getUrgencyFactor() { return urgencyFactor; }
    public double getWeaknessFactor() { return weaknessFactor; }
    public double getWeakScoreThreshold() { return weakScoreThreshold; }

    /** UC-08 special requirement 4. The three weights are balanced to add up to 1. */
    public void setFactors(double weightage, double urgency, double weakness) {
        double sum = weightage + urgency + weakness;
        if (weightage < 0 || urgency < 0 || weakness < 0 || sum <= 0) {
            throw new com.cramless.util.ValidationException("At least one weight must be above zero.");
        }
        this.weightageFactor = weightage / sum;
        this.urgencyFactor = urgency / sum;
        this.weaknessFactor = weakness / sum;
    }

    public void setWeakScoreThreshold(double v) { this.weakScoreThreshold = v; }
}
