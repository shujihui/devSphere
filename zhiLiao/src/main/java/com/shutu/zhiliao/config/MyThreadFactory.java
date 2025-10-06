package com.shutu.zhiliao.config;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ThreadFactory;

/**
 * 线程工厂
 *
 * @author cong
 * @date 2024/02/18
 */
@Slf4j
@AllArgsConstructor
public class MyThreadFactory implements ThreadFactory {

    private final ThreadFactory factory;

    @Override
    public Thread newThread(@NotNull Runnable r) {
        Thread thread = factory.newThread(r);
        thread.setName("MyThreadFactory-" + thread.getName());
        return thread;
    }
}
