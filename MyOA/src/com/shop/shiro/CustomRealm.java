package com.shop.shiro;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.shop.pojo.ActiveUser;
import com.shop.pojo.Employee;
import com.shop.pojo.MenuTree;
import com.shop.pojo.SysPermission;
import com.shop.service.EmployeeService;
import com.shop.service.SysService;

public class CustomRealm extends AuthorizingRealm {

	@Autowired
	private EmployeeService employeeService;
	@Autowired
	private SysService sysService;
	
	/**
	 * 用于授权
	 */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
		//获取用户信息 -- 认证成功后，才能获取到内容
		//SimpleAuthenticationInfo若封装的是对象，则获取到对象；若封装的是string，则获取到string
		ActiveUser activeUser = (ActiveUser) principalCollection.getPrimaryPrincipal();
		
		//通过用户查询出权限
		List<SysPermission> permissions = new ArrayList<SysPermission>();
		try { 
			permissions = sysService.findPermissionListByUserId(activeUser.getUserid()); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//存储权限percode
		List<String> percodes = new ArrayList<String>();
		for(SysPermission permission : permissions) {
			percodes.add(permission.getPercode());
		}
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		info.addStringPermissions(percodes);
		return info;
	}

	/**
	 * 用于认证方法
	 */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		System.out.println("【】【】【】用户登录授权认证【】【】【】");
		String username = (String) token.getPrincipal();	//用户输入账号

		Employee user = employeeService.selectByName(username);
		if(user==null) {
			return null;			
		}
		
		List<MenuTree> menuTree = sysService.loadMenuTree();
		//将需要的信息保存在自定义ActiveUser中
		ActiveUser au = new ActiveUser();
		au.setId(user.getId());
		au.setUserid(user.getName());
		au.setUsercode(user.getName());
		au.setUsername(user.getName());
		au.setManagerId(user.getManagerId());
		au.setMenuTree(menuTree);
		
		if (user.getSalt()==null) {
			user.setSalt("");
		}
		SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(au, user.getPassword(), ByteSource.Util.bytes(user.getSalt()), "CustomRealm");
		return info;
	}

}
