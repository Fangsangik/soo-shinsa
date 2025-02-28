package com.Soo_Shinsa.global.utils;

import com.Soo_Shinsa.global.auth.UserDetailsImp;
import com.Soo_Shinsa.user.model.User;
import org.springframework.security.core.userdetails.UserDetails;

public class UserUtils {

    public static User getUser(UserDetails userDetails) {
        return ((UserDetailsImp) userDetails).getUser();
    }
}
