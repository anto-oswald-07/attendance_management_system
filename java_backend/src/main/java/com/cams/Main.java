package com.cams;

import com.cams.repository.UserRepository;

public class Main {

    public static void main(String[] args) {
        UserRepository userRepository = new UserRepository();

        userRepository.createUser("Oswald","password", "STUDENT");   

        userRepository.getAllUsers();
    }
}