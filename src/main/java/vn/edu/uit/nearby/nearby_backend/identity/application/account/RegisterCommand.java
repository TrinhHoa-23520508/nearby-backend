package vn.edu.uit.nearby.nearby_backend.identity.application.account;

public record RegisterCommand(String email, String password, String displayName, String clientIp) {

}
