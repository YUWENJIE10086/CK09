package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.barn.barn.entity.BarnComponent;
import com.barn.barn.mapper.BarnComponentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BarnComponentService {

    @Autowired
    private BarnComponentMapper barnComponentMapper;

    public List<BarnComponent> listByOvenId(String ovenId) {
        LambdaQueryWrapper<BarnComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BarnComponent::getOvenId, ovenId);
        wrapper.orderByDesc(BarnComponent::getScore);
        return barnComponentMapper.selectList(wrapper);
    }
}
