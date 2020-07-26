package com.jin.demo.guavademo;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author wangjin
 */
public interface UserDao extends JpaRepository<User,Integer> {
}
