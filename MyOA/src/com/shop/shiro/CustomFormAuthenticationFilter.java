package com.shop.shiro;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;

/*
 * 自定义表单认证类
 */
public class CustomFormAuthenticationFilter extends FormAuthenticationFilter {
	/*
	 * 	返回值：true，表示请求不会继续向下执行，不会再调用realm
	 */
	@Override
	protected boolean onAccessDenied(ServletRequest req, ServletResponse resp) throws Exception {
		System.out.println("【】【】【】校验验证码，记住密码【】【】【】");
		
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		//获取用户输入的验证码
		String randomcode = request.getParameter("randomcode");
		String validateCode = (String) request.getSession().getAttribute("validateCode");
		//若验证码不正确，返回true
		if (randomcode!=null && validateCode!=null && !randomcode.equals(validateCode)) {
			request.setAttribute("shiroLoginFailure", "randomcodeError");
			return true;
		}

		return super.onAccessDenied(request, response);
	} 
}
