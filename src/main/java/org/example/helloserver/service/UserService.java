package org.example.helloserver.service;

import org.example.helloserver.common.Result;
import org.example.helloserver.dto.UserDTO;

public interface UserService {
    Result<String> register(UserDTO userDTO);

    Result<String> login(UserDTO userDTO);

    Result<String> getUserById(Long id);
}
