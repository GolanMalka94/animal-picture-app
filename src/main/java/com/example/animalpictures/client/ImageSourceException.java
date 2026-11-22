package com.example.animalpictures.client;

/** Thrown when an external picture API is unreachable or returns something that is not an image. */
public class ImageSourceException extends RuntimeException {

    public ImageSourceException(String message) {
        super(message);
    }

    public ImageSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
