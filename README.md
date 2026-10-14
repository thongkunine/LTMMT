# ChatApplication — Ứng dụng Chat Client–Server bằng Java

> Đồ án môn **Lập trình mạng máy tính**. Xây dựng ứng dụng trò chuyện nhiều người dùng trên nền tảng Java, sử dụng mô hình Client–Server và giao diện Java Swing (Apache NetBeans GUI Builder).

## 1. Giới thiệu

ChatApplication là dự án học tập nhằm thực hành lập trình socket, trao đổi thông điệp, xử lý nhiều client đồng thời và lưu trữ dữ liệu. Mã nguồn được tổ chức theo các nhóm `client`, `server`, `common` và `database`.

**Repository:** https://github.com/thongkunine/LTMMT

## 2. Chức năng

Các thành phần có trong mã nguồn dự án:

- Đăng ký, đăng nhập tài khoản.
- Chat nhóm, quản lý phòng và danh sách người dùng trực tuyến.
- Nhắn tin riêng giữa các người dùng.
- Lưu và truy xuất lịch sử tin nhắn qua SQL Server.
- Gửi và nhận tệp.
- Gọi thoại và gọi video; có các thành phần TCP/UDP phục vụ truyền thông.
- Xử lý kết nối và ngắt kết nối client.

> **Lưu ý:** Danh sách phản ánh phạm vi chức năng của dự án; hãy kiểm thử từng chức năng trên nhánh nộp bài trước khi xác nhận tất cả hoạt động ổn định.

## 3. Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java |
| IDE | Apache NetBeans |
| Hệ thống build | Apache Ant (`build.xml`) |
| Giao diện | Java Swing, JFrame Form (`.java` và `.form`) |
| Truyền thông | TCP Socket, UDP cho các phần truyền thông tương ứng |
| Cơ sở dữ liệu | Microsoft SQL Server, JDBC |
| Mật khẩu | jBCrypt |
| Webcam | Webcam Capture, BridJ |
| Logging | SLF4J |
| Quản lý mã nguồn | Git, GitHub |

## 4. Cấu trúc dự án

```text
LTMMT/
├── .gitignore
├── README.md
└── ChatApplication/
    ├── src/
    │   ├── client/       # Giao diện và logic phía client
    │   ├── common/       # Message, MessageType, Protocol
    │   ├── database/     # DatabaseConnection, MessageDAO...
    │   └── server/       # ChatServer, ClientHandler, Room...
    ├── test/             # Mã nguồn kiểm thử
    ├── nbproject/        # Cấu hình dự án NetBeans (trừ private)
    ├── build.xml         # Script Ant
    ├── manifest.mf
    └── lib/              # JAR cài đặt cục bộ; không commit
```

Các file `.form` phải đi cùng file `.java` tương ứng để có thể tiếp tục chỉnh sửa giao diện bằng NetBeans GUI Builder.

## 5. Yêu cầu môi trường

- JDK tương thích với phiên bản Java đang dùng trong dự án.
- Apache NetBeans hỗ trợ Java with Ant.
- Microsoft SQL Server đang hoạt động và có cơ sở dữ liệu phù hợp.
- Kết nối mạng giữa máy chạy server và client.
- Webcam và microphone nếu kiểm thử gọi video/gọi thoại.

**Chưa cố định trong README:** phiên bản JDK, tên cơ sở dữ liệu, địa chỉ IP, các cổng TCP/UDP và script khởi tạo bảng. Cần đối chiếu với `Protocol.java`, `DatabaseConnection.java` và cấu hình hiện tại trước khi triển khai.

## 6. Clone và mở dự án

```bash
git clone https://github.com/thongkunine/LTMMT.git
cd LTMMT
```

Trong NetBeans:

1. Chọn **File → Open Project**.
2. Chọn thư mục `ChatApplication` (thư mục chứa `build.xml` và `nbproject`).
3. Đợi NetBeans tải cấu hình dự án.
4. Cài các thư viện bên dưới nếu NetBeans báo thiếu dependency.

## 7. Cài đặt thư viện (không commit JAR)

Theo quy định vệ sinh mã nguồn, các tệp `.jar`, `.zip` và thư mục build **không được commit lên GitHub**.

Tải các thư viện từ nguồn tin cậy, đặt trong `ChatApplication/lib/` trên máy:

| Thư viện | Phiên bản đã sử dụng |
| --- | --- |
| BridJ | `0.7.0` |
| jBCrypt | `0.4` |
| Microsoft SQL Server JDBC | `13.4.0.jre8` |
| SLF4J API | `1.7.2` |
| Webcam Capture | `0.3.12` |

Nguồn tham khảo: [Maven Central](https://central.sonatype.com/), [Microsoft JDBC Driver](https://learn.microsoft.com/sql/connect/jdbc/).

**Các bước cấu hình:**

1. Tạo thư mục `ChatApplication/lib` nếu chưa có.
2. Tải đúng phiên bản JAR phù hợp với JDK.
3. Trong NetBeans, nhấp phải dự án → **Properties → Libraries**.
4. Thêm các JAR bằng **Add JAR/Folder** nếu các tham chiếu chưa được nhận diện.
5. Kiểm tra các đường dẫn trong `nbproject/project.properties`; ưu tiên tham chiếu tương đối, tránh đường dẫn tuyệt đối tới máy cá nhân.
6. Chọn **Clean and Build** để kiểm tra.

Lưu ý: Thư viện có thể cần thêm các dependency phụ tùy cấu hình; nếu xuất hiện `ClassNotFoundException` hoặc `NoClassDefFoundError`, cần kiểm tra dependency còn thiếu.

## 8. Cấu hình SQL Server

1. Khởi động SQL Server.
2. Tạo hoặc khôi phục cơ sở dữ liệu và các bảng theo schema của dự án.
3. Mở `ChatApplication/src/database/DatabaseConnection.java`.
4. Cấu hình địa chỉ máy chủ, cổng, tên cơ sở dữ liệu và phương thức xác thực theo môi trường chạy.
5. Kiểm tra kết nối trước khi đăng nhập ứng dụng.

**Bảo mật:** Không commit mật khẩu SQL Server, chuỗi kết nối chứa thông tin bí mật hoặc tệp cấu hình cá nhân. Nên sử dụng biến môi trường hoặc cấu hình cục bộ được Git bỏ qua. Không sử dụng tài khoản quản trị SQL Server cho ứng dụng nếu không cần thiết.

> Repository cần bổ sung script SQL/schema nếu muốn người khác có thể tái tạo cơ sở dữ liệu từ đầu.

## 9. Chạy ứng dụng

### 9.1. Chạy server

Trong NetBeans, mở và chạy lớp `server.ChatServer` (nếu đây là entry point đang được sử dụng). Quan sát cửa sổ Output để xác nhận server khởi động thành công.

### 9.2. Chạy client

Chạy lớp khởi động client được cấu hình trong dự án (ví dụ `client.ChatClient` hoặc form đăng nhập tương ứng). Nhập thông tin đăng nhập để kết nối.

**Chạy trên cùng máy:** Có thể sử dụng `localhost` hoặc `127.0.0.1` nếu server lắng nghe trên loopback hoặc mọi giao diện mạng.

**Chạy trên nhiều máy:** Cấu hình IP server có thể truy cập được, mở đúng cổng trên firewall và đảm bảo hai máy kết nối mạng. Nếu dùng Tailscale, sử dụng địa chỉ IP Tailscale của máy server và kiểm tra kết nối trước.

**Voice/video:** Cần kiểm tra thêm các cổng UDP và quyền truy cập microphone/webcam. Không mặc định rằng kết nối TCP thành công đồng nghĩa truyền âm thanh/video đã hoạt động.

## 10. Kiểm thử đề xuất

- Khởi động server và kết nối từ ít nhất hai client.
- Đăng ký, đăng nhập đúng/sai thông tin.
- Gửi và nhận tin nhắn trong phòng chat.
- Nhắn tin riêng và cập nhật danh sách người dùng trực tuyến.
- Ngắt kết nối đột ngột và kiểm tra xử lý ở server.
- Kiểm tra lưu và tải lịch sử tin nhắn.
- Kiểm tra gửi file, gọi thoại và gọi video trên mạng thực tế.
- Thử clone dự án vào thư mục khác, cài dependency và **Clean and Build**.

## 11. Quy định Git và vệ sinh mã nguồn

Repository sử dụng `.gitignore` để bỏ qua:

```gitignore
*.class
*.jar
*.zip
**/build/
**/dist/
**/target/
**/bin/
**/node_modules/
**/nbproject/private/
**/.idea/
**/.vscode/
*.log
.env
.env.*
!.env.example
```

Giữ lại mã nguồn `.java`, file giao diện `.form`, `build.xml`, `manifest.mf` và các cấu hình dự án có thể chia sẻ. Không đưa mật khẩu, token, file nhị phân hoặc file cấu hình cá nhân lên GitHub.

**Quy trình làm việc đề xuất:**

```bash
git switch main
git pull origin main
git switch -c feature/ten-chuc-nang
# Chỉnh sửa và kiểm thử
git add <duong-dan-file-can-commit>
git commit -m "feat: mo ta thay doi"
git push -u origin feature/ten-chuc-nang
```

Tạo Pull Request vào `main` và kiểm tra tab **Files changed** trước khi merge. Không commit trực tiếp lên `main` khi đang phát triển tính năng.

## 12. Giới hạn và lưu ý

- Dự án hiện dùng **Java with Ant**, không yêu cầu Maven.
- Các JAR không nằm trong repository; người dùng cần cài thủ công.
- File `.gitignore` không xóa các tệp đã tồn tại trong lịch sử commit cũ.
- Cần xác nhận các giá trị IP, port, JDK, schema SQL và entry point trước khi sử dụng trên máy khác.
- Chỉ nên đánh dấu chức năng là hoàn thiện sau khi đã kiểm thử thực tế.

## 13. Thông tin dự án

- **Tên đề tài:** Xây dựng ứng dụng Chat nhóm Client–Server sử dụng giao thức TCP Socket trên nền tảng Java.
- **Môn học:** Lập trình mạng máy tính.
- **Mục đích:** Học tập, thực hành lập trình mạng và phát triển ứng dụng Java.
- **Repository:** [thongkunine/LTMMT](https://github.com/thongkunine/LTMMT)

---

*Tài liệu README cần được cập nhật khi thay đổi cấu trúc, thư viện, giao thức hoặc quy trình chạy ứng dụng.*
