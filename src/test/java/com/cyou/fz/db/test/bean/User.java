package com.cyou.fz.db.test.bean;



import com.cyou.fz.db.mybatis.annotations.Id;
import com.cyou.fz.db.mybatis.annotations.Table;
import com.cyou.fz.db.mybatis.util.ColumnUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

/**
 * Created by Administrator on 14-3-3.
 */
@Table("t_user")
public class User extends BaseBean implements Serializable{
    public static final Integer STATUS_ENABLED = 10;

    public static final Integer STATUS_DISABLED = 11;
    public static final String USER_ID = "id";
    public static final String USER_ACCOUNT = "userAccount";
    public static final String USER_STATUS = "status";
    public static final String USER_CREATE_TIME = "createTime";
    public static final String USER_UPDATE_TIME = "updateTime";
    public static final String USER_NAME = "userName";


    @Id("user_id")
    private Integer id;

    private String userAccount;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private String userName;

    private String userEmail;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }


    public static void main(String[] args) {
        for (Field field : ColumnUtils.getAllFields(User.class)){
            System.out.println("filed name is:" + field);
        }

        for (Field field : ColumnUtils.getAllFieldArray(User.class)){
            System.out.println("filed name is:"+field);
        }
    }

}
