package com.example.uni_voice;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;


public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "BT.example.camera";
    String temp = "";
    MethodChannel.Result mResult;

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode == 101) {
//            if(resultCode == RESULT_OK){
//                temp = data.getStringExtra("result");
//                Log.d("@@@@@@", temp);
//                mResult.success(temp);
//            }
//        }
//    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            if(call.method.equals("getCamera")) {
                                mResult = result;
                                
                            } else {
                                result.notImplemented();
                            }
                        }
                );
    }

    
}
