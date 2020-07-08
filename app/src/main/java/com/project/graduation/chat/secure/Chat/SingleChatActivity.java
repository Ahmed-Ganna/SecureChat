package com.project.graduation.chat.secure.Chat;

import android.Manifest;
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
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.project.graduation.chat.secure.Adapter.MessageAdapter;
import com.project.graduation.chat.secure.Model.Message;
import com.project.graduation.chat.secure.R;
import com.project.graduation.chat.secure.Utils.UserLastSeenTime;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.hasnat.sweettoast.SweetToast;


public class SingleChatActivity extends AppCompatActivity {

    private String messageReceiverID;
    private String messageReceiverName;

    private Toolbar chatToolbar;
    private TextView chatUserName;
    private TextView chatUserActiveStatus, ChatConnectionTV;
    private CircleImageView chatUserImageView;

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

    private ConnectivityReceiver connectivityReceiver;
    private boolean imgSecurity;
    private long encryptSecs;
    private ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        dialog = new ProgressDialog(this);

        dialog.setMessage("Please wait.");

        rootReference = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();

        messageReceiverID = getIntent().getExtras().get("visitUserId").toString();
        messageReceiverName = getIntent().getExtras().get("userName").toString();

        imageMessageStorageRef = FirebaseStorage.getInstance().getReference().child("messages_image");

        // appbar / toolbar
        chatToolbar = findViewById(R.id.chats_appbar);
        setSupportActionBar(chatToolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.appbar_chat, null);
        actionBar.setCustomView(view);

        ChatConnectionTV = findViewById(R.id.ChatConnectionTV);
        chatUserName = findViewById(R.id.chat_user_name);
        chatUserActiveStatus = findViewById(R.id.chat_active_status);
        chatUserImageView = findViewById(R.id.chat_profile_image);

        // sending message declaration
        send_message = findViewById(R.id.c_send_message_BTN);
        send_image = findViewById(R.id.c_send_image_BTN);
        input_user_message = findViewById(R.id.c_input_message);

        // setup for showing messages
        messageAdapter = new MessageAdapter(this,messageList, false);
        messageList_ReCyVw = findViewById(R.id.message_list);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageList_ReCyVw.setLayoutManager(linearLayoutManager);
        messageList_ReCyVw.setHasFixedSize(true);
        //linearLayoutManager.setReverseLayout(true);
        messageList_ReCyVw.setAdapter(messageAdapter);

        fetchMessages();

        chatUserName.setText(messageReceiverName);
        rootReference.child("users").child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final String active_status = dataSnapshot.child("active_now").getValue().toString();
                        final String thumb_image = dataSnapshot.child("user_thumb_image").getValue().toString();

//                        // FOR TESTING
//                        if (currentUser != null){
//                            rootReference.child("active_now").setValue(ServerValue.TIMESTAMP);
//                        }

                        // show image on appbar
                        Picasso.get()
                                .load(thumb_image)
                                //.networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                                .placeholder(R.drawable.default_profile_image)
                                .into(chatUserImageView, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        Picasso.get()
                                                .load(thumb_image)
                                                .placeholder(R.drawable.default_profile_image)
                                                .into(chatUserImageView);
                                    }
                                });

                        //active status
                        if (active_status.contains("true")){
                            chatUserActiveStatus.setText("Active now");
                        } else {
                            UserLastSeenTime lastSeenTime = new UserLastSeenTime();
                            long last_seen = Long.parseLong(active_status);

                            //String lastSeenOnScreenTime = lastSeenTime.getTimeAgo(last_seen).toString();
                            String lastSeenOnScreenTime = lastSeenTime.getTimeAgo(last_seen, getApplicationContext()).toString();
                            Log.e("lastSeenTime", lastSeenOnScreenTime);
                            if (lastSeenOnScreenTime != null){
                                chatUserActiveStatus.setText(lastSeenOnScreenTime);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });


        /**
         *  SEND TEXT MESSAGE BUTTON
         */
        send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEncTimeDialog("text");
            }
        });


        /** SEND IMAGE MESSAGE BUTTON */
        send_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showEncTimeDialog("image");

            }
        });
    } // ending onCreate


    private void showEncTimeDialog(final String type) {
        final long[] times = {5,10,15};
        final String[] types = {"5 sec", "10 sec" , "15 sec"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Time");
        builder.setItems(types, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


                showSecureOptionsDialog(type,times[which]);

            }
        });
        builder.show();
    }

    private void showSecureOptionsDialog(final String type, final long encSecs) {
        String[] types = {"Secure message", "Don`t secure message"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Security");
        builder.setItems(types, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (type.equals("text")){


                    sendMessage(which==0,encSecs );


                }else {
                    imgSecurity = which==0;
                    encryptSecs = encSecs;

                    requestImagePermission();

                }
            }
        });
        builder.show();
    }

    private void requestImagePermission() {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        Intent galleryIntent = new Intent().setAction(Intent.ACTION_GET_CONTENT);
                        galleryIntent.setType("image/*");
                        startActivityForResult(galleryIntent, GALLERY_PICK_CODE);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
    }


    @Override
    protected void onResume() {
        super.onResume();
        //Register Connectivity Broadcast receiver
        connectivityReceiver = new ConnectivityReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, intentFilter);
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Unregister Connectivity Broadcast receiver
        unregisterReceiver(connectivityReceiver);
    }


    @Override // for gallery picking
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         //  For image sending
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_PICK_CODE && resultCode == RESULT_OK && data != null && data.getData() != null){

            showLoading();

            Uri imageUri = data.getData();

            // image message sending size compressing will be placed below

            final String message_sender_reference = "messages/" + messageSenderId + "/" + messageReceiverID;
            final String message_receiver_reference = "messages/" + messageReceiverID + "/" + messageSenderId;

            DatabaseReference user_message_key = rootReference.child("messages").child(messageSenderId).child(messageReceiverID).push();
            final String message_push_id = user_message_key.getKey();

            final StorageReference file_path = imageMessageStorageRef.child(message_push_id + ".jpg");

            UploadTask uploadTask = file_path.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task){
                    if (!task.isSuccessful()){
                        hideLoading();
                        SweetToast.error(SingleChatActivity.this, "Error: " + task.getException().getMessage());
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
                            message_text_body.put("toEncrypt", imgSecurity);
                            message_text_body.put("encryptSecs", encryptSecs);

                            HashMap<String, Object> messageBodyDetails = new HashMap<>();
                            messageBodyDetails.put(message_sender_reference + "/" + message_push_id, message_text_body);
                            messageBodyDetails.put(message_receiver_reference + "/" + message_push_id, message_text_body);

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
                            hideLoading();
                            SweetToast.warning(SingleChatActivity.this, "Failed to send image. Try again");
                        }
                    }
                }
            });
        }
    }

    private void fetchMessages() {
        rootReference.child("messages").child(messageSenderId).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.exists()){
                            Message message = dataSnapshot.getValue(Message.class);
                            message.setDbReference(dataSnapshot.getRef());
                            messageList.add(message);
                            messageAdapter.notifyDataSetChanged();
                            messageList_ReCyVw.smoothScrollToPosition(messageList.size() -1);
                        }
                    }
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        if (dataSnapshot.exists()){

                            int position = 0 ;
                            for (int i = 0; i < messageList.size(); i++) {
                                if (messageList.get(i).getDbReference().getKey().equals(dataSnapshot.getKey())){
                                    position = i;
                                }
                            }

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



    private void sendMessage(boolean toEncrypt, long encSecs) {
        String message = input_user_message.getText().toString();
        if (TextUtils.isEmpty(message)){
            SweetToast.info(SingleChatActivity.this, "Please type a message");
        } else {
            String message_sender_reference = "messages/" + messageSenderId + "/" + messageReceiverID;
            String message_receiver_reference = "messages/" + messageReceiverID + "/" + messageSenderId;

            DatabaseReference user_message_key = rootReference.child("messages").child(messageSenderId).child(messageReceiverID).push();
            String message_push_id = user_message_key.getKey();

            HashMap<String, Object> message_text_body = new HashMap<>();
            message_text_body.put("message", message);
            message_text_body.put("seen", false);
            message_text_body.put("type", "text");
            message_text_body.put("time", ServerValue.TIMESTAMP);
            message_text_body.put("from", messageSenderId);
            message_text_body.put("toEncrypt", toEncrypt);
            message_text_body.put("encryptSecs", encSecs);

            HashMap<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(message_sender_reference + "/" + message_push_id, message_text_body);
            messageBodyDetails.put(message_receiver_reference + "/" + message_push_id, message_text_body);

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


    // Broadcast receiver for network checking
    public class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            ChatConnectionTV.setVisibility(View.GONE);
            if (networkInfo != null && networkInfo.isConnected()){
                ChatConnectionTV.setText("Internet connected");
                ChatConnectionTV.setTextColor(Color.WHITE);
                ChatConnectionTV.setVisibility(View.VISIBLE);

                // LAUNCH activity after certain time period
                new Timer().schedule(new TimerTask(){
                    public void run() {
                        SingleChatActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                ChatConnectionTV.setVisibility(View.GONE);
                            }
                        });
                    }
                }, 1200);
            } else {
                ChatConnectionTV.setText("No internet connection! ");
                ChatConnectionTV.setTextColor(Color.WHITE);
                ChatConnectionTV.setBackgroundColor(Color.RED);
                ChatConnectionTV.setVisibility(View.VISIBLE);
            }
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


}
