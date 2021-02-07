package com.shop.utils;

import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.shop.pojo.ActiveUser;
import com.shop.pojo.Employee;
import com.shop.service.EmployeeService;

public class CustomerTaskAssignee implements TaskListener {
	private static final long serialVersionUID = 1L;

	/**
	 * 	一旦启动流程后，会将流程进行监听，会自动触发监听器（一般需要在模型中进行配置，否则无法监听）
	 * 	一旦该监听器触发后，就可以自动动态设置代办人
	 */
	//调用业务查询出当前代办人的上级(任务会提交到代办人的上级)
	@Override
	public void notify(DelegateTask delegateTask) {
		
		WebApplicationContext applicationContext = ContextLoader.getCurrentWebApplicationContext();
		
		EmployeeService employeeService = (EmployeeService) applicationContext.getBean("employeeService");
		
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		
		ActiveUser user = (ActiveUser) request.getSession().getAttribute(Constants.GLOBLE_USER_SESSION);
	
		//获取当前用户的上级
		long manageId = user.getManagerId();
		//调用业务类方法查询用户
		Employee manager = employeeService.selectByPrimaryKey(manageId);
		
		//设置变量
		delegateTask.setAssignee(manager.getName());
	}

}
