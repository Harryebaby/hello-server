package org.example.helloserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.helloserver.common.Result;
import org.example.helloserver.common.ResultCode;
import org.example.helloserver.dto.UserDTO;
import org.example.helloserver.entity.User;
import org.example.helloserver.mapper.UserMapper;
import org.example.helloserver.service.UserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Result<String> register(UserDTO userDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userDTO.getUsername());
        User dbUser = userMapper.selectOne(queryWrapper);
        if (dbUser != null) {
            return Result.error(ResultCode.USER_HAS_EXISTED);
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        userMapper.insert(user);
        return Result.success("注册成功！");
    }

    @Override
    public Result<String> login(UserDTO userDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userDTO.getUsername());
        User dbUser = userMapper.selectOne(queryWrapper);
        if (dbUser == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        if (!dbUser.getPassword().equals(userDTO.getPassword())) {
            return Result.error(ResultCode.PASSWORD_ERROR);
        }

        return Result.success("Bearer " + UUID.randomUUID());
    }

    @Override
    public Result<String> getUserById(Long id) {
        User dbUser = userMapper.selectById(id);
        if (dbUser == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        return Result.success("查询成功：id=" + dbUser.getId() + ", username=" + dbUser.getUsername());
    }
}
