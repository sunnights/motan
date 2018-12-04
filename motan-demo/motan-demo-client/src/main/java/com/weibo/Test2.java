package com.weibo;

import java.text.SimpleDateFormat;

/**
 * @author sunnights
 */
public class Test2 {

    public static void main(String[] args) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");
        System.out.println(dateFormat.format(1544085145754L));
        System.out.println(dateFormat.format(1544085146899L));
    }
}
