package com.axxie.tiddlywikiandroid;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;


public class ViewActivity extends AppCompatActivity {

    private Uri uri;
    private ProgressBar progress;
    private String title;

    final class TiddlyWikiInterface {

        private final ViewActivity parent;

        TiddlyWikiInterface(ViewActivity parent)
        {
            this.parent = parent;
        }

        @JavascriptInterface
        public boolean saveFile(String filename, String data) {
            try{
                // TODO: handle filename
                OutputStream out = parent.getContentResolver().openOutputStream(uri);
                out.write(data.getBytes());
            }
            catch (IOException  e) {
                // TODO: display error
                return false;
            }

            return true;
        }
    };

    public String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = null;

            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (IllegalArgumentException e) {
                // TODO: handle error
                return null;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        uri = getIntent().getData();
        WebView view = findViewById(R.id.view);
        progress = findViewById(R.id.progressBar);
        progress.setIndeterminate(true);
        progress.setMax(100);

        title = getFileNameFromUri(uri);
        if (title == null) {
            title = "Tiddlywiki";
        }
        setTitle("Loading...");

        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowContentAccess(true);

        final ViewActivity activity = this;
        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100)
                {
                    activity.progress.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                setTitle(title);
            }
        });
        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // TODO: proper error handling
                Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("content://com.axxie.fileprovider"))) {
                    // TODO: propose download for unhandled files (e.g. bash scripts) (images should be opened ok)
                    view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }
        });

        view.addJavascriptInterface(new TiddlyWikiInterface(this), "twi");

        view.loadUrl(uri.toString());
    }
}
