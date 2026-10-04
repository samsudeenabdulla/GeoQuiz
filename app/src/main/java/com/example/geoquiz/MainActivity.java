package com.example.geoquiz;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView questionTextView;
    private TextView progressTextView;
    private TextView feedbackTextView;
    private TextView actionResultTextView;
    private ImageView selectedImageView;

    private int currentIndex = 0;

    private final Question[] questionBank = new Question[] {
            new Question(R.string.question_australia, true),
            new Question(R.string.question_asia, true),
            new Question(R.string.question_pacific, false),
            new Question(R.string.question_everest, true),
            new Question(R.string.question_egypt, false)
    };

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getExtras() != null) {

                            Bitmap photo = (Bitmap) result.getData()
                                    .getExtras()
                                    .get("data");

                            if (photo != null) {
                                selectedImageView.setImageBitmap(photo);
                                selectedImageView.setVisibility(View.VISIBLE);
                                showActionResult(getString(R.string.photo_captured));
                            }
                        }
                    });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();

                            if (imageUri != null) {
                                selectedImageView.setImageURI(imageUri);
                                selectedImageView.setVisibility(View.VISIBLE);
                                showActionResult(getString(R.string.photo_selected));
                            }
                        }
                    });

    private final ActivityResultLauncher<Intent> contactsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri contactUri = result.getData().getData();

                            if (contactUri != null) {
                                Cursor cursor = getContentResolver().query(
                                        contactUri,
                                        new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                                        null,
                                        null,
                                        null
                                );

                                if (cursor != null) {
                                    try {
                                        if (cursor.moveToFirst()) {
                                            String contactName = cursor.getString(
                                                    cursor.getColumnIndexOrThrow(
                                                            ContactsContract.Contacts.DISPLAY_NAME
                                                    )
                                            );

                                            showActionResult(
                                                    getString(
                                                            R.string.selected_contact_format,
                                                            contactName
                                                    )
                                            );
                                        }
                                    } finally {
                                        cursor.close();
                                    }
                                }
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionTextView = findViewById(R.id.question_text_view);
        progressTextView = findViewById(R.id.progress_text_view);
        feedbackTextView = findViewById(R.id.feedback_text_view);
        actionResultTextView = findViewById(R.id.action_result_text_view);
        selectedImageView = findViewById(R.id.selected_image_view);

        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt("currentIndex", 0);
        }

        findViewById(R.id.true_button).setOnClickListener(view -> checkAnswer(true));
        findViewById(R.id.false_button).setOnClickListener(view -> checkAnswer(false));
        findViewById(R.id.next_button).setOnClickListener(view -> moveToNextQuestion());
        findViewById(R.id.reset_button).setOnClickListener(view -> restartQuiz());

        findViewById(R.id.share_button).setOnClickListener(view -> shareQuiz());
        findViewById(R.id.phone_button).setOnClickListener(view -> openPhoneDialer());
        findViewById(R.id.camera_button).setOnClickListener(view -> openCamera());
        findViewById(R.id.gallery_button).setOnClickListener(view -> openGallery());
        findViewById(R.id.contacts_button).setOnClickListener(view -> openContacts());

        displayQuestion();
    }

    private void displayQuestion() {
        Question question = questionBank[currentIndex];

        questionTextView.setText(question.getTextResId());
        progressTextView.setText(
                getString(R.string.progress_format, currentIndex + 1, questionBank.length)
        );

        feedbackTextView.setVisibility(View.GONE);
    }

    private void checkAnswer(boolean userAnswer) {
        boolean correctAnswer = questionBank[currentIndex].isAnswerTrue();

        if (userAnswer == correctAnswer) {
            feedbackTextView.setText(R.string.correct_toast);
            Toast.makeText(this, R.string.correct_toast, Toast.LENGTH_SHORT).show();
        } else {
            feedbackTextView.setText(R.string.incorrect_toast);
            Toast.makeText(this, R.string.incorrect_toast, Toast.LENGTH_SHORT).show();
        }

        feedbackTextView.setVisibility(View.VISIBLE);
    }

    private void moveToNextQuestion() {
        currentIndex = (currentIndex + 1) % questionBank.length;
        displayQuestion();
    }

    private void restartQuiz() {
        currentIndex = 0;
        displayQuestion();
        Toast.makeText(this, R.string.quiz_restarted, Toast.LENGTH_SHORT).show();
    }

    private void shareQuiz() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message));

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_button)));
    }

    private void openPhoneDialer() {
        Intent dialIntent = new Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:" + getString(R.string.phone_number))
        );
        startActivity(dialIntent);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, R.string.no_camera_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        galleryIntent.setType("image/*");
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);

        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
            galleryLauncher.launch(galleryIntent);
        } else {
            Toast.makeText(this, R.string.no_gallery_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void openContacts() {
        Intent contactsIntent = new Intent(
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI
        );

        if (contactsIntent.resolveActivity(getPackageManager()) != null) {
            contactsLauncher.launch(contactsIntent);
        } else {
            Toast.makeText(this, R.string.no_contacts_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void showActionResult(String message) {
        actionResultTextView.setText(message);
        actionResultTextView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentIndex", currentIndex);
    }
}