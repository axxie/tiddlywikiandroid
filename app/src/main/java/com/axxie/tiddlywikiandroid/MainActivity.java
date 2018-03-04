package com.axxie.tiddlywikiandroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.content.FileProvider.getUriForFile;

// TODO: create app icon

public class MainActivity extends AppCompatActivity {

    String root_path;
    List<String> names = new ArrayList<>();
    private ListView list;


    private boolean isExtSdPermitted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // TODO: refresh on media changes (added files, mounts, etc)
    private void RefreshFilesList() {
        names.clear();

        if (isExtSdPermitted()) {
            File dir = new File(root_path);
            // TODO: check dir for null
            dir.mkdir();
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                // TODO: filter out directories
                names.add(files[i].getName());
            }
        } else {
            names.add("Need access to external storage");
        }

        ListView list = findViewById(R.id.files);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        list.setAdapter(adapter);
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                0);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            RefreshFilesList();
        }
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // TODO: move strings to language resource
                Snackbar.make(list, "Access to external storage is required to read Tidllywiki files", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                requestPermission();
                            }
                        }).show();

            }
            else {
                // TODO: show empty state with instruction
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isExtSdPermitted()) {
            requestPermission();
        } else {
            RefreshFilesList();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: create file
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        root_path = Environment.getExternalStorageDirectory().toString() + "/tiddlywiki";

        list = findViewById(R.id.files);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = (String)parent.getItemAtPosition(position);
                File file = new File(root_path + "/" + name);
                Uri contentUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                {
                    contentUri = getUriForFile(MainActivity.this, "com.axxie.fileprovider", file);
                }
                else
                {
                    contentUri = Uri.fromFile(file);
                }
                Intent intent = new Intent(MainActivity.this, ViewActivity.class);
                intent.setData(contentUri);

                startActivity(intent);
            }
        });
    }

}
