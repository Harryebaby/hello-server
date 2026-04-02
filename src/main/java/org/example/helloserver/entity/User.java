package org.example.helloserver.entity;

public class User {
    private Long id;
    private String name;
    private Integer age;

    // 无参构造 (Command + N -> Constructor -> Select None)
    public User() {}

    // 全参构造 (Command + N -> Constructor -> 全选)
    public User(Long id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    // Getter 和 Setter (Command + N)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}