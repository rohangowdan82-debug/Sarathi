package com.sarathi.api.auth;
public record AuthOutcome(boolean rejected,String accessToken,String refreshToken,UserView user) {
 public static AuthOutcome denied(){return new AuthOutcome(true,null,null,null);}
}
