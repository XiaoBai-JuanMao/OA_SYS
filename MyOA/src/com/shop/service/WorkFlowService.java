package com.shop.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;

import com.shop.pojo.Baoxiaobill;
import com.shop.pojo.Leavebill;

public interface WorkFlowService {
	//部署流程
	void saveNewDeploy(InputStream in,String processName);
	//查询定义流程
	List<ProcessDefinition> findProcessDefinitionList();
	//启动请假流程
	void saveStartLeave(Long leaveId, String name);
	//通过用户名获取代办任务列表
	List<Task> findTaskListByName(String name);
	//通过任务id查询请假清单
	Leavebill findLeaveBillByTaskId(String taskId);
	//通过任务id查询批注
	List<Comment> findCommentListByTaskId(String taskId);
	//提交任务
	void submitTask(String id, String taskId, String comment, String username);
	//查询流程图坐标
	Map<String, Object> findCoordingByTask(String taskId);
	//获取流程图io流
	InputStream findImageInputStream(String deploymentId, String imageName);
	//通过任务id查询流程
	ProcessDefinition findProcessDefinitionByTaskId(String taskId);
	//通过请假单id查询批注
	List<Comment> findCommentListByleaveId(String leaveId);
	//通过请假单id查询流程
	ProcessDefinition findProcessDefinitionByleaveId(String leaveId);
	//通过请假单id查询流程图坐标
	Map<String, Object> findCoordingByleaveId(String leaveId);
	//查询部署表
	List<Deployment> findDeployment();
	//删除部署表
	void deletDeployById(String deploymentId);
	//启动报销流程
	void saveStartBaoxiao(Integer baoxiaoId, String username);
	//通过报销单id查询批注
	List<Comment> findCommentListBybaoxiaoId(String baoxiaoId);
	//通过报销单id查询流程
	ProcessDefinition findProcessDefinitionBybaoxiaoId(String billId);
	//通过报销单id查询流程图坐标
	Map<String, Object> findCoordingBybaoxiaoId(String billId);
	//通过任务id查询报销单
	Baoxiaobill findBaoxiaoBillByTaskId(String taskId);
	//提交报销单
	void submitBaoxiaoTask(String id, String taskId, String comment, String outcome, String username);
	//通过任务id查询连线
	List<String> findOutComeListByTaskId(String taskId);
	//查询报销任务
	List<Task> findBaoxiaoTaskListByName(String name);
}
