package com.rongkecloud.test.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

/**
 * 常用的正则表达式校验
 */
public class RegularCheckTools {
	public static final int PWD_MIN_LENGTH = 6;
	public static final int PWD_MAX_LENGTH = 20;
	
	/**
	 * 校验密码的组成格式，密码只能由6-20位的数字和字母组成
	 * @param str
	 * @return boolean 
	 * 		true：合法 
	 * 		false: 不合法
	 */
	public static boolean checkPwd(String str){
		if(TextUtils.isEmpty(str)){
			return false;
		}
		
		if(str.length()<PWD_MIN_LENGTH || str.length()>PWD_MAX_LENGTH){
			return false;
		}
		
		String exp = "^[0-9a-zA-Z]*$";
		Pattern p = Pattern.compile(exp);
		Matcher m = p.matcher(str);
		return m.matches();
	}
	
	/**
	 * 
	 * 判断用户名 的格式，用户名是由字母开头的6-20位数字或字母祖成
	 * 
	 * */
	public static boolean checkAccount(String str){
		if(TextUtils.isEmpty(str)){
			return false;
		}

		String exp = "^[a-zA-Z][0-9a-zA-Z]{5,19}$";
		Pattern p = Pattern.compile(exp);
		Matcher m = p.matcher(str);
		return m.matches();
	}
	
	/**
	 * 
	 * 判断用户名 的格式
	 * 
	 * */
	public static boolean checkAccount2(String str){
		if(TextUtils.isEmpty(str)){
			return false;
		}
		
		String exp = "^[a-zA-Z][0-9a-zA-Z]*$";
		Pattern p = Pattern.compile(exp);
		Matcher m = p.matcher(str);
		return m.matches();
	}	
	
	/**
	 *@function 判断是否是Email
	 *@param: string str 校验的字符串
	 *@boolean: true: is email  false: not email 
	**/
	public static boolean isEmail(String str){
		if(TextUtils.isEmpty(str)){
			return false;
		}
		String exp = "^(([0-9a-zA-Z]+)|([0-9a-zA-Z]+[_.0-9a-zA-Z-]*[0-9a-zA-Z]+))@([a-zA-Z0-9-]+[.])+([a-zA-Z]{2}|net|NET|com|COM|gov|GOV|mil|MIL|org|ORG|edu|EDU|int|INT)$";
		if(str.matches(exp)){
			return true;		
		}	
		return false;	
	}	
	
	/**
	 *@function 判断是否是mobile号码
	 *@param: string str 校验的字符串
	 *@boolean:
	 **/
	public static boolean isMobile(String str){
		if(TextUtils.isEmpty(str)){
			return false;
		}
		String exp = "^1[34578][0-9]{9}$";
		if(str.matches(exp)){
			return true;
		}
		return false;
	}
	
	/**
	 *@function 判断是否是电话号码
	 *@param: string str 校验的字符串
	 *@boolean:
	 **/
	public static boolean isTelephone(String str){
		if(TextUtils.isEmpty(str)){
			return false;
		}
		String exp = "^(0[1-9]{2,3}-)?[1-9]\\d{6,7}$";
		if(str.matches(exp)){
			return true;
		}
		return false;
	}
	
	/**
	 * 过滤特殊字符
	 * @param content
	 * @return
	 */
	public static String filterSpecialChars(String content){		
		return content.replace("-", "").replace(" ", "");
	}
	
	/**
	 * 过滤手机号码
	 * @param content
	 * @return
	 */
	public static String filterMobile(String content){
		content = filterSpecialChars(content);
		if(content.length() < 11){
			return null;
		}
		// 如果是以国码开头则先去掉相关的国码信息，如果是以ip(17911/17951/17909)开头则先去掉ip电话
		if(content.startsWith("0086")){
			content = content.substring(4);
		}else if(content.startsWith("+86")){
			content = content.substring(3);
		}else if(content.startsWith("17911") || content.startsWith("17951") || content.startsWith("17909")){
			content = content.substring(5);
		}
		
		String phoneReg = "^1[3458][0-9]{9}$";
		if(content.matches(phoneReg)){
			return content;
		}
		
		return null;
	}	
}
