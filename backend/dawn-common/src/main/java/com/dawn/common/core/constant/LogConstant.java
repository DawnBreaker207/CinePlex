package com.dawn.common.core.constant;

public final class LogConstant {

    private LogConstant() {
    }

    public static final class Action {
        public static final String CREATE_USER = "CREATE_USER";
        public static final String UPDATE_INFO = "UPDATE_INFO";
        public static final String UPDATE_STATUS = "UPDATE_STATUS";
        public static final String UPDATE_ROLE = "UPDATE_ROLE";
        public static final String REACTIVATE_USER = "REACTIVATE_USER";
        public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String LOGOUT = "LOGOUT";
        public static final String VOUCHER_CREATE = "VOUCHER_CREATE";
        public static final String VOUCHER_UPDATE = "VOUCHER_UPDATE";
        public static final String VOUCHER_DELETE = "VOUCHER_DELETE";
        public static final String ARTICLE_CREATE = "ARTICLE_CREATE";
        public static final String ARTICLE_UPDATE = "ARTICLE_UPDATE";
        public static final String ARTICLE_DELETE = "ARTICLE_DELETE";

        private Action() {
        }
    }

    public static final class Entity {
        public static final String USER = "USER";
        public static final String VOUCHER = "VOUCHER";
        public static final String ARTICLE = "ARTICLE";

        private Entity() {
        }
    }

    public static final class Status {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILED = "FAILED";

        private Status() {
        }
    }
}