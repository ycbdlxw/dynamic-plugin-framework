package com.ycbd.demo.utils;

import cn.hutool.crypto.digest.BCrypt;

public class BCryptHashGenerator {

    public static void main(String[] args) {
       
        String plain = "ycbd1234";
        String hash = BCrypt.hashpw(plain);
        System.out.println("明文: " + plain);
        System.out.println("BCrypt加密: " + hash);
    }
}
