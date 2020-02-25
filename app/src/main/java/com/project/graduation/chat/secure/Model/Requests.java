package com.project.graduation.chat.secure.Model;

public class Requests {

    public String user_name , user_status , user_thumb_image ;

    public Requests() {
    }

    public Requests(String user_name, String user_status, String user_thumb_image) {
        this.user_name = user_name;
        this.user_status = user_status;
        this.user_thumb_image = user_thumb_image;
    }
}
