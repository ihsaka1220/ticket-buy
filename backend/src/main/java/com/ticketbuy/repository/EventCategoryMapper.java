package com.ticketbuy.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketbuy.entity.EventCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventCategoryMapper extends BaseMapper<EventCategory> {
}
