/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public  class FirebaseUtil {

    private static FirebaseDatabase fbDatabase;
    private static FirebaseDatabase DatabaseInstance(){
        if(fbDatabase == null) fbDatabase = FirebaseDatabase.getInstance();
        return fbDatabase;
    }
    public static DatabaseReference getBaseRef() {
        return DatabaseInstance().getReference();
    }
    public static DatabaseReference GetMusicDataRef() {
        return getBaseRef().child(Constants.FIREBASE_MUISC_DATA_NODE);
    }
    public static DatabaseReference getFeedbacksRef() {
        return getBaseRef().child(Constants.FIREBASE_MUISC_FEEDBACK_NODE);
    }





}
