package com.shop.service;

import java.util.List;

import com.shop.pojo.MenuTree;
import com.shop.pojo.SysPermission;
import com.shop.pojo.SysRole;
import com.shop.pojo.SysUserRole;

public interface SysService {
	
	//根据用户账号查询用户信息
	//public Employee findSysUserByUserCode(String userCode)throws Exception;
	
	//根据用户id查询权限范围的菜单
	public List<SysPermission> findMenuListByUserId(String userid) throws Exception;
	
	//根据用户id查询权限范围的url
	public List<SysPermission> findPermissionListByUserId(String userid) throws Exception;
	
	public List<MenuTree> loadMenuTree();
	
	public List<SysRole> findAllRoles();
	
	public SysRole findRolesAndPermissionsByUserId(String userId);
	
	public void addRoleAndPermissions(SysRole role,int[] permissionIds);
	
	//查询所有menu类permission
	public List<SysPermission> findAllMenus();
	
	public void addSysPermission(SysPermission permission);
	
	//根据用户ID查询其所有的菜单和权限
	public List<SysPermission> findMenuAndPermissionByUserId(String userId);
	//查询所有的权限子菜单菜单
	public List<MenuTree> getAllMenuAndPermision();
	
	//根据角色ID查询权限
	public List<SysPermission> findPermissionsByRoleId(String roleId);
	
	public void updateRoleAndPermissions(String roleId,int[] permissionIds);

	public List<SysRole> findRolesAndPermissions();
	//添加用户——角色表
	public void addUserAndRole(SysUserRole sysUserRole);
	//更新用户——角色表
	public int updateUserRole(SysUserRole sysUserRole);
	//通过用户id查找用户-角色信息
	public SysUserRole findUserRoleByUserId(String userId);
	//通过id查询权限
	public SysPermission findPermissionById(Long parentid);
	//通过id删除角色
	public void deleteRoleById(String roleId);
	//通过用户id删除用户-角色信息
	public void deleteUserRoleByUserId(String userId);
}
