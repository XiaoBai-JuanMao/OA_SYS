package com.shop.utils;

import org.apache.shiro.crypto.hash.Md5Hash;

public class MyMD5 {

	public String mdkPassword(String password,String salt,int number) {
		Md5Hash md5 = new Md5Hash(password,salt,number);
		return md5.toString();
	}
}
