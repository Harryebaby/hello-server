package org.example.helloserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;

    // 无参构造 (Command + N -> Constructor -> Select None)
    public User() {}

    // 全参构造 (Command + N -> Constructor -> 全选)
    public User(Long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    // Getter 和 Setter (Command + N)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
