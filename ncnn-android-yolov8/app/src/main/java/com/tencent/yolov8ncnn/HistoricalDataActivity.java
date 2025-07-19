package com.tencent.yolov8ncnn;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class HistoricalDataActivity extends Activity {
    private DatabaseHelper dbHelper;
    private DataAdapter adapter;
    private Button btnDeleteSelected;

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
                dbHelper.insertData("Item " + System.currentTimeMillis(),
                        Math.random() * 100);
                refreshData();
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
