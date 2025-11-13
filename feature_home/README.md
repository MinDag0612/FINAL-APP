# Feature Home

Màn hình Home của ứng dụng Ticketbox mini được tách thành module riêng để dễ mở rộng và tái sử dụng. Dưới đây là mô tả nhanh kiến trúc và cách hoạt động.

## Kiến trúc

- **presentation/HomeActivity**: Activity chính hiển thị UI. Bao gồm:
  - `SwipeRefreshLayout` để kéo là làm mới dữ liệu.
  - `ChipGroup` hiển thị danh mục sự kiện (tự sinh từ dữ liệu).
  - RecyclerView dọc cho danh sách sự kiện (`HomeEventAdapter`) và RecyclerView ngang cho nghệ sĩ theo dõi (`HomeArtistAdapter`).
  - Thẻ hero “Featured event” + thẻ “Vé gần đây”.
- **domain/GetHomeContentUseCase**: Use case trung gian, nhận callback từ repository và trả về `HomeContent`.
- **data/HomeRepository**: Truy vấn Firestore:
  - Lấy người dùng hiện tại (qua `FirebaseAuth` + `UserInfor_API`).
  - Lấy danh sách sự kiện (`Events` + `Tickets_infor`) để biết giá thấp nhất.
  - Lấy đơn hàng gần nhất (`Orders`) và build thông tin vé.

Mọi dữ liệu được gom vào `HomeContent` (models trong `feature_home/model`) để Activity bind một lần.

## Luồng load dữ liệu

1. `HomeActivity` gọi `GetHomeContentUseCase.execute`.
2. Use case ngược xuống `HomeRepository.loadHomeContent`.
3. Repository:
   - Tìm user theo email (Firestore collection `User_Infor`).
   - Query 10 sự kiện mới nhất (`Events`) + giá vé rẻ nhất.
   - Query `Orders` của user để lấy vé gần đây.
4. Khi thành công, Activity:
   - Cập nhật lời chào + avatar initials.
   - Đổ sự kiện vào hero + RecyclerView.
   - Render chip filter tự động (Concert / Workshop …).
   - Đổ nghệ sĩ theo cast trong events.
   - Hiển thị thông tin vé gần nhất (nếu có).

## UI/Resources chính

- `activity_home.xml`: layout tổng.
- `item_home_event.xml`: card sự kiện dạng MaterialCardView.
- `item_home_artist.xml`: chip nghệ sĩ (RecyclerView ngang).
- `item_filter_chip.xml`: layout chip lọc chung.
- Drawable/icon mới nằm trong `feature_home/src/main/res/drawable`.
- Chuỗi tiếng Việt tại `feature_home/src/main/res/values/strings.xml`.

## Dependency

Module sử dụng:

- Firebase Auth + Firestore (đã khai trong `feature_home/build.gradle.kts` và `core`).
- RecyclerView, SwipeRefreshLayout, Material Components (AndroidX).

## Mở rộng

- Nếu cần điều hướng sang chi tiết sự kiện, dùng callback `HomeEventAdapter.EventClickListener` (hiện hiển thị Toast).
- Có thể thêm cache/pagination bằng cách mở rộng `HomeRepository`.
- Khi thêm field mới trong Firestore, cập nhật `StoreField` + map tương ứng trong repository.
