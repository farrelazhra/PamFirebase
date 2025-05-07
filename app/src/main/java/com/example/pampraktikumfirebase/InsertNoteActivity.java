package com.example.pampraktikumfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class InsertNoteActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvEmail;
    private TextView tvUid;
    private Button btnKeluar;
    private FirebaseAuth mAuth;
    private EditText etTitle;
    private EditText etDesc;
    private Button btnSubmit;
    private Button btnLoad; // Tombol untuk load data
    private Button btnUpdate; // Tombol untuk update
    private Button btnDelete; // Tombol untuk delete
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private Note currentNote; // Untuk menyimpan note yang sedang diproses

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_note);

        tvEmail = findViewById(R.id.tv_email);
        tvUid = findViewById(R.id.tv_uid);
        btnKeluar = findViewById(R.id.btn_keluar);
        mAuth = FirebaseAuth.getInstance();
        btnKeluar.setOnClickListener(this);

        etTitle = findViewById(R.id.et_title);
        etDesc = findViewById(R.id.et_description);
        btnSubmit = findViewById(R.id.btn_submit);
        btnLoad = findViewById(R.id.btn_load);
        btnUpdate = findViewById(R.id.btn_update);
        btnDelete = findViewById(R.id.btn_delete);

        firebaseDatabase = FirebaseDatabase.getInstance("https://pamfirebase-553fc-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference("notes").child(mAuth.getUid());

        btnSubmit.setOnClickListener(this);
        btnLoad.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            tvUid.setText(currentUser.getUid());
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_keluar) {
            logOut();
        } else if (view.getId() == R.id.btn_submit) {
            submitData();
        } else if (view.getId() == R.id.btn_load) {
            loadData();
        } else if (view.getId() == R.id.btn_update) {
            updateData();
        } else if (view.getId() == R.id.btn_delete) {
            deleteData();
        }
    }

    public void logOut() {
        mAuth.signOut();
        Intent intent = new Intent(InsertNoteActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // CREATE - Tambah data baru
    public void submitData() {
        if (!validateForm()) {
            return;
        }
        String title = etTitle.getText().toString();
        String desc = etDesc.getText().toString();

        String noteId = databaseReference.push().getKey();
        Note newNote = new Note(title, desc);
        newNote.setId(noteId);

        databaseReference.child(noteId).setValue(newNote)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add note", Toast.LENGTH_SHORT).show());
    }

    // READ - Load data terakhir
    public void loadData() {
        databaseReference.limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    currentNote = dataSnapshot.getValue(Note.class);
                    if (currentNote != null) {
                        currentNote.setId(dataSnapshot.getKey());
                        etTitle.setText(currentNote.getTitle());
                        etDesc.setText(currentNote.getDescription());
                        Toast.makeText(InsertNoteActivity.this, "Note loaded", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(InsertNoteActivity.this, "Failed to load note", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // UPDATE - Update data yang sedang diproses
    public void updateData() {
        if (currentNote == null) {
            Toast.makeText(this, "No note selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateForm()) {
            return;
        }

        currentNote.setTitle(etTitle.getText().toString());
        currentNote.setDescription(etDesc.getText().toString());

        databaseReference.child(currentNote.getId()).setValue(currentNote)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update note", Toast.LENGTH_SHORT).show());
    }

    // DELETE - Hapus data yang sedang diproses
    public void deleteData() {
        if (currentNote == null) {
            Toast.makeText(this, "No note selected", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child(currentNote.getId()).removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                    clearForm();
                    currentNote = null;
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show());
    }

    private boolean validateForm() {
        boolean result = true;
        if (TextUtils.isEmpty(etTitle.getText().toString())) {
            etTitle.setError("Required");
            result = false;
        } else {
            etTitle.setError(null);
        }
        if (TextUtils.isEmpty(etDesc.getText().toString())) {
            etDesc.setError("Required");
            result = false;
        } else {
            etDesc.setError(null);
        }
        return result;
    }

    private void clearForm() {
        etTitle.setText("");
        etDesc.setText("");
    }
}