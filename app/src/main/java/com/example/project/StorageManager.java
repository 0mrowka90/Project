package com.example.project;

import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static android.content.Context.MODE_PRIVATE;

public class StorageManager {
    public static void saveFaceRecognition(File filePath, FaceRecognitionData data) {
        try {
            File file = new File(filePath, "map");
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(data);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static FaceRecognitionData loadFaceRecognition(File filePath) {
        try {
            File file = new File(filePath, "map");
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(file));
            FaceRecognitionData data = (FaceRecognitionData) is.readObject();
            is.close();
            return data;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}

