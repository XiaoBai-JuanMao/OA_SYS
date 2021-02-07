package com.shop.service;

import java.util.List;

import com.shop.pojo.Leavebill;

public interface LeavebillService {
	//保存请假单数据
	void saveLeaveBill(Leavebill leavebill);

	//根据用户id查询请假单
	List<Leavebill> selectByUserId(Long userId);

	//根据请假单id查询请假单
	Leavebill selectById(String id);

	//根据id删除请假单
	int deleteById(String id);
}
