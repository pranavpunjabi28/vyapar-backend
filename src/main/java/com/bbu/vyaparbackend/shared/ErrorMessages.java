package com.bbu.vyaparbackend.shared;

public final class ErrorMessages {
    private ErrorMessages() {
    }

    public static final class Codes {
        public static final String NOT_FOUND = "not_found";
        public static final String FORBIDDEN = "forbidden";
        public static final String CONFLICT = "conflict";
        public static final String INVALID_REQUEST = "invalid_request";
        public static final String VALIDATION_FAILED = "validation_failed";
        public static final String DATA_CONFLICT = "data_conflict";
        public static final String CONCURRENT_UPDATE = "concurrent_update";
        public static final String STORAGE_UNAVAILABLE = "storage_unavailable";

        private Codes() {
        }
    }

    public static final class Messages {
        public static final String FORBIDDEN = "You do not have permission for this operation";
        public static final String VALIDATION_FAILED = "One or more fields are invalid";
        public static final String DATA_CONFLICT = "The operation conflicts with existing data";
        public static final String CONCURRENT_UPDATE = "The record changed while this request was being processed; reload and try again";
        public static final String MALFORMED_REQUEST = "The request body or uploaded file is invalid";
        public static final String INVALID_SORT = "Unsupported sort field";

        private Messages() {
        }
    }
}
