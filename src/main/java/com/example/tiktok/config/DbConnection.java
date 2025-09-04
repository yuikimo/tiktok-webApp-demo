package com.example.tiktok.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;

@Component
public class DbConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "root";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
