package com.polo.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.polo.demo.entity.DemoRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DemoRecordMapper extends BaseMapper<DemoRecordEntity> {
}
