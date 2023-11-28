package com.ble.demo.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import com.ble.ble.scan.LeScanner;
import com.ble.demo.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 加载本地OAD文件
 */
public class FileActivity extends AppCompatActivity {
    static final String TAG = "FileActivity";

    public final static String EXTRA_FILE_PATH = "com.ble.demo.ui.FileActivity.EXTRA_FILE_PATH";

    private final File DIR = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/" + Environment.DIRECTORY_DOWNLOADS);
    private FileAdapter mFileAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView listView = new ListView(this);
        listView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setContentView(listView);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFileAdapter = new FileAdapter();
        listView.setAdapter(mFileAdapter);
        listView.setOnItemClickListener(mOnItemClickListener);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermission()) {
            mFileAdapter.updateFiles(getLocalFiles());
        } else {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(R.string.no_read_external_storage_permission)
                    .setPositiveButton(R.string.to_grant_permission, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LeScanner.startAppDetailsActivity(FileActivity.this);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
        }
    }


    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT < 23) return true;

        return PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_GRANTED;
    }

    private List<String> getLocalFiles() {
        List<String> fileList = new ArrayList<String>();

        if (DIR.exists()) {
            getSupportActionBar().setTitle(DIR.getAbsolutePath());

            File[] files = DIR.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    String lowercaseName = name.toLowerCase(Locale.ROOT);
                    return lowercaseName.endsWith(".bin")
                            || lowercaseName.endsWith(".hexe");
                }
            });

            if (files != null) {
                for (File file : files)
                    if (!file.isDirectory()) {
                        fileList.add(file.getName());
                    }
            }

            if (fileList.size() == 0)
                Toast.makeText(this, "No OAD images available", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, DIR.getAbsolutePath() + " does not exist", Toast.LENGTH_LONG).show();
        }
        return fileList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private final AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_FILE_PATH, DIR.getAbsolutePath() + File.separator + mFileAdapter.getItem(position));
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private class FileAdapter extends BaseAdapter {
        final List<String> mFiles = new ArrayList<>();

        void updateFiles(List<String> files) {
            mFiles.clear();
            mFiles.addAll(files);
            notifyDataSetChanged();
        }

        public int getCount() {
            return mFiles.size();
        }

        public String getItem(int pos) {
            return mFiles.get(pos);
        }

        public long getItemId(int pos) {
            return pos;
        }

        public View getView(int pos, View view, ViewGroup parent) {
            if (view == null) {
                TextView twName = new TextView(FileActivity.this);
                int height = getDimen(R.dimen.file_list_item_height);
                AbsListView.LayoutParams params = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, height);
                twName.setLayoutParams(params);
                twName.setGravity(Gravity.CENTER_VERTICAL);
                twName.setPadding(getDimen(R.dimen.activity_horizontal_margin), 0, 0, 0);
                view = twName;
            }

            ((TextView) view).setText(mFiles.get(pos));
            return view;
        }
    }

    private int getDimen(int id) {
        return getResources().getDimensionPixelSize(id);
    }
}