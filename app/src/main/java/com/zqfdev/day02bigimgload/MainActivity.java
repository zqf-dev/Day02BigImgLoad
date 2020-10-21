package com.zqfdev.day02bigimgload;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.zqfdev.imglib.LargeImageView;
import com.zqfdev.imglib.factory.InputStreamBitmapDecoderFactory;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LargeImageView bigView = findViewById(R.id.big_img_view);
        try {
            InputStream is = getAssets().open("b.png");
            bigView.setImage(new InputStreamBitmapDecoderFactory(is));
//            Glide.with(this).load(BitmapFactory.decodeStream(is)).into(bigView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
