package com.rongkecloud.test.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import android.widget.TextView;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.sdkbase.RKCloudBaseErrorCode;
import com.rongkecloud.test.R;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.AccountManager;
import com.rongkecloud.test.manager.SDKManager;
import com.rongkecloud.test.manager.uihandlermsg.AccountUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.utility.FileLog;
import com.rongkecloud.test.utility.NetworkUtil;
import com.rongkecloud.test.utility.OtherUtilities;
import com.rongkecloud.test.utility.RegularCheckTools;

/**
 * 登录
 * 
 * @author Administrator
 *
 */
public class LoginActivity extends RKCloudChatBaseActivity
{

	private String TAG = getClass().getSimpleName();
	public static final String INTENT_KEY_ACCOUNT_ERROR = "INTENT_KEY_ACCOUNT_ERROR";// 账号异常
	public static final String INTENT_ACCOUNT_ERROR = "INTENT_ACCOUNT_ERROR";// 账号异常

	private EditText mNameEt;
	private EditText mPwdEt;
	private Button mLogin;
	private TextView mRegisterNormalBtn;
	private AccountManager mAccountManager;
	private Dialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		findViews();
		setListeners();
		mAccountManager = AccountManager.getInstance();
		mNameEt.setText(RKCloudDemo.config.getString(ConfigKey.LAST_LOGIN_NAME, ""));
		mNameEt.setSelection(mNameEt.getText().toString().trim().length());
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mAccountManager.bindUiHandler(mUiHandler);
		SDKManager.getInstance().bindUiHandler(mUiHandler);
	}

	private void findViews()
	{
		mNameEt = (EditText) this.findViewById(R.id.login_name_edittext);
		mPwdEt = (EditText) this.findViewById(R.id.login_pwd_edittext);
		mLogin = (Button) this.findViewById(R.id.login);
		mRegisterNormalBtn = (TextView) findViewById(R.id.reg_normal_btn);
	}

	private void setListeners()
	{
		mLogin.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				doLogin();
			}
		});

		mRegisterNormalBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
				startActivity(intent);
			}
		});
	}

	private void doLogin()
	{
		// 如果网络不可用，则给出提示
		if (!NetworkUtil.isNetworkAvaliable())
		{
			OtherUtilities.showToastText(this, getResources().getString(R.string.network_off));
			return;
		}
		String loginName = mNameEt.getText().toString().trim();
		String loginPwd = mPwdEt.getText().toString().trim();

		if (TextUtils.isEmpty(loginName))
		{
			OtherUtilities.showToastText(this, getResources().getString(R.string.login_name_not_empty));
			mNameEt.setFocusable(true);
			return;
		}

		if (!RegularCheckTools.checkAccount(loginName))
		{
			OtherUtilities.showToastText(this, getResources().getString(R.string.login_name_format_error));
			mNameEt.setFocusable(true);
			return;
		}

		if (TextUtils.isEmpty(loginPwd))
		{
			OtherUtilities.showToastText(this, getResources().getString(R.string.login_pwd_not_empty));
			mPwdEt.setFocusable(true);
			return;
		}

		if (!RegularCheckTools.checkPwd(loginPwd))
		{
			OtherUtilities.showToastText(this, getResources().getString(R.string.login_pwd_format_error));
			mPwdEt.setFocusable(true);
			return;
		}

		// 打开等待框
		showProgressDialog(getString(R.string.tip), getString(R.string.login_tip));
		mAccountManager.login(loginName, loginPwd);
	}

	@Override
	public void processResult(Message msg)
	{
		switch (msg.what)
		{
			case AccountUiMessage.RESPONSE_LOGIN:
				closeProgressDialog();
				if (HttpResponseCode.OK == msg.arg1)
				{
					showProgressDialog(getString(R.string.tip), getString(R.string.sdk_init_tip));
					SDKManager.getInstance().initSDK();

				}
				else if ((HttpResponseCode.BANNED_USER == msg.arg1) || (HttpResponseCode.ACCOUNT_PWD_ERROR == msg.arg1))
				{
					OtherUtilities.showToastText(this, getString(R.string.login_account_error));
				}
				else if (HttpResponseCode.ACCOUNT_NOT_EXISTS == msg.arg1)
				{
					OtherUtilities.showToastText(this, getString(R.string.login_account_not_exist));
				}
				else if (HttpResponseCode.NO_NETWORK == msg.arg1)
				{
					OtherUtilities.showToastText(this, getString(R.string.network_off));
				}
				else
				{
					OtherUtilities.showToastText(this, getString(R.string.login_result_failed));
				}
				break;

			case AccountUiMessage.SDK_INIT_FINISHED:
				closeProgressDialog();
				if (RKCloudBaseErrorCode.RK_NOT_NETWORK == msg.arg1 || RKCloudBaseErrorCode.RK_SUCCESS == msg.arg1)
				{
					startActivity(new Intent(this, MainActivity.class));
					finish();
				}
				else if (RKCloudBaseErrorCode.BASE_ACCOUNT_PW_ERROR == msg.arg1)
				{
					OtherUtilities.showToastText(this, getString(R.string.sdk_init_accounterror));

				}
				else if (RKCloudBaseErrorCode.BASE_ACCOUNT_BANNED == msg.arg1)
				{
					OtherUtilities.showToastText(this, getString(R.string.sdk_init_banneduser));

				}
				else if (RKCloudBaseErrorCode.BASE_APP_KEY_AUTH_FAIL == msg.arg1)
				{
					OtherUtilities.showToastText(this, getString(R.string.sdk_init_authfailed));

				}
				else
				{
					OtherUtilities.showToastText(this, getString(R.string.sdk_init_failed));
				}
				break;
		}
	}

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        accountErrorDialog(intent);
    }

    private void accountErrorDialog(Intent intent)
	{
		if (TextUtils.isEmpty(intent.getStringExtra(INTENT_KEY_ACCOUNT_ERROR)))
		{
			return;
		}

		FileLog.e(TAG, "accounterror = " + getIntent().getStringExtra(INTENT_KEY_ACCOUNT_ERROR));
		if (null != dialog && dialog.isShowing())
		{
			return;
		}

		dialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT).setMessage(getString(R.string.offline_content))
				.setPositiveButton(getString(R.string.bnt_confirm), new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				}).create();

		dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				dialog.dismiss();
			}
		});
		dialog.show();
	}
}
