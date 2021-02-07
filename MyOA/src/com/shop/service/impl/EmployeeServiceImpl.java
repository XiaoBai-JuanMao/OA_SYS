package com.shop.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shop.mapper.EmployeeMapper;
import com.shop.mapper.SysPermissionMapper;
import com.shop.pojo.Employee;
import com.shop.pojo.EmployeeExample;
import com.shop.service.EmployeeService;

@Service("employeeService")
public class EmployeeServiceImpl implements EmployeeService {

	@Autowired
	EmployeeMapper employeeMapper;
	@Autowired
	SysPermissionMapper sysPermissionMapper;
	
	@Override
	public Employee selectByPrimaryKey(Long id) {
		return employeeMapper.selectByPrimaryKey(id);
	}

	@Override
	public Employee selectByName(String name) {
		EmployeeExample example = new EmployeeExample();
		EmployeeExample.Criteria cri = example.createCriteria();
		cri.andNameEqualTo(name);
		List<Employee> list = employeeMapper.selectByExample(example);
		if (list.size()>0) {
			return list.get(0);
		}
		return null;
	}

	@Override
	public List<Employee> selectAll() {
		return employeeMapper.selectByExample(new EmployeeExample());
	}

	@Override
	public List<Employee> findByLevel(Integer level) {
		EmployeeExample example = new EmployeeExample();
		EmployeeExample.Criteria criteria = example.createCriteria();
		criteria.andRoleEqualTo(String.valueOf(level));
		List<Employee> list = employeeMapper.selectByExample(example);
		return list;
	}

	@Override
	public void save(Employee employee) {
		employeeMapper.insertSelective(employee);
	}

	@Override
	public int updateByName(Employee employee) {
		EmployeeExample example = new EmployeeExample();
		EmployeeExample.Criteria cri = example.createCriteria();
		cri.andNameEqualTo(employee.getName());
		return employeeMapper.updateByExampleSelective(employee, example);
	}

	@Override
	public void deleteUserByName(String name) {
		EmployeeExample example = new EmployeeExample();
		EmployeeExample.Criteria cri = example.createCriteria();
		cri.andNameEqualTo(name);
		employeeMapper.deleteByExample(example);
	}

	@Override
	public Employee selectByEmail(String email) {
		EmployeeExample example = new EmployeeExample();
		EmployeeExample.Criteria cri = example.createCriteria();
		cri.andEmailEqualTo(email);
		List<Employee> list = employeeMapper.selectByExample(example);
		if (list.size()>0) {
			return list.get(0);
		}
		return null;
	}
}
