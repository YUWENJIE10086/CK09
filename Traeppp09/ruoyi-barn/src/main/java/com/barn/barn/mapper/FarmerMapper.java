package com.barn.barn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.barn.barn.entity.Farmer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 烟农Mapper
 */
@Mapper
public interface FarmerMapper extends BaseMapper<Farmer> {
}
