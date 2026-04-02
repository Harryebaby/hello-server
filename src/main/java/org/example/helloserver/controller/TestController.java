package org.example.helloserver.controller;

import org.example.helloserver.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public Result<String> test() {
        return Result.success("这是一个需要携带 Authorization 才能访问的测试接口");
    }
}