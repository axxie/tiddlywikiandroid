package com.axxie.tiddlywikiandroid;

import android.Manifest;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.content.FileProvider.getUriForFile;

// TODO: create app icon

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    String root_path;
    List<String> names = new ArrayList<>();
    private ListView list;


    private boolean isExtSdPermitted()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // TODO: refresh on media changes (added files, mounts, etc)
    private void RefreshFilesList()
    {
        names.clear();

        if (isExtSdPermitted())
        {
            File dir = new File(root_path);
            // TODO: check dir for null
            dir.mkdir();
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                // TODO: filter out directories
                names.add(files[i].getName());
            }
        }
        else
        {
            names.add("Need access to external storage");
        }

        ListView list = findViewById(R.id.files);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        list.setAdapter(adapter);
    }

    private void CreateWikiFile(String name) throws IllegalArgumentException, IOException
    {
        if (name.equals(".html"))
        {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        InputStream in = getResources().openRawResource(R.raw.empty);
        FileOutputStream out = new FileOutputStream(root_path + "/" + name);
        byte[] buff = new byte[1024];
        int read = 0;

        try
        {
            while ((read = in.read(buff)) > 0)
            {
                out.write(buff, 0, read);
            }
        }
        finally
        {
            in.close();
            out.close();
        }
        RefreshFilesList();
    }


    private void requestPermission()
    {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                0);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           String permissions[], int[] grantResults)
    {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            RefreshFilesList();
        }
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                // TODO: move strings to language resource
                Snackbar.make(list, "Access to external storage is required to read Tidllywiki files", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry", new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                requestPermission();
                            }
                        }).show();

            }
            else
            {
                // TODO: show empty state with instruction
            }
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (!isExtSdPermitted())
        {
            requestPermission();
        }
        else
        {
            RefreshFilesList();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        root_path = Environment.getExternalStorageDirectory().toString() + "/tiddlywiki";

        list = findViewById(R.id.files);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String name = (String) parent.getItemAtPosition(position);
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

    @Override
    public void onClick(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enter file name");

        // Setup custom view for dialog
        LayoutInflater inflater = getLayoutInflater();
        final View content = inflater.inflate(R.layout.fragment_new_file_dialog, null);
        final EditText filename = content.findViewById(R.id.filename);
        builder.setView(content);

        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(final DialogInterface dlg)
            {
                final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        try
                        {
                            CreateWikiFile(filename.getText().toString() + ".html");
                        }
                        catch (IOException | IllegalArgumentException e)
                        {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                            return;
                        }
                        dialog.dismiss();
                    }
                });
            }
        });


        // Show keyboard immediately
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // Accept "Done" and <Enter> keys as "Save" button
        filename.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE))
                {
                    if (filename.getText().length() != 0)
                    {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    }
                }
                return false;
            }
        });

        dialog.show();

    }
}
