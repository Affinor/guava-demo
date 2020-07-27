package com.jin.demo.guavademo;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author wangjin
 */
@Service
public class CacheUtil {

    private LoadingCache<String, Object> cache;

    @Autowired
    private UserDao userDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @PostConstruct
    public void init(){
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(60,TimeUnit.SECONDS)
                .build(new CacheLoader<String, Object>() {
            //读取数据源
            @Override
            public Object load(String s) throws Exception {
                //添加分布式锁，防止缓存击穿
                RLock lock = redissonClient.getLock(s);
                lock.lock();
                //先从redis中找
                Object o = redisTemplate.opsForValue().get(s);
                if (null != o) {
                    return o;
                }
                //如果找不到，再去mysql
                User user = userDao.findById(Integer.parseInt(s)).get();
                redisTemplate.opsForValue().set(s,user,60, TimeUnit.SECONDS);
                //释放锁
                lock.unlock();
                return user;
            }
        });
        //初始化本地缓存
        for (int i = 1; i <= 3; i++) {
            try {
                cache.get(String.valueOf(i));
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    public User getUserById(String id) {
        try {
            return (User) cache.get(id, new Callable<User>() {
                @Override
                public User call() throws Exception {
                    //添加分布式锁，防止缓存击穿
                    RLock lock = redissonClient.getLock(id);
                    lock.lock();
                    //先从redis中找
                    User o = (User) redisTemplate.opsForValue().get(id);
                    if (null != o) {
                        return o;
                    }
                    //如果找不到，再去mysql
                    User user = userDao.findById(Integer.parseInt(id)).get();
                    redisTemplate.opsForValue().set(id,user,60, TimeUnit.SECONDS);
                    //释放锁
                    lock.unlock();
                    return user;
                }
            });
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
