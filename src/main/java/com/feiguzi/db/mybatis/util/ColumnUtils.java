package com.feiguzi.db.mybatis.util;


import com.feiguzi.db.mybatis.annotations.*;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * User: wangj
 * Date: 13-11-11
 * Time: 下午5:12
 */
public  final class ColumnUtils {

    public final static <M> String getTableName(Class<M> clazz) {
        try {
            Table table = clazz.getAnnotation(Table.class);
            return table.value();
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("table name not set!");
    }

    /**
     * 获取属性列表
     * @param clazz
     * @return
     */
    public static   Field[] getAllFieldArray(Class<?> clazz){
        List<Field> fields = getAllFields(clazz);
        Field[] fieldArray = new Field[fields.size()];
        fields.toArray(fieldArray);
        return fieldArray;
    }

    public static   List<Field> getAllFields(Class<?> clazz){
        List<Field> fields = new ArrayList<Field>();
        while (true) {
            if (clazz.equals(Object.class)) {
                break;
            }

            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isFinal(field.getModifiers())){
                    continue;
                }
                boolean isNotExist = true;
                for (Field temp : fields) {
                    if (temp.getName().equals(field.getName())) {
                        isNotExist =false;
                        break;
                    }
                }

                if (isNotExist){
                    fields.add(field);
                }

            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * 获取标记了id注解的属性名称
     *
     * @param clazz
     * @param <M>
     * @return
     */
    public  static <M> String getIdFieldName(Class<M> clazz) {
        for (Field field : getAllFields(clazz)) {
            Id idField = field.getAnnotation(Id.class);
            if (idField == null) {
                continue;
            }
            return field.getName();
        }
        throw new RuntimeException("@id field not find ");
    }

    public  static <M> String getIdColumnName(Class<M> clazz) {
        for (Field field : getAllFields(clazz)) {
            Id idField = field.getAnnotation(Id.class);
            if (idField == null) {
                continue;
            }
            if (idField.value() != null) {
                return idField.value();
            }
            return getColumnName(field);
        }
        throw new RuntimeException("@id field not find ");
    }


    private static boolean skipField(Class clazz, Field field) {
        try {
            UnColumn unColumn = field.getAnnotation(UnColumn.class);
            if (unColumn != null) {
                return true;
            }

            if (!ClassUtil.isProperty(clazz, field.getName())) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 生成字段名称
     * @param field
     * @return
     */
    public static String getColumnName(Field field) {
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


    public final static Map map = new HashMap<Class,String>();

    static {
        map.put(Integer.class , "integer(11) NOT NULL DEFAULT 0");
        map.put(String.class , "varchar(50) NOT NULL DEFAULT ''");
        map.put(Date.class , "timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'");
    }

    public static String generateTableDLL(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();

        sb.append("CREATE TABLE ").append(getTableName(clazz)).append("(").append("\n");
        String idName = getIdFieldName(clazz);
        for (Field field : getAllFields(clazz)) {
            if (skipField(clazz , field)) {
                continue;
            }

            if (idName.equals(field.getName())) {
                sb.append("\t").append(getColumnName(field)).append(" ").append("integer not null").append(" ").append(" primary key  AUTO_INCREMENT,").append("\n");
            }else {
                sb.append("\t").append(getColumnName(field)).append(" ").append(map.get(field.getType())).append(",").append("\n");
            }
        }

        String dll = sb.toString().substring(0 , sb.toString().lastIndexOf(",") );
        return (dll + "\n)engine=innodb default charset=utf8;").toLowerCase();
    }

}
