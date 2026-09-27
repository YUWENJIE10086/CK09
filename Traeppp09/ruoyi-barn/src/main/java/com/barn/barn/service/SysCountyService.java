package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.barn.barn.entity.SysCounty;
import com.barn.barn.mapper.SysCountyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysCountyService {

    @Autowired
    private SysCountyMapper sysCountyMapper;

    public List<SysCounty> listAll(String cityCode) {
        LambdaQueryWrapper<SysCounty> wrapper = new LambdaQueryWrapper<>();
        if (cityCode != null && !cityCode.isEmpty()) {
            wrapper.eq(SysCounty::getCityCode, cityCode);
        }
        wrapper.orderByAsc(SysCounty::getCountyCode);
        return sysCountyMapper.selectList(wrapper);
    }

    public SysCounty getByCode(String code) {
        return sysCountyMapper.selectById(code);
    }
}
