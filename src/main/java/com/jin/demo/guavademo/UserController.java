package com.jin.demo.guavademo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private CacheUtil cacheUtil;

    @GetMapping("/user")
    public String getUser(String id){
        User user = cacheUtil.getUserById(id);
        return user.toString();
    }
}
