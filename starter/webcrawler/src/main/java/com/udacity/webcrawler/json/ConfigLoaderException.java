package com.udacity.webcrawler.json;

public class ConfigLoaderException extends Exception {
    
    public ConfigLoaderException(String message){
        super(message);
    }

    public ConfigLoaderException(String message, Throwable throwable){
        super(message, throwable);
    }
}
