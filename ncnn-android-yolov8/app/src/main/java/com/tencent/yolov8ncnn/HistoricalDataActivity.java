package com.tencent.yolov8ncnn;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoricalDataActivity extends Activity {
    private static final int PICK_IMAGE_REQUEST = 100; // 图片选择请求码
    private static final int REQUEST_WRITE_STORAGE = 200;
    private DatabaseHelper dbHelper;
    private DataAdapter adapter;
    private Button btnDeleteSelected;
    private ImageView thumbnailView; // 用于显示缩略图
    private Bitmap selectedImageBitmap; // 存储选择的图片
    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historicaldata);

        dbHelper = new DatabaseHelper(this);
        ListView listView = findViewById(R.id.listView);
        Button btnAdd = findViewById(R.id.btnAdd);
        final Button btnMultiSelect = findViewById(R.id.btnMultiSelect);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        // 初始化适配器
        Cursor cursor = dbHelper.getAllData();
        adapter = new DataAdapter(this, cursor);
        listView.setAdapter(adapter);

        // 添加数据
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });

        // 多选模式切换
        btnMultiSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newMode = !adapter.isMultiSelectMode();
                adapter.setMultiSelectMode(newMode);
                btnDeleteSelected.setVisibility(newMode ? View.VISIBLE : View.GONE);
                btnMultiSelect.setText(newMode ? "Cancel" : "Select Items");
            }
        });

        // 删除选中项
        btnDeleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getSelectedIds().size() > 0) {
                    new AlertDialog.Builder(HistoricalDataActivity.this)
                            .setTitle("Confirm Delete")
                            .setMessage("Delete " + adapter.getSelectedIds().size() + " items?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int deleted = dbHelper.deleteMultipleData(
                                            adapter.getSelectedIdsString());
                                    Toast.makeText(HistoricalDataActivity.this,
                                            "Deleted " + deleted + " items", Toast.LENGTH_SHORT).show();

                                    // 退出多选模式并刷新
                                    adapter.setMultiSelectMode(false);
                                    btnDeleteSelected.setVisibility(View.GONE);
                                    btnMultiSelect.setText("Select Items");
                                    refreshData();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Toast.makeText(HistoricalDataActivity.this,
                            "No items selected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 长按编辑
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (adapter.isMultiSelectMode()) return false;

                Cursor itemCursor = (Cursor) adapter.getItem(position);
                long itemId = itemCursor.getLong(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_ID));
                String label = itemCursor.getString(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_LABEL));
                double value = itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_VALUE));

                showEditDialog(itemId, label, value);
                return true;
            }
        });

        reload();
    }

    private void reload() {
        boolean ret_init = yolov8ncnn.loadModel(getResources().getAssets(), 0, 0);
        if (!ret_init) {
            Log.e("ImageFragment", "yolov8ncnn loadModel failed");
        }
    }
    // 显示图片选择对话框
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Item with Image");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_picker, null);
        builder.setView(dialogView);

        thumbnailView = dialogView.findViewById(R.id.thumbnailView);
        Button btnPickImage = dialogView.findViewById(R.id.btnPickImage);

        // 初始化缩略图为空
        thumbnailView.setImageResource(android.R.color.transparent);
        selectedImageBitmap = null;

        // 图片选择按钮点击事件
        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 插入数据（此处保持原随机数据逻辑，可根据需求扩展）
                dbHelper.insertData("Item " + System.currentTimeMillis(),
                        Math.random() * 100);
                refreshData();
                Toast.makeText(HistoricalDataActivity.this,
                        "Item added", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    // 打开相册选择图片
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    // 处理图片选择结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                // 加载并显示缩略图
                Bitmap fullBitmap = BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(selectedImageUri));

                Bitmap resultBitmap = yolov8ncnn.processImage(fullBitmap);
                saveImage();
                // 创建缩略图（最大宽度200px）
                int maxWidth = 200;
                int newHeight = (int) (maxWidth * (double) fullBitmap.getHeight() / fullBitmap.getWidth());
                selectedImageBitmap = Bitmap.createScaledBitmap(fullBitmap, maxWidth, newHeight, true);

                thumbnailView.setImageBitmap(selectedImageBitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // 显示编辑对话框
    private void showEditDialog(final long id, String label, double value) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Item");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit, null);
        builder.setView(dialogView);

        final EditText editLabel = dialogView.findViewById(R.id.editLabel);
        final EditText editValue = dialogView.findViewById(R.id.editValue);

        editLabel.setText(label);
        editValue.setText(String.valueOf(value));

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newLabel = editLabel.getText().toString();
                double newValue;
                try {
                    newValue = Double.parseDouble(editValue.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(HistoricalDataActivity.this, "Invalid number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.updateData(id, newLabel, newValue)) {
                    Toast.makeText(HistoricalDataActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                    refreshData();
                }
            }
        });

        builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AlertDialog.Builder(HistoricalDataActivity.this)
                        .setTitle("Confirm Delete")
                        .setMessage("Delete this item?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (dbHelper.deleteData(id)) {
                                    Toast.makeText(HistoricalDataActivity.this,
                                            "Deleted", Toast.LENGTH_SHORT).show();
                                    refreshData();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        builder.setNeutralButton("Cancel", null);
        builder.create().show();
    }

    private void saveImage() {
        // 检查存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // 请求存储权限
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE
            );
        } else {
            saveCurrentImage();
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveCurrentImage();
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveCurrentImage() {
        // 创建时间戳格式的文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "IMG_" + sdf.format(new Date()) + ".jpg";

        // 创建子文件夹路径
        String folderName = "YOLOv8_Images";

        if (yolov8ncnn.saveCurrentImage(folderName, fileName)) {
            Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "保存失败，请检查存储权限", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshData() {
        Cursor newCursor = dbHelper.getAllData();
        adapter.changeCursor(newCursor);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
