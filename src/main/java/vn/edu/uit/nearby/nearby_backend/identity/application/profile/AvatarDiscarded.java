package vn.edu.uit.nearby.nearby_backend.identity.application.profile;

/**
 * Internal to the identity module: an avatar was replaced or removed and its object can
 * be deleted once the change is committed.
 */
public record AvatarDiscarded(String objectKey) {

}
