package com.example.personalcenter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    // 输入框
    private TextInputEditText accountInput;
    private TextInputEditText passwordInput;

    // 按钮
    private Button loginButton;
    private Button wechatLoginButton;
    private Button appleLoginButton;

    // 文本按钮
    private TextView forgetPasswordButton;
    private TextView registerButton;

    // 存储账号密码
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定布局文件
        setContentView(R.layout.activity_login);

        // 初始化本地账号密码
        initEncryptedPrefs();

        // 绑定界面组件
        initViews();

        // 绑定点击事件
        initListeners();
    }

    private void initViews(){
        accountInput=findViewById(R.id.accountInput);
        passwordInput=findViewById(R.id.passwordInput);

        loginButton=findViewById(R.id.loginButton);
        wechatLoginButton=findViewById(R.id.wechcatLoginButton);
        appleLoginButton=findViewById(R.id.appleLoginButton);

        forgetPasswordButton=findViewById(R.id.forgetPasswordButton);
        registerButton=findViewById(R.id.registerButton);
    }

    private void initListeners(){
        // 登录按钮
        loginButton.setOnClickListener(v->performLogin());

        // 忘记密码
        forgetPasswordButton.setOnClickListener(v->showForgetPasswordDialog());

        // 立即注册
        registerButton.setOnClickListener(v->showRegisterDialog());

        // 微信登录
        wechatLoginButton.setOnClickListener(v->{
            Toast.makeText(LoginActivity.this,"点击了微信登录",Toast.LENGTH_SHORT).show();
        });

        // Apple登录
        appleLoginButton.setOnClickListener(c->{
            Toast.makeText(LoginActivity.this,"点击了Apple登录",Toast.LENGTH_SHORT).show();
        });
    }

    private void initEncryptedPrefs() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            prefs = EncryptedSharedPreferences.create(
                    "user_data",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showRegisterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_register, null);

        TextInputEditText etAccount = dialogView.findViewById(R.id.etAccount);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("注册账户")
                .setView(dialogView)
                .setPositiveButton("确定", null) // 先传 null，稍后手动重写逻辑
                .setNegativeButton("取消", null)
                .create();

        dialog.show();  // 先 show 才能拿到按钮

        // 重写“确定”点击逻辑，当存在已有账号时不要自动关闭
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String account = etAccount.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (account.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                return;  // 不关闭 dialog
            }

            String key = "user_" + account + "_password";

            if (prefs.contains(key)) {
                Toast.makeText(this, "该账号已存在，请重新输入", Toast.LENGTH_SHORT).show();
                return; // 不关闭 dialog
            }

            // 写入 SharedPreferences
            prefs.edit().putString(key, password).apply();
            Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show();

            dialog.dismiss(); // 手动关闭
        });
    }

    private void showForgetPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forget_password, null);
        TextInputEditText etForgetAccount = dialogView.findViewById(R.id.etForgetAccount);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("找回密码")
                .setView(dialogView)
                .setPositiveButton("下一步", null) // 稍后自定义点击逻辑
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String account = etForgetAccount.getText().toString().trim();
            if (account.isEmpty()) {
                Toast.makeText(this, "账号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            String key = "user_" + account + "_password";
            if (!prefs.contains(key)) {
                Toast.makeText(this, "该账号尚未注册", Toast.LENGTH_SHORT).show();
                // 不关闭对话框，方便重新输入
                return;
            }

            dialog.dismiss();
            // 账号存在，进入下一步：重置密码
            showResetPasswordDialog(account);
        });
    }

    private void showResetPasswordDialog(String account) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reset_password, null);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("重置密码")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString().trim();
            if (newPassword.isEmpty()) {
                Toast.makeText(this, "新密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            String key = "user_" + account + "_password";
            prefs.edit().putString(key, newPassword).apply();

            Toast.makeText(this, "密码重置成功！", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void performLogin() {
        String account = accountInput.getText() != null
                ? accountInput.getText().toString().trim()
                : "";
        String password = passwordInput.getText() != null
                ? passwordInput.getText().toString().trim()
                : "";

        if (account.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "账号或密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 和注册、忘记密码保持一致的 key 规则
        String key = "user_" + account + "_password";

        // 1. 账号是否存在
        if (!prefs.contains(key)) {
            Toast.makeText(this, "该账号尚未注册，请先注册", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 校验密码
        String realPassword = prefs.getString(key, "");
        if (!password.equals(realPassword)) {
            Toast.makeText(this, "密码错误，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 登录成功：可选记录“当前登录账号”
        prefs.edit().putString("current_account", account).apply();

        Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show();

        // 4. 跳转到用户中心页面 UserActivity，并把账号传过去
        Intent intent = new Intent(LoginActivity.this, UserActivity.class);
        intent.putExtra("account", account);
        startActivity(intent);

        // 可选：登录成功后关闭当前 LoginActivity
        finish();
    }

}