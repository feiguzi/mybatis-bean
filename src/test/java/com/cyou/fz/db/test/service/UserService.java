package com.cyou.fz.db.test.service;


import com.feiguzi.db.mybatis.service.BaseService;
import com.cyou.fz.db.test.bean.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Created by Administrator on 14-3-3.
 */
@Service
public class UserService extends BaseService<User> {
    private final static Logger logger = LoggerFactory.getLogger(UserService.class);

    private final static Integer USER_STATUS_NOMAL = 10;
    private final static Integer USER_STATUS_UNNMAL = 11;


}
