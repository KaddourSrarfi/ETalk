package com.example.kaddour.ekuegertalk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class ProfileActivity extends AppCompatActivity
{

    private String receiverUserId, senderUserId, current_state ;

    private CircleImageView userProfileImage;
    private TextView userProfileName , userProfileStatus;
    private Button sendMessageRequestButton, declineMessageRequestButton;

    private DatabaseReference UserRef , ChatRequestRef , ContactsRef ;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();

        userProfileImage = (CircleImageView) findViewById(R.id.visit_profile_image);
        userProfileName = (TextView) findViewById(R.id.visit_user_name);
        userProfileStatus = (TextView) findViewById(R.id.visit_user_status);
        sendMessageRequestButton = (Button) findViewById(R.id.send_message_request_button);
        declineMessageRequestButton = (Button) findViewById(R.id.decline_message_request_button);
        current_state = "new";
        senderUserId = mAuth.getCurrentUser().getUid();

         RetrieveUserInfo ();

    }




    private void RetrieveUserInfo()
    {
      UserRef.child(receiverUserId).addValueEventListener(new ValueEventListener()
      {
          @Override
          public void onDataChange(@NonNull DataSnapshot dataSnapshot)
          {

            if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("image")))
            {
              String userImage = dataSnapshot.child("image").getValue().toString();
              String userName = dataSnapshot.child("user").getValue().toString();
              String userStatus = dataSnapshot.child("status").getValue().toString();

              Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
              userProfileName.setText(userName);
              userProfileStatus.setText(userStatus);

              ManageChatRequests();
            }

            else
            {
                String userName = dataSnapshot.child("user").getValue().toString();
                String userStatus = dataSnapshot.child("status").getValue().toString();

                userProfileName.setText(userName);
                userProfileStatus.setText(userStatus);

                ManageChatRequests();
            }

          }

          @Override
          public void onCancelled(@NonNull DatabaseError databaseError)
          {

          }
      });
    }




    private void ManageChatRequests()
    {

        ChatRequestRef.child(senderUserId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {

                if (dataSnapshot.hasChild(receiverUserId))
                {

                    String request_type = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();

                    if (request_type.equals("sent"))
                    {
                        current_state = "request_sent";
                        sendMessageRequestButton.setText("Cancel Chat Request");
                    }

                    else if (request_type.equals("received"))
                    {
                        current_state = "request_received";
                        sendMessageRequestButton.setText(" Accept Chat Request");

                        declineMessageRequestButton.setVisibility(View.VISIBLE);
                        declineMessageRequestButton.setEnabled(true);

                        declineMessageRequestButton.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                CancelChatRequest();
                            }
                        });
                    }
                }

                else
                {
                    ContactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                        {
                            if(dataSnapshot.hasChild(receiverUserId))
                            {
                                current_state = "friends";
                                sendMessageRequestButton.setText("Remove Contact");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        if (!senderUserId.equals(receiverUserId))
        {

            sendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {

                    sendMessageRequestButton.setEnabled(false);
                    if (current_state.equals("new"))
                    {

                        SendChatRequest();

                    }

                    if (current_state.equals("request_sent"))
                    {
                        CancelChatRequest();
                    }
                    if (current_state.equals("request_received"))
                    {
                        AcceptChatRequest();

                    }
                    if (current_state.equals("friends"))
                    {
                        RemoveContact();

                    }

                }
            });
        }
        else
        {
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        }

    }


    private void RemoveContact()
    {

        ContactsRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if (task.isSuccessful())
                {
                    ContactsRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {

                            if (task.isSuccessful())
                            {
                                sendMessageRequestButton.setEnabled(true);
                                current_state = "new";
                                sendMessageRequestButton.setText(" Send A Message");

                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                declineMessageRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });

    }


    private void AcceptChatRequest()
    {

        ContactsRef.child(senderUserId).child(receiverUserId).child("Contacts").setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if (task.isSuccessful())
                {
                    ContactsRef.child(receiverUserId).child(senderUserId).child("Contacts").setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if (task.isSuccessful())
                            {

                                ChatRequestRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if(task.isSuccessful())
                                        {
                                            ChatRequestRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>()
                                            {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task)
                                                {

                                                    sendMessageRequestButton.setEnabled(true);
                                                    current_state = "friends";
                                                    sendMessageRequestButton.setText("Remove Contact");

                                                    declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                    declineMessageRequestButton.setEnabled(false);
                                                }
                                            });
                                        }
                                    }
                                });
                            }

                        }
                    });
                }

            }
        });

    }





    private void CancelChatRequest()
    {
        ChatRequestRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if (task.isSuccessful())
                {
                    ChatRequestRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {

                            if (task.isSuccessful())
                            {
                                sendMessageRequestButton.setEnabled(true);
                                current_state = "new";
                                sendMessageRequestButton.setText(" Send A Message");

                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                declineMessageRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }



    private void SendChatRequest()
    {

        ChatRequestRef.child(senderUserId).child(receiverUserId).child("request_type").setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if (task.isSuccessful())
                {
                    ChatRequestRef.child(receiverUserId).child(senderUserId).child("request_type").setValue("received").addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if (task.isSuccessful())
                            {
                                sendMessageRequestButton.setEnabled(true);
                                current_state = "request_sent";
                                sendMessageRequestButton.setText("Cancel Chat Request");
                            }
                        }
                    });
                }
            }
        });

    }
}
