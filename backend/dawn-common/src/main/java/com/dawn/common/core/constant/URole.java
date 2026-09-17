package com.dawn.common.core.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum URole {
    USER(1),
    STAFF(2),
    MANAGER(3),
    ADMIN(4),
    OWNER(5);

    private final int level;
}
