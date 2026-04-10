package org.example.helloserver.service.impl;

import org.example.helloserver.common.Result;
import org.example.helloserver.common.ResultCode;
import org.example.helloserver.dto.UserDTO;
import org.example.helloserver.service.UserService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private static final Map<String, String> USER_DB = new HashMap<>();

    @Override
    public Result<String> register(UserDTO userDTO) {
        if (USER_DB.containsKey(userDTO.getUsername())) {
            return Result.error(ResultCode.USER_HAS_EXISTED);
        }

        USER_DB.put(userDTO.getUsername(), userDTO.getPassword());
        return Result.success("注册成功！");
    }

    @Override
    public Result<String> login(UserDTO userDTO) {
        if (!USER_DB.containsKey(userDTO.getUsername())) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        String dbPassword = USER_DB.get(userDTO.getUsername());
        if (!dbPassword.equals(userDTO.getPassword())) {
            return Result.error(ResultCode.PASSWORD_ERROR);
        }

        return Result.success("Bearer " + UUID.randomUUID());
    }
}
