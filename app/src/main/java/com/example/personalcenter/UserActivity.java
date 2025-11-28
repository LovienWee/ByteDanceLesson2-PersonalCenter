package com.example.personalcenter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class UserActivity extends AppCompatActivity {

    private String account;

    private TextView tvUserName;
    private TextView tvUserSign;
    private ImageView homeButton;

    private LinearLayout personalInfoLayout;
    private LinearLayout myCollectionLayout;
    private LinearLayout visitedHistoryLayout;
    private LinearLayout settingLayout;
    private LinearLayout aboutUsLayout;
    private LinearLayout opinionFeedbackLayout;

    // 负责“用户名 & 签名”的 prefs（和账号密码分开）
    private SharedPreferences profilePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // 1. 拿到从 LoginActivity 传来的账号（登录成功时传过来的）
        account = getIntent().getStringExtra("account");
        if (account == null) {
            account = "guest";   // 防御性兜底
        }

        // 2. 绑定所有控件
        initViews();

        // 3. 初始化“用户资料”的 EncryptedSharedPreferences
        initProfilePrefs();

        // 4. 进入页面时，先从本地读取该账号的用户名/签名，刷新到界面
        loadUserInfoToView();

        // 5. 把所有点击监听框架搭好
        initClickListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.userName);
        tvUserSign = findViewById(R.id.userSign);
        homeButton = findViewById(R.id.homeButton);

        personalInfoLayout = findViewById(R.id.personalInfoLayout);
        myCollectionLayout = findViewById(R.id.myCollectionLayout);
        visitedHistoryLayout = findViewById(R.id.visitedHistoryLayout);
        settingLayout = findViewById(R.id.settingLayout);
        aboutUsLayout = findViewById(R.id.aboutUsLayout);
        opinionFeedbackLayout = findViewById(R.id.opinionFeedbackLayout);
    }

    private void initProfilePrefs() {
        try {
            // 和登录界面可以共用同一个 MasterKey，只是文件名不同
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            profilePrefs = EncryptedSharedPreferences.create(
                    this,
                    "user_profile_prefs",   // 存“用户名/签名”的文件
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 进入页面时，把本地保存的用户名/签名读出来显示
    private void loadUserInfoToView() {
        String defaultName = "路人甲";
        String defaultSign = "这个人很懒，什么也没留下~";

        String nameKey = "user_" + account + "_name";
        String signKey = "user_" + account + "_sign";

        String savedName = profilePrefs.getString(nameKey, defaultName);
        String savedSign = profilePrefs.getString(signKey, defaultSign);

        tvUserName.setText(savedName);
        tvUserSign.setText(savedSign);
    }

    private void initClickListeners() {
        // 1. 点击用户名 → 之后弹出“修改用户名”的弹窗
        tvUserName.setOnClickListener(v -> {
            showEditUserNameDialog();
        });

        // 2. 点击签名 → 之后弹出“修改签名”的弹窗
        tvUserSign.setOnClickListener(v -> {
            showEditUserSignDialog();
        });

        // 3. 功能区六个条目 → 只需要 Toast 提示
        personalInfoLayout.setOnClickListener(v ->
                Toast.makeText(this, "个人信息选项触发", Toast.LENGTH_SHORT).show()
        );

        myCollectionLayout.setOnClickListener(v ->
                Toast.makeText(this, "我的收藏选项触发", Toast.LENGTH_SHORT).show()
        );

        visitedHistoryLayout.setOnClickListener(v ->
                Toast.makeText(this, "浏览历史选项触发", Toast.LENGTH_SHORT).show()
        );

        settingLayout.setOnClickListener(v ->
                Toast.makeText(this, "设置选项触发", Toast.LENGTH_SHORT).show()
        );

        aboutUsLayout.setOnClickListener(v ->
                Toast.makeText(this, "关于我们选项触发", Toast.LENGTH_SHORT).show()
        );

        opinionFeedbackLayout.setOnClickListener(v ->
                Toast.makeText(this, "意见反馈选项触发", Toast.LENGTH_SHORT).show()
        );

        // 4. 底部 Home 按钮 → 之后实现“返回登录界面”
        homeButton.setOnClickListener(v -> {
            goBackToLogin();
        });
    }

    private void showEditUserNameDialog() {
        // 1. 把布局文件 inflate 成一个 View
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user_name, null);
        TextInputEditText etUserName = dialogView.findViewById(R.id.etUserName);

        // 2. 默认填入当前用户名，方便用户修改
        etUserName.setText(tvUserName.getText().toString());

        // 3. 构建对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改用户名")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 4. 手动处理“确定”按钮点击，让它在校验失败时不要自动关闭
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = etUserName.getText() != null
                    ? etUserName.getText().toString().trim()
                    : "";

            if (newName.isEmpty()) {
                Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                return; // 不关闭对话框
            }

            // 长度限制
            if (newName.length() > 12) {
                Toast.makeText(this, "用户名不能超过12个字符", Toast.LENGTH_SHORT).show();
                return;
            }

            // 5. 保存到 EncryptedSharedPreferences（profilePrefs）
            String nameKey = "user_" + account + "_name";
            profilePrefs.edit().putString(nameKey, newName).apply();

            // 6. 刷新页面上的显示
            tvUserName.setText(newName);

            // 7. 关闭对话框
            dialog.dismiss();
        });
    }

    private void showEditUserSignDialog() {
        // 1. 填充布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user_sign, null);
        TextInputEditText etUserSign = dialogView.findViewById(R.id.etUserSign);

        // 2. 预填当前签名，方便用户直接编辑
        etUserSign.setText(tvUserSign.getText().toString());

        // 3. 构建对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改个性签名")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 4. 接管“确定”按钮点击逻辑
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newSign = etUserSign.getText() != null
                    ? etUserSign.getText().toString().trim()
                    : "";

            if (newSign.isEmpty()) {
                Toast.makeText(this, "签名不能为空", Toast.LENGTH_SHORT).show();
                return; // 不关闭对话框
            }

            // 虽然 XML 已经限制了 maxLength=20，这里再保险检查一下
            if (newSign.length() > 20) {
                Toast.makeText(this, "签名不能超过20个字符", Toast.LENGTH_SHORT).show();
                return;
            }

            // 5. 保存到 EncryptedSharedPreferences（profilePrefs）
            String signKey = "user_" + account + "_sign";
            profilePrefs.edit().putString(signKey, newSign).apply();

            // 6. 刷新当前界面上的显示
            tvUserSign.setText(newSign);

            // 7. 关闭对话框
            dialog.dismiss();
        });
    }

    private void goBackToLogin() {
        Intent intent = new Intent(UserActivity.this,LoginActivity.class);
        startActivity(intent);

        finish();
    }
}