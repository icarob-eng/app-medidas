package com.medidaspla;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    // views:
    private Button btnAnalise;
    private ImageButton btnPic;
    private EditText txtPasta, txtArquivo;
    private PreviewView viewFinder;
    private Switch switchBordas;

    // use cases:
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instantiateViews();

        btnAnalise.setOnClickListener(v -> startActivity(new Intent(
                MainActivity.this, AnaliseActivity.class)
        ));

        // solicita permissões:
        permissionHandler(Manifest.permission.CAMERA);
        permissionHandler(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionHandler(Manifest.permission.READ_EXTERNAL_STORAGE);

        // tenta inicializar casos de uso
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider
                .getInstance(this);
        cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    startCameraX(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, getMainExecutor());

        btnPic.setOnClickListener(v -> capturePhoto());
    }

    private void instantiateViews(){
        // incializa views
        txtPasta = findViewById(R.id.textNomePasta);
        txtArquivo = findViewById(R.id.textNomeArquivo);
        viewFinder = findViewById(R.id.viewFinder);
        btnPic = findViewById(R.id.btnPic);
        btnAnalise = findViewById(R.id.btnAnalise);
        switchBordas = findViewById(R.id.switchBordas);
    }

    private void permissionHandler(String permission){
        // no estado atual o aplicativo simplesmente não funciona se não tiver permissões

        // checa se não tem a permissão
        if(ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
        ) {
            // solicita a permissão
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), b -> {}
            ).launch(permission);
        }
    }

    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        // Camera Selector use case
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        // Image Capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void capturePhoto(){
        String subfolder = txtPasta.getText().toString();
        String fileName = txtArquivo.getText().toString() + ".jpg";

//        File appDir = new File(
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
//                "PLA"
//        );

        File appDir = new File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),  // aqui mora o problema
                "PLA"
        );
        if(! appDir.exists())
            appDir.mkdir();

        File subfolderDir = new File(appDir.getAbsolutePath(), subfolder);
        if(! subfolderDir.exists())
            subfolderDir.mkdir();

        File photoFile = new File(subfolderDir.getAbsolutePath(), fileName);
        Log.d("PhotoPath", photoFile.getAbsolutePath());

        // salva a imagem:
        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                getMainExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this,
                                subfolder + "/" + fileName + " salvo com sucesso",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this,
                                "Erro ao salvar imagem",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}