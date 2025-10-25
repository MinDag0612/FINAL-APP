final_app/
│
├── app/                     # Module chính (entry point)
│   ├── src/main/java/...    # MainActivity, Navigation host
│   ├── src/main/res/        # Resource gốc (theme, icon,...)
│   └── build.gradle.kts
│
├── core/                    # Module chứa logic & utils dùng chung
│   ├── src/main/java/...    # Network, Repository base, Model base,...
│   └── build.gradle.kts
│
├── feature_login/           # Module feature (VD: Đăng nhập)
│   ├── src/main/java/...    # LoginActivity / Fragment
│   ├── src/main/res/        # Layout + strings riêng của feature
│   └── build.gradle.kts
│
└── settings.gradle.kts      # Liệt kê module

````````````````````````````````````````````````````````
settings.gradle.kts

Khai báo tất cả module trong project:
rootProject.name = "final_app"
include(":app")
include(":core")
include(":feature_login")

````````````````````````````````````````````````````````
app/build.gradle.kts

Thêm dependencies để app nhận biết các module:
dependencies {
    implementation(project(":core"))
    implementation(project(":feature_login"))

    implementation(libs.appcompat)
    implementation(libs.material)
}
````````````````````````````````````````````````````````

2. Tạo module feature mới
Khi muốn team thêm một chức năng (VD: Register hay Profile), mỗi dev làm module riêng để tránh xung đột.

Cách tạo:

Vào File → New → New Module → Android Library

Đặt tên: feature_register

Trong settings.gradle.kts, thêm:

include(":feature_register")


Trong app/build.gradle.kts, thêm dependency:

implementation(project(":feature_register"))
````````````````````````````````````````````````````````

3. Cấu trúc trong mỗi feature

Ví dụ: feature_login

feature_login/
├── src/main/java/com/example/feature_login/
│   ├── data/                # Repository / API cho riêng feature
│   ├── ui/                  # Activity / Fragment / ViewModel
│   ├── di/                  # (tuỳ chọn) Cấu hình dependency injection
│   └── LoginActivity.java
│
└── src/main/res/layout/     # layout_login.xml, string.xml, v.v.
````````````````````````````````````````````````````````

4. Cách gọi feature từ app

Từ MainActivity trong module :app, bạn có thể mở feature bằng Intent:

Intent intent = new Intent(this, com.example.feature_login.ui.LoginActivity.class);
startActivity(intent);
````````````````````````````````````````````````````````

5. Module core

core là nơi chứa logic dùng chung giữa các feature:

Models (User, Product, v.v.)
API Service base
Repository base
Utils (DateTime, Logger,...)
Constants (AppConfig,...)

Ví dụ:

core/
└── src/main/java/com/example/core/
    ├── network/ApiService.java
    ├── utils/Logger.java
    └── model/User.java
````````````````````````````````````````````````````````

7. Chạy app chính

app là module khởi chạy chính (launcher)

MainActivity có intent-filter để chạy khi bấm Run

Các feature chỉ là library module → không có launcher riêng