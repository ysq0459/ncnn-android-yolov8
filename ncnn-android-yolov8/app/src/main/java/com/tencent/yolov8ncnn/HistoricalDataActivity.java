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
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    // 存储当前图片信息
    private String currentImagePath = null;
    private String currentDetectionResult = null;

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
        // 在onCreate方法中添加ListView的单击事件监听器
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 多选模式下不触发预览
                if (adapter.isMultiSelectMode()) return;

                Cursor itemCursor = (Cursor) adapter.getItem(position);
                long itemId = itemCursor.getLong(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_ID));
                String label = itemCursor.getString(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_LABEL));
                String imagePath = itemCursor.getString(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_IMAGE_PATH));
                String detectionResult = itemCursor.getString(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_DETECTION_RESULT));

                // 显示预览对话框
                showPreviewDialog(label, imagePath, detectionResult);
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
                // 移除对 value 字段的引用
                // 添加对图片路径和检测结果的引用（如果需要）
                String imagePath = itemCursor.getString(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_IMAGE_PATH));
                String detectionResult = itemCursor.getString(itemCursor.getColumnIndexOrThrow(
                        DatabaseHelper.COL_DETECTION_RESULT));

                // 更新方法调用，移除 value 参数
                showEditDialog(itemId, label, imagePath, detectionResult);
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
        currentImagePath = null;
        currentDetectionResult = null;

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
                if (currentImagePath != null && currentDetectionResult != null) {
                    // 插入数据（使用图片路径和检测结果）
                    dbHelper.insertData("检测结果 " + System.currentTimeMillis(),
                            currentImagePath,
                            currentDetectionResult);
                    refreshData();
                    Toast.makeText(HistoricalDataActivity.this,
                            "Item added with image", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HistoricalDataActivity.this,
                            "Please select an image first", Toast.LENGTH_SHORT).show();
                }
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
                // 保存图片并获取路径
                currentImagePath = saveImageToStorage(resultBitmap);
                // 模拟检测结果（实际项目中替换为真实数据）
                currentDetectionResult = "1,2,3,4,5,6";
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
    // 添加预览对话框方法
    private void showPreviewDialog(String title, String imagePath, String detectionResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_preview, null);
        builder.setView(dialogView);

        ImageView previewImageView = dialogView.findViewById(R.id.previewImageView);
        TextView detectionResultView = dialogView.findViewById(R.id.detectionResultView);

        // 加载图片
        if (imagePath != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // 缩小图片尺寸防止OOM
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            if (bitmap != null) {
                previewImageView.setImageBitmap(bitmap);
            } else {
                previewImageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            previewImageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // 显示检测结果
        detectionResultView.setText(detectionResult != null ?
                detectionResult : "No detection data");

        builder.setPositiveButton("OK", null);
        builder.create().show();
    }
    // 显示编辑对话框（更新参数）
    private void showEditDialog(final long id, String label, String imagePath, String detectionResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Item");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit, null);
        builder.setView(dialogView);

        final EditText editLabel = dialogView.findViewById(R.id.editLabel);

        // 移除或隐藏 value 编辑框
        EditText editValue = dialogView.findViewById(R.id.editValue);
        if (editValue != null) {
            editValue.setVisibility(View.GONE);
        }

        // 添加其他信息显示（可选）
        TextView txtImagePath = dialogView.findViewById(R.id.txtImagePath);
        if (txtImagePath != null) {
            txtImagePath.setText("Image: " + (imagePath != null ?
                    new File(imagePath).getName() : "None"));
        }

        TextView txtDetectionResult = dialogView.findViewById(R.id.txtDetectionResult);
        if (txtDetectionResult != null) {
            txtDetectionResult.setText("Detection: " + detectionResult);
        }

        editLabel.setText(label);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newLabel = editLabel.getText().toString();

                if (dbHelper.updateData(id, newLabel)) {
                    Toast.makeText(HistoricalDataActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                    refreshData();
                }
            }
        });

        // 删除按钮保持不变
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

    // 保存图片到存储并返回路径
    private String saveImageToStorage(Bitmap bitmap) {
        // 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE
            );
            return null;
        }

        // 创建文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "IMG_" + sdf.format(new Date()) + ".jpg";
        String folderName = "YOLOv8_Images";

        // 创建目录
        File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                folderName);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // 创建文件
        File imageFile = new File(storageDir, fileName);

        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，可以重新尝试保存
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法保存图片", Toast.LENGTH_SHORT).show();
            }
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
