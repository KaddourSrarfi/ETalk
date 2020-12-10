package com.example.kaddour.ekuegertalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity
{

    private Button UpdateAccountSettings;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;

    private String currentUserID;
    private DatabaseReference RootRef;

    private static final int GalleryPick = 1;
    private StorageReference UserProfileImagesRef;
    private ProgressDialog loadingBar;

    private Toolbar SettingsToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile Images");


        InitializeFields();

        userName.setVisibility(View.INVISIBLE);

        UpdateAccountSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UpdateSettings();
            }
        });

        RetrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
              Intent galleryIntent = new Intent();
              galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
              galleryIntent.setType("image/*");
              startActivityForResult(galleryIntent,GalleryPick);
            }
        });

    }




    private void InitializeFields()
    {
       UpdateAccountSettings = (Button) findViewById(R.id.update_settings_button);
       userName = (EditText) findViewById(R.id.set_user_name);
       userStatus = (EditText) findViewById(R.id.set_profile_status);
       userProfileImage = (CircleImageView) findViewById(R.id.set_profile_image);
       loadingBar = new ProgressDialog(this);
       SettingsToolBar = (Toolbar) findViewById(R.id.settings_toolbar);
       setSupportActionBar(SettingsToolBar);
       getSupportActionBar().setDisplayHomeAsUpEnabled(true);
       getSupportActionBar().setDisplayShowCustomEnabled(true);
       getSupportActionBar().setTitle("Account Settings");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==GalleryPick && resultCode==RESULT_OK && data!=null)
        {
            Uri ImageUri = data.getData();
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK)
            {
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage(" Please wait, your Profile Image is Updating ...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri resultUri = result.getUri();

                StorageReference filePath = UserProfileImagesRef.child(currentUserID + ".jpg");
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {

                    @Override
                    public void onComplete(@androidx.annotation.NonNull Task<UploadTask.TaskSnapshot> task)
                    {
                      if (task.isSuccessful())
                      {
                          Toast.makeText(SettingsActivity.this, " Profile Image Uploaded Successfully ...", Toast.LENGTH_SHORT).show();
                          final  String downloadUrl = Objects.requireNonNull(task.getResult()).getStorage().getDownloadUrl().toString();
                          RootRef.child("Users").child(currentUserID).child("image").setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                              @Override
                              public void onComplete(@androidx.annotation.NonNull Task<Void> task)
                              {
                                  if (task.isSuccessful())
                                  {
                                      Toast.makeText(SettingsActivity.this, "Image saved in Database successfully ....", Toast.LENGTH_SHORT).show();
                                      loadingBar.dismiss();
                                  }
                                  else
                                  {
                                      String message = Objects.requireNonNull(task.getException()).toString();
                                      Toast.makeText(SettingsActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                      loadingBar.dismiss();
                                  }
                              }
                          });
                      }
                      else
                      {
                          String message = task.getException().toString();
                          Toast.makeText(SettingsActivity.this, " Error " + message , Toast.LENGTH_SHORT).show();
                          loadingBar.dismiss();
                      }
                    }
                });

            }

        }


    }

    private void UpdateSettings()
    {
        String setUserName = userName.getText().toString();
        String setStatus = userStatus.getText().toString();

        if (TextUtils.isEmpty(setUserName))
        {
            Toast.makeText(this, "Please Choose a User Name ...", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(setStatus))
        {
            Toast.makeText(this, "Please Write your Status ...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            HashMap<String, Object> profileMap = new HashMap<>();
                profileMap.put("uid", currentUserID);
                profileMap.put("name", setUserName);
                profileMap.put("status", setStatus);
             RootRef.child("Users").child(currentUserID).updateChildren(profileMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {
                                if (task.isSuccessful())
                                {
                                    sendUserToMainActivity();
                                    Toast.makeText(SettingsActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();

                                }
                                else
                                {
                                    String message = Objects.requireNonNull(task.getException()).toString();
                                    Toast.makeText(SettingsActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

        }


    }

    private void RetrieveUserInfo()
    {
        RootRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                       if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")) && (dataSnapshot.hasChild("image")))
                       {
                           String retrieveUserName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                           String retrieveStatus = dataSnapshot.child("status").getValue().toString();
                           String retrieveProfileImage = dataSnapshot.child("image").getValue().toString();

                           userName.setText(retrieveUserName);
                           userStatus.setText(retrieveStatus);
                           Picasso.get().load(retrieveProfileImage).into(userProfileImage);
                       }
                       else if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")))
                        {
                            String retrieveUserName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                            String retrieveStatus = Objects.requireNonNull(dataSnapshot.child("status").getValue()).toString();

                            userName.setText(retrieveUserName);
                            userStatus.setText(retrieveStatus);
                        }
                        else
                       {
                           userName.setVisibility(View.VISIBLE);
                           Toast.makeText(SettingsActivity.this, "Please Set & Update your Profile information  ...", Toast.LENGTH_SHORT).show();
                       }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError)
                    {

                    }
                });
    }


    private void sendUserToMainActivity()
    {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

}
