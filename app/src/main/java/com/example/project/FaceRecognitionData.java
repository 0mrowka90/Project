package com.example.project;

import android.graphics.PointF;

import java.io.Serializable;

public class FaceRecognitionData implements Serializable {
    private double earsDistance;

    public FaceRecognitionData(double earsDistance) {
        this.earsDistance = earsDistance;
    }

    public double getEarsDistance() {
        return earsDistance;
    }

    public void setEarsDistance(double earsDistance) {
        this.earsDistance = earsDistance;
    }

    @Override
    public String toString() {
        return "FaceRecognitionData{" +
                "earsDistance=" + earsDistance +
                '}';
    }
}
