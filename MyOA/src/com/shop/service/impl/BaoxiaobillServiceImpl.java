package com.shop.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shop.mapper.BaoxiaobillMapper;
import com.shop.pojo.Baoxiaobill;
import com.shop.pojo.BaoxiaobillExample;
import com.shop.service.BaoxiaobillService;

@Service("baoxiaobillService")
public class BaoxiaobillServiceImpl implements BaoxiaobillService {

	@Autowired
	private BaoxiaobillMapper baoxiaobillMapper;
	
	@Override
	public void saveBaoxiaoBill(Baoxiaobill baoxiaobill) {
		baoxiaobillMapper.insert(baoxiaobill);
	}

	@Override
	public List<Baoxiaobill> selectByUserId(Long userId) {
		BaoxiaobillExample example = new BaoxiaobillExample();
		BaoxiaobillExample.Criteria cri = example.createCriteria();
		cri.andUserIdEqualTo(Integer.parseInt(String.valueOf(userId)));
		List<Baoxiaobill> list = baoxiaobillMapper.selectByExample(example);
		return list;
	}

	@Override
	public Baoxiaobill selectById(String id) {
		return baoxiaobillMapper.selectByPrimaryKey(Integer.parseInt(id));
	}

	@Override
	public int deleteById(String id) {
		return baoxiaobillMapper.deleteByPrimaryKey(Integer.parseInt(id));
	}

}
