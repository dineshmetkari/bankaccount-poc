package com.poc.android.bankaccount.authentication;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("UnusedDeclaration")
public class AuthResponse {
    private static final String TAG = "AuthResponse";

    /**
     * Java POJO, Gson marshaled from a login request
     *
     * {"access_token":"e881b435-1e2f-4f8f-81f5-21274a522af8",
     * "token_type":"bearer",
     * "refresh_token":"0f2d5cf9-d047-4f73-8c77-75e8296fb2e5",
     * "expires_in":5002253,
     * "scope":" write read"}
     */

    @Expose
    @SerializedName("access_token")
    private String accessToken;
    @Expose
    @SerializedName("token_type")
    private String tokenType;
    @Expose
    @SerializedName("refresh_token")
    private String refreshToken;
    @Expose
    @SerializedName("expires_in")
    private long expiresIn; //milliseconds
    @Expose
    @SerializedName("scope")
    private String scope;

    private Exception error;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", scope='" + scope + '\'' +
                ", error=" + error +
                '}';
    }
}
