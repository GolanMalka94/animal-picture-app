package com.example.animalpictures.service;

public class PictureNotFoundException extends RuntimeException {

    public PictureNotFoundException(String message) {
        super(message);
    }
}
