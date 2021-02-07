package com.shop.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shop.mapper.BaoxiaobillMapper;
import com.shop.mapper.LeavebillMapper;
import com.shop.pojo.Baoxiaobill;
import com.shop.pojo.Leavebill;
import com.shop.service.WorkFlowService;
import com.shop.utils.Constants;

@Service("workFlowService")
public class WorkFlowServiceImpl implements WorkFlowService {
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private HistoryService historyService;
	/*
	@Autowired
	private FormService formService;
	*/
	@Autowired
	private LeavebillMapper leavebillMapper;
	@Autowired
	private BaoxiaobillMapper baoxiaobillMapper;
	
	/**
	 * 	部署流程定义
	 */
	@Override
	public void saveNewDeploy(InputStream in, String processName) {
		try {
			ZipInputStream zip = new ZipInputStream(in);
			repositoryService.createDeployment()		//创建部署对象
							 .name(processName)			//部署名称
							 .addZipInputStream(zip)	//添加部署文件
							 .deploy();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取流程列表
	 */
	@Override
	public List<ProcessDefinition> findProcessDefinitionList() {
		return repositoryService.createProcessDefinitionQuery().list();
	}

	/**
	 * 	启动请假流程
	 */
	@Override
	public void saveStartLeave(Long leaveId, String userName) {
		//获取到要执行key -- act_re_procdef的 KEY_ 字段
		String key = Constants.Leave_KEY;
		
		/**
		 * 	从session获取到的当前任务待办人，使用流程变量监听器设置下一个任务的代办人
		 */
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userName", userName);
		//设置Bussiness_key的规则，记录任务清单ID -- act_ru_execution表的Bussiness_key字段
		String BUSSINESS_KEY = key+"."+leaveId;
		map.put("objId", BUSSINESS_KEY);	
		
		runtimeService.startProcessInstanceByKey(key, BUSSINESS_KEY, map);
	}

	/**
	 * 	通过代办人查找请假流程任务
	 */
	@Override
	public List<Task> findTaskListByName(String name) {
		List<Task> list = taskService.createTaskQuery().taskAssignee(name)
				.orderByTaskCreateTime().asc().list();
		
		if (list.size()>0) {
			List<Task> levelTasklist = new ArrayList<Task>();
			for (Task task : list) {
				if (task.getProcessDefinitionId().startsWith(Constants.Leave_KEY)) {
					levelTasklist.add(task);
				}
			}
			return levelTasklist;
		}
		return null;
	}

	/**
	 * 	通过任务id查询请假单
	 */
	@Override
	public Leavebill findLeaveBillByTaskId(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		//然后根据流程实例ID，获取到ProcessInstance
		//act_hi_procinst
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(task.getProcessInstanceId()).singleResult();
		//然后再从processInstance 获取BusinessKey_key-》再获取leaveId
		String businessKey = processInstance.getBusinessKey();
		String leaveId = "";
		if (businessKey!=null && !"".equals(businessKey)) {
			leaveId = businessKey.substring(businessKey.indexOf(".")+1);
		}
		//根据leaveId查询Leavebill
		Leavebill leavebill = leavebillMapper.selectByPrimaryKey(Long.parseLong(leaveId));
		return leavebill;
	}

	/**
	 * 	通过任务ID查询批注列表
	 */
	@Override
	public List<Comment> findCommentListByTaskId(String taskId) {
		//获取到任务对象
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		//然后根据流程实例ID，获取到ProcessInstanceID
		String processInstanceId = task.getProcessInstanceId();
		return taskService.getProcessInstanceComments(processInstanceId);
	}

	/**
	 *	通过请假单id查询批注列表
	 */
	@Override
	public List<Comment> findCommentListByleaveId(String leaveId){
		//获取到要执行key -- act_re_procdef的 KEY_ 字段
		String key = Constants.Leave_KEY;
		//根据任务清单ID设置Bussiness_key -- act_ru_execution表的Bussiness_key字段
		String BUSSINESS_KEY = key+"."+leaveId;
		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(BUSSINESS_KEY).singleResult();
		//获取历史流程实例ID
		String hpiId = historicProcessInstance.getId();
		return taskService.getProcessInstanceComments(hpiId);
	}
	
	/**
	 * 	提交任务
	 */
	@Override
	public void submitTask(String id, String taskId, String comment, String username) {
		//获取任务对象
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		//然后根据流程实例ID，获取到ProcessInstanceID
		String processInstanceId = task.getProcessInstanceId();
		//设置批注人
		Authentication.setAuthenticatedUserId(username);
		//添加批注
		taskService.addComment(taskId, processInstanceId, comment);
		//完成任务
		taskService.complete(taskId);
		
		//修改请假单状态
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processDefinitionId(task.getProcessDefinitionId())
				.processInstanceBusinessKey(Constants.Leave_KEY+"."+id)
				.singleResult();
		if (processInstance==null) {
			//更新状态
			Leavebill leavebill = leavebillMapper.selectByPrimaryKey(Long.parseLong(id));
			leavebill.setState(2);
			//重新更新数据库
			leavebillMapper.updateByPrimaryKey(leavebill);
		}
	}

	/**
	 * 	查询坐标
	 */
	@Override
	public Map<String, Object> findCoordingByTask(String taskId) {
		//存放坐标
		Map<String, Object> map = new HashMap<String,Object>();
		//使用任务ID，查询任务对象
		Task task = taskService.createTaskQuery()//
					.taskId(taskId)//使用任务ID查询
					.singleResult();
		//获取流程定义的ID
		String processDefinitionId = task.getProcessDefinitionId();
		//获取流程定义的实体对象（对应.bpmn文件中的数据）
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processDefinitionId);
		//流程实例ID
		String processInstanceId = task.getProcessInstanceId();
		//使用流程实例ID，查询正在执行的执行对象表，获取当前活动对应的流程实例对象
		ProcessInstance pi = runtimeService.createProcessInstanceQuery()//创建流程实例查询
											.processInstanceId(processInstanceId)//使用流程实例ID查询
											.singleResult();
		//获取当前活动的ID
		String activityId = pi.getActivityId();
		//获取当前活动对象
		ActivityImpl activityImpl = processDefinitionEntity.findActivity(activityId);//活动ID
		//获取坐标
		map.put("x", activityImpl.getX());
		map.put("y", activityImpl.getY());
		map.put("width", activityImpl.getWidth());
		map.put("height", activityImpl.getHeight());
		return map;
	}

	/**
	 * 获取图片输入流
	 */
	@Override
	public InputStream findImageInputStream(String deploymentId, String imageName) {
		return repositoryService.getResourceAsStream(deploymentId, imageName);
	}

	/**
	 * 	根据任务ID查询流程
	 */
	@Override
	public ProcessDefinition findProcessDefinitionByTaskId(String taskId) {
		//使用任务ID，查询任务对象
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		//获取流程定义ID
		String processDefinitionId = task.getProcessDefinitionId();
		//查询流程定义的对象
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()//创建流程定义查询对象，对应表act_re_procdef 
					.processDefinitionId(processDefinitionId)//使用流程定义ID查询
					.singleResult();
		return processDefinition;
	}

	/**
	 * 根据请假单id查询流程
	 */
	@Override
	public ProcessDefinition findProcessDefinitionByleaveId(String leaveId) {
		//获取到要执行key -- act_re_procdef的 KEY_ 字段
		String key = Constants.Leave_KEY;
		//根据任务清单ID设置Bussiness_key -- act_ru_execution表的Bussiness_key字段
		String BUSSINESS_KEY = key+"."+leaveId;
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(BUSSINESS_KEY).singleResult();
		//获取流程定义的实体对象（对应.bpmn文件中的数据）
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
		return processDefinitionEntity;
	}

	@Override
	public Map<String, Object> findCoordingByleaveId(String leaveId) {
		//存放坐标
		Map<String, Object> map = new HashMap<String,Object>();
		String key = Constants.Leave_KEY;
		//根据任务清单ID设置Bussiness_key -- act_ru_execution表的Bussiness_key字段
		String BUSSINESS_KEY = key+"."+leaveId;
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(BUSSINESS_KEY).singleResult();
		//获取流程定义的实体对象（对应.bpmn文件中的数据）
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());//流程实例ID
		//获取流程定义ID
		String processInstanceId = processInstance.getProcessInstanceId();
		//使用流程实例ID，查询正在执行的执行对象表，获取当前活动对应的流程实例对象
		ProcessInstance pi = runtimeService.createProcessInstanceQuery()//创建流程实例查询
											.processInstanceId(processInstanceId)//使用流程实例ID查询
											.singleResult();
		//获取当前活动的ID
		String activityId = pi.getActivityId();
		//获取当前活动对象
		ActivityImpl activityImpl = processDefinitionEntity.findActivity(activityId);//活动ID
		//获取坐标
		map.put("x", activityImpl.getX());
		map.put("y", activityImpl.getY());
		map.put("width", activityImpl.getWidth());
		map.put("height", activityImpl.getHeight());
		return map;
	}

	/**
	 * 获取部署表
	 */
	@Override
	public List<Deployment> findDeployment() {
		return repositoryService.createDeploymentQuery().list();
	}

	/**
	 * 删除部署表
	 */
	@Override
	public void deletDeployById(String deploymentId) {
		try {
			repositoryService.deleteDeployment(deploymentId,true);
			System.out.println("删除成功");
		} catch (ActivitiObjectNotFoundException ae) {
			ae.printStackTrace();
			System.out.println("任务不存在");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("未知错误，删除失败");
		} 
	}

	/**
	 * 	启动报销流程
	 */
	@Override
	public void saveStartBaoxiao(Integer baoxiaoId, String userName) {
		//获取到要执行key -- act_re_procdef的 KEY_ 字段
		String key = Constants.Baoxiao_KEY;
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userName", userName);
		//设置Bussiness_key的规则，记录任务清单ID -- act_ru_execution表的Bussiness_key字段
		String BUSSINESS_KEY = key+"."+baoxiaoId;
		map.put("objId", BUSSINESS_KEY);	
		
		runtimeService.startProcessInstanceByKey(key, BUSSINESS_KEY, map);
	}
	
	/**
	 *	通过报销单id查询批注列表
	 */
	@Override
	public List<Comment> findCommentListBybaoxiaoId(String baoxiaoId){
		String key = Constants.Baoxiao_KEY;
		String BUSSINESS_KEY = key+"."+baoxiaoId;
		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
				.processInstanceBusinessKey(BUSSINESS_KEY).singleResult();
		String hpiId = historicProcessInstance.getId();
		return taskService.getProcessInstanceComments(hpiId);
	}
	
	/**
	 * 根据报销单id查询流程
	 */
	@Override
	public ProcessDefinition findProcessDefinitionBybaoxiaoId(String baoxiaoId) {
		String key = Constants.Baoxiao_KEY;
		String BUSSINESS_KEY = key+"."+baoxiaoId;
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(BUSSINESS_KEY).singleResult();
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
		return processDefinitionEntity;
	}

	@Override
	public Map<String, Object> findCoordingBybaoxiaoId(String baoxiaoId) {
		Map<String, Object> map = new HashMap<String,Object>();
		String key = Constants.Baoxiao_KEY;
		String BUSSINESS_KEY = key+"."+baoxiaoId;
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(BUSSINESS_KEY).singleResult();
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
		String processInstanceId = processInstance.getProcessInstanceId();
		ProcessInstance pi = runtimeService.createProcessInstanceQuery()
											.processInstanceId(processInstanceId)
											.singleResult();
		String activityId = pi.getActivityId();
		ActivityImpl activityImpl = processDefinitionEntity.findActivity(activityId);//活动ID
		map.put("x", activityImpl.getX());
		map.put("y", activityImpl.getY());
		map.put("width", activityImpl.getWidth());
		map.put("height", activityImpl.getHeight());
		return map;
	}
	
	/**
	 * 	通过任务id查询报销单
	 */
	@Override
	public Baoxiaobill findBaoxiaoBillByTaskId(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(task.getProcessInstanceId()).singleResult();
		String businessKey = processInstance.getBusinessKey();
		String baoxiaoId = "";
		if (businessKey!=null && !"".equals(businessKey)) {
			baoxiaoId = businessKey.substring(businessKey.indexOf(".")+1);
		}
		return baoxiaobillMapper.selectByPrimaryKey(Integer.parseInt(baoxiaoId));
	}
	
	/**
	 * 	提交报销任务
	 */
	@Override
	public void submitBaoxiaoTask(String id, String taskId, String comment,String outcome, String username) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		String processInstanceId = task.getProcessInstanceId();
		Authentication.setAuthenticatedUserId(username);
		taskService.addComment(taskId, processInstanceId, comment);
		//添加分支变量
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("message", outcome);
		taskService.complete(taskId,map);
		
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processDefinitionId(task.getProcessDefinitionId())
				.processInstanceBusinessKey(Constants.Baoxiao_KEY+"."+id)
				.singleResult();
		if (processInstance==null) {
			Baoxiaobill baoxiaobill = baoxiaobillMapper.selectByPrimaryKey(Integer.parseInt(id));
			baoxiaobill.setState(2);
			baoxiaobillMapper.updateByPrimaryKey(baoxiaobill);
		}
	}
	
	/**
	 * 查询任务连线
	 */
	public List<String> findOutComeListByTaskId(String taskId){
		//返回存放连线的名称集合
		List<String> list = new ArrayList<String>();
		//1.使用任务ID，查询任务对象
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		//2.获取流程定义ID
		String processDefintionId = task.getProcessDefinitionId();
		//3.查询ProcessDefinitionEntiy对象
		ProcessDefinitionEntity pdEntity = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processDefintionId);
		//使用任务对象Task获取流程实例ID
		String processInstanceId = task.getProcessInstanceId();
		//使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
		ProcessInstance pi = runtimeService.createProcessInstanceQuery()
			.processInstanceId(processInstanceId).singleResult();
		//获取当前活动的id
		String activityId = pi.getActivityId();
		//获取当前的活动
		ActivityImpl activityImpl = pdEntity.findActivity(activityId);
		//获取当前获得完成之后连线的名称
		List<PvmTransition> pvmList = activityImpl.getOutgoingTransitions();
		if (pvmList!=null && pvmList.size()>0) {
			for (PvmTransition pvm : pvmList) {
				String name = (String) pvm.getProperty("name");
				if (StringUtils.isNotBlank(name)) {
					list.add(name);
				}else {
					list.add("默认提交");
				}
			}
		}
		return list;
	}

	@Override
	public List<Task> findBaoxiaoTaskListByName(String name) {
		List<Task> list = taskService.createTaskQuery().taskAssignee(name)
				.orderByTaskCreateTime().asc().list();
		
		if (list.size()>0) {
			List<Task> levelTasklist = new ArrayList<Task>();
			for (Task task : list) {
				if (task.getProcessDefinitionId().startsWith(Constants.Baoxiao_KEY)) {
					levelTasklist.add(task);
				}
			}
			return levelTasklist;
		}
		return null;
	}
}
