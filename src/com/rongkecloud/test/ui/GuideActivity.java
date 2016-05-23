package com.rongkecloud.test.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.sdkbase.RKCloudBaseErrorCode;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Account;
import com.rongkecloud.test.manager.AccountManager;
import com.rongkecloud.test.manager.SDKManager;
import com.rongkecloud.test.manager.uihandlermsg.AccountUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.utility.OtherUtilities;

import java.util.ArrayList;
import java.util.List;

public class GuideActivity extends RKCloudChatBaseActivity implements OnPageChangeListener
{

	public static int VERSION = 1;
    private static final int MSG_ENTER_MAIN = 1;// 进入主页面
    private static final int MSG_ENTER_LOGIN = 2;// 进入登录页面
	private ViewPager vp;
	private ViewPagerAdapter vpAdapter;
	private List<View> views;
    LinearLayout ll;
    boolean gui_flag = false;
	// 底部小点图片
	private ImageView[] dots;

	// 记录当前选中位置
	private int currentIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_guide);

		// 初始化页面
		initViews();

		// 初始化底部小点
		initDots();
	}

	private void initViews()
	{
		LayoutInflater inflater = LayoutInflater.from(this);
		views = new ArrayList<>();
		// 初始化引导图片列表
		views.add(inflater.inflate(R.layout.viewpager_item_guide_one, null));
		views.add(inflater.inflate(R.layout.viewpager_item_guide_two, null));
		views.add(inflater.inflate(R.layout.viewpager_item_guide_three, null));
		views.add(inflater.inflate(R.layout.viewpager_item_guide_four, null));

		// 初始化Adapter
		vpAdapter = new ViewPagerAdapter(views, this);

		vp = (ViewPager) findViewById(R.id.viewpager);
		vp.setAdapter(vpAdapter);
		// 绑定回调
		vp.addOnPageChangeListener(this);
	}

	private void initDots()
	{
		ll = (LinearLayout) findViewById(R.id.ll);
        ll.setVisibility(LinearLayout.VISIBLE);
		dots = new ImageView[views.size()];

		// 循环取得小点图片
		for (int i = 0; i < views.size(); i++)
		{
			dots[i] = (ImageView) ll.getChildAt(i);
			dots[i].setEnabled(true);// 都设为灰色
		}

		currentIndex = 0;
		dots[currentIndex].setEnabled(false);// 设置为白色，即选中状态
	}

	private void setCurrentDot(int position)
	{
		if (position < 0 || position > views.size() - 1 || currentIndex == position)
		{
			return;
		}

		dots[position].setEnabled(false);
		dots[currentIndex].setEnabled(true);

		currentIndex = position;

        if(position == views.size()-1){
            ll.setVisibility(LinearLayout.GONE);
        }else{
            ll.setVisibility(LinearLayout.VISIBLE);
        }
	}

	// 当滑动状态改变时调用
	@Override
	public void onPageScrollStateChanged(int arg0)
	{

	}

	// 当当前页面被滑动时调用
	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2)
	{
	}

	// 当新的页面被选中时调用
	@Override
	public void onPageSelected(int arg0)
	{
		// 设置底部小点选中状态
		setCurrentDot(arg0);
	}

	private class ViewPagerAdapter extends PagerAdapter
	{

		// 界面列表
		private List<View> views;
		private Activity activity;

		public ViewPagerAdapter(List<View> views, Activity activity)
		{
			this.views = views;
			this.activity = activity;
		}

		// 销毁arg1位置的界面
		@Override
		public void destroyItem(View arg0, int arg1, Object arg2)
		{
			((ViewPager) arg0).removeView(views.get(arg1));
		}

		@Override
		public void finishUpdate(View arg0)
		{
		}

		// 获得当前界面数
		@Override
		public int getCount()
		{
			if (views != null)
			{
				return views.size();
			}
			return 0;
		}

		// 初始化arg1位置的界面
		@Override
		public Object instantiateItem(ViewGroup arg0, int arg1)
		{
			arg0.addView(views.get(arg1), 0);
			if (arg1 == views.size() - 1)
			{
				Button mStartWeiboImageButton = (Button) arg0.findViewById(R.id.iv_start_weibo);
				mStartWeiboImageButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						// 设置已经引导
                        onLoadCompleted();
					}
				});
			}
			return views.get(arg1);
		}

        private void onLoadCompleted() {
            RKCloudDemo.config.put(ConfigKey.SP_GUIDEPAGES_SHOW,true);
            Account account = AccountManager.getInstance().getCurrentAccount();
            if(null == account){
                mUiHandler.sendEmptyMessage(MSG_ENTER_LOGIN);
            }else{
                if(SDKManager.getInstance().getSDKInitStatus()){
                    mUiHandler.sendEmptyMessage(MSG_ENTER_MAIN);
                }
            }
        }


		// 判断是否由对象生成界面
		@Override
		public boolean isViewFromObject(View arg0, Object arg1)
		{
			return (arg0 == arg1);
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1)
		{
		}

		@Override
		public Parcelable saveState()
		{
			return null;
		}

		@Override
		public void startUpdate(View arg0)
		{
		}

	}


    @Override
    public void processResult(Message msg) {
        switch(msg.what){
            case MSG_ENTER_LOGIN:
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                break;

            case MSG_ENTER_MAIN:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;

            case AccountUiMessage.SDK_INIT_FINISHED:
                if(RKCloudBaseErrorCode.RK_NOT_NETWORK==msg.arg1 || RKCloudBaseErrorCode.RK_SUCCESS==msg.arg1){
                    gui_flag = RKCloudDemo.config.getBoolean(ConfigKey.SP_GUIDEPAGES_SHOW,false);
                    if(gui_flag){
                        mUiHandler.sendEmptyMessage(MSG_ENTER_MAIN);
                    }
                }else if(RKCloudBaseErrorCode.BASE_ACCOUNT_PW_ERROR == msg.arg1){
                    OtherUtilities.showToastText(this, getString(R.string.sdk_init_accounterror));
                    AccountManager.getInstance().logout();
                    startActivity(new Intent(this, LoginActivity.class));

                }else if(RKCloudBaseErrorCode.BASE_ACCOUNT_BANNED == msg.arg1){
                    OtherUtilities.showToastText(this, getString(R.string.sdk_init_banneduser));
                    AccountManager.getInstance().logout();
                    startActivity(new Intent(this, LoginActivity.class));

                }else if(RKCloudBaseErrorCode.BASE_APP_KEY_AUTH_FAIL == msg.arg1){
                    OtherUtilities.showToastText(this, getString(R.string.sdk_init_authfailed));
                    AccountManager.getInstance().logout();
                    startActivity(new Intent(this, LoginActivity.class));

                }else{
                    OtherUtilities.showToastText(this, getString(R.string.sdk_init_failed));
                }
                break;
        }
    }

}
