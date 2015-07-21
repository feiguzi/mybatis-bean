package com.cyou.fz.db.mybatis.service;

import com.cyou.fz.db.mybatis.bean.Paged;
import com.cyou.fz.db.mybatis.bean.Query;
import com.cyou.fz.db.mybatis.util.ClassUtil;
import com.cyou.fz.db.test.bean.User;
import com.cyou.fz.db.test.service.UserService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath*:application-*.xml"
})
public class BaseServiceTest {

    @Autowired
    UserService userService;

    User user = new User();
    @Before
    public void before() {
        user.setUserName("12");
        user.setCreateTime(new Date());
        user.setUserAccount("hello");
        user.setStatus(1);
        user.setUpdateTime(new Date());
        user.setUserEmail("xx@mail.com");
        userService.insert(user);
    }

    @After
    public  void after() {
        userService.delete(user);
    }

    @org.junit.Test
    public void testCount() throws Exception {
        long count = userService.count(Query.build(User.class));
        Assert.assertTrue(count >= 0);
    }

    @org.junit.Test
    public void testInsert() throws Exception {
        User user = new User();
        try {
            user.setUserName("12");
            user.setCreateTime(new Date());
            user.setUserAccount("hello");
            user.setStatus(1);
            user.setUpdateTime(new Date());
            user.setUserEmail("xx@mail.com");
            userService.insert(user);
        }finally {
            userService.delete(user);
        }
    }

    @org.junit.Test
    public void testGet() throws Exception {
        User dbUser = userService.get(user.getId());
        Assert.assertNotNull(dbUser);
    }

    @org.junit.Test
    public void testGet1() throws Exception {
        User dbUser = userService.get(Query.build(User.class).addEq(User.USER_ID ,user.getId()));
        Assert.assertNotNull(dbUser);
    }



    @org.junit.Test
    public void testUpdate() throws Exception {
        String newName = "hahaha";
        user.setUserName(newName);
        userService.update(user);
        User dbUser = userService.get(user.getId());
        Assert.assertEquals(dbUser.getUserName() ,newName);
    }



    @org.junit.Test
    public void testRemoveByQuery() throws Exception {
        userService.removeByQuery(Query.build(User.class).addEq(User.USER_ID , user.getId()));
        User dbUser = userService.get(user.getId());
        Assert.assertNull(dbUser);
    }

    @org.junit.Test
    public void testFindAll() throws Exception {
        List<User> userList = userService.findAll();
        Assert.assertTrue(userList.size() > 0);
    }

    @org.junit.Test
    public void testFindByQuery() throws Exception {
        List<User> userList = userService.findByQuery(Query.build(User.class).addEq(User.USER_ID, user.getId()));
        Assert.assertEquals(userList.size(), 1);
    }

    @org.junit.Test
    public void testFindPagedByQuery() throws Exception {
        Paged<User> userList = userService.findPagedByQuery(Query.build(User.class).addEq(User.USER_ID, user.getId()).setPaged(1,10));
        Assert.assertEquals(userList.getListData().size()  ,1);
        Assert.assertEquals(userList.getTotalCount()  ,1);
    }

    @org.junit.Test
    public void testFindMapByIds() throws Exception {
        List<User> list = new ArrayList<User>();
        list.add(user);

        List<Integer> idList = ClassUtil.getRefIdList(list , User.USER_ID);
        Assert.assertTrue(userService.findMapByIds(idList , User.class).size() > 0);
    }



    @org.junit.Test
    public void testIsExist() throws Exception {
        boolean exist = userService.isExist(Query.build(User.class).addEq(User.USER_NAME , user.getUserName()) , user.getId());
        Assert.assertFalse(exist);
        exist = userService.isExist(Query.build(User.class).addEq(User.USER_NAME , user.getUserName()) , null);
        Assert.assertTrue(exist);
    }

    @org.junit.Test
    public void testFindPageWithBean() throws Exception {

    }

    @org.junit.Test
    public void testFindPageWithBeans() throws Exception {

    }
}