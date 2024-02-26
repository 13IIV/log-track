package com.example.demo.controller;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.example.demo.service.FeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;

@RestController
@Slf4j
public class TestController {
    
    @Autowired
    private FeignService feignService;
    
    @GetMapping("/hello")
    public String hello() {
        log.info("================demo:it's a info===================");
        log.warn("================demo:warning!!!====================");
        log.error("===============demo:error!!!======================");
        return feignService.test();
    }
    
    //定义线程池
    private ThreadFactory myThreadFactory = new ThreadFactoryBuilder().setNamePrefix("my-pool-%d").build();
    
    private ExecutorService fixedThreadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors()*2,
            Runtime.getRuntime().availableProcessors()*40,0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(Runtime.getRuntime().availableProcessors() * 20),myThreadFactory);
    
    @GetMapping("/hello2") 
    private String testAsync() {
        log.info("==========================demo:日志打印============================");
        fixedThreadPool.execute(() -> {
            log.info("======================demo:异步执行================================");
            try {
                String s = null;
                int a = Integer.parseInt(s);
            } catch (Exception e) {
                log.error("=========================异步执行错误:{}========================",e);
            }
        });
        return "success";
    }
}
