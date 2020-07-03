package com.project.graduation.chat.secure.Chat;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.project.graduation.chat.secure.Adapter.MessageAdapter;
import com.project.graduation.chat.secure.Model.Message;
import com.project.graduation.chat.secure.Model.ProfileInfo;
import com.project.graduation.chat.secure.R;
import com.project.graduation.chat.secure.Utils.UserLastSeenTime;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;
import xyz.hasnat.sweettoast.SweetToast;


public class GroupChatActivity extends AppCompatActivity {



    private DatabaseReference rootReference;

    // sending message
    private ImageView send_message, send_image;
    private EditText input_user_message;
    private FirebaseAuth mAuth;
    private String messageSenderId, download_url;

    private RecyclerView messageList_ReCyVw;
    private final List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;

    private final static int GALLERY_PICK_CODE = 2;
    private StorageReference imageMessageStorageRef;


    private ArrayList<String> lastSelectedSendUsersIds = new ArrayList<>();

    private ProgressDialog dialog;

    private boolean imgSecurity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootReference = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();

        dialog = new ProgressDialog(this);

        dialog.setMessage("Please wait.");

        imageMessageStorageRef = FirebaseStorage.getInstance().getReference().child("messages_image");

        // appbar / toolbar
        Toolbar chatToolbar = findViewById(R.id.chats_appbar);
        setSupportActionBar(chatToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Group chat");

        // sending message declaration
        send_message = findViewById(R.id.c_send_message_BTN);
        send_image = findViewById(R.id.c_send_image_BTN);
        input_user_message = findViewById(R.id.c_input_message);

        // setup for showing messages
        messageAdapter = new MessageAdapter(this,messageList,true );
        messageList_ReCyVw = findViewById(R.id.message_list);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageList_ReCyVw.setLayoutManager(linearLayoutManager);
        messageList_ReCyVw.setHasFixedSize(true);
        //linearLayoutManager.setReverseLayout(true);
        messageList_ReCyVw.setAdapter(messageAdapter);

        fetchMessages();


        /**
         *  SEND TEXT MESSAGE BUTTON
         */
        send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimeDialog("text");
            }
        });


        /** SEND IMAGE MESSAGE BUTTON */
        send_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showTimeDialog("image");

            }
        });
    } // ending onCreate

    private void showTimeDialog(final String type) {
        String[] types = {"Secure message", "Don`t secure message"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Security");
        builder.setItems(types, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


                showSelectUsersDialog(type,which ==0);

            }
        });
        builder.show();
    }

    private void showSelectUsersDialog(final String type, final boolean encrypt) {

        lastSelectedSendUsersIds.clear();

        FirebaseDatabase.getInstance().getReference("users").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                final ArrayList<ProfileInfo> users = new ArrayList<>();

                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    ProfileInfo user = postSnapshot.getValue(ProfileInfo.class);
                    user.user_id = postSnapshot.getKey();

                    if (!user.user_id.equals(FirebaseAuth.getInstance().getUid()))  users.add(user);

                }


                if (!users.isEmpty()){

                    final String[] usersNames = new String[users.size()];
                    final boolean[] checked = new boolean[users.size()];

                    for (int i = 0; i < users.size(); i++) {
                        usersNames[i] = users.get(i).getUser_name();
                        checked[i] = false;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
                    builder.setTitle("Select users");
                    builder.setMultiChoiceItems(usersNames, checked, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            if (isChecked){
                                lastSelectedSendUsersIds.add(users.get(which).user_id);
                            }else {
                                lastSelectedSendUsersIds.remove(users.get(which).user_id);
                            }
                        }
                    });
                    builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                            if (type.equals("text")){

                                sendMessage(encrypt);

                            }else {
                                imgSecurity = encrypt;
                                Intent galleryIntent = new Intent().setAction(Intent.ACTION_GET_CONTENT);
                                galleryIntent.setType("image/*");
                                startActivityForResult(galleryIntent, GALLERY_PICK_CODE);
                            }

                        }
                    });
                    builder.show();



                }else {
                    SweetToast.info(GroupChatActivity.this,"No users found");
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }



    @Override // for gallery picking
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         //  For image sending
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_PICK_CODE && resultCode == RESULT_OK && data != null && data.getData() != null){

            Uri imageUri = data.getData();

            showLoading();

            DatabaseReference user_message_key = rootReference.child("group_chat").push();
            final String message_push_id = user_message_key.getKey();

            final StorageReference file_path = imageMessageStorageRef.child(message_push_id + ".jpg");

            UploadTask uploadTask = file_path.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task){
                    if (!task.isSuccessful()){
                        SweetToast.error(GroupChatActivity.this, "Error: " + task.getException().getMessage());
                    }
                    download_url = file_path.getDownloadUrl().toString();
                    return file_path.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()){
                        if (task.isSuccessful()){
                            hideLoading();
                            download_url = task.getResult().toString();
                            //Toast.makeText(ChatActivity.this, "From ChatActivity, link: " +download_url, Toast.LENGTH_SHORT).show();

                            HashMap<String, Object> message_text_body = new HashMap<>();
                            message_text_body.put("message", download_url);
                            message_text_body.put("seen", false);
                            message_text_body.put("type", "image");
                            message_text_body.put("time", ServerValue.TIMESTAMP);
                            message_text_body.put("from", messageSenderId);
                            message_text_body.put("to", TextUtils.join("," , lastSelectedSendUsersIds));
                            message_text_body.put("toEncrypt", imgSecurity);

                            HashMap<String, Object> messageBodyDetails = new HashMap<>();
                            messageBodyDetails.put("group_chat" + "/" + message_push_id, message_text_body);

                            rootReference.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError != null){
                                        Log.e("from_image_chat: ", databaseError.getMessage());
                                    }
                                    input_user_message.setText("");
                                }
                            });
                            Log.e("tag", "Image sent successfully");
                        } else{
                            SweetToast.warning(GroupChatActivity.this, "Failed to send image. Try again");
                        }
                    }else {
                        hideLoading();
                        SweetToast.error(GroupChatActivity.this,"Error occurred while uploading");
                    }
                }
            });
        }
    }

    private void showLoading() {
        dialog.show();
    }

    private void hideLoading() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void fetchMessages() {
        rootReference.child("group_chat")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.exists()){
                            Message message = dataSnapshot.getValue(Message.class);

                            String userId = FirebaseAuth.getInstance().getUid();

                            if (message.getTo().contains(userId) || message.getFrom().equals(userId)) {
                                message.setDbReference(dataSnapshot.getRef());
                                messageList.add(message);
                                messageAdapter.notifyDataSetChanged();
                                messageList_ReCyVw.smoothScrollToPosition(messageList.size() -1);
                            }

                        }
                    }
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        if (dataSnapshot.exists()){


                            Integer position  = null ;
                            for (int i = 0; i < messageList.size(); i++) {
                                if (messageList.get(i).getDbReference().getKey().equals(dataSnapshot.getKey())){
                                    position = i;
                                }
                            }

                            if (position == null) return; // Not found in adapter because user was not supposed to receive it

                            Message message = dataSnapshot.getValue(Message.class);

                            message.setDbReference(dataSnapshot.getRef());
                            messageList.set(position,message);
                            messageAdapter.notifyItemChanged(position);
                        }

                    }
                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                    }
                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }



    private void sendMessage(boolean encrypt) {
        String message = input_user_message.getText().toString();
        if (TextUtils.isEmpty(message)){
            SweetToast.info(GroupChatActivity.this, "Please type a message");
        } else {

            DatabaseReference user_message_key = rootReference.child("group_chat").push();
            String message_push_id = user_message_key.getKey();

            HashMap<String, Object> message_text_body = new HashMap<>();
            message_text_body.put("message", message);
            message_text_body.put("seen", false);
            message_text_body.put("type", "text");
            message_text_body.put("time", ServerValue.TIMESTAMP);
            message_text_body.put("from", messageSenderId);
            message_text_body.put("to", TextUtils.join("," , lastSelectedSendUsersIds));
            message_text_body.put("toEncrypt", encrypt);

            HashMap<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put("group_chat" + "/" + message_push_id, message_text_body);

            rootReference.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                    if (databaseError != null){
                        Log.e("Sending message", databaseError.getMessage());
                    }
                    input_user_message.setText("");
                }
            });
        }
    }


}
