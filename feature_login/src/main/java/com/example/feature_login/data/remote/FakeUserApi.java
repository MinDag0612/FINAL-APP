package com.example.feature_login.data.remote;

import com.example.feature_login.domain.models.Account;
import com.example.feature_login.domain.models.UserInfor;

import java.util.ArrayList;
import java.util.HashMap;

public class FakeUserApi {

    ArrayList<Account> dataAccount = new ArrayList<>(){{
        add(new Account(1, "admin", "admin", "organizer"));
        add(new Account(2, "user", "user", "attendee"));
    }};

    ArrayList<UserInfor> dataInfor = new ArrayList<>(){{
        add(new UserInfor(1, "admin", "admin@gmail.com", "012345", "admin123", "organizer"));
        add(new UserInfor(1, "user", "user@gmail.com", "543210", "user123", "attendee"));
    }};


    public Account login(String username, String password, String role){
        for(Account account : dataAccount){
            if(account.getUsername().equals(username) && account.getPassword().equals(password) && account.getRole().equals(role)){
                return account;
            }
        }
        return null;
    }

    public UserInfor signup(String username, String email, String phone, String password, String role){
        for(UserInfor infor : dataInfor){
            if (infor.getEmail().equals(email)){
                return null;
            }
        }
        UserInfor newUser = new UserInfor(dataInfor.size() + 1, username, email, phone, password, role);
        Account newAccount = new Account(dataAccount.size() + 1, username, password, role);

        dataAccount.add(newAccount);
        dataInfor.add(newUser);
        return newUser;
    }

}
