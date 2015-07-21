package com.cyou.fz.db.test.dao;


import com.cyou.fz.db.mybatis.dao.BaseDAO;
import com.cyou.fz.db.test.bean.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * Created by Administrator on 14-3-3.
 */
@Repository
public interface UserDAO extends BaseDAO<User> {

    @Select("select user_name from t_user where user_id = #{userId}")
    public String getUserName(@Param("userId") Integer userId);
}
