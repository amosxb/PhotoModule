package com.ssdy.education.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.ssdy.education.mobile.ui.PhotoActivity;
import com.ssdy.education.mobile.utils.LogUtil;

import java.io.File;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

/**
 * TestActivity
 */
public class MainActivity extends AppCompatActivity {

    private File mPhotoFile;
    private ImageView mIvImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIvImg = findViewById(R.id.iv_img);
        findViewById(R.id.btn_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPhotoFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + UUID.randomUUID() + ".jpg");
                Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
                intent.putExtra(PhotoActivity.FILEPATH, mPhotoFile.getAbsolutePath());
                MainActivity.this.startActivityForResult(intent, PhotoActivity.REQUESTCODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PhotoActivity.REQUESTCODE:
                    LogUtil.d("REQUESTCODE");
                    Picasso.with(MainActivity.this).load(mPhotoFile).into(mIvImg);
                    break;
            }
        }
    }
}
