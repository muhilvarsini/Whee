package com.example.whee4.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.whee4.Model.ChatModel;
import com.example.whee4.R;
import com.example.whee4.Model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileSettings extends AppCompatActivity {
    SwitchMaterial switchMat;
    Button changePassword;
    DatabaseReference dbref;
    EditText oldPassword,newPassword, uname;
    FirebaseUser user;
    ImageView editUName;
    ProgressDialog progressDialog;
    TextView toAbout, delAcc;
    SharedPreferences sharedPreferences=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ProfileSettings.this.setTitle("Settings");

        setContentView(R.layout.activity_profile_settings);

        toAbout = (TextView) findViewById(R.id.textView3);
        toAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileSettings.this, about.class));
            }
        });
        progressDialog = new ProgressDialog(ProfileSettings.this);

        String currUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(currUser);

        uname = (EditText)findViewById(R.id.uname);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel um = snapshot.getValue(UserModel.class);
                uname.setText(um.getUsername());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        editUName = (ImageView)findViewById(R.id.edit_uname);
        editUName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText taskEditText = new EditText(ProfileSettings.this);
                AlertDialog.Builder unameChanger = new AlertDialog.Builder(ProfileSettings.this);
                unameChanger.setTitle("Change Username");
                unameChanger.setView(taskEditText);
                unameChanger.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String newUname = String.valueOf(taskEditText.getText());
                        String regex = "^[A-Za-z]\\w{5,29}$";
                        Pattern p = Pattern.compile(regex);
                        Matcher m = p.matcher(newUname);
                        if(!m.matches()){
                            Toast.makeText(ProfileSettings.this, "Invalid username!Your username should have atleast 5 characters and should not contain any special characters.", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(currUser);

                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        UserModel um = dataSnapshot.getValue(UserModel.class);
                                        String newUname = String.valueOf(taskEditText.getText());
                                        HashMap<String, String> hashMap = new HashMap<>();
                                        hashMap.put("id", um.getId());
                                        hashMap.put("username", newUname);
                                        hashMap.put("imageURL", um.getImageURL());

                                        ref.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()){
                                                    uname.setText(newUname);
                                                    Toast.makeText(ProfileSettings.this, "Username changed successfuly!", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    }
                });
                unameChanger.setNegativeButton("Cancel", null)
                        .create().show();
            }
        });


        changePassword=(Button)findViewById(R.id.changePass);
        oldPassword=(EditText)findViewById(R.id.oldpassword);
        newPassword=(EditText)findViewById(R.id.newpassword) ;
        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldp=oldPassword.getText().toString().trim();
                String newp=newPassword.getText().toString().trim();
                if(TextUtils.isEmpty(oldp)){
                    Toast.makeText(getApplicationContext(),"Enter your current password...",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(newp.length()<6){
                    Toast.makeText(getApplicationContext(),"Your password should contain atleast 6 characters..",Toast.LENGTH_SHORT).show();
                    return;
                }
                updatePassword(oldp,newp);
            }
        });


        toAbout = (TextView) findViewById(R.id.textView3);
        toAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


        delAcc=(TextView) findViewById(R.id.deleteAccount);
        delAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder deletealert = new AlertDialog.Builder(ProfileSettings.this);
                deletealert.setTitle("Are you sure?")
                        .setMessage("Deleteing this account will result in completely removing the account " +
                                "from the system along with all your uploads.")
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                user = FirebaseAuth.getInstance().getCurrentUser();
                                progressDialog.setTitle("Deleting Account...");
                                progressDialog.show();
                                String userId = user.getUid();
                                user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){

                                            dbref = FirebaseDatabase.getInstance().getReference("Found");

                                            dbref.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot postSnapshot : snapshot.getChildren()){
                                                        UploadInfo imageUploadInfo = postSnapshot.getValue(UploadInfo.class);
                                                        if(imageUploadInfo.getUserId().equals(userId))
                                                            postSnapshot.getRef().removeValue();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });

                                            dbref = FirebaseDatabase.getInstance().getReference("Lost");
                                            dbref.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot postSnapshot : snapshot.getChildren()){
                                                        UploadInfo itemInfo = postSnapshot.getValue(UploadInfo.class);
                                                        if(itemInfo.getUserId().equals(userId))
                                                            postSnapshot.getRef().removeValue();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });

                                            dbref = FirebaseDatabase.getInstance().getReference("Chats");

                                            dbref.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot postSnapshot : snapshot.getChildren()){
                                                        ChatModel chatInfo = postSnapshot.getValue(ChatModel.class);
                                                        if(chatInfo.getSender().equals(userId) || chatInfo.getReceiver().equals(userId))
                                                            postSnapshot.getRef().removeValue();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });

                                            dbref = FirebaseDatabase.getInstance().getReference("Chatlist").child(userId);
                                            dbref.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    dbref = FirebaseDatabase.getInstance().getReference("Users").child(userId);
                                                    dbref.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(getApplicationContext(), "Account Deleted!", Toast.LENGTH_LONG).show();
                                                            Intent intent = new Intent(ProfileSettings.this, RegisterActivity.class);
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(intent);
                                                        }
                                                    });
                                                }
                                            });
                                        }else
                                        {
                                            Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).show();
            }
        });



        switchMat=findViewById(R.id.switchMaterial);
        sharedPreferences=getSharedPreferences("night",0);
        Boolean booleanValue=sharedPreferences.getBoolean("night_mode",true);
        if(booleanValue)
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            switchMat.setChecked(true);
        }
        switchMat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    switchMat.setChecked(true);
                    SharedPreferences.Editor editor=sharedPreferences.edit();
                    editor.putBoolean("night_mode",true);
                    editor.commit();
                    Toast.makeText(getApplicationContext(), "Night Mode Activated", Toast.LENGTH_LONG).show();
                }
                else{
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    switchMat.setChecked(false);
                    SharedPreferences.Editor editor=sharedPreferences.edit();
                    editor.putBoolean("night_mode",false);
                    editor.commit();
                    Toast.makeText(getApplicationContext(), "Night Mode Deactivated", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private void updatePassword(String oldp, String newp) {
        user = FirebaseAuth.getInstance().getCurrentUser();
        AuthCredential authCredential= EmailAuthProvider.getCredential(user.getEmail(),oldp);
        user.reauthenticate(authCredential).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressDialog.setTitle("Updating Password...");
                progressDialog.show();
                user.updatePassword(newp)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Password Updated....", Toast.LENGTH_LONG).show();
                                oldPassword.setText("");
                                newPassword.setText("");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext()," e.getMessage()", Toast.LENGTH_LONG).show();
                                oldPassword.setText("");
                                newPassword.setText("");
                            }
                        });
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        oldPassword.setText("");
                        newPassword.setText("");
                    }
                });

    }
}