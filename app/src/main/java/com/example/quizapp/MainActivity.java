package com.example.quizapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import com.example.quizapp.Adapters.CategoryAdapter;
import com.example.quizapp.Models.CategoryModel;
import com.example.quizapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

ActivityMainBinding binding;
FirebaseDatabase database;
FirebaseStorage storage;
CircleImageView categoryImage;
EditText inputCategoryName;
Button uploadCategory;
View  fetchImage;

Dialog dialog;
Uri imageUri;
int i = 0;
ArrayList<CategoryModel>list;
CategoryAdapter adapter;
ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        list = new ArrayList<>();

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.item_add_category_dialoge);

        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(true);
        }

        progressDialog= new ProgressDialog(this);
        progressDialog.setTitle("Uploading");
        progressDialog.setMessage("Please Wait");

        uploadCategory = dialog.findViewById(R.id.btnUpload);
        inputCategoryName = dialog.findViewById(R.id.inputCategoryName);
        categoryImage  = dialog.findViewById(R.id.categoryimage);
        fetchImage = dialog.findViewById(R.id.fetchImage);


        GridLayoutManager layoutManager = new GridLayoutManager(this,2);
        binding.recyCategory.setLayoutManager(layoutManager);

        adapter = new CategoryAdapter(this,list);
        binding.recyCategory.setAdapter(adapter);

        database.getReference().child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
               if(snapshot.exists()){
                   list.clear();
                 for(DataSnapshot dataSnapshot : snapshot.getChildren()){

//                     CategoryModel model = dataSnapshot.getValue(CategoryModel.class);
//                     list.add(model);
                     list.add(new CategoryModel(
                             dataSnapshot.child("categoryName").getValue().toString(),
                             dataSnapshot.child("categoryImage").getValue().toString(),
                             dataSnapshot.getKey(),
                             Integer.parseInt(dataSnapshot.child("setnum").getValue().toString())
                     ));
                 }

                 adapter.notifyDataSetChanged();




               }else {
                   Toast.makeText(MainActivity.this, "category not exist", Toast.LENGTH_SHORT).show();
               }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        binding.addCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

        fetchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });


        uploadCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = inputCategoryName.getText().toString();

                if(imageUri==null){
                    Toast.makeText(MainActivity.this, "Please upload category image", Toast.LENGTH_SHORT).show();
                }
               if (name.isEmpty()) {
                    inputCategoryName.setError("Enter Category name");
                }else{
                    progressDialog.show();
                    uploadData();
                }
            }
        });

    }

    private void uploadData() {

        final  StorageReference reference = storage.getReference().child("category")
                .child(new Date().getTime()+"");
        reference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        CategoryModel categoryModel = new CategoryModel();
                        categoryModel.setCategoryName(inputCategoryName.getText().toString());
                       categoryModel.setSetnum(0);
                       categoryModel.setCategoryImage(imageUri.toString());

                       database.getReference().child("categories").child("category"+i++)
                               .setValue(categoryModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                   @Override
                                   public void onSuccess(Void unused) {
                                       Toast.makeText(MainActivity.this, "data uploaded", Toast.LENGTH_SHORT).show();
                                   progressDialog.dismiss();
                                   }
                               }).addOnFailureListener(new OnFailureListener() {
                                   @Override
                                   public void onFailure(@NonNull Exception e) {
                                       Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                  progressDialog.dismiss();
                                   }
                               });
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1){
            if(data!=null){
                imageUri = data.getData();
                categoryImage.setImageURI(imageUri);
            }
        }
    }
}