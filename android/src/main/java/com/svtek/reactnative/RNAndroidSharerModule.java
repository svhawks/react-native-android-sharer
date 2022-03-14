package com.svtek.reactnative;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.Telephony;
import androidx.annotation.NonNull;
import javax.annotation.Nullable;
import androidx.core.content.FileProvider;

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
  private static final String name = "RNAndroidSharer";
  private static final String LEO_WEB_URL = "https://www.leoapp.com";
  private static final String REALITY_SHARE_TITLE = "Reality by Leo AR Camera";
  private static final String INSTAGRAM_PACKAGE_NAME = "com.instagram.android";

  public RNAndroidSharerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @NonNull
  @Override
  public String getName() {
    return RNAndroidSharerModule.name;
  }

  private static String getMimeType(@NonNull String filePath) {
    String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
    if (extension.equals("mp4")) {
      return "video/mp4";
    } else {
      return String.format("image/%s", extension);
    }
  }

  private boolean isAppInstalled(String uri) {
    PackageManager pm = this.reactContext.getPackageManager();
    try {
      pm.getApplicationInfo(uri, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      // Error
    }
    return false;
  }

  private void shareContentViaFacebook(String filePath, String shareUrl, final Promise promise) {
    if (!shareUrl.isEmpty()) {
      ShareDialog shareDialog = new ShareDialog(this.reactContext.getCurrentActivity());
      ShareLinkContent content = new ShareLinkContent.Builder().setContentUrl(Uri.parse(shareUrl)).build();
      shareDialog.show(content, ShareDialog.Mode.WEB);
    } else {
      String mimeType = getMimeType(filePath);
      if (mimeType.startsWith("image")) {
        this.shareImageViaFacebook(filePath, promise);
      } else {
        this.shareVideoViaFacebook(filePath, promise);
      }
    }
  }

  private void shareImageViaFacebook(String filePath, final Promise promise) {
    try {
      ParcelFileDescriptor fd = this.reactContext.getContentResolver()
          .openFileDescriptor(this.uriFromFilePath(filePath), "r");
      Bitmap image = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor());
      SharePhoto photo = new SharePhoto.Builder().setBitmap(image).build();
      SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo).build();
      ShareDialog shareDialog = new ShareDialog(this.reactContext.getCurrentActivity());
      shareDialog.show(content, ShareDialog.Mode.AUTOMATIC);
      fd.close();
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  private void shareVideoViaFacebook(String filePath, final Promise promise) {
    try {
      ShareVideo video = new ShareVideo.Builder().setLocalUrl(this.uriFromFilePath(filePath)).build();
      ShareVideoContent content = new ShareVideoContent.Builder().setVideo(video).build();
      ShareDialog shareDialog = new ShareDialog(this.reactContext.getCurrentActivity());
      shareDialog.show(content, ShareDialog.Mode.AUTOMATIC);
      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      promise.reject(this.getName(), ex);
    }
  }

  public @Nullable Uri uriFromFilePath(@NonNull String filePath) {
    filePath = filePath.replace("file://", "");
    File file = new File(filePath);
    final String packageName = this.reactContext.getApplicationContext().getPackageName();
    final String authority = packageName + ".provider";
    try {
      return FileProvider.getUriForFile(this.reactContext, authority, file);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @ReactMethod
  public void shareViaSms(String filePath, String shareUrl, final Promise promise) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);

      if (shareUrl.isEmpty()) {
        shareIntent.putExtra(Intent.EXTRA_TEXT, LEO_WEB_URL);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
        shareIntent.setType(getMimeType(filePath));
      } else {
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
        shareIntent.setType("text/plain");
      }

      Activity activity = reactContext.getCurrentActivity();
      String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(activity);

      if (defaultSmsPackageName != null) {
        shareIntent.setPackage(defaultSmsPackageName);
      }

      activity.startActivity(shareIntent);
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
        shareIntent.putExtra(Intent.EXTRA_TEXT, LEO_WEB_URL);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
        shareIntent.setType(getMimeType(filePath));
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
      Uri uri = uriFromFilePath(filePath);
      String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
      Intent feedIntent = new Intent(Intent.ACTION_SEND);
      feedIntent.setType(getMimeType(filePath));
      feedIntent.putExtra(Intent.EXTRA_STREAM, uri);
      feedIntent.setPackage(INSTAGRAM_PACKAGE_NAME);

      Intent storiesIntent = new Intent("com.instagram.share.ADD_TO_STORY");
      storiesIntent.setDataAndType(uri, extension);
      storiesIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      storiesIntent.setPackage(INSTAGRAM_PACKAGE_NAME);

      Activity activity = reactContext.getCurrentActivity();
      activity.grantUriPermission(INSTAGRAM_PACKAGE_NAME, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

      Intent chooserIntent = Intent.createChooser(feedIntent, REALITY_SHARE_TITLE);
      chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { storiesIntent });
      activity.startActivity(chooserIntent);

      promise.resolve(null);
    } catch (Exception e) {
      e.printStackTrace();
      promise.reject(getName(), e);
    }
  }

  @ReactMethod
  public void shareViaFacebook(String filePath, String shareUrl, final Promise promise) {

    String applicationUri = "com.facebook.katana";

    if (isAppInstalled(applicationUri)) {
      try {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (!shareUrl.isEmpty()) {
          shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
          shareIntent.setType("text/plain");
        } else {
          shareIntent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
          shareIntent.setType(getMimeType(filePath));
        }

        shareIntent.setPackage(applicationUri);
        this.reactContext.getCurrentActivity().startActivity(shareIntent);

        promise.resolve(null);
      } catch (Exception ex) {
        ex.printStackTrace();
        shareContentViaFacebook(filePath, shareUrl, promise);
      }
    } else {
      shareContentViaFacebook(filePath, shareUrl, promise);
    }
  }

  @ReactMethod
  public void shareViaEmail(String filePath, String shareUrl, final Promise promise) {
    try {
      List<Intent> intentShareList = new ArrayList<Intent>();
      Intent shareIntent = new Intent();
      shareIntent.setAction(Intent.ACTION_SEND);
      shareIntent.setType("message/rfc822");
      List<ResolveInfo> resolveInfoList = this.reactContext.getPackageManager().queryIntentActivities(shareIntent, 0);

      for (ResolveInfo resInfo : resolveInfoList) {
        String packageName = resInfo.activityInfo.packageName;
        String name = resInfo.activityInfo.name;
        if (packageName.contains("com.android.email") || packageName.contains("com.google.android.gm")
            || packageName.contains("mail")) {
          Intent intent = new Intent();
          intent.setComponent(new ComponentName(packageName, name));
          intent.setAction(Intent.ACTION_SEND);
          intent.setType("message/rfc822");
          if (shareUrl.isEmpty()) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, this.uriFromFilePath(filePath));
            intent.putExtra(Intent.EXTRA_SUBJECT, LEO_WEB_URL);
          } else {
            intent.putExtra(Intent.EXTRA_SUBJECT, REALITY_SHARE_TITLE);
            intent.putExtra(Intent.EXTRA_TEXT, shareUrl);
          }
          intentShareList.add(intent);
        }
      }

      if (intentShareList.isEmpty()) {
        promise.reject(this.getName(), "Failed to find an email app.");
      } else {
        Intent chooserIntent = Intent.createChooser(intentShareList.remove(0), "Share via Email");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentShareList.toArray(new Parcelable[] {}));
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
        shareIntent.setType(getMimeType(filePath));
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
        shareIntent.setType(getMimeType(filePath));
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
        shareIntent.setType(getMimeType(filePath));
      } else {
        shareIntent.setAction(Intent.ACTION_SEND);
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
        shareIntent.setType(getMimeType(filePath));
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
