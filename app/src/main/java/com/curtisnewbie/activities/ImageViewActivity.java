package com.curtisnewbie.activities;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.curtisnewbie.database.User;
import com.curtisnewbie.services.App;
import com.curtisnewbie.util.CryptoUtil;
import com.curtisnewbie.util.IOManager;
import com.curtisnewbie.util.ImageUtil;
import com.curtisnewbie.database.AppDatabase;
import com.curtisnewbie.database.DBManager;
import com.curtisnewbie.util.ThreadManager;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

import javax.inject.Inject;

/**
 * ------------------------------------
 * <p>
 * Author: Yongjie Zhuang
 * <p>
 * ------------------------------------
 * <p>
 * {@code Activity} that shows the image that is selected from the
 * {@code ImageListActivity}
 * </p>
 */
public class ImageViewActivity extends AppCompatActivity implements Promptable {
    private ImageView imageView;
    @Inject
    protected AppDatabase db;
    private Bitmap bitmap;
    private ThreadManager tm = ThreadManager.getThreadManager();
    /**
     * This key is used to encrypt and decrypt images, this is essentially a hash of
     * (password + img_salt).
     *
     * @see User
     */
    private String imgKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        App.getAppComponent().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        imageView = this.findViewById(R.id.imageView);
        // get the imageName passed by intent
        String imageName = getIntent().getStringExtra(ImageListAdapter.IMG_NAME);
        // get the path to the decrypted image, decrypt data and display
        tm.submit(() -> {
            try {
                // read encrypted data
                String imgPath = db.imgDao().getImagePath(imageName);
                byte[] encryptedData = IOManager.read(new File(imgPath));

                // decrypt the data
                imgKey = getIntent().getStringExtra(DBManager.IMG_KEY_TAG);
                byte[] decryptedData = CryptoUtil.decrypt(encryptedData, imgKey);

                // get the allowed maximum size of texture in OpenGL ES3.0
                int[] maxsize = new int[1];
                GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, maxsize, 0);
                int reqWidth, reqHeight;
                reqWidth = reqHeight = maxsize[0];

                // decode and downscale if needed to avoid OutOfMemory exception
                this.bitmap = ImageUtil.decodeBitmapWithScaling(decryptedData, reqWidth, reqHeight);
                runOnUiThread(() -> {
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imageView.setImageBitmap(bitmap);
                });

                // setup dialogue for zoomable PhotoView
                setupZoomableView();
            } catch (Exception e) {
                prompt("Decryption Failed");
                e.printStackTrace();
            }
        });
    }

    /**
     * Create dialogue that contains a zoomable PhotoView when the imageView is
     * clicked
     */
    private void setupZoomableView() {
        imageView.setClickable(true);
        imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // create dialogue for a zoomable PhotoView object.
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(ImageViewActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.dialogue_zoomable_layout, null);
                PhotoView zoomView = mView.findViewById(R.id.zoomView);
                zoomView.setImageBitmap(bitmap);
                zoomView.setMaximumScale(20.0f);
                mBuilder.setView(mView);
                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            }
        });
    }

    @Override
    public void prompt(String msg) {
        this.runOnUiThread(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}
