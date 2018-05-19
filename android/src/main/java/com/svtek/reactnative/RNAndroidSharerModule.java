
package com.svtek.reactnative;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
      return "";
    }
  }

  private void shareImageViaFacebook(String filePath, final Promise promise) {
    try {
      ParcelFileDescriptor fd = this.reactContext.getContentResolver()
              .openFileDescriptor(this.uriFromFilePath(filePath), "r");
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
              .setLocalUrl(this.uriFromFilePath(filePath)).build();
      ShareVideoContent content = new ShareVideoContent.Builder().setVideo(video).build();
      ShareDialog.show(this.reactContext.getCurrentActivity(), content);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  public @Nullable Uri uriFromFilePath(@NonNull String filePath) {
    filePath = filePath.replace("file://", "");
    File file = new File(filePath);
    Uri result = null;
    if (Build.VERSION.SDK_INT <= 21) {
      result = Uri.fromFile(file);
    }
    else {
      final String packageName = this.reactContext.getApplicationContext().getPackageName();
      final String authority = new StringBuilder(packageName).append(".provider").toString();
      try {
        result = FileProvider.getUriForFile(this.reactContext, authority, file);
      }
      catch(IllegalArgumentException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  @ReactMethod
  public void shareViaSms(String filePath, String shareUrl, final Promise promise) {
    try {
      String defaultSmsApplication = Settings.Secure.getString(
              this.reactContext.getContentResolver(),
              "sms_default_application");
      PackageManager pm = this.reactContext.getPackageManager();
      Intent shareIntent;
      if (shareUrl.isEmpty()) {
        shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra("sms_body", "https://www.leoapp.com");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
        shareIntent.setType(this.getMimeType(filePath));
      } else {
        shareIntent = new Intent(Intent.ACTION_VIEW);
        shareIntent.setType("vnd.android-dir/mms-sms");
        shareIntent.putExtra("sms_body", shareUrl);
      }
      shareIntent.setPackage(defaultSmsApplication);
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaWhatsApp(String filePath, String shareUrl, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      if (shareUrl.isEmpty()) {
        shareIntent.putExtra(Intent.EXTRA_TEXT, "https://www.leoapp.com");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
        shareIntent.setType(this.getMimeType(filePath));
      } else {
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
      }
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
      shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
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
  public void shareViaFacebook(String filePath, String shareUrl, final Promise promise) {
    if (!shareUrl.isEmpty()) {
      ShareLinkContent content = new ShareLinkContent.Builder()
              .setContentUrl(Uri.parse(shareUrl))
              .build();
      ShareDialog.show(this.reactContext.getCurrentActivity(), content);
      return;
    }

    String mimeType = this.getMimeType(filePath);
    if (mimeType.startsWith("image")) {
      this.shareImageViaFacebook(filePath, promise);
    } else {
      this.shareVideoViaFacebook(filePath, promise);
    }
  }

  @ReactMethod
  public void shareViaEmail(String filePath, String shareUrl, final Promise promise) {
    try {
      List<Intent> intentShareList = new ArrayList<Intent>();
      Intent shareIntent = new Intent();
      shareIntent.setAction(Intent.ACTION_SEND);
      shareIntent.setType("message/rfc822");
      List<ResolveInfo> resolveInfoList =
              this.reactContext.getPackageManager().queryIntentActivities(shareIntent, 0);

      for (ResolveInfo resInfo : resolveInfoList) {
        String packageName = resInfo.activityInfo.packageName;
        String name = resInfo.activityInfo.name;
        if (packageName.contains("com.android.email") ||
                packageName.contains("com.google.android.gm") ||
                packageName.contains("mail")) {
          Intent intent = new Intent();
          intent.setComponent(new ComponentName(packageName, name));
          intent.setAction(Intent.ACTION_SEND);
          intent.setType("message/rfc822");
          if (shareUrl.isEmpty()) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
            intent.putExtra(Intent.EXTRA_SUBJECT, "https://www.leoapp.com");
          } else {
            intent.putExtra(Intent.EXTRA_SUBJECT, "Reality by Leo AR Camera");
            intent.putExtra(Intent.EXTRA_TEXT, shareUrl);
          }
          intentShareList.add(intent);
        }
      }

      if (intentShareList.isEmpty()) {
        promise.reject(this.getName(), "Failed to find an email app.");
      } else {
        Intent chooserIntent = Intent.createChooser(intentShareList.remove(0), "Share via Email");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentShareList.toArray(new Parcelable[]{}));
        this.reactContext.getCurrentActivity().startActivity(chooserIntent);
        promise.resolve(null);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaTwitter(String filePath, String shareUrl, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      if (shareUrl.isEmpty()) {
        shareIntent.setType(this.getMimeType(filePath));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
      } else {
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
      }
      shareIntent.setPackage("com.twitter.android");
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaTumblr(String filePath, String shareUrl, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      if (shareUrl.isEmpty()) {
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
        shareIntent.setType(this.getMimeType(filePath));
      } else {
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
      }
      shareIntent.setPackage("com.tumblr");
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaMoreOptions(String filePath, String shareUrl, final Promise promise) {
    try {
      Intent shareIntent = new Intent();
      if (shareUrl.isEmpty()) {
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
        shareIntent.setType(this.getMimeType(filePath));
      } else {
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
      }
      this.reactContext.getCurrentActivity().startActivity(shareIntent);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  @ReactMethod
  public void shareViaFBMessenger(String filePath, String shareUrl, final Promise promise) {
    String EXTRA_PROTOCOL_VERSION = "com.facebook.orca.extra.PROTOCOL_VERSION";
    String EXTRA_APP_ID = "com.facebook.orca.extra.APPLICATION_ID";
    int PROTOCOL_VERSION = 20150314;
    String YOUR_FB_APP_ID = "756927241168396";
    int SHARE_TO_MESSENGER_REQUEST_CODE = 1;

    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setPackage("com.facebook.orca");
      if (shareUrl.isEmpty()) {
        shareIntent.setType(this.getMimeType(filePath));
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } else {
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
      }
      shareIntent.putExtra(EXTRA_PROTOCOL_VERSION, PROTOCOL_VERSION);
      shareIntent.putExtra(EXTRA_APP_ID, YOUR_FB_APP_ID);
      this.reactContext.getCurrentActivity().startActivityForResult(shareIntent, SHARE_TO_MESSENGER_REQUEST_CODE);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }
}
