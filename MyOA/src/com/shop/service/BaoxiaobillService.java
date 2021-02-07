package com.shop.service;

import java.util.List;

import com.shop.pojo.Baoxiaobill;

public interface BaoxiaobillService {
	void saveBaoxiaoBill(Baoxiaobill baoxiaobill);
	List<Baoxiaobill> selectByUserId(Long userId);
	Baoxiaobill selectById(String id);
	int deleteById(String id);
}
