package com.project.graduation.chat.secure.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.graduation.chat.secure.Model.Message;
import com.project.graduation.chat.secure.R;
import com.makeramen.roundedimageview.RoundedImageView;
import com.project.graduation.chat.secure.Utils.BlurImage;
import com.project.graduation.chat.secure.Utils.Constants;
import com.project.graduation.chat.secure.Utils.CryptLib;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{

    private static final long TIME_MESSAGE_MILLIES = 3 * 1000; // Milleseconds
    private List<Message> messageList;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private DatabaseReference rootDbReference;
    private Context context;
    private Handler handler;
    private boolean isGroup;

    public MessageAdapter(Context context, List<Message> messageList, boolean isGroup) {
        this.isGroup = isGroup;
        this.messageList = messageList;
        this.context = context;
        this.handler = new Handler();
        mAuth = FirebaseAuth.getInstance();
        rootDbReference = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_message_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        String sender_UID = mAuth.getCurrentUser().getUid();
        final Message message = messageList.get(position);

        String from_user_ID = message.getFrom();
        final String messageType = message.getType();

        databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(from_user_ID);
        databaseReference.keepSynced(true); // for offline
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String userName = dataSnapshot.child("user_name").getValue().toString();
                    String userProfileImage = dataSnapshot.child("user_thumb_image").getValue().toString();
                    //
                    Picasso.get()
                            .load(userProfileImage)
                            //.networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                            .placeholder(R.drawable.default_profile_image)
                            .into(holder.user_profile_image);
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // if message type is TEXT
        if (messageType.equals("text")){
            holder.receiver_text_message.setVisibility(View.INVISIBLE);
            holder.user_profile_image.setVisibility(View.INVISIBLE);

            // when msg is TEXT, image views are gone
            holder.senderImageMsg.setVisibility(View.GONE);
            holder.receiverImageMsg.setVisibility(View.GONE);

            if (from_user_ID.equals(sender_UID)){
                holder.sender_text_message.setVisibility(View.VISIBLE);
                holder.sender_text_message.setBackgroundResource(R.drawable.single_message_text_another_background);
                holder.sender_text_message.setTextColor(Color.BLACK);
                holder.sender_text_message.setGravity(Gravity.LEFT);

                setMessageText(message,holder.sender_text_message);
            } else {
                holder.sender_text_message.setVisibility(View.INVISIBLE);
                holder.receiver_text_message.setVisibility(View.VISIBLE);
                holder.user_profile_image.setVisibility(View.VISIBLE);

                holder.receiver_text_message.setBackgroundResource(R.drawable.single_message_text_background);
                holder.receiver_text_message.setTextColor(Color.WHITE);
                holder.receiver_text_message.setGravity(Gravity.LEFT);

                setMessageText(message,holder.receiver_text_message);

            }
        }

        // if message type is Image
        if (messageType.equals("image")){
            // when msg has IMAGE, text views are GONE
            holder.sender_text_message.setVisibility(View.GONE);
            holder.receiver_text_message.setVisibility(View.GONE);

            if (from_user_ID.equals(sender_UID)){
                holder.user_profile_image.setVisibility(View.GONE);
                holder.receiverImageMsg.setVisibility(View.GONE);
                holder.senderImageMsg.setVisibility(View.VISIBLE);


                loadMessageImage(message,holder.senderImageMsg);

                Log.e("tag","from adapter, link : "+ message.getMessage());
            } else {
                holder.user_profile_image.setVisibility(View.VISIBLE);
                holder.senderImageMsg.setVisibility(View.GONE);
                holder.receiverImageMsg.setVisibility(View.VISIBLE);

                loadMessageImage(message,holder.receiverImageMsg);

                Log.e("tag","from adapter, link : "+ message.getMessage());

            }
        }


        if (message.isToEncrypt() && !message.isEncrypted() ){

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (isGroup){

                        String messageReference = "group_chat/"+message.getDbReference().getKey();

                        HashMap<String,Object> map = new HashMap<>();

                        if (messageType.equals("text")){
                            String encrypted = getEncryptedMessage(message.getMessage());
                            map.put("message",encrypted);
                        }
                        map.put("encrypted",true);
                        rootDbReference.child(messageReference).updateChildren(map);

                    }else {
                        String message_sender_reference = "messages/" + message.getFrom() + "/" + mAuth.getCurrentUser().getUid()+"/"+message.getDbReference().getKey();
                        String message_receiver_reference = "messages/" + mAuth.getCurrentUser().getUid() + "/" + message.getFrom()+"/"+message.getDbReference().getKey();

                        HashMap<String,Object> map = new HashMap<>();

                        if (messageType.equals("text")){
                            String encrypted = getEncryptedMessage(message.getMessage());
                            map.put("message",encrypted);
                        }
                        map.put("encrypted",true);
                        rootDbReference.child(message_sender_reference).updateChildren(map);
                        rootDbReference.child(message_receiver_reference).updateChildren(map);
                    }

                }
            },TIME_MESSAGE_MILLIES);
        }

    }

    private void setMessageText(Message message, TextView textView) {


        textView.setText(message.getMessage());
        if (message.isEncrypted()){

            float radius = textView.getTextSize() / 4;
            BlurMaskFilter filter = new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL);
            textView.getPaint().setMaskFilter(filter);

        }else {

            textView.getPaint().setMaskFilter(null);

        }


    }

    private void loadMessageImage(Message message, final RoundedImageView imageView) {
        if (message.isEncrypted()){

            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    imageView.setImageBitmap(BlurImage.fastblur(bitmap, 1f, 80));
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                }


                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };

            Picasso.get()
                    .load(message.getMessage())
                    //.networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                    .placeholder(new ColorDrawable(ContextCompat.getColor(context,R.color.gray)))
                    .into(target);

        }else {

            Picasso.get()
                    .load(message.getMessage())
                    //.networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                    .placeholder(new ColorDrawable(ContextCompat.getColor(context,R.color.gray)))
                    .into(imageView);


        }
    }

    private String getEncryptedMessage(String message) {
        try {
            CryptLib _crypt = new CryptLib();
            String output= "";
            String key = CryptLib.SHA256(Constants.AES_KEY, 32); //32 bytes = 256 bit
            String iv = CryptLib.generateRandomIV(16); //16 bytes = 128 bit
            output = _crypt.encrypt(message, key, iv); //encrypt
            System.out.println("encrypted text=" + output);
            //output = _crypt.decrypt(output, key,iv); //decrypt
            //System.out.println("decrypted text=" + output);
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    @Override
    public int getItemCount() {
        return messageList.size();
    }


    public class MessageViewHolder extends RecyclerView.ViewHolder{
        TextView sender_text_message, receiver_text_message;
        CircleImageView user_profile_image;
        RoundedImageView senderImageMsg, receiverImageMsg;

        MessageViewHolder(View view){
            super(view);
            sender_text_message = view.findViewById(R.id.senderMessageText);
            receiver_text_message = view.findViewById(R.id.receiverMessageText);
            user_profile_image = view.findViewById(R.id.messageUserImage);

            senderImageMsg = view.findViewById(R.id.messageImageVsender);
            receiverImageMsg = view.findViewById(R.id.messageImageVreceiver);
        }

    }
}
