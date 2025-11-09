
project-root/
│
├── app/                # Entry point của ứng dụng, chứa google-services.json
│
── core/                           # Module dùng chung (base cho toàn app)
│   ├── firebase/                   # Cấu hình và lớp tiện ích Firebase
│   │   ├── FirebaseAuthHelper.java
│   │   ├── FirebaseFirestoreHelper.java
│   │   └── FirebaseStorageHelper.java
│   │
│   ├── model/                      # Các lớp dữ liệu dùng chung
│   │   ├── User.java
│   │   ├── Product.java
│   │   └── Result.java
│   │
│   │
│   └── build.gradle                 # Chứa logic hoặc cấu hình dùng chung (Firebase, model, util,...)
│
└── feature_login/      # Một feature riêng biệt (ví dụ: Đăng nhập)
    ├── data/           # Giao tiếp với Firebase hoặc các nguồn dữ liệu khác
    ├── domain/         # Chứa logic nghiệp vụ (UseCase)
    └── presentation/   # Giao diện, Activity, ViewModel,...

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

feature_login/      # Một feature riêng biệt (ví dụ: Đăng nhập)
    ├── data/           # Giao tiếp với Firebase hoặc các nguồn dữ liệu khác
    ├── domain/         # Chứa logic nghiệp vụ (UseCase)
    └── presentation/
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
└── firebase/
    ├── FirebaseAuthHelper.java
    └── ...
````````````````````````````````````````````````````````

7. Chạy app chính

app là module khởi chạy chính (launcher)

MainActivity có intent-filter để chạy khi bấm Run

Các feature chỉ là library module → không có launcher riêng