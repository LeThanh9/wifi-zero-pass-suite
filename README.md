# WiFi Zero-Pass Suite & Scanner (Android)

Ứng dụng quét sóng và kết nối mạng Wi-Fi tự động trên Android (API 26 - 34+), tích hợp các phương thức kết nối không cần nhập mật khẩu thủ công theo chuẩn Wi-Fi Alliance và Android Native Framework.

---

## 🚀 Các Tính Năng Chính (Làm Được Gì?)

### 1. Dò tìm và phân tích sóng Wi-Fi thời gian thực (Scanner)
- **Đo cường độ tín hiệu (RSSI):** Tự động tính toán tỷ lệ phần trăm (0 - 100%) và mã màu cảnh báo trực quan (Xanh: Tốt, Vàng: Trung bình, Đỏ: Yếu).
- **Phân loại băng tần:** Nhận diện mạng 2.4 GHz, 5 GHz và 6 GHz (Wi-Fi 6E).
- **Phân tích giao thức bảo mật:** Xác định tự động Open, WEP, WPA, WPA2-Personal, WPA3, và 802.1x EAP.
- **Phát hiện mạng có hỗ trợ WPS:** Đánh dấu các Access Point cho phép kích hoạt ghép nối vật lý.

### 2. Các phương thức kết nối không cần gõ mật khẩu (Zero-Pass)
1. **Kết nối mạng Mở (Open / Unsecured Network):**
   - Tự động nhận diện các mạng không cài khóa.
   - Nhấn "Kết Nối Ngay" để hệ thống tự tạo `WifiNetworkSuggestion` và kích hoạt bắt tay vào mạng không cần bất kỳ bước cấu hình nào.
2. **Quét mã QR Camera tự động cấu hình:**
   - Tích hợp camera quét mã QR chuẩn `WIFI:T:...;S:...;P:...;;` (thường dán tại quán cà phê, khách sạn hoặc chia sẻ từ máy khác).
   - Tự động trích xuất thông tin, mã hóa cấu hình bảo mật vào Android OS và kết nối tức thì mà người dùng không cần nhìn thấy hay gõ ký tự mật khẩu.
3. **Wi-Fi Easy Connect (DPP - Device Provisioning Protocol):**
   - Hỗ trợ chuẩn kết nối thế hệ mới của Wi-Fi Alliance (trên Android 10+ thay thế WPS truyền thống).
   - Xác thực an toàn bằng cặp khóa công khai / URI mã hóa mà không truyền mật khẩu thô.
4. **Passpoint (Hotspot 2.0 / 802.11u):**
   - Đăng ký hồ sơ xác thực dịch vụ chuyển vùng (roaming) tự động cho các điểm phát Wi-Fi công cộng đối tác hoặc nhà mạng mà không cần nhập lại mật khẩu.
5. **Cơ sở dữ liệu điểm phát chia sẻ (Community Hotspots Matching):**
   - Tự động đối chiếu tên mạng (SSID) với danh mục các điểm phát công cộng / chuỗi quán phổ biến để ghép nối tự động 1-chạm.
6. **Hỗ trợ Captive Portal (Mạng chào công cộng):**
   - Nhận diện khi kết nối vào mạng miễn phí yêu cầu xác nhận điều khoản đăng nhập và tự động chuyển hướng màn hình kích hoạt.

---

## 🛠 Hướng Dẫn Cài Đặt & Biên Dịch (Build APK)

### Cách 1: Tự động qua GitHub Actions (Khuyến nghị)
1. Dự án đã cấu hình workflow `.github/workflows/build_apk.yml`.
2. Mỗi khi code được push lên kho lưu trữ GitHub, máy chủ đám mây Ubuntu sẽ tự động tải môi trường Java 17 và biên dịch ra file APK debug.
3. Vào tab **Actions** trên GitHub repository → chọn lượt build mới nhất → tải file **WiFi-ZeroPass-Suite-APK.zip** về điện thoại và cài đặt.

### Cách 2: Biên dịch cục bộ bằng Android Studio
1. Mở thư mục `wifi_apk/` bằng Android Studio (Hedgehog hoặc mới hơn).
2. Chờ Gradle đồng bộ thư viện (Sync Project with Gradle Files).
3. Chọn **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**.
4. File APK được tạo tại: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔒 Cơ Chế Bảo Mật & Tuân Thủ Chuẩn Kỹ Thuật
- Ứng dụng hoạt động hoàn toàn theo các API chuẩn mực của Android Native WLAN Subsystem (`WifiManager`, `ConnectivityManager`, `WifiNetworkSuggestion`).
- Không can thiệp root trái phép, không tấn công deauth hay bẻ khóa xâm nhập hệ thống, đảm bảo tuân thủ tiêu chuẩn an ninh mạng quốc tế.

