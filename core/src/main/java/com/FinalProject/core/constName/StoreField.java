package com.FinalProject.core.constName;

public class StoreField {

    // 🔹 Tên các collection chính
    public static final String USER_INFOR = "User_Infor";
    public static final String EVENTS = "Events";
    public static final String TICKETS_INFOR = "Tickets_infor";
    public static final String ORDERS = "Orders";
    public static final String REVIEWS = "review";

    // 🔹 Field trong collection User_Infor
    public static class UserFields {
        public static final String FULLNAME = "fullname";
        public static final String PHONE = "phone";
        public static final String EMAIL = "email";
        public static final String ROLE = "role";
    }

    // 🔹 Field trong collection Events
    public static class EventFields {
        public static final String EVENT_NAME = "event_name";
        public static final String ORGANIZER_UID = "organizer_uid";
        public static final String EVENT_DATE = "event_date";
        public static final String EVENT_LOCATION = "event_location";
    }

    // 🔹 Field trong collection Tickets_infor
    public static class TicketFields {
        public static final String TICKETS_CLASS = "tickets_class";
        public static final String TICKETS_PRICE = "tickets_price";
        public static final String TICKETS_QUANTITY = "tickets_quantity";
        public static final String TICKETS_SOLD = "tickets_sold";
    }

    // 🔹 Field trong collection Orders
    public static class OrderFields {
        public static final String USER_ID = "user_id";
        public static final String TOTAL_PRICE = "total_price";
        public static final String IS_PAID = "is_paid";
        public static final String TICKET_ITEMS = "ticket_items";
        public static final String PAYMENT_METHOD = "payment_method";
    }

    public static class ReviewFields {
        public static final String UID = "uid";
        public static final String RATE = "rate";
        public static final String COMMENT = "comment";
        public static final String CREATED_AT = "created_at";
    }

}
