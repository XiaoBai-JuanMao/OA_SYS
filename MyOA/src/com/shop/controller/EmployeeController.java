package com.shop.controller;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.shop.pojo.ActiveUser;
import com.shop.pojo.Employee;
import com.shop.pojo.MenuTree;
import com.shop.pojo.ReturnType;
import com.shop.pojo.SysPermission;
import com.shop.pojo.SysRole;
import com.shop.pojo.SysUserRole;
import com.shop.service.EmployeeService;
import com.shop.service.SysService;
import com.shop.utils.Constants;
import com.shop.utils.MyMD5;

@Controller
public class EmployeeController {
	@Autowired
	EmployeeService employeeService;
	@Autowired
	SysService sysService;
	
	@RequestMapping("/main")
	public String main(Model model,HttpSession session) {
		System.out.println("【】【】【】进入主页跳转【】【】【】");
		ActiveUser activeUser = (ActiveUser) SecurityUtils.getSubject().getPrincipal();
		System.out.println("保存用户【】【】【】"+activeUser.toString());
		session.setAttribute(Constants.GLOBLE_USER_SESSION, activeUser);
		model.addAttribute("activeUser", activeUser);
		return "index";
	}
	
	@RequestMapping("/login")
	public String login(HttpServletRequest request,String rememberMe,Model model) {
		System.out.println("【】【】【】登录验证完毕【】【】【】");
		String exceptionName = (String) request.getAttribute("shiroLoginFailure");
		if (exceptionName != null) {
			if (UnknownAccountException.class.getName().equals(exceptionName)) {
				model.addAttribute("error", "用户账号不存在");
			} else if (IncorrectCredentialsException.class.getName().equals(exceptionName)) {
				model.addAttribute("error", "密码不正确");
			} else if("randomcodeError".equals(exceptionName)) {
				model.addAttribute("error", "验证码不正确");
			} else {
				model.addAttribute("error", "未知错误");
			}
		}

		return "login";
	}

	//系统管理-【用户列表】
	@RequestMapping("/userList")
	public String userList(Model model) {
		List<Employee> userlist = employeeService.selectAll();
		for (Employee employee : userlist) {
			if (employee.getManagerId()==null) {
				employee.setManager("无");
			}else {
				Employee manager = employeeService.selectByPrimaryKey(employee.getManagerId());
				employee.setManager(manager.getName());				
			}
			if (employee.getRole()!=null && !"".equals(employee.getRole())) {
				SysRole sysRole = sysService.findRolesAndPermissionsByUserId(employee.getName());
				employee.setRolename(sysRole.getName());
			}
		}
		model.addAttribute("userList", userlist);
		
		List<SysRole> allRoles = sysService.findAllRoles();
		model.addAttribute("allRoles", allRoles);
		return "userlist";
	}
	
	//系统管理-用户列表-用户列表-【查看权限】
	@RequestMapping("/viewPermissionByUser")
	@ResponseBody
	public SysRole viewPermissionByUser(String userName) {
		return sysService.findRolesAndPermissionsByUserId(userName);
	}
	
	//系统管理-用户列表-新建用户-【上级主管】
	//根据员工级别查询上级主管
	@RequestMapping("/findNextManager")
	@ResponseBody
	public List<Employee> findNextManager(Integer level){
		level++;
		List<Employee> managerList = employeeService.findByLevel(level);
		return managerList;
	}
	
	//系统管理-用户列表-新建用户-【邮箱】
	@RequestMapping("/checkEmailAjax")
	public Employee checkEmailAjax(String email) {
		return employeeService.selectByEmail(email);
	}
	
	//系统管理-用户列表-新建用户-【保存】
	@RequestMapping("/saveUser")
	@Transactional
	public String saveUser(Employee employee) {
		//新建用户
		String password = employee.getPassword();
		MyMD5 md5 = new MyMD5();
		employee.setPassword(md5.mdkPassword(password, "", 2));
		employeeService.save(employee);
		
		//新建用户-角色信息
		String sysRoleId = "";
		if ("1".equals(employee.getRole())) {
			sysRoleId = "1";
		} else if ("2".equals(employee.getRole())) {
			sysRoleId = "2";
		} else if ("3".equals(employee.getRole())) {
			sysRoleId = "3";
		} 
		SysUserRole sysUserRole = new SysUserRole();
		sysUserRole.setSysUserId(employee.getName());
		sysUserRole.setSysRoleId(sysRoleId);
		sysService.addUserAndRole(sysUserRole);
		return "redirect:/userList";
	}
	
	//系统管理-用户列表-【角色分配】
	@RequestMapping("/assignRole")
	@ResponseBody
	@Transactional
	public ReturnType assignRole(String roleId,String userId) {
		ReturnType data = new ReturnType();
		
		//更新用户信息
		Employee employee = new Employee();
		employee.setName(userId);
		if ("1".equals(roleId)) {
			employee.setRole("1");
			employee.setManagerId(Long.parseLong("3"));
		} else if ("2".equals(roleId)||"4".equals(roleId)) {
			employee.setRole("2");
			employee.setManagerId(Long.parseLong("1"));
		} else if ("3".equals(roleId)) {
			employee.setRole("3");
			employee.setManagerId(null);
		} else {
			employee.setRole("1");	//赋予默认等级为1
			employee.setManagerId(null);	//默认无经理
		}
		//更新用户
		if (employeeService.updateByName(employee)==0) {
			data.setMsg("更新失败，更新用户信息失败");
			return data;
		}
		
		//查询当前用户对应的用户-角色表
		SysUserRole sysUserRole = sysService.findUserRoleByUserId(userId);
		//更新用户-角色表
		sysUserRole.setSysUserId(userId);
		sysUserRole.setSysRoleId(roleId);
		if (sysService.updateUserRole(sysUserRole)==0) {
			data.setMsg("更新失败，更新角色信息失败");
			return data;
		}
		
		//通过当前id查询主管
		Employee manager = employeeService.selectByPrimaryKey(employee.getManagerId());
		String managerName = "";
		if (manager!=null) {
			managerName = manager.getName();
		} else {
			managerName = "无";
		}
		data.setManager(managerName);
		
		data.setMsg("更新成功");
		return data;
	}
	
	//系统管理-用户列表-【删除】
	@RequestMapping("/userAction_delete")
	public String userAction_delete(String userId) {
		//删除用户角色关联
		sysService.deleteUserRoleByUserId(userId);
		//删除用户
		employeeService.deleteUserByName(userId);
		return "redirect:/userList";
	}
	
	//系统管理-【角色管理】
	@RequestMapping("/roleList")
	public String roleList(Model model) {
		//查询所有角色
		List<SysRole> allRoles = sysService.findAllRoles();
		model.addAttribute("allRoles",allRoles);
		
		//查询所有菜单和权限
		List<MenuTree> allMenuAndPermissions = sysService.getAllMenuAndPermision();
		model.addAttribute("allMenuAndPermissions", allMenuAndPermissions);
		return "rolelist";
	}
	
	//系统管理-角色管理-【编辑】
	@RequestMapping("/loadMyPermissions")
	@ResponseBody
	public List<SysPermission> loadMyPermissions(String roleId) {
		List<SysPermission> permissionList = sysService.findPermissionsByRoleId(roleId);
		return permissionList;
	}
	
	//系统管理-角色管理-编辑-【保存】
	@RequestMapping("/updateRoleAndPermission")
	@Transactional
	public String updateRoleAndPermission(String roleId,int[] permissionIds) {
		sysService.updateRoleAndPermissions(roleId, permissionIds);
		return "redirect:/roleList";
	}
	
	//系统管理-角色管理-【删除】
	@RequestMapping("/deleteRole")
	@Transactional
	public String deleteRole(String roleId) {
		sysService.deleteRoleById(roleId);
		return "redirect:/roleList";
	}
	
	//系统管理-【权限管理】
	@RequestMapping("/permissionList")
	public String permissionList(Model model) {
		//查询权限菜单
		List<MenuTree> allPermissions = sysService.getAllMenuAndPermision();
		model.addAttribute("allPermissions", allPermissions);
		
		//查询父节点
		List<SysPermission> menuTypes = sysService.findAllMenus();
		model.addAttribute("menuTypes", menuTypes);
		return "permissionlist";
	}
	
	//系统管理-权限管理-【保存角色和权限】
	@RequestMapping("/saveRoleAndPermissions")
	@Transactional
	public String saveRoleAndPermissions(String name,int[] permissionIds,Model model) {
		SysRole sysRole = new SysRole();
		String uuid = UUID.randomUUID().toString();
		sysRole.setId(uuid);
		sysRole.setName(name);
		sysRole.setAvailable("1");
		sysService.addRoleAndPermissions(sysRole, permissionIds);
		return "redirect:/permissionList";
	}
	
	//系统管理-权限管理-新建权限-【保存】
	@RequestMapping("/saveSubmitPermission")
	@Transactional
	public String saveSubmitPermission(SysPermission sysPermission,Model model){
		SysPermission parent = sysService.findPermissionById(sysPermission.getParentid());
		sysPermission.setParentids(parent.getParentids()+parent.getId()+"/");
		if ("menu".equals(sysPermission.getType())) {
			List<SysPermission> menus = sysService.findAllMenus();
			SysPermission lastmenu = menus.get(menus.size()-1);
			String lastSortString = lastmenu.getSortstring();
			lastSortString = lastSortString.substring(0,lastSortString.indexOf("."));
			Integer lastSort = Integer.valueOf(lastSortString)+1;
			sysPermission.setSortstring(lastSort+".");
		}else {
			sysPermission.setSortstring("");			
		}
		if (sysPermission.getAvailable()==null || "".equals(sysPermission.getAvailable())) {
			sysPermission.setAvailable("0");
		}
		sysService.addSysPermission(sysPermission);
		return "redirect:/permissionList";
	}
}
