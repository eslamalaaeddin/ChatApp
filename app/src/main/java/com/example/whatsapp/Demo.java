package com.example.whatsapp;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.whatsapp.databinding.ActivityDemoBinding;

public class Demo extends AppCompatActivity {

    ActivityDemoBinding demoBinding;
    private AnimatorSet animatorSet;
    Boolean clicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        demoBinding = DataBindingUtil.setContentView(this,R.layout.activity_demo);
        animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.loading_animator);

        demoBinding.sendVoiceMessageButton.setOnClickListener(v -> {

            if (!clicked){
                startAnimation();
                clicked = true;
            }
            else{
                stopAnimation();
                clicked = false;
            }

        });

    }

    private void startAnimation() {
            animatorSet.setTarget(demoBinding.sendVoiceMessageButton);
            animatorSet.start();

    }

    private void stopAnimation() {
            animatorSet.cancel();


    }
}