package com.ticketbuy.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketbuy.entity.TicketType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTypeMapper extends BaseMapper<TicketType> {
}
