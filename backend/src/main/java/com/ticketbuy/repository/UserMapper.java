package com.ticketbuy.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketbuy.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    default LambdaQueryWrapper<User> selectByUsername(String username) {
        return new LambdaQueryWrapper<User>().eq(User::getUsername, username);
    }
}
