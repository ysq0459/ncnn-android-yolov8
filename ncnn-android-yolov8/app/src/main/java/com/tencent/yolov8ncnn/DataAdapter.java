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
        TextView textValue = view.findViewById(R.id.textValue);
        CheckBox checkBox = view.findViewById(R.id.checkBox);

        // 将id提取为final变量
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
        String label = cursor.getString(cursor.getColumnIndexOrThrow(
                DatabaseHelper.COL_LABEL));
        double value = cursor.getDouble(cursor.getColumnIndexOrThrow(
                DatabaseHelper.COL_VALUE));

        textLabel.setText(label);
        textValue.setText(String.format("Value: %.2f", value));

        // 多选模式处理
        checkBox.setChecked(selectedIds.contains(id));
        checkBox.setVisibility(multiSelectMode ? View.VISIBLE : View.GONE);

        // 使用final变量id替代局部变量
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