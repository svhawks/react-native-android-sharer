
package com.svtek.reactnative;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareDialog;

import java.io.File;
import java.io.FileDescriptor;

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

  private String getMimeType(String filePath) {
    try {
      String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
      if (extension.equals("mp4")) {
        return "video/mp4";
      }
      return String.format("image/%s", extension);
    } catch (Exception ex) {
      Log.d(this.getName(), ex.toString());
      return null;
    }
  }

  private void shareImageViaFacebook(String filePath, final Promise promise) {
    try {
      ParcelFileDescriptor fd = this.reactContext.getContentResolver()
              .openFileDescriptor(Uri.fromFile(new File(filePath)), "r");
      Bitmap image = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor());
      SharePhoto photo = new SharePhoto.Builder().setBitmap(image).build();
      SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo).build();
      ShareDialog.show(this.reactContext.getCurrentActivity(), content);
      fd.close();
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  private void shareVideoViaFacebook(String filePath, final Promise promise) {
    try {
      ShareVideo video = new ShareVideo.Builder()
              .setLocalUrl(Uri.fromFile(new File(filePath))).build();
      ShareVideoContent content = new ShareVideoContent.Builder().setVideo(video).build();
      ShareDialog.show(this.reactContext.getCurrentActivity(), content);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaSms(String filePath, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.putExtra("sms_body", "https://www.leoapp.com");
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
      shareIntent.setType(this.getMimeType(filePath));
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaWhatsApp(String filePath, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.putExtra(Intent.EXTRA_TEXT, "https://www.leoapp.com");
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
      shareIntent.setType(this.getMimeType(filePath));
      shareIntent.setPackage("com.whatsapp");
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaInstagram(String filePath, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
      shareIntent.setType(this.getMimeType(filePath));
      shareIntent.setPackage("com.instagram.android");
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaFacebook(String filePath, final Promise promise) {
    String mimeType = this.getMimeType(filePath);
    if (mimeType.startsWith("image")) {
      this.shareImageViaFacebook(filePath, promise);
    } else {
      this.shareVideoViaFacebook(filePath, promise);
    }
  }

  @ReactMethod
  public void shareViaEmail(String filePath, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType("message/rfc822");;
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
      shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Reality by Leo App");
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaTwitter(String filePath, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType("/*");
      shareIntent.setClassName("com.twitter.android",
              "com.twitter.android.composer.ComposerShareActivity");
      shareIntent.putExtra(Intent.EXTRA_TEXT, "https://www.leoapp.com");
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaTumblr(String filePath, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
      shareIntent.setType(this.getMimeType(filePath));
      shareIntent.setPackage("com.tumblr");
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaMoreOptions(String filePath, final Promise promise) {
    try {
      Intent shareIntent = new Intent();
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
      shareIntent.setType(this.getMimeType(filePath));
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }
}
