package com.arifin.photoeditorbasic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private SeekBar seekBrightness;
    private Button btnChoose, btnGrayscale, btnSepia, btnReset, btnCrop, btnRotate, btnFlip, btnSave;

    private Bitmap originalBitmap;
    private Bitmap editedBitmap;
    private Uri imageUri;

    ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri sourceUri = result.getData().getData();
                        if (sourceUri != null) {
                            Uri destUri = Uri.fromFile(new File(getCacheDir(), UUID.randomUUID() + ".jpg"));
                            UCrop.of(sourceUri, destUri).start(this);
                        }
                    }
                });

        btnChoose.setOnClickListener(v -> openImagePicker());

        btnGrayscale.setOnClickListener(v -> {
            if (editedBitmap != null) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                applyColorMatrix(matrix);
            }
        });

        btnSepia.setOnClickListener(v -> {
            if (editedBitmap != null) {
                ColorMatrix sepia = new ColorMatrix();
                sepia.setScale(1f, 0.95f, 0.82f, 1.0f);
                applyColorMatrix(sepia);
            }
        });

        btnReset.setOnClickListener(v -> {
            if (originalBitmap != null) {
                editedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(editedBitmap);
                seekBrightness.setProgress(100);
            }
        });

        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (editedBitmap != null) {
                    float scale = progress / 100f;
                    Bitmap newBitmap = Bitmap.createBitmap(
                            editedBitmap.getWidth(), editedBitmap.getHeight(), editedBitmap.getConfig());

                    for (int x = 0; x < editedBitmap.getWidth(); x++) {
                        for (int y = 0; y < editedBitmap.getHeight(); y++) {
                            int pixel = editedBitmap.getPixel(x, y);
                            int r = (int) (android.graphics.Color.red(pixel) * scale);
                            int g = (int) (android.graphics.Color.green(pixel) * scale);
                            int b = (int) (android.graphics.Color.blue(pixel) * scale);
                            newBitmap.setPixel(x, y, android.graphics.Color.rgb(
                                    Math.min(255, r), Math.min(255, g), Math.min(255, b)));
                        }
                    }
                    imageView.setImageBitmap(newBitmap);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnRotate.setOnClickListener(v -> {
            if (editedBitmap != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                editedBitmap = Bitmap.createBitmap(
                        editedBitmap, 0, 0,
                        editedBitmap.getWidth(),
                        editedBitmap.getHeight(),
                        matrix, true);
                imageView.setImageBitmap(editedBitmap);
            }
        });

        btnFlip.setOnClickListener(v -> {
            if (editedBitmap != null) {
                Matrix matrix = new Matrix();
                matrix.preScale(-1.0f, 1.0f);
                editedBitmap = Bitmap.createBitmap(
                        editedBitmap, 0, 0,
                        editedBitmap.getWidth(),
                        editedBitmap.getHeight(),
                        matrix, true);
                imageView.setImageBitmap(editedBitmap);
            }
        });

        btnCrop.setOnClickListener(v -> {
            if (imageUri != null) {
                Uri destUri = Uri.fromFile(new File(getCacheDir(), UUID.randomUUID() + ".jpg"));
                UCrop.of(imageUri, destUri).start(this);
            }
        });

        btnSave.setOnClickListener(v -> {
            if (editedBitmap != null) {
                try {
                    Uri uri = MediaStore.Images.Media.insertImage(getContentResolver(), editedBitmap, "edited_" + System.currentTimeMillis(), null) != null
                            ? imageUri : null;
                    Toast.makeText(this, "Gambar disimpan", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal menyimpan", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initViews() {
        imageView = findViewById(R.id.imageView);
        seekBrightness = findViewById(R.id.seekBarBrightness);
        btnChoose = findViewById(R.id.btnPickImage);
        btnGrayscale = findViewById(R.id.btnGrayscale);
        btnSepia = findViewById(R.id.btnSepia);
        btnReset = findViewById(R.id.btnReset);
        btnCrop = findViewById(R.id.btnCrop);
        btnRotate = findViewById(R.id.btnRotate);
        btnFlip = findViewById(R.id.btnFlip);
        btnSave = findViewById(R.id.btnSave);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void applyColorMatrix(ColorMatrix colorMatrix) {
        Bitmap result = Bitmap.createBitmap(
                editedBitmap.getWidth(), editedBitmap.getHeight(), editedBitmap.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(result);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(editedBitmap, 0, 0, paint);
        editedBitmap = result;
        imageView.setImageBitmap(editedBitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            try {
                imageUri = resultUri;
                originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(resultUri));
                editedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(editedBitmap);
                seekBrightness.setProgress(100);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Crop Error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
