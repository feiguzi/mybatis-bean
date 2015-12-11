package com.feiguzi.db.mybatis.service;

import com.feiguzi.db.mybatis.bean.Paged;
import com.feiguzi.db.mybatis.bean.Query;
import com.feiguzi.db.mybatis.bean.SqlQuery;
import com.feiguzi.db.mybatis.dao.BaseDAO;
import com.feiguzi.db.mybatis.util.ClassUtil;
import com.feiguzi.db.mybatis.util.ColumnUtils;
import com.feiguzi.db.mybatis.util.ObjectUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;

import static java.util.Locale.ENGLISH;

/**
 * User: wangj
 * Date: 13-11-19
 * Time: 上午9:13
 */
public class BaseService<T> implements ApplicationContextAware {
    private final static Logger logger = LoggerFactory.getLogger(BaseService.class);

    private Class<T> t;
    private ApplicationContext applicationContext;

    public BaseService() {
        Class<T> type = ClassUtil.getActualType(this.getClass());

        if (type == null) {
            throw new RuntimeException("继承类没有加泛型!");
        }

        this.t = type;

    }


    public int count(Query query) {
        return getDAO().count(query);
    }

    /**
     * 插入对象
     *
     * @param obj
     * @return
     */
    public T insert(T obj) {
        try {
            getDAO().insert(obj);
            return obj;
        } catch (Exception e) {
            logger.error("db op error!" ,e);
            throw new RuntimeException("插入数据失败");
        }
    }

    /**
     * 获取单个
     *
     * @param id
     * @return
     */
    public T get(Integer id) {
        if (id == null) {
            return null;
        }

        try {
            Query query = Query.build(t);
            query.addEq(ColumnUtils.getIdFieldName(t), id);
            List<T> objects = findByQuery(query);
            if (objects.size() > 0) {
                return objects.get(0);
            }
        } catch (Exception e) {
            logger.error("db op error!" ,e);
        }

        return null;
    }


    public T get(Query query) {
        List<Map<String, Object>> list = getDAO().findByQuery(query);
        List<T> objects = ObjectUtil.toBeanList(t, list);
        if (objects.size() > 0) {
            return objects.get(0);
        }

        return null;
    }

    public void delete(int id) {
        try {
            Query query = Query.build(t);
            query.addEq(ColumnUtils.getIdFieldName(t), id);
            getDAO().deleteByQuery(query);
        } catch (Exception e) {
            logger.error("db op error!" ,e);
            throw new RuntimeException("删除数据失败,id:"+id);
        }
    }

    public void update(T obj) {
        try {
            getDAO().update(obj);
        } catch (Exception e) {
            logger.error("db op error!" ,e);
            throw new RuntimeException("更新数据失败");
        }
    }

    public void delete(T obj) {
        try {
            String idName = ColumnUtils.getIdFieldName(t);
            Integer id = Integer.valueOf(BeanUtils.getProperty(obj ,idName));
            delete(id);
        } catch (Exception e) {
            logger.error("db op error!" ,e);
            throw new RuntimeException("删除数据失败");
        }
    }

    public void removeByQuery(Query query) {
        try {
            getDAO().deleteByQuery(query);
        } catch (Exception e) {
            logger.error("db op error!" ,e);
            throw new RuntimeException("删除数据失败");
        }
    }

    /**
     * 查询所有的
     *
     * @return
     */
    public List<T> findAll() {
        try {
            Query query = Query.build(t);
            List<T> objects = findByQuery(query);
            return objects;
        } catch (Exception e) {
            logger.error("db op error!" ,e);
        }

        return Collections.emptyList();
    }


    public List<T> findByQuery(Query query) {
        try {
            List<Map<String, Object>> list = getDAO().findByQuery(query);
            List<T> objects = ObjectUtil.toBeanList(t, list);
            return objects;
        } catch (Exception e) {
            logger.error("db op error!" ,e);
        }

        return Collections.emptyList();
    }

    /**
     * 获取分页数据
     *
     * @param query
     * @return
     */
    public Paged<T> findPagedByQuery(Query query) {
        List<T> objects = findByQuery(query);
        int count = getDAO().count(query);
        return new Paged<T>(objects, count, query.getPageNo(), query.getPageSize());
    }


    public BaseDAO<T> getDAO() {
        String daoName = lowerTop(t.getSimpleName()) + "DAO";
        if (applicationContext.containsBean(daoName)) {
            Object dao = applicationContext.getBean(daoName);
            if (dao != null) {
                return (BaseDAO<T>) dao;
            } else {
                logger.error("com.cyou.fz.db.mybatis.bean not exist by name:" + daoName);
            }

        } else {
            logger.error("com.cyou.fz.db.mybatis.bean not exist by name:" + daoName);
        }
        return null;
    }


    /**
     * Returns a String which capitalizes the first letter of the string.
     */
    public static String lowerTop(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        name = name.replace("PO", "");
        return name.substring(0, 1).toLowerCase(ENGLISH) + name.substring(1);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public   List<Map<String, Object>> findMapByIds(List<Integer> idList , Class clazz) {
        if (idList.size() > 1000) {
            throw  new RuntimeException("id size too large,now is :"+idList.size());
        }
        if (idList.size() == 0) {
            return Collections.emptyList();
        }


        String idName  = ColumnUtils.getIdFieldName(clazz);
        Query query = Query.build(clazz);
        query.addIn(idName , idList);
        return getDAO().findByQuery(query);
    }

    public List<Integer> getListMapIds(List<Map<String , Object>> objList , String fieldName) {
        List<Integer> idList = new ArrayList<Integer>();

        for (Map<String , Object> obj : objList) {
            Integer id = null;
            try {
                id = (Integer)obj.get(fieldName) ;
            } catch (Exception e) {
                e.printStackTrace();
            }
            idList.add(id);
        }
        return idList;
    }

    /**
     * 检测是否使用
     * @param query
     * @param oid
     */
    public boolean isExist(Query query , Integer oid ) {
        T type =  get(query);
        if (type == null ) {
            return false;
        }

        try {
            Integer id = Integer.valueOf(BeanUtils.getProperty(type, ColumnUtils.getIdFieldName(t))) ;
            if (oid != null && oid.equals(id)) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 依赖其他表字段的时候可以用,采用id方式查询
     * @param query
     * @param refIdFieldName 关联id
     * @param includeNames 关联类所需要查询的字段
     * @param clazz 关联的类
     * @param retClazz 返回类型
     * @param <M>
     * @return
     */
    public <M> Paged<M> findPageWithBean(Query query, String refIdFieldName ,String[] includeNames,  Class clazz ,Class<M> retClazz) {
        return findPageWithBeans(query,
                OtherBeansRef.buildRefs(new String[]{refIdFieldName},new Class[]{clazz} , new String[][]{includeNames}) , retClazz);
    }

    /**
     * 使用sql语句查询分页
     *
     * @param sql
     * @param retClass
     * @param pageNo
     * @param pageSize
     * @param params
     * @param <M>
     * @return
     */
    public <M> Paged<M> findPageBySql(String sql, Class<M> retClass, Integer pageNo, Integer pageSize, Object... params) {
        SqlQuery sqlQuery = new SqlQuery(sql, pageNo, pageSize, params);

        List<Map<String, Object>> list = getDAO().findBySqlQuery(sqlQuery);
        List<M> objects = ObjectUtil.toBeanExtList(retClass, list);

        int totalCount = getDAO().countBySqlQuery(sqlQuery);
        return new Paged<M>(objects, totalCount, pageNo, pageSize);
    }

    /**
     * 多表依赖
     * @param query
     * @param refs
     * @param retClazz
     * @param <M>
     * @return
     */
    public <M> Paged<M> findPageWithBeans(Query query , OtherBeansRef[] refs ,Class<M> retClazz) {
        try{
            List<Map<String, Object>> mainList = getDAO().findByQuery(query);
            List<List<Map<String, Object>>> subLists = new ArrayList<List<Map<String, Object>>>();

            for (OtherBeansRef otherBeansRef : refs) {
                subLists.add(findMapByIds(getListMapIds(mainList , otherBeansRef.getRefIdFieldName()) , otherBeansRef.getClazz()));
            }


            for (Map<String , Object> map : mainList) {
                for (Integer i = 0; i< subLists.size() ;i++ ) {
                    OtherBeansRef ref = refs[i];

                    String idName  = ColumnUtils.getIdFieldName(ref.getClazz());

                    for (Map<String, Object> subMap : subLists.get(i)) {
                        Object val = map.get(ref.getRefIdFieldName());

                        if (val == null) {
                            continue;
                        }

                        Object idVal = subMap.get(idName);
                        if (val.equals(idVal)) {
                            Map<String,Object> tmpMap = new HashMap<String, Object>();
                            for (String key : ref.includeNames) {
                                tmpMap.put(key, subMap.get(key));
                            }
                            map.putAll(tmpMap);
                        }
                    }
                }
            }
            List<M> objects = ObjectUtil.toBeanList(retClazz, mainList);
            int count = getDAO().count(query);
            return new Paged<M>(objects, count, query.getPageNo(), query.getPageSize());
        }catch (Exception e) {
            e.printStackTrace();
            return new Paged<M>();
        }
    }

    public static class OtherBeansRef{
        /**
         * 本表关联其他表字段,如:templateId
         */
        String refIdFieldName;
        /**
         * 关联的类
         */
        Class clazz;
        /**
         * 需要拷贝的字段
         */
        String[] includeNames;

        public static OtherBeansRef[] buildRefs(String[] fieldNames , Class[] classes , String[][] includeNames) {
            if (fieldNames.length != classes.length) {
                return new OtherBeansRef[0];
            }

            OtherBeansRef[] refs = new OtherBeansRef[fieldNames.length];
            for (int i= 0; i< fieldNames.length ; i++) {
                refs[i] = new OtherBeansRef(fieldNames[i] , classes[i] , includeNames[i]);
            }
            return refs;
        }


        public OtherBeansRef(String fieldName, Class clazz ,String[] includeNames) {
            this.refIdFieldName = fieldName;
            this.clazz = clazz;
            this.includeNames = includeNames;
        }

        public String getRefIdFieldName() {
            return refIdFieldName;
        }

        public void setRefIdFieldName(String refIdFieldName) {
            this.refIdFieldName = refIdFieldName;
        }

        public Class getClazz() {
            return clazz;
        }

        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }

        public String[] getIncludeNames() {
            return includeNames;
        }

        public void setIncludeNames(String[] includeNames) {
            this.includeNames = includeNames;
        }
    }
}
