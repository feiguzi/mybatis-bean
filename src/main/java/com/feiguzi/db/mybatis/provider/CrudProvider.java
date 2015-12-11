package com.feiguzi.db.mybatis.provider;



import com.feiguzi.db.mybatis.annotations.*;
import com.feiguzi.db.mybatis.bean.Query;
import com.feiguzi.db.mybatis.bean.SqlQuery;
import com.feiguzi.db.mybatis.util.ClassUtil;
import com.feiguzi.db.mybatis.util.ColumnUtils;
import com.feiguzi.db.mybatis.util.HashUtils;
import com.feiguzi.db.mybatis.util.ObjectUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.jdbc.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认的CRUD
 * User: wangj
 * Date: 13-11-8
 * Time: 上午9:32
 */
public class CrudProvider<T> {
    private static final Logger logger = LoggerFactory.getLogger(CrudProvider.class);

    final static Pattern pattern = Pattern.compile("#\\{(\\w+)\\}");

    public String findBySqlQuery(SqlQuery query) {
        String sql = query.getSql();
        sql = addParamPrefix(sql, SqlQuery.PARAMS_FIELD_NAME, query.getParams());
        if (query.getOffset() >= 0) {
            sql = sql + " limit #{offset},#{pageSize}";
        }
        if (logger.isDebugEnabled()) {
            logger.debug("sql is:" + sql);
        }
        return sql;
    }

    private String addParamPrefix(String subQuery, String prefix, List<Object> list) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = pattern.matcher(subQuery);

        int paramNum = 0;
        while (matcher.find()) {
            StringBuilder builder = new StringBuilder();
            builder.append("#{");
            builder.append(prefix).append("[").append(paramNum).append("]");
            builder.append("}");

            matcher.appendReplacement(sb, builder.toString());
            paramNum++;
        }
        if (paramNum != list.size()) {
            throw new RuntimeException("传入参数个数不正确,需要不为空参数个数为:" + paramNum);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }


    public String countBySqlQuery(SqlQuery sqlQuery) {
        String sql = sqlQuery.getSql().toLowerCase();


        //去除select xxx 到 from的部分
        int fromIndex = sql.indexOf("from");
        if (fromIndex != -1) {
            sql = sql.substring(fromIndex);
        }
        if (StringUtils.isNotEmpty(sqlQuery.getDistinctParam())) {
            sql = "select count(distinct " + sqlQuery.getDistinctParam() + ")" + sql;
        } else {
            sql = "select count(*) " + sql;
        }

        //去除group部分
        int groupIndex = sql.indexOf("group by");
        if (groupIndex != -1) {
            sql = sql.substring(0, groupIndex);
        }

        //去除limit部分
        int limitIndex = sql.indexOf("limit");
        if (limitIndex != -1) {
            sql = sql.substring(0, limitIndex);
        }
        sql = addParamPrefix(sql, SqlQuery.PARAMS_FIELD_NAME, sqlQuery.getParams());
        if (logger.isDebugEnabled()) {
            logger.debug("sql is:" + sql);
        }
        return sql;
    }


    /**
     * 执行查询
     *
     * @param query
     * @return
     * @throws Exception
     */
    public String findByQuery(Query<T> query) throws Exception {
        T obj = ObjectUtil.toBean(query.getType(), query.getParams());
        ObjectUtil.addToBean(obj, query.getType(), query.getParamLikes());
        ObjectUtil.addToBean(obj, query.getType(), query.getLtParams());
        ObjectUtil.addToBean(obj, query.getType(), query.getGtParams());
        return findPageByObject(obj, query);
    }


    /**
     * 执行删除操作
     */
    public String deleteByQuery(Query<T> query) throws Exception {
        T obj = ObjectUtil.toBean(query.getType(), query.getParams());
        ObjectUtil.addToBean(obj, query.getType(), query.getParamLikes());
        ObjectUtil.addToBean(obj, query.getType(), query.getLtParams());
        ObjectUtil.addToBean(obj, query.getType(), query.getGtParams());

        SQL sql = new SQL();
        sql.DELETE_FROM(obtainTableName(obj));

        List<String> whereConditions = buildWhereCommands(obj, query);
        if (whereConditions.size() == 0) {
            throw new RuntimeException("condition is empty,can't delete");
        }

        for (String str : whereConditions) {
            sql.WHERE(str);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("sql is:" + sql.toString());
        }
        return sql.toString();
    }

    /**
     * 执行统计,用于分页
     *
     * @param query
     * @return
     * @throws Exception
     */
    public String count(Query<T> query) throws Exception {
        T obj = ObjectUtil.toBean(query.getType(), query.getParams());
        ObjectUtil.addToBean(obj, query.getType(), query.getParamLikes());
        ObjectUtil.addToBean(obj, query.getType(), query.getLtParams());
        ObjectUtil.addToBean(obj, query.getType(), query.getGtParams());

        return count(obj, query);

    }


    private String buildColumnCommand(Query query, Field field) {
        String expression = query.getExpression(field.getName());
        String objPrefix = query.getExpressionParam(field.getName());
        if (objPrefix == null) {
            return (getColumnName(field) + expression + "#{" + field.getName() + "}");
        } else {
            return (getColumnName(field) + expression + "#{" + objPrefix + "." + field.getName() + "}");
        }
    }

    private List<String> buildInCommands(Query<T> query) {
        List<String> list = new ArrayList<String>();

        for (String fieldName : query.getInArrayParams().keySet()) {
            if (ObjectUtil.isEmpty(query.getInArrayParams().get(fieldName))) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(getColName(query.getType(), fieldName));
            sb.append(" in (");

            StringBuilder innerStr = new StringBuilder();
            for (int i = 0; i < query.getInArrayParams().get(fieldName).size(); i++) {
                innerStr.append("#{").append("inArrayParams.").append(fieldName).append("[").append(i).append("]}").append(",");
            }
            sb.append(innerStr.substring(0, innerStr.length() - 1)).append(")");
            list.add(sb.toString());
        }

        return list;
    }

    private List<String> buildBetweenCommands(Query<T> query) {
        List<String> list = new ArrayList<String>();

        for (String fieldName : query.getBetweens().keySet()) {
            if (query.getBetweens().get(fieldName).getBegin() == null ||
                    query.getBetweens().get(fieldName).getEnd() == null) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(getColName(query.getType(), fieldName));
            sb.append(" between ");
            sb.append(" #{").append("betweens.").append(fieldName).append(".begin}");
            sb.append(" and ");
            sb.append(" #{").append("betweens.").append(fieldName).append(".end}");
            list.add(sb.toString());
        }

        return list;
    }

    private List<String> buildWhereCommands(T obj, Query<T> query) throws Exception {
        List<String> list = new ArrayList<String>();
        list.addAll(buildBetweenCommands(query));
        list.addAll(buildInCommands(query));

        if (obj == null) {
            return list;
        }

        for (Field field : ColumnUtils.getAllFields(obj.getClass())) {
            if (skipField(obj, field)) {
                continue;
            }

            list.add(buildColumnCommand(query, field));
        }
        return list;
    }


    public String count(T obj, Query<T> query) throws Exception {
        SQL sql = new SQL();

        sql.SELECT("count(1)");
        sql.FROM(obtainTableName(obj));

        for (String str : buildWhereCommands(obj, query)) {
            sql.WHERE(str);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(sql.toString());
        }
        return sql.toString();
    }

    /**
     * 查询对象
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public String findPageByObject(T obj, Query<T> query) throws Exception {
        SQL sql = new SQL();

        StringBuilder cols = new StringBuilder();
        for (Field field : ColumnUtils.getAllFields(obj.getClass())) {
            if (!ClassUtil.isProperty(obj.getClass(), field.getName())) {
                continue;
            }

            if (field.getAnnotation(UnColumn.class) != null) {
                continue;
            }


            if (!query.hasOrder()) {
                String sortField = getSortColumnName(field);
                if (sortField != null) {
                    sql.ORDER_BY(sortField + " DESC");
                }
            } else {
                Query.DBOrder dbOrder = query.getOrders().get(field.getName());
                if (dbOrder != null) {
                    sql.ORDER_BY(getColumnName(field) + " " + dbOrder.getName());
                }
            }

            if (query.isSearchAllField() || query.isFieldSearch(field.getName())) {
                String columnName = getColumnName(field);
                cols.append(columnName + " as " + field.getName() + ",");
            }

        }

        sql.SELECT(cols.toString().substring(0, cols.length() - 1));
        sql.FROM(obtainTableName(obj));

        for (String str : buildWhereCommands(obj, query)) {
            sql.WHERE(str);
        }

        String sqlCommand;
        if (query.getOffset() >= 0) {
            sqlCommand = sql.toString() + " limit #{offset},#{pageSize}";
        } else {
            sqlCommand = sql.toString();
        }
        if (logger.isDebugEnabled()) {
            logger.debug(sqlCommand);
        }
        return sqlCommand;
    }


    /**
     * 保存对象
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public String save(T obj) throws Exception {
        String sql;
        if (!isIdExist(obj)) {
            sql = insert(obj);
        } else {
            sql = update(obj);
        }
        return sql;
    }

    /**
     * 更新对象
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public String update(T obj) throws Exception {
        SQL sql = new SQL();
        sql.UPDATE(obtainTableName(obj));

        String idName = "";
        String idFieldName = "";
        for (Field field : ColumnUtils.getAllFields(obj.getClass())) {
            Id idField = field.getAnnotation(Id.class);
            if (idField != null) {
                idName = idField.value();
                idFieldName = field.getName();
                continue;
            }
            if (skipField(obj, field)) {
                continue;
            }

            sql.SET(getColumnName(field) + "=#{" + field.getName() + "}");
        }

        sql.WHERE(idName + "=#{" + idFieldName + "}");

        if (logger.isDebugEnabled()) {
            logger.debug(sql.toString());
        }
        return sql.toString();
    }

    /**
     * 删除对象
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public String insert(T obj) throws Exception {
        SQL sql = new SQL();
        sql.INSERT_INTO(obtainTableName(obj));
        StringBuilder cols = new StringBuilder();
        StringBuilder values = new StringBuilder();
        Field[] fields = ColumnUtils.getAllFieldArray(obj.getClass());
        for (Field field : fields) {
            if (skipField(obj, field)) {
                continue;
            }

            values.append("#{" + field.getName() + "},");
            cols.append(getColumnName(field) + ",");
        }

        sql.VALUES(cols.toString().substring(0, cols.length() - 1),
                values.toString().substring(0, values.length() - 1));
        if (logger.isDebugEnabled()) {
            logger.debug(sql.toString());
        }
        return sql.toString();
    }

    public <M> String getColName(Class<M> clazz, String fieldName) {
        for (Field field : ColumnUtils.getAllFields(clazz)) {
            String columnName = getColumnName(field);
            if (fieldName == field.getName()) {
                return columnName;
            }
        }
        throw new RuntimeException("fieldName :" + fieldName + " not find ");
    }


    private String getColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        SortColumn sortColumn = field.getAnnotation(SortColumn.class);
        Id idField = field.getAnnotation(Id.class);
        if (column != null && !StringUtils.isEmpty(column.value())) {
            return column.value();
        }
        if (sortColumn != null && !StringUtils.isEmpty(sortColumn.value())) {
            return sortColumn.value();
        } else if (idField != null && !StringUtils.isEmpty(idField.value())) {
            return idField.value();
        } else {
            // 约定：sysId > SYS_ID
            String propName = field.getName();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < propName.length(); i++) {
                char c = propName.charAt(i);
                if (Character.isUpperCase(c)) {
                    builder.append('_').append(c);
                } else {
                    builder.append(Character.toUpperCase(c));
                }
            }
            return builder.toString();
        }
    }

    private String getSortColumnName(Field field) {
        SortColumn sortColumn = field.getAnnotation(SortColumn.class);
        if (sortColumn != null) {
            if (StringUtils.isEmpty(sortColumn.value())) {
                return field.getName();
            } else {
                return sortColumn.value();
            }
        }
        return null;
    }


    private boolean isIdExist(T obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            Id idField = field.getAnnotation(Id.class);
            if (idField != null) {
                try {
                    String value = BeanUtils.getProperty(obj, field.getName());
                    if (value != null) {
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("get id prop error!", e);
                }
            }
        }
        return false;
    }

    public String obtainTableName(Object object) {
        Table table = object.getClass().getAnnotation(Table.class);
        if (table != null) {
            //判断是否是分片的.
            if (table.isShard()) {
                int tableSuffix = 0;
                if (table.shardNum() > 0) {
                    Field fields[] = ColumnUtils.getAllFieldArray(object.getClass());
                    Object value = new Object();

                    Field.setAccessible(fields, true);
                    for (int i = 0; i < fields.length; i++) {

                        if (fields[i].getName().equalsIgnoreCase(table.shardParam())) {
                            try {
                                value = fields[i].get(object);
                                break;
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("undefine POJO @Table, need Tablename(@Table)");
                            }

                        }
                    }
                    tableSuffix = HashUtils.execute(table.shardNum(), value);
                }
                if (tableSuffix != 0) {
                    return table.value() + "_" + tableSuffix;
                }else{
                    return  table.value() + "_" + 1;
                }
            }
            return table.value();
        } else {
            throw new RuntimeException("undefine POJO @Table, need Tablename(@Table)");
        }
    }



    private boolean skipField(T obj, Field field) {
        try {
            UnColumn unColumn = field.getAnnotation(UnColumn.class);
            if (unColumn != null) {
                return true;
            }

            if (!ClassUtil.isProperty(obj.getClass(), field.getName())) {
                return true;
            }

            String value = BeanUtils.getProperty(obj, field.getName());
            if (value == null) {
                return true;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
