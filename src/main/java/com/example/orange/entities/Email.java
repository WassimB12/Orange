package com.example.orange.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Email {
    private int id;
    private String sender;
    private String receiver;
    private String fes;
    private String result;
    private Date date;
    private String couloir;
    private int couloirID;

    // Constructor, getters, and setters
}

