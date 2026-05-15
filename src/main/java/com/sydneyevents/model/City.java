package com.sydneyevents.model;

public record City(
        String slug,
        String name,
        String state,
        double latitude,
        double longitude,
        String timezone,
        String heroImage
) {
}
