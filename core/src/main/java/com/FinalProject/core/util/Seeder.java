package com.FinalProject.core.util;

import com.FinalProject.core.model.Orders;
import com.FinalProject.core.model.TicketInfor;

public class Seeder {
    public static void runSeed(){
        Event_API.addEventForUser(
                "523h0011@student.tdtu.edu.vn",
                "Live Concert Sơn Tùng",
                "Đêm nhạc đặc biệt tại Hà Nội",
                "2025-12-10T19:00:00Z",
                "2025-12-10T22:00:00Z",
                "Sơn Tùng M-TP",
                "SVĐ Mỹ Đình",
                "Music"
        );

        Order_API.addOrder(
                "tonminhdang9@gmail.com",
                "Live Concert Sơn Tùng",
                "VIP",
                2,
                "QR"
        );

        TicketS_Infor_API.addTicketInfor(
                "523h0011@student.tdtu.edu.vn",
                "VIP",
                "Live Concert Sơn Tùng",
                200,
                800000,
                150
        );

    }
}
