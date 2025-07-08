package com.ycbd.demo.utils;

import cn.hutool.crypto.digest.BCrypt;

public class BCryptHashGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("用法: java BCryptHashGenerator <明文密码>");
            return;
        }
        String plain = args[0];
        String hash = BCrypt.hashpw(plain);
        System.out.println("明文: " + plain);
        System.out.println("BCrypt加密: " + hash);
    }
}
