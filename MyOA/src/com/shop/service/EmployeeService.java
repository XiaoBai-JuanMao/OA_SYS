package com.shop.service;

import java.util.List;

import com.shop.pojo.Employee;

public interface EmployeeService {
	//根据员工主键查询员工
	Employee selectByPrimaryKey(Long id);
	
	//根据员工账号查询员工
	Employee selectByName(String name);

	//查询所有用户
	List<Employee> selectAll();
	
	//通过级别查询员工
	List<Employee> findByLevel(Integer level);

	//保存员工
	void save(Employee employee);

	//通过用户名修改用户
	int updateByName(Employee employee);

	//通过用户名删除用户
	void deleteUserByName(String name);

	//通过邮箱查询用户
	Employee selectByEmail(String email);
}
