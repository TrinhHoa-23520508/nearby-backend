package vn.edu.uit.nearby.nearby_backend.identity.application.profile;

import org.jspecify.annotations.Nullable;

import vn.edu.uit.nearby.nearby_backend.shared.geo.GeoPoint;

/**
 * @param homeArea null clears the default area
 */
public record UpdateProfileCommand(String displayName, @Nullable GeoPoint homeArea) {

}
