package com.shop.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shop.mapper.SysPermissionMapper;
import com.shop.mapper.SysPermissionMapperCustom;
import com.shop.mapper.SysRoleMapper;
import com.shop.mapper.SysRolePermissionMapper;
import com.shop.mapper.SysUserRoleMapper;
import com.shop.pojo.MenuTree;
import com.shop.pojo.SysPermission;
import com.shop.pojo.SysPermissionExample;
import com.shop.pojo.SysRole;
import com.shop.pojo.SysRolePermission;
import com.shop.pojo.SysRolePermissionExample;
import com.shop.pojo.SysUserRole;
import com.shop.pojo.SysUserRoleExample;
import com.shop.service.SysService;

@Service
public class SysServiceImpl implements SysService {
	@Autowired
	private SysPermissionMapperCustom sysPermissionMapperCustom;
	@Autowired
	private SysRoleMapper sysRoleMapper;
	@Autowired
	private SysRolePermissionMapper sysRolePermissionMapper;
	@Autowired
	private SysPermissionMapper sysPermissionMapper;
	@Autowired
	private SysUserRoleMapper sysUserRoleMapper;
	
	@Override
	public List<SysPermission> findMenuListByUserId(String userid)
			throws Exception {
		return sysPermissionMapperCustom.findMenuListByUserId(userid);
	}

	@Override
	public List<SysPermission> findPermissionListByUserId(String userid)
			throws Exception {
		return sysPermissionMapperCustom.findPermissionListByUserId(userid);
	}

	@Override
	public List<MenuTree> loadMenuTree() {
		return sysPermissionMapperCustom.getMenuTree();
	}

	@Override
	public List<SysRole> findAllRoles() {
		return sysRoleMapper.selectByExample(null);
	}

	//根据用户帐号，查询所有角色和其权限列表
	@Override
	public SysRole findRolesAndPermissionsByUserId(String userId) {
		return sysPermissionMapperCustom.findRoleAndPermissionListByUserId(userId);
	}

	@Override
	public void addRoleAndPermissions(SysRole role, int[] permissionIds) {
		//添加角色
		sysRoleMapper.insert(role);
		//添加角色和权限关系表
		for (int i = 0; i < permissionIds.length; i++) {
			SysRolePermission rolePermission = new SysRolePermission();
			//16进制随机码
			String uuid = UUID.randomUUID().toString();
			rolePermission.setId(uuid);
			rolePermission.setSysRoleId(role.getId());
			rolePermission.setSysPermissionId(permissionIds[i]+"");
			sysRolePermissionMapper.insert(rolePermission);
		}
	}

	@Override
	public List<SysPermission> findAllMenus() {
		SysPermissionExample example = new SysPermissionExample();
		SysPermissionExample.Criteria criteria = example.createCriteria();
		//criteria.andTypeLike("%menu%");
		criteria.andTypeEqualTo("menu");
		return sysPermissionMapper.selectByExample(example);
	}

	@Override
	public void addSysPermission(SysPermission permission) {
		sysPermissionMapper.insert(permission);
	}

	@Override
	public List<SysPermission> findMenuAndPermissionByUserId(String userId) {
		return sysPermissionMapperCustom.findMenuAndPermissionByUserId(userId);
	}

	@Override
	public List<MenuTree> getAllMenuAndPermision() {
		return sysPermissionMapperCustom.getAllMenuAndPermision();
	}

	@Override
	public List<SysPermission> findPermissionsByRoleId(String roleId) {
		return sysPermissionMapperCustom.findPermissionsByRoleId(roleId);
	}

	@Override
	public void updateRoleAndPermissions(String roleId, int[] permissionIds) {
		//先删除角色权限关系表中角色的权限关系
		SysRolePermissionExample example = new SysRolePermissionExample();
		SysRolePermissionExample.Criteria criteria = example.createCriteria();
		criteria.andSysRoleIdEqualTo(roleId);
		sysRolePermissionMapper.deleteByExample(example);
		//重新创建角色权限关系
		for (Integer pid : permissionIds) {
			SysRolePermission rolePermission = new SysRolePermission();
			String uuid = UUID.randomUUID().toString();
			rolePermission.setId(uuid);
			rolePermission.setSysRoleId(roleId);
			rolePermission.setSysPermissionId(pid.toString());
			
			sysRolePermissionMapper.insert(rolePermission);
		}
	}

	//查询所有角色和其权限列表
	@Override
	public List<SysRole> findRolesAndPermissions() {
		return sysPermissionMapperCustom.findRoleAndPermissionList();
	}

	//添加用户角色表
	@Override
	public void addUserAndRole(SysUserRole sysUserRole) {
		String uuid = UUID.randomUUID().toString();
		sysUserRole.setId(uuid);
		sysUserRoleMapper.insert(sysUserRole);
	}

	//更新用户角色表
	@Override
	public int updateUserRole(SysUserRole sysUserRole) {
		return sysUserRoleMapper.updateByPrimaryKey(sysUserRole);
	}

	//查询用户角色表
	@Override
	public SysUserRole findUserRoleByUserId(String userId) {
		SysUserRoleExample example = new SysUserRoleExample();
		SysUserRoleExample.Criteria cri = example.createCriteria();
		cri.andSysUserIdEqualTo(userId);
		List<SysUserRole> list = sysUserRoleMapper.selectByExample(example);
		if (list.size()>0) {
			return list.get(0);
		}
		return null;
	}

	//通过id查询权限
	@Override
	public SysPermission findPermissionById(Long parentid) {
		return sysPermissionMapper.selectByPrimaryKey(parentid);
	}

	@Override
	public void deleteRoleById(String roleId) {
		//删除角色权限表
		SysRolePermissionExample example = new SysRolePermissionExample();
		SysRolePermissionExample.Criteria cri = example.createCriteria();
		cri.andSysRoleIdEqualTo(roleId);
		sysRolePermissionMapper.deleteByExample(example);
		//删除角色表
		sysRoleMapper.deleteByPrimaryKey(roleId);
	}

	@Override
	public void deleteUserRoleByUserId(String userId) {
		//删除用户角色表
		SysUserRoleExample example = new SysUserRoleExample();
		SysUserRoleExample.Criteria cri = example.createCriteria();
		cri.andSysUserIdEqualTo(userId);
		sysUserRoleMapper.deleteByExample(example);
	}
}
