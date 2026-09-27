package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.barn.barn.entity.FundManagement;
import com.barn.barn.mapper.FundManagementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FundManagementService {

    @Autowired
    private FundManagementMapper fundManagementMapper;

    public List<FundManagement> listAll(Integer fundYear, String countyCode) {
        LambdaQueryWrapper<FundManagement> wrapper = new LambdaQueryWrapper<>();
        if (fundYear != null) {
            wrapper.eq(FundManagement::getFundYear, fundYear);
        }
        if (countyCode != null && !countyCode.isEmpty()) {
            wrapper.eq(FundManagement::getCountyCode, countyCode);
        }
        wrapper.orderByDesc(FundManagement::getFundYear);
        return fundManagementMapper.selectList(wrapper);
    }

    public FundManagement getById(Long id) {
        return fundManagementMapper.selectById(id);
    }

    public int save(FundManagement fund) {
        return fundManagementMapper.insert(fund);
    }

    public int update(FundManagement fund) {
        return fundManagementMapper.updateById(fund);
    }

    public int delete(Long id) {
        return fundManagementMapper.deleteById(id);
    }
}
