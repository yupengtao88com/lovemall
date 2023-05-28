package cn.sc.love.product.service.impl;

import cn.sc.love.product.service.TestService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author YPT
 * @create 2023-05-27-23:03
 */
@Service
public class TestServiceImpl implements TestService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地锁的演示
     */
//    @Override
//    public synchronized void testLock() {
//        String value = stringRedisTemplate.opsForValue().get("num");
//
//        if (StringUtils.isBlank(value)) {
//            return;
//        }
//        int num = Integer.parseInt(value);
//
//        stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
//    }
    @Override
    public void testLock() {
        //生成uuid
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");

        //随便设置一个键充当锁(后续优化后用uuid充当)
//        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", "111");

        //设置超时时间    (不是原子操作，可能在设置之前出现问题)
//        stringRedisTemplate.expire("lock",7, TimeUnit.SECONDS);


        //优化：原子操作
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 7, TimeUnit.SECONDS);
        //从redis中获取锁
        if (lock) {
            String value = stringRedisTemplate.opsForValue().get("num");

            if (StringUtils.isBlank(value)) {
                return;
            }
            int num = Integer.parseInt(value);

            stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
//            释放锁,
//            存在的问题：如果设置了锁的超时，在程序执行过程中，其中某一
//            个线程超时被释放了锁，就会导致锁的混乱释放,
//            导致相当于没有锁控制
//            解决：
//            UUid方案
//            1.在获取锁之前，获取一个uuid唯一值，目的是在获取锁时，添加标识
//            2，在获取锁时，作为value存储
//            3，释放锁时，判断本地uuid和存储到锁对应的值的uuid是否一致

//            衍生问题：防止误删除的操作，不是原子性


            //Lua脚本
            String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                    "then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
            defaultRedisScript.setScriptText(script);
            defaultRedisScript.setResultType(Long.class);


            //key 和value匹配时才删除
            stringRedisTemplate.execute(defaultRedisScript, Arrays.asList("lock"), uuid);

            String ruuid = stringRedisTemplate.opsForValue().get("lock");

            if (uuid.equals(ruuid)) {
                stringRedisTemplate.delete("lock");
            }


        } else {
            try {
                //自旋
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }

}
