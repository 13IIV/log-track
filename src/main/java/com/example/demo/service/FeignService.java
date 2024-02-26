package com.example.demo.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "demo2",url = "http://127.0.0.1:8002/demo")
public interface FeignService {
    
    @GetMapping("/test")
    String test();
}
