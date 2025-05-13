package com.arifin.photoeditorbasic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.yalantis.ucrop.UCrop;
import java.util.Stack;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private SeekBar seekBrightness;
    private Button btnChoose, btnGrayscale, btnSepia, btnReset, btnCrop, btnRotate, btnFlip, btnHistogram, btnBlur, btnKontrass;
    private ImageButton btnUndo, btnRedo, btnSave;
    private ConstraintLayout layoutEdit;
    private Stack<Bitmap> undoStack = new Stack<>();
    private Stack<Bitmap> redoStack = new Stack<>();
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

        btnChoose.setOnClickListener(v -> {
            openImagePicker();
//            layoutEdit.setVisibility(View.VISIBLE);
            layoutEdit.postDelayed(() -> layoutEdit.setVisibility(View.VISIBLE), 3000);
        });

        btnGrayscale.setOnClickListener(v -> {
            if (editedBitmap != null) {
                pushToUndoStack(); // tambahkan ini
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                applyColorMatrix(matrix);
            }
        });

        btnSepia.setOnClickListener(v -> {
            if (editedBitmap != null) {
                pushToUndoStack(); // tambahkan ini
                ColorMatrix sepia = new ColorMatrix();
                sepia.setScale(1f, 0.95f, 0.82f, 1.0f);
                applyColorMatrix(sepia);
            }
        });

        btnReset.setOnClickListener(v -> {
            if (originalBitmap != null) {
                pushToUndoStack(); // tambahkan ini
                editedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(editedBitmap);
                seekBrightness.setProgress(100);
            }
        });

        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (editedBitmap != null) {
                    pushToUndoStack(); // tambahkan ini
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
                pushToUndoStack(); // tambahkan ini
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
                pushToUndoStack(); // tambahkan ini
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
                pushToUndoStack(); // tambahkan ini
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

        btnUndo.setOnClickListener(v -> {
            if (!undoStack.isEmpty()) {
                redoStack.push(editedBitmap.copy(editedBitmap.getConfig(), true));
                editedBitmap = undoStack.pop();
                imageView.setImageBitmap(editedBitmap);
            }
        });

        btnRedo.setOnClickListener(v -> {
            if (!redoStack.isEmpty()) {
                undoStack.push(editedBitmap.copy(editedBitmap.getConfig(), true));
                editedBitmap = redoStack.pop();
                imageView.setImageBitmap(editedBitmap);
            }
        });

        btnHistogram.setOnClickListener(v -> {
            if (editedBitmap != null) {
                pushToUndoStack(); // tambahkan ini
                editedBitmap = equalizeHistogram(editedBitmap);
                imageView.setImageBitmap(editedBitmap);
            }
        });

        btnBlur.setOnClickListener(v -> {
            if (editedBitmap != null) {
                pushToUndoStack(); // tambahkan ini
                editedBitmap = applyBlur(editedBitmap);
                imageView.setImageBitmap(editedBitmap);
            }
        });

        btnKontrass.setOnClickListener(v -> {
            if (editedBitmap != null) {
                pushToUndoStack(); // tambahkan ini
                editedBitmap = adjustContrast(editedBitmap, 1.5f); // 1.5x kontras
                imageView.setImageBitmap(editedBitmap);
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
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnHistogram = findViewById(R.id.btnHistogram);
        btnBlur = findViewById(R.id.btnBlur);
        btnKontrass = findViewById(R.id.btnKontrass);
        layoutEdit = findViewById(R.id.layoutEdit);
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

    private void pushToUndoStack() {
        if (editedBitmap != null) {
            undoStack.push(editedBitmap.copy(editedBitmap.getConfig(), true));
            // Kosongkan redo karena operasi baru dilakukan
            redoStack.clear();
        }
    }

    private Bitmap equalizeHistogram(Bitmap src) {
        Bitmap bmp = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        int width = src.getWidth();
        int height = src.getHeight();
        int[] histogram = new int[256];
        int[] lut = new int[256];
        int totalPixels = width * height;

        // Grayscale & histogram
        int[] pixels = new int[totalPixels];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < totalPixels; i++) {
            int gray = (int) (Color.red(pixels[i]) * 0.3 + Color.green(pixels[i]) * 0.59 + Color.blue(pixels[i]) * 0.11);
            histogram[gray]++;
        }

        // Cumulative histogram
        int sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += histogram[i];
            lut[i] = (sum * 255) / totalPixels;
        }

        // Apply LUT
        for (int i = 0; i < totalPixels; i++) {
            int gray = (int) (Color.red(pixels[i]) * 0.3 + Color.green(pixels[i]) * 0.59 + Color.blue(pixels[i]) * 0.11);
            int newGray = lut[gray];
            pixels[i] = Color.rgb(newGray, newGray, newGray);
        }

        bmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return bmp;
    }

    private Bitmap applyBlur(Bitmap src) {
        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());

        for (int x = 1; x < src.getWidth() - 1; x++) {
            for (int y = 1; y < src.getHeight() - 1; y++) {
                int r = 0, g = 0, b = 0;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int pixel = src.getPixel(x + dx, y + dy);
                        r += Color.red(pixel);
                        g += Color.green(pixel);
                        b += Color.blue(pixel);
                    }
                }

                r /= 9;
                g /= 9;
                b /= 9;

                output.setPixel(x, y, Color.rgb(r, g, b));
            }
        }

        return output;
    }

    private Bitmap adjustContrast(Bitmap src, float contrast) {
        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());

        float scale = contrast;
        float translate = (-0.5f * scale + 0.5f) * 255.f;

        for (int x = 0; x < src.getWidth(); x++) {
            for (int y = 0; y < src.getHeight(); y++) {
                int pixel = src.getPixel(x, y);
                int r = Math.min(255, Math.max(0, (int)(Color.red(pixel) * scale + translate)));
                int g = Math.min(255, Math.max(0, (int)(Color.green(pixel) * scale + translate)));
                int b = Math.min(255, Math.max(0, (int)(Color.blue(pixel) * scale + translate)));

                output.setPixel(x, y, Color.rgb(r, g, b));
            }
        }

        return output;
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
