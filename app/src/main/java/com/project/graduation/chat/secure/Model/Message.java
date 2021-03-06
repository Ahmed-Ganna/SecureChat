package com.project.graduation.chat.secure.Model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

public class Message {

    private String message, type,to;
    private long time;
    private boolean seen;
    private String from;
    private boolean encrypted;
    private boolean toEncrypt;
    private long encryptSecs;

    @Exclude
    private DatabaseReference dbReference;



    // default constructor
    public Message() {
    }

    // constructor
    public Message(String message, String type, long time, boolean seen, String from,boolean encrypted,boolean toEncrypt,long encryptSecs) {
        this.message = message;
        this.type = type;
        this.time = time;
        this.encrypted = encrypted;
        this.toEncrypt = toEncrypt;
        this.seen = seen;
        this.from = from;
        this.encryptSecs = encryptSecs;
    }

    // getter & setter
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }


    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isToEncrypt() {
        return toEncrypt;
    }

    public void setToEncrypt(boolean toEncrypt) {
        this.toEncrypt = toEncrypt;
    }


    public String getTo(){
        return to;
    }

    public void setTo(String to){
        this.to = to;
    }

    public DatabaseReference getDbReference() {
        return dbReference;
    }

    public void setDbReference(DatabaseReference dbReference) {
        this.dbReference = dbReference;
    }

    public long getEncryptSecs() {
        return encryptSecs;
    }

    public void setEncryptSecs(long encryptSecs) {
        this.encryptSecs = encryptSecs;
    }
}
