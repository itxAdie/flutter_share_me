package zhuoyuan.li.fluttershareme;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;

import java.io.File;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class FlutterShareMePlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {

    private Activity activity;
    private static CallbackManager callbackManager;
    private MethodChannel methodChannel;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel = new MethodChannel(binding.getBinaryMessenger(), "flutter_share_me");
        methodChannel.setMethodCallHandler(this);
        callbackManager = CallbackManager.Factory.create();
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (methodChannel != null) {
            methodChannel.setMethodCallHandler(null);
            methodChannel = null;
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        String url, msg;
        switch (call.method) {
            case "facebook_share":
                url = call.argument("url");
                msg = call.argument("msg");
                shareToFacebook(url, msg, result);
                break;
            case "messenger_share":
                url = call.argument("url");
                msg = call.argument("msg");
                shareToMessenger(url, msg, result);
                break;
            case "whatsapp_share":
                msg = call.argument("msg");
                url = call.argument("url");
                shareWhatsApp(url, msg, result, false);
                break;
            case "system_share":
                msg = call.argument("msg");
                shareSystem(result, msg);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void shareSystem(Result result, String msg) {
        try {
            Intent textIntent = new Intent(Intent.ACTION_SEND);
            textIntent.setType("text/plain");
            textIntent.putExtra(Intent.EXTRA_TEXT, msg);
            activity.startActivity(Intent.createChooser(textIntent, "Share to"));
            result.success("success");
        } catch (Exception e) {
            result.error("error", e.toString(), "");
        }
    }

    private void shareToFacebook(String url, String msg, Result result) {
        ShareDialog shareDialog = new ShareDialog(activity);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {}
            @Override
            public void onCancel() {}
            @Override
            public void onError(FacebookException error) {}
        });

        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse(url))
                .setQuote(msg)
                .build();
        if (ShareDialog.canShow(ShareLinkContent.class)) {
            shareDialog.show(content);
            result.success("success");
        }
    }

    private void shareToMessenger(String url, String msg, Result result) {
        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse(url))
                .setQuote(msg)
                .build();
        MessageDialog shareDialog = new MessageDialog(activity);
        if (shareDialog.canShow(content)) {
            shareDialog.show(content);
            result.success("success");
        } else {
            result.error("error", "Cannot share through messenger", "");
        }
    }

    private void shareWhatsApp(String imagePath, String msg, Result result, boolean isBusiness) {
        try {
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setPackage(isBusiness ? "com.whatsapp.w4b" : "com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, msg);
            if (!TextUtils.isEmpty(imagePath)) {
                whatsappIntent.setType("*/*");
                File file = new File(imagePath);
                Uri fileUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", file);
                whatsappIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                whatsappIntent.setType("text/plain");
            }
            activity.startActivity(whatsappIntent);
            result.success("success");
        } catch (Exception e) {
            result.error("error", e.toString(), "");
        }
    }
}
