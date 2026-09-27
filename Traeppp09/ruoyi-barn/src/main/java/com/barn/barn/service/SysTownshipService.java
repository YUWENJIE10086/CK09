package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.barn.barn.entity.SysTownship;
import com.barn.barn.mapper.SysTownshipMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysTownshipService {

    @Autowired
    private SysTownshipMapper sysTownshipMapper;

    public List<SysTownship> listAll(String countyCode) {
        LambdaQueryWrapper<SysTownship> wrapper = new LambdaQueryWrapper<>();
        if (countyCode != null && !countyCode.isEmpty()) {
            wrapper.eq(SysTownship::getCountyCode, countyCode);
        }
        wrapper.orderByAsc(SysTownship::getTownshipCode);
        return sysTownshipMapper.selectList(wrapper);
    }
}
