
package com.svtek.reactnative;

import android.content.Intent;
import android.net.Uri;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

public class RNAndroidSharerModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNAndroidSharerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNAndroidSharer";
  }

  @ReactMethod
  public void shareViaSms(String filePath, final Promise promise) {
    try {
      Intent smsIntent = new Intent(Intent.ACTION_VIEW);
      smsIntent.setType("vnd.android-dir/mms-sms");
      smsIntent.putExtra("sms_body","https://www.leoapp.com");
      smsIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
      this.reactContext.startActivity(smsIntent);
      promise.resolve();
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }
}
