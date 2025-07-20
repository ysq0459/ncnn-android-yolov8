package com.tencent.yolov8ncnn;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DataAdapter extends CursorAdapter {
    private Set<Long> selectedIds = new HashSet<>();
    private boolean multiSelectMode = false;

    public DataAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView textLabel = view.findViewById(R.id.textLabel);
        TextView textValue = view.findViewById(R.id.textValue); // 现在用于显示检测结果
        TextView textImage = view.findViewById(R.id.textImage); // 新增的图片信息视图
        CheckBox checkBox = view.findViewById(R.id.checkBox);

        // 获取数据
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
        String label = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LABEL));
        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_IMAGE_PATH));
        String detectionResult = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DETECTION_RESULT));

        // 设置视图内容
        textLabel.setText(label);

        // 显示检测结果
        textValue.setText("Detection: " + detectionResult);

        // 显示图片文件名（如果有）
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            textImage.setText("Image: " + imageFile.getName());
            textImage.setVisibility(View.VISIBLE);
        } else {
            textImage.setVisibility(View.GONE);
        }

        // 多选模式处理
        checkBox.setChecked(selectedIds.contains(id));
        checkBox.setVisibility(multiSelectMode ? View.VISIBLE : View.GONE);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectedIds.add(id);
                } else {
                    selectedIds.remove(id);
                }
            }
        });
    }

    // 设置多选模式
    public void setMultiSelectMode(boolean enabled) {
        multiSelectMode = enabled;
        if (!enabled) {
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }

    // 检查是否在多选模式
    public boolean isMultiSelectMode() {
        return multiSelectMode;
    }

    // 获取选中的ID集合
    public Set<Long> getSelectedIds() {
        return selectedIds;
    }

    // 获取选中的ID字符串(逗号分隔)
    public String getSelectedIdsString() {
        StringBuilder sb = new StringBuilder();
        for (Long id : selectedIds) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        return sb.toString();
    }
}