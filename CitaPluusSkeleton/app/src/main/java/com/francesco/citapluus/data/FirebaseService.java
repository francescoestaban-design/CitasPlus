package com.francesco.citapluus.data;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseService {

    private static FirebaseService instance;
    private final FirebaseFirestore db;

    private FirebaseService() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }
}
