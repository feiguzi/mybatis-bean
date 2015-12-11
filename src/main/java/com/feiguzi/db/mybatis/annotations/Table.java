package com.feiguzi.db.mybatis.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * User: wangj
 * Date: 13-11-8
 * Time: 上午9:33
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Table {
    String value() default "";

    /**
     * 是否分片,默认是否.
     *
     * @return
     */
    boolean isShard() default false;

    String shardParam() default "";

    int shardNum() default 0;

}
