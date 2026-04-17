package com.ticketbuy.config;

import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.TimeUnit;

@Configuration
public class NettyConfig {

    /**
     * Netty 空闲状态处理器
     * 读空闲 60 秒、超时关闭连接
     */
    @Bean
    public IdleStateHandler idleStateHandler() {
        return new IdleStateHandler(60, 60, 0, TimeUnit.SECONDS);
    }
}
