package com.rongkecloud.chat.demo.ui.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rongkecloud.test.R;

public class RKCloudChatCustomDialog extends Dialog {
	private int dialogId;

	public RKCloudChatCustomDialog(Context paramContext) {
		super(paramContext);
	}

	public RKCloudChatCustomDialog(Context paramContext, int paramInt) {
		super(paramContext, paramInt);
	}

	public int getDialogId() {
		return this.dialogId;
	}

	public void setDialogId(int paramInt) {
		this.dialogId = paramInt;
	}
	
	public void setPositiveBntStatus(boolean status){
		Button btn = (Button)getWindow().findViewById(R.id.positiveButton);
		if(null != btn){
			btn.setEnabled(status);
		}
	}

	public static class Builder {		
		private Context context;
		
		private String title;		
		private CharSequence message;
		
		private Button negativeBnt;
		private DialogInterface.OnClickListener negativeButtonClickListener;
		private String negativeButtonText;
		
		private Button positiveBnt;
		private DialogInterface.OnClickListener positiveButtonClickListener;
		private String positiveButtonText;	
		
		private boolean cancelable;
		private DialogInterface.OnCancelListener CancelListener;
		private DialogInterface.OnDismissListener dismissListener;
		
		private View contentView;		
		private View subView;
		
		public Builder(Context paramContext) {
			context = paramContext;
			CancelListener = new DefaultCancleHandler();
		}

		public RKCloudChatCustomDialog create() {
			final RKCloudChatCustomDialog localCustomDialog = new RKCloudChatCustomDialog(context,R.style.rkcloud_chat_Dialog);
			View localView = LayoutInflater.from(this.context).inflate(R.layout.rkcloud_chat_myself_dialog, null);
			TextView titleTV = (TextView)localView.findViewById(R.id.dialog_title);
			TextView msgTV = (TextView) localView.findViewById(R.id.message);
			LinearLayout contentView = (LinearLayout) localView.findViewById(R.id.content);
			positiveBnt = (Button) localView.findViewById(R.id.positiveButton);
			negativeBnt = (Button) localView.findViewById(R.id.negativeButton);
			localCustomDialog.addContentView(localView,new ViewGroup.LayoutParams(-1, -2));
			
			titleTV.setText(this.title);

			if (this.positiveButtonText != null) {
				positiveBnt.setVisibility(View.VISIBLE);
				positiveBnt.setText(this.positiveButtonText);
				positiveBnt.setOnClickListener(new View.OnClickListener() {
					public void onClick(View paramView) {
						localCustomDialog.dismiss();
						if (RKCloudChatCustomDialog.Builder.this.positiveButtonClickListener != null) {
							RKCloudChatCustomDialog.Builder.this.positiveButtonClickListener.onClick(localCustomDialog, -1);
						}
					}
				});
			}else{
				positiveBnt.setVisibility(View.GONE);
			}

			if (this.negativeButtonText != null) {
				negativeBnt.setVisibility(View.VISIBLE);
				negativeBnt.setText(this.negativeButtonText);
				negativeBnt.setOnClickListener(new View.OnClickListener() {
					public void onClick(View paramView) {
						localCustomDialog.dismiss();
						if (RKCloudChatCustomDialog.Builder.this.negativeButtonClickListener != null) {
							RKCloudChatCustomDialog.Builder.this.negativeButtonClickListener.onClick(localCustomDialog, -2);
						}
					}
				});				
			} else {
				negativeBnt.setVisibility(View.GONE);
				if (this.CancelListener != null) {
					localCustomDialog.setOnCancelListener(this.CancelListener);
				}
			}
			
			if(null != this.dismissListener){
				localCustomDialog.setOnDismissListener(this.dismissListener);
			}

			if (this.message != null) {
				msgTV.setVisibility(View.VISIBLE);
				msgTV.setText(this.message);
			} else {
				msgTV.setVisibility(View.GONE);
			}
			
			if (this.contentView != null) {
				contentView.removeAllViews();
				contentView.addView(this.contentView,new ViewGroup.LayoutParams(-2, -2));
			}
			
			if(null != subView){
				LinearLayout ll = (LinearLayout)localView.findViewById(R.id.dialogview);
				ll.setVisibility(View.VISIBLE);
				ll.addView(subView);
			}
			
			if(!this.cancelable){
				localCustomDialog.setCancelable(false);
			}
			localCustomDialog.setContentView(localView);
			return localCustomDialog;

		}
		
		public Builder addContentView(View view){
			this.subView = view;
			return this;
		}
		
		public Builder setContentView(View paramView) {
			this.contentView = paramView;
			return this;
		}

		public Builder setMessage(int paramInt) {
			this.message = ((String) this.context.getText(paramInt));
			return this;
		}

		public Builder setMessage(String paramString) {
			this.message = paramString;
			return this;
		}

		public Builder setMessage(CharSequence message){
			this.message = message;
			return this;
		}
		
		public Builder setNegativeButton(int paramInt,DialogInterface.OnClickListener paramOnClickListener) {
			this.negativeButtonText = ((String) this.context.getText(paramInt));
			this.negativeButtonClickListener = paramOnClickListener;
			return this;
		}

		public Builder setNegativeButton(String paramString,
				DialogInterface.OnClickListener paramOnClickListener) {
			this.negativeButtonText = paramString;
			this.negativeButtonClickListener = paramOnClickListener;
			return this;
		}

		public Builder setOnCancelListener(
				DialogInterface.OnCancelListener paramOnCancelListener) {
			this.CancelListener = paramOnCancelListener;
			return this;
		}
		
		public Builder setOnDismissListener(DialogInterface.OnDismissListener dimissListener){
			this.dismissListener = dimissListener;
			return this;
		}

		public Builder setPositiveButton(int paramInt,
				DialogInterface.OnClickListener paramOnClickListener) {
			this.positiveButtonText = ((String) this.context.getText(paramInt));
			this.positiveButtonClickListener = paramOnClickListener;
			return this;
		}

		public Builder setPositiveButton(String paramString,
				DialogInterface.OnClickListener paramOnClickListener) {
			this.positiveButtonText = paramString;
			this.positiveButtonClickListener = paramOnClickListener;
			return this;
		}

		public Builder setTitle(int paramInt) {
			this.title = ((String) this.context.getText(paramInt));
			return this;
		}

		public Builder setTitle(String paramString) {
			this.title = paramString;
			return this;
		}

		public Builder setCancelable(boolean cancelable){
			this.cancelable = cancelable;
			return this;
		}
		
		private class DefaultCancleHandler implements DialogInterface.OnCancelListener {
			private DefaultCancleHandler() {
			}

			public void onCancel(DialogInterface paramDialogInterface) {
				paramDialogInterface.dismiss();
			}
		}
		
		public Button getPositiveButton(){
			return positiveBnt;
		}
		
		public void setPositiveButtonEnabled(boolean enabled){
			if(null != positiveBnt){
				positiveBnt.setEnabled(enabled);
			}
		}
	}
}