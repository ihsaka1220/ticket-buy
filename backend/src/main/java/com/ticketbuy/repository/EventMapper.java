package com.ticketbuy.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketbuy.entity.Event;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventMapper extends BaseMapper<Event> {
}
