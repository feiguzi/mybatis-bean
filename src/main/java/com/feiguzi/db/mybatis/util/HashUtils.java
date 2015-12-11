/**
 *    系统名称：17173内容管理系统.
 *    作者：陈宇翔
 *    (C) Copyright cyou corporation 2014  All Rights Reserved. 
 *    注意： 本内容仅限于软件公司内部使用，禁止转发
 */
package com.feiguzi.db.mybatis.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * com.cyou.fz.cms.core.mybatis.com.cyou.fz.db.mybatis.util.
 * <p>User:宇翔
 * <p>Date:14-4-16  上午10:47
 */
public class HashUtils {
    private TreeMap<Long, Object> nodes = null;
    //真实服务器节点信息
    private List<Object> shards = new ArrayList();
    //设置虚拟节点数目
    private int VIRTUAL_NUM = 4;

    /**
     * 初始化一致环
     */
    public void init(int num) {

        for (int i = 0; i < num; i++) {
            int temp = i;
            shards.add("_" + (temp + 1));
        }

        nodes = new TreeMap<Long, Object>();
        for (int i = 0; i < shards.size(); i++) {
            Object shardInfo = shards.get(i);
            for (int j = 0; j < VIRTUAL_NUM; j++) {
                nodes.put(hash(computeMd5("SHARD-" + i + "-NODE-" + j), j), shardInfo);
            }
        }
    }

    /**
     * 根据key的hash值取得服务器节点信息
     *
     * @param hash
     * @return
     */
    public Object getShardInfo(long hash) {
        Long key = hash;
        SortedMap<Long, Object> tailMap = nodes.tailMap(key);
        if (tailMap.isEmpty()) {
            key = nodes.firstKey();
        } else {
            key = tailMap.firstKey();
        }
        return nodes.get(key);
    }

    /**
     * 打印圆环节点数据
     */
    public void printMap() {
        System.out.println(nodes);
    }

    /**
     * 根据2^32把节点分布到圆环上面。
     *
     * @param digest
     * @param nTime
     * @return
     */
    public long hash(byte[] digest, int nTime) {
        long rv = ((long) (digest[3 + nTime * 4] & 0xFF) << 24)
                | ((long) (digest[2 + nTime * 4] & 0xFF) << 16)
                | ((long) (digest[1 + nTime * 4] & 0xFF) << 8)
                | (digest[0 + nTime * 4] & 0xFF);

        return rv & 0xffffffffL; /* Truncate to 32-bits */
    }

    /**
     * Get the md5 of the given key.
     * 计算MD5值
     */
    public byte[] computeMd5(String k) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = k.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + k, e);
        }

        md5.update(keyBytes);
        return md5.digest();
    }

    public static void main(String[] args) {

        for (int i = 0; i < 50; i++) {
            System.out.println(HashUtils.execute(5, String.valueOf(i)));
        }
    }


    public static int execute(int num, Object params) {
        int i = params.hashCode();
        return (i % num);
    }


}

