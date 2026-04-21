package org.example.helloserver.service;

import org.example.helloserver.common.Result;
import org.example.helloserver.dto.UserDTO;
import org.example.helloserver.entity.UserInfo;
import org.example.helloserver.vo.UserDetailVO;

public interface UserService {
    Result<String> register(UserDTO userDTO);

    Result<String> login(UserDTO userDTO);

    Result<String> getUserById(Long id);

    Result<Object> getUserPage(Integer pageNum, Integer pageSize);

    Result<UserDetailVO> getUserDetail(Long userId);

    Result<String> updateUserInfo(UserInfo userInfo);

    Result<String> deleteUser(Long userId);
}
