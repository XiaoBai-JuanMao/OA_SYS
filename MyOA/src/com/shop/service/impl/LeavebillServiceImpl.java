package com.shop.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shop.mapper.LeavebillMapper;
import com.shop.pojo.Leavebill;
import com.shop.pojo.LeavebillExample;
import com.shop.service.LeavebillService;

@Service("leavebillService")
public class LeavebillServiceImpl implements LeavebillService {

	@Autowired
	private LeavebillMapper leavebillMapper;
	
	@Override
	public void saveLeaveBill(Leavebill leavebill) {
		leavebillMapper.insert(leavebill);
	}

	@Override
	public List<Leavebill> selectByUserId(Long userId) {
		LeavebillExample example = new LeavebillExample();
		LeavebillExample.Criteria cri = example.createCriteria();
		cri.andUserIdEqualTo(userId);
		List<Leavebill> list = leavebillMapper.selectByExample(example);
		return list;
	}

	@Override
	public Leavebill selectById(String id) {
		return leavebillMapper.selectByPrimaryKey(Long.parseLong(id));
	}

	@Override
	public int deleteById(String id) {
		return leavebillMapper.deleteByPrimaryKey(Long.parseLong(id));
	}

}
