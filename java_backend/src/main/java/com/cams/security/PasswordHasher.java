package com.cams.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher{

    public static String hashPassword(String password){
        String salt = BCrypt.gensalt(12);
        return BCrypt.hashpw(password, salt);
    }

    public static boolean verifyPassword(String password, String hashedPassword){
        return BCrypt.checkpw(password, hashedPassword);
    }

}