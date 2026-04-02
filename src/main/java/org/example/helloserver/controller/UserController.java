package org.example.helloserver.controller;

import org.example.helloserver.common.Result;
import org.example.helloserver.entity.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // 1. 获取用户信息 (查)
    @GetMapping("/{id}")
    public Result<String> getUser(@PathVariable("id") Long id) {
        String data = "查询成功,正在返回 ID 为 " + id + " 的用户信息";
        return Result.success(data);
    }

    // 2. 新增用户 (增)
    @PostMapping
    public Result<String> createUser(@RequestBody User user) {
        return Result.success("新增成功,接收到用户:" + user.getName() + ",年龄:" + user.getAge());
    }

    // 3. 全量更新用户信息 (改)
    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable("id") Long id, @RequestBody User user) {
        return Result.success("更新成功,ID " + id + " 的用户已修改为:" + user.getName());
    }

    // 4. 删除用户 (删)
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable("id") Long id) {
        return Result.success("删除成功,已移除 ID 为 " + id + " 的用户");
    }
}