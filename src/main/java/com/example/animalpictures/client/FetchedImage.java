package com.example.animalpictures.client;

/** Raw image as returned by an external picture API. */
public record FetchedImage(String sourceUrl, String contentType, byte[] data) {
}
