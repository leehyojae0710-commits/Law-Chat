package com.lawchat.domain.chat.entity;

/**
 * DB: chat_messages.role = enum('user','ai')
 * Frontend: ChatMessage.role = "user" | "assistant"
 * 이 enum이 DB 값 <-> 프론트 값 매핑을 전담한다.
 */
public enum ChatRole {

    USER("user", "user"),
    AI("ai", "assistant");

    private final String dbValue;
    private final String frontendValue;

    ChatRole(String dbValue, String frontendValue) {
        this.dbValue = dbValue;
        this.frontendValue = frontendValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String toFrontendValue() {
        return frontendValue;
    }

    public static ChatRole fromFrontendValue(String frontendValue) {
        for (ChatRole role : values()) {
            if (role.frontendValue.equals(frontendValue)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown chat role: " + frontendValue);
    }
}
