package com.cams;

import com.cams.repository.UserRepository;

public class Main {

    public static void main(String[] args) {

        UserRepository repository = new UserRepository();

        repository.createUser(
                "TEST001",
                "mypassword",
                "STUDENT"
        );
    }
}