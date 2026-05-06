package org.example.helloserver.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.helloserver.common.Result;
import org.example.helloserver.common.ResultCode;
import org.example.helloserver.dto.UserDTO;
import org.example.helloserver.entity.User;
import org.example.helloserver.entity.UserInfo;
import org.example.helloserver.mapper.UserInfoMapper;
import org.example.helloserver.mapper.UserMapper;
import org.example.helloserver.security.JwtUtil;
import org.example.helloserver.service.UserService;
import org.example.helloserver.vo.UserDetailVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final String CACHE_KEY_PREFIX = "user:detail:";

    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper, UserInfoMapper userInfoMapper,
                           StringRedisTemplate redisTemplate, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.userInfoMapper = userInfoMapper;
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
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

        return Result.success(jwtUtil.generateToken(dbUser.getUsername()));
    }

    @Override
    public Result<String> getUserById(Long id) {
        User dbUser = userMapper.selectById(id);
        if (dbUser == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        return Result.success("查询成功：id=" + dbUser.getId() + ", username=" + dbUser.getUsername());
    }

    @Override
    public Result<Object> getUserPage(Integer pageNum, Integer pageSize) {
        Page<User> pageParam = new Page<>(pageNum, pageSize);
        Page<User> resultPage = userMapper.selectPage(pageParam, null);
        return Result.success(resultPage);
    }

    @Override
    public Result<UserDetailVO> getUserDetail(Long userId) {
        String key = CACHE_KEY_PREFIX + userId;

        // 1. 先查缓存
        String json = redisTemplate.opsForValue().get(key);
        if (json != null && !json.isBlank()) {
            try {
                UserDetailVO cacheVO = JSONUtil.toBean(json, UserDetailVO.class);
                return Result.success(cacheVO);
            } catch (Exception e) {
                // 缓存数据异常，删掉脏缓存，继续查数据库
                redisTemplate.delete(key);
            }
        }

        // 2. 查数据库
        UserDetailVO detail = userInfoMapper.getUserDetail(userId);
        if (detail == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        // 3. 写缓存
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(detail), 10, TimeUnit.MINUTES);

        return Result.success(detail);
    }

    @Override
    @Transactional
    public Result<String> updateUserInfo(UserInfo userInfo) {
        if (userInfo == null || userInfo.getUserId() == null) {
            return Result.error(ResultCode.ERROR);
        }

        LambdaUpdateWrapper<UserInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserInfo::getUserId, userInfo.getUserId());
        userInfoMapper.update(userInfo, updateWrapper);

        // 删除旧缓存
        redisTemplate.delete(CACHE_KEY_PREFIX + userInfo.getUserId());

        return Result.success("更新成功！");
    }

    @Override
    @Transactional
    public Result<String> deleteUser(Long userId) {
        userMapper.deleteById(userId);

        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUserId, userId);
        userInfoMapper.delete(wrapper);

        redisTemplate.delete(CACHE_KEY_PREFIX + userId);

        return Result.success("删除成功！");
    }
}
