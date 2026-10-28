# Meeting Room Booking API

Backend REST API cho hệ thống đặt phòng họp nội bộ (~10 phòng, ~200 nhân viên): quản lý phòng, đặt phòng, xem lịch, tìm phòng trống. Hai vai trò: **USER** (nhân viên) và **ADMIN**.

## Tech stack

- Java 21, Spring Boot 3.5
- PostgreSQL 17
- Docker Compose

## Cách chạy

### Yêu cầu

- Docker (Docker Desktop / OrbStack)
- JDK 21 + Maven

## Phân tích nghiệp vụ

### Vấn đề

Nhân viên đặt phòng bằng cách nhắn tin cho nhau, không có nơi lưu lịch chung nên thường xuyên trùng lịch. Hệ thống cần:

1. Là **nguồn dữ liệu duy nhất** về lịch của các phòng họp.
2. **Không bao giờ để 2 cuộc họp chồng giờ trên cùng một phòng**, kể cả khi nhiều người bấm đặt cùng lúc.
3. Giúp nhân viên nhanh chóng tìm được phòng trống phù hợp.

### Vai trò

| Vai trò | Ai | Được làm gì |
| --- | --- | --- |
| **USER** | Nhân viên | Xem phòng, xem lịch, tìm phòng trống; tạo / sửa / huỷ booking **của mình** |
| **ADMIN** | Người quản lý cơ sở vật chất | Mọi quyền của USER + quản lý phòng, sửa / huỷ booking của bất kỳ ai |

### Danh sách use case

| ID | Use case | Actor |
| --- | --- | --- |
| UC01 | Đăng nhập | USER, ADMIN |
| UC02 | Xem danh sách / chi tiết phòng | USER, ADMIN |
| UC03 | Tìm phòng trống theo khung giờ và số người | USER, ADMIN |
| UC04 | Xem lịch của một phòng theo ngày / tuần | USER, ADMIN |
| UC05 | Đặt phòng | USER, ADMIN |
| UC06 | Sửa booking (đổi giờ, đổi phòng, đổi tiêu đề / số người) | Chủ booking, ADMIN |
| UC07 | Huỷ booking | Chủ booking, ADMIN |
| UC08 | Xem danh sách booking của tôi | USER, ADMIN |
| UC09 | Quản lý phòng: tạo, sửa, vô hiệu hoá | ADMIN |

### Use case chi tiết

#### UC05 — Đặt phòng

##### UC05 — Luồng chính

1. Người dùng tìm phòng trống (UC03) hoặc chọn một phòng cụ thể.
2. Gửi thông tin: phòng, tiêu đề, giờ bắt đầu, giờ kết thúc, số người tham dự.
3. Hệ thống kiểm tra các quy tắc nghiệp vụ và lưu booking với trạng thái `CONFIRMED`.
4. Trả về booking vừa tạo.

##### UC05 — Luồng ngoại lệ

| Tình huống | Kết quả |
| --- | --- |
| Khung giờ đã có người đặt | Từ chối — `409 Conflict` |
| Hai người đặt cùng một khung giờ **cùng lúc** | Chỉ **một** người thành công, người còn lại nhận `409` |
| Giờ bắt đầu ≥ giờ kết thúc, thiếu / sai định dạng dữ liệu | `400 Bad Request` |
| Đặt vào quá khứ, thời lượng không hợp lệ, vượt sức chứa, phòng đã vô hiệu | `422 Unprocessable Entity` |
| Phòng không tồn tại | `404 Not Found` |

#### UC06 — Sửa booking

Đổi phòng hoặc đổi khung giờ đều là sửa booking, và được **kiểm tra lại toàn bộ quy tắc như khi đặt mới** (trừ việc so trùng với chính nó).

##### UC06 — Luồng ngoại lệ

| Tình huống | Kết quả |
| --- | --- |
| Không phải chủ booking và không phải ADMIN | `403 Forbidden` |
| Khung giờ / phòng mới bị trùng với booking khác | `409 Conflict` |
| Hai người sửa cùng một booking cùng lúc | Một người thành công, người còn lại nhận `409` (phải tải lại rồi sửa) |
| Booking đã kết thúc | `422` |
| Booking đang diễn ra | Chỉ được sửa giờ kết thúc (gia hạn / kết thúc sớm) |

#### UC07 — Huỷ booking

Huỷ là **huỷ mềm**: booking chuyển sang `CANCELLED`, không bị xoá, và khung giờ được giải phóng cho người khác đặt. Có thể kèm lý do huỷ.

**Luồng ngoại lệ:** không phải chủ booking / ADMIN → `403`; booking đã kết thúc → `422`.

#### UC09 — Vô hiệu hoá phòng

Phòng không bị xoá mà chỉ chuyển sang không hoạt động, để giữ lịch sử booking. Nếu phòng còn booking `CONFIRMED` **trong tương lai** thì hệ thống từ chối (`409`); ADMIN phải huỷ các booking đó trước.

### Quy tắc nghiệp vụ

| # | Quy tắc | Vi phạm |
| --- | --- | --- |
| BR1 | Không có 2 booking `CONFIRMED` chồng giờ trên cùng một phòng | `409` |
| BR2 | Giờ bắt đầu < giờ kết thúc | `400` |
| BR3 | Không đặt / dời giờ bắt đầu về quá khứ | `422` |
| BR4 | Thời lượng từ 15 phút đến 8 giờ, theo bước 15 phút | `422` |
| BR5 | Chỉ được đặt trước tối đa 90 ngày | `422` |
| BR6 | Số người tham dự ≤ sức chứa của phòng | `422` |
| BR7 | Chỉ đặt được phòng đang hoạt động | `422` |
| BR8 | Chỉ chủ booking hoặc ADMIN được sửa / huỷ; booking đã kết thúc thì không sửa / huỷ được | `403` / `422` |
| BR9 | Khung giờ tính theo kiểu nửa mở `[bắt đầu, kết thúc)`: 09:00–10:00 và 10:00–11:00 **không** bị coi là trùng | — |
| BR10 | Không vô hiệu hoá phòng còn booking trong tương lai | `409` |

Các giới hạn ở BR4 và BR5 được cấu hình qua biến môi trường.

### Giả định

- Công ty có một múi giờ hiển thị. Thời gian được lưu theo UTC; API nhận và trả về chuẩn ISO-8601 có kèm múi giờ (vd `2026-10-28T09:00:00+07:00`).
- Tài khoản người dùng có sẵn (seed khi triển khai), không có chức năng đăng ký. Khi đưa vào dùng thật sẽ thay bằng SSO của công ty.
- Booking được xác nhận ngay khi tạo, không cần ai phê duyệt.
- Chỉ lưu **số lượng** người tham dự, không lưu danh sách người tham dự.

### Ngoài phạm vi (hướng mở rộng)

- Đăng ký / quản lý người dùng, đăng nhập SSO
- Tiện ích của phòng (máy chiếu, TV…) và lọc phòng theo tiện ích
- Danh sách người tham dự, gửi thông báo email / Slack
- Họp định kỳ (hằng tuần, hằng tháng)
- Check-in và tự giải phóng phòng khi không ai đến
- Đồng bộ với Google Calendar / Outlook
- Luồng phê duyệt booking

## Tiến độ

- [x] Khởi tạo project Spring Boot + PostgreSQL qua Docker Compose
- [x] Phân tích nghiệp vụ
