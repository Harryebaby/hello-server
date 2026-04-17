package org.example.helloserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.helloserver.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
