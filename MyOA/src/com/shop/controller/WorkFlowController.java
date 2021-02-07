package com.shop.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.shop.pojo.ActiveUser;
import com.shop.pojo.Baoxiaobill;
import com.shop.pojo.Leavebill;
import com.shop.service.BaoxiaobillService;
import com.shop.service.LeavebillService;
import com.shop.service.WorkFlowService;
import com.shop.utils.Constants;

@Controller
public class WorkFlowController {
	
	@Autowired
	private WorkFlowService workFlowService;
	@Autowired
	private LeavebillService leavebillService;
	@Autowired
	private BaoxiaobillService baoxiaobillService;
	
	//请假管理-请假申请-【提交请假申请】
	//保存请假单并启动请假流程
	@Transactional
	@RequestMapping("/saveStartLeave")
	public String saveStartLeave(Leavebill leavebill,HttpSession session) {
		//设置当前时间
		leavebill.setLeavedate(new Date());
		//设置当前状态
		leavebill.setState(1);
		//设置当前用户
		ActiveUser user = (ActiveUser) session.getAttribute(Constants.GLOBLE_USER_SESSION);
		leavebill.setUserId(user.getId());
		
		//保存当前请假流程
		leavebillService.saveLeaveBill(leavebill);
		//启动请假流程，并执行任务
		workFlowService.saveStartLeave(leavebill.getId(), user.getUsername());
		return "redirect:/myTaskList";
	}
	
	//请假管理-【我的请假单】
	@RequestMapping("/myLeaveBill")
	public String myLeaveBill(HttpSession session,Model model) {
		//获取当前用户
		ActiveUser user = (ActiveUser) session.getAttribute(Constants.GLOBLE_USER_SESSION);
		
		//根据当前用户查询请假单leaveList
		List<Leavebill> leaveList = leavebillService.selectByUserId(user.getId());
		model.addAttribute("leaveList", leaveList);
		return "leavebill";
	}
	
	//请假管理-我的请假单-【查看审核记录】
	//根据请假单id查询审核记录
	@RequestMapping("/viewHisComment")
	public String viewHisComment(String id,Model model) {
		Leavebill leaveBill = leavebillService.selectById(id);
		model.addAttribute("leaveBill", leaveBill);
		
		List<Comment> commentList = workFlowService.findCommentListByleaveId(id);
		model.addAttribute("commentList", commentList);
		return "workflow_commentlist";
	}
	
	//请假管理-我的请假单-【查看当前流程图】
	@RequestMapping("/viewCurrentImageByBill")
	public String viewCurrentImageByBill(String billId,Model model) {
		//通过任务查询事务表
		ProcessDefinition processDefinition = workFlowService.findProcessDefinitionByleaveId(billId);
		model.addAttribute("deploymentId", processDefinition.getDeploymentId());
		model.addAttribute("imageName", processDefinition.getDiagramResourceName());
		
		//查看对应坐标
		Map<String, Object> map = workFlowService.findCoordingByleaveId(billId);
		model.addAttribute("acs", map);
		return "viewimage";
	}
	
	//viewimage.jsp跳转
	//流程管理-查看流程-【查看定义流程图】
	@RequestMapping("/viewImage")
	public void viewImage(String deploymentId,String imageName,HttpServletResponse response) throws Exception{
		//2：获取资源文件表（act_ge_bytearray）中资源图片输入流InputStream
		InputStream in = workFlowService.findImageInputStream(deploymentId,imageName);
		//3：从response对象获取输出流
		OutputStream out = response.getOutputStream();
		//4：将输入流中的数据读取出来，写到输出流中
		for(int b=-1;(b=in.read())!=-1;){
			out.write(b);
		}
		out.close();
		in.close();
	}
	
	//请假管理-我的请假单-【删除】
	@Transactional
	@RequestMapping("/leaveBillAction_delete")
	public String leaveBillAction_delete(String id,Model model) {
		if (leavebillService.deleteById(id) == 0) {
			System.out.println("删除失败");
		}
		return "redirect:/myLeaveBill";
	}
	
	//请假管理-【我的待办事务】
	//查询当前用户的待办任务
	@RequestMapping("/myTaskList")
	public String myTaskList(Model model,HttpSession session) {
		String name = ((ActiveUser) session.getAttribute(Constants.GLOBLE_USER_SESSION)).getUsername();
		
		List<Task> taskList = workFlowService.findTaskListByName(name);
		model.addAttribute("taskList", taskList);
		
		return "workflow_task";
	}
	
	//请假管理-我的待办事务-【办理任务】
	//根据当前任务id，去查询当前请假单信息+查询出历史批注信息
	@RequestMapping("/viewTaskForm")
	public String viewTaskForm(String taskId,Model model) {
		model.addAttribute("taskId", taskId);
		
		//查询当前请假单信息
		Leavebill leavebill = workFlowService.findLeaveBillByTaskId(taskId);
		model.addAttribute("bill",leavebill);
		
		//根据taskId查询历史批注表
		List<Comment> commentList = workFlowService.findCommentListByTaskId(taskId);
		model.addAttribute("commentList",commentList);
		return "approve_leave";
	}
	
	//请假管理-我的待办事务-办理任务-【提交】
	//提交任务
	@RequestMapping("/submitTask")
	@Transactional
	public String submitTask(String id,String taskId,String comment,HttpSession session) {
		ActiveUser user = (ActiveUser) session.getAttribute(Constants.GLOBLE_USER_SESSION);
		workFlowService.submitTask(id, taskId, comment, user.getUsername());
		return "redirect:/myTaskList";
	}
	
	//请假管理-我的待办事务-【查看当前流程图】
	//报销管理-我的待办事务-【查看当前流程图】
	@RequestMapping("/viewCurrentImage")
	public String viewCurrentImage(String taskId,Model model) {
		//通过任务查询事务表
		ProcessDefinition processDefinition = workFlowService.findProcessDefinitionByTaskId(taskId);
		model.addAttribute("deploymentId", processDefinition.getDeploymentId());
		model.addAttribute("imageName", processDefinition.getDiagramResourceName());
		
		//查看对应坐标
		Map<String, Object> map = workFlowService.findCoordingByTask(taskId);
		model.addAttribute("acs", map);
		
		return "viewimage";
	}
	
	//报销管理-报销申请-【提交报销申请】
	//保存报销单并启动报销流程
	@RequestMapping("/saveStartBaoxiao")
	@Transactional
	public String saveStartBaoxiao(Baoxiaobill baoxiaobill,HttpSession session) {
		baoxiaobill.setCreatdate(new Date());
		baoxiaobill.setState(1);
		ActiveUser user = (ActiveUser) session.getAttribute(Constants.GLOBLE_USER_SESSION);
		baoxiaobill.setUserId(Integer.parseInt(String.valueOf(user.getId())));

		baoxiaobillService.saveBaoxiaoBill(baoxiaobill);
		
		workFlowService.saveStartBaoxiao(baoxiaobill.getId(), user.getUsername());
		return "redirect:/myBaoxiaoTaskList";
	}
	
	//报销管理-【我的报销单】
	@RequestMapping("/myBaoxiaoBill")
	public String myBaoxiaoBill(HttpSession session,Model model) {
		ActiveUser user = (ActiveUser) session.getAttribute(Constants.GLOBLE_USER_SESSION);
		
		List<Baoxiaobill> baoxiaoList = baoxiaobillService.selectByUserId(user.getId());
		model.addAttribute("baoxiaoList", baoxiaoList);
		return "baoxiaobill";
	}
	
	//报销管理-我的报销单-【查看审核记录】
	@RequestMapping("/viewBaoxiaoHisComment")
	public String viewBaoxiaoHisComment(String id,Model model) {
		Baoxiaobill baoxiaoBill = baoxiaobillService.selectById(id);
		model.addAttribute("baoxiaoBill", baoxiaoBill);
		
		List<Comment> commentList = workFlowService.findCommentListBybaoxiaoId(id);
		model.addAttribute("commentList", commentList);
		return "workflow_baoxiaocommentlist";
	}
	
	//报销管理-我的报销单-【查看当前流程图】
	@RequestMapping("/viewCurrentImageByBaoxiaoBill")
	public String viewCurrentImageByBaoxiaoBill(String billId,Model model) {
		ProcessDefinition processDefinition = workFlowService.findProcessDefinitionBybaoxiaoId(billId);
		model.addAttribute("deploymentId", processDefinition.getDeploymentId());
		model.addAttribute("imageName", processDefinition.getDiagramResourceName());
		
		Map<String, Object> map = workFlowService.findCoordingBybaoxiaoId(billId);
		model.addAttribute("acs", map);
		return "viewimage";
	}
	
	//报销管理-我的报销单-【删除】
	@RequestMapping("/baoxiaoBillAction_delete")
	@Transactional
	public String baoxiaoBillAction_delete(String id,Model model) {
		if (baoxiaobillService.deleteById(id) == 0) {
			System.out.println("删除失败");
		}
		return "redirect:/myBaoxiaoBill";
	}
	
	//报销管理-【我的待办事务】
	@RequestMapping("/myBaoxiaoTaskList")
	public String myBaoxiaoTaskList(Model model,HttpSession session) {
		String name = ((ActiveUser) session.getAttribute(Constants.GLOBLE_USER_SESSION)).getUsername();
		
		List<Task> taskList = workFlowService.findBaoxiaoTaskListByName(name);
		model.addAttribute("taskList", taskList);
		
		return "workflow_baoxiaotask";
	}
	
	//报销管理-我的待办事务-【办理任务】
	//根据当前任务id，去查询当前报销单信息+查询出历史批注信息
	@RequestMapping("/viewBaoxiaoTaskForm")
	public String viewBaoxiaoTaskForm(String taskId,Model model) {
		model.addAttribute("taskId", taskId);
		
		//查询当前报销单信息
		Baoxiaobill baoxiaobill = workFlowService.findBaoxiaoBillByTaskId(taskId);
		model.addAttribute("baoxiaoBill",baoxiaobill);
		
		//根据taskId查询历史批注表
		List<Comment> commentList = workFlowService.findCommentListByTaskId(taskId);
		model.addAttribute("commentList",commentList);
		
		//根据taskId查询任务提交分支
		List<String> outcomeList = workFlowService.findOutComeListByTaskId(taskId);
		model.addAttribute("outcomeList", outcomeList);
		return "approve_baoxiao";
	}
	
	
	//报销管理-我的待办事务-办理任务-【提交】
	@RequestMapping("/submitBaoxiaoTask")
	@Transactional
	public String submitBaoxiaoTask(String id,String taskId,String comment,String outcome,HttpSession session) {
		ActiveUser user = (ActiveUser) session.getAttribute(Constants.GLOBLE_USER_SESSION);
		workFlowService.submitBaoxiaoTask(id, taskId, comment, outcome, user.getUsername());
		return "redirect:/myBaoxiaoTaskList";
	}
	
	//流程管理-【查看流程】
	@RequestMapping("/processDefinitionList")
	public String processDefinitionList(Model model) {
		//查询部署表
		List<Deployment> depList = workFlowService.findDeployment();
		model.addAttribute("depList",depList);
		
		//查询流程定义表
		List<ProcessDefinition> pdList = workFlowService.findProcessDefinitionList();
		model.addAttribute("pdList", pdList);
		return "workflow_list";
	}
	
	//流程管理-查看流程-【删除】
	@RequestMapping("/delDeployment")
	@Transactional
	public String delDeployment(String deploymentId,Model model) {
		workFlowService.deletDeployById(deploymentId);
		return "redirect:/processDefinitionList";
	}
	
	//流程管理-发布流程-【部署流程】
	@RequestMapping("/deployProcess")
	@Transactional
	public String deployProcess(String processName,MultipartFile fileName) {
		try {
			workFlowService.saveNewDeploy(fileName.getInputStream(), processName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "redirect:/processDefinitionList";
	}
}
