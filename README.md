# Meeting Room Booking API

Backend REST API cho hệ thống đặt phòng họp nội bộ (~10 phòng, ~200 nhân viên): quản lý phòng, đặt phòng, xem lịch, tìm phòng trống. Hai vai trò: **USER** (nhân viên) và **ADMIN**.

## Tech stack

- Java 21, Spring Boot 3.5 (Web, Data JPA, Validation, Security + OAuth2 Resource Server)
- PostgreSQL 17, Flyway
- JWT (HS256), springdoc-openapi (Swagger UI)
- Docker Compose

## Cách chạy

### Yêu cầu

- Docker (Docker Desktop / OrbStack)

### Khởi chạy

Chạy với cấu hình mặc định:

```bash
docker compose up -d
```

- API: `http://localhost:8082/api/v1`
- Swagger UI: `http://localhost:8082/swagger-ui/index.html`

Không cần file `.env`: mọi biến đều có giá trị mặc định. Lần đầu chạy, Flyway tạo bảng và seed dữ liệu demo (3 tài khoản, 6 phòng). Xoá sạch dữ liệu: `docker compose down -v`.

### Tuỳ chỉnh cấu hình (tuỳ chọn)

Tạo file `.env` từ file mẫu, rồi sửa các giá trị cần thay đổi:

```bash
cp .env.example .env
```

Sau đó khởi chạy lại 
```bash
docker compose up -d
``` 

hoặc build lại và khởi chạy:

```bash
docker compose up -d --build
```

Danh sách biến: xem [Biến môi trường](#biến-môi-trường).

### Tài khoản demo

Seed sẵn bằng Flyway (`V1__create_users.sql`), chỉ dùng để demo/test:

| Email | Mật khẩu | Role |
| --- | --- | --- |
| `admin@kw.local` | `Admin@123` | ADMIN |
| `user1@kw.local` | `User@123` | USER |
| `user2@kw.local` | `User@123` | USER |

### Thử nhanh

1. Swagger → `POST /api/v1/auth/login` với một tài khoản ở trên → copy `accessToken`.
2. Bấm **Authorize**, dán token.
3. Gọi các API còn lại.

Bằng `curl`:

```bash
TOKEN=$(curl -s localhost:8082/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"user1@kw.local","password":"User@123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

curl -s localhost:8082/api/v1/bookings -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"roomId":3,"title":"Sprint review","startAt":"2026-11-10T09:00:00+07:00","endAt":"2026-11-10T10:00:00+07:00","attendees":5}'
```

> **Thời gian trên URL:** dấu `+` trong `+07:00` bị hiểu là dấu cách → dùng giờ UTC có `Z` (`2026-11-10T02:00:00Z`) hoặc mã hoá `%2B`. Trong body JSON thì không cần.

### Chạy khi phát triển (IDE)

Cần JDK 21 + Maven. Chạy app từ IDE / `mvn spring-boot:run`: Spring Boot Docker Compose support tự bật **chỉ** container `postgres` trong `compose.yaml` và tự nối datasource. App chạy ở cổng `8080`.

### Biến môi trường

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `APP_PORT` | `8082` | Cổng API trên máy host (trong container luôn là 8080) |
| `JWT_SECRET` | chuỗi dev công khai | Khoá ký JWT HS256, ≥ 32 byte. **Bắt buộc đổi khi triển khai thật** |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `meeting_room_booking` / `app` / `app` | Postgres chỉ áp dụng khi tạo volume lần đầu — đổi sau đó thì `docker compose down -v` |
| `BOOKING_MIN_DURATION` | `PT5M` | BR4: thời lượng tối thiểu |
| `BOOKING_DURATION_STEP` | `PT5M` | BR4: thời lượng phải chia hết cho bước này |
| `BOOKING_ZONE` | `Asia/Ho_Chi_Minh` | BR4: múi giờ công ty — booking không được kéo qua nửa đêm theo múi giờ này |
| `BOOKING_MAX_ADVANCE` | `P90D` | BR5: đặt trước tối đa bao lâu. `P0D` = không giới hạn |

Thời lượng viết theo ISO-8601 (`PT15M` = 15 phút, `PT1H` = 1 giờ, `P90D` = 90 ngày). Sửa `.env` xong chạy lại `docker compose up -d` để app nhận giá trị mới.

## Thiết kế

### Kiến trúc

Một ứng dụng Spring Boot, chia package theo nghiệp vụ, mỗi package có Controller → Service → Repository:

```text
auth/      đăng nhập, phát JWT
user/      entity User, Role
room/      quản lý phòng, tìm phòng trống
booking/   đặt / sửa / huỷ, danh sách của tôi, lịch
common/    cấu hình (security, properties), xử lý lỗi tập trung
```

- **Controller**: nhận request, validate DTO (`@Valid`), lấy user từ JWT.
- **Service**: toàn bộ quy tắc nghiệp vụ, transaction (`@Transactional`).
- **Repository**: Spring Data JPA; bộ lọc tuỳ chọn viết bằng `Specification` (`RoomSpecs`, `BookingSpecs`).
- Entity không lộ ra API — luôn đổi sang DTO (`*Response`).

### Cơ sở dữ liệu

3 bảng, schema do **Flyway** quản lý (`src/main/resources/db/migration`), Hibernate chỉ `validate`. Chi tiết: [docs/schema.md](docs/schema.md).

| Bảng | Ghi chú |
| --- | --- |
| `users` | email unique, mật khẩu BCrypt, role `ADMIN` / `USER` |
| `rooms` | `code` unique, `capacity > 0`, `active` (vô hiệu hoá thay vì xoá) |
| `bookings` | FK tới `rooms`, `users`; `status` `CONFIRMED` / `CANCELLED`; `version` cho khoá lạc quan |

Ràng buộc quan trọng nhất — chặn trùng giờ ngay trong DB:

```sql
CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (room_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&)
    WHERE (status = 'CONFIRMED')
```

### API

Mọi endpoint (trừ đăng nhập và Swagger) cần header `Authorization: Bearer <token>`.

| Method | Path | Ai | UC | Mô tả |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | mọi người | UC01 | Đăng nhập, trả JWT (hạn 1 giờ) |
| GET | `/api/v1/rooms` | USER, ADMIN | UC02, UC03 | Danh sách phòng. Lọc: `status` (`ACTIVE` mặc định; `INACTIVE` / `ALL` chỉ ADMIN), `minCapacity`, `location`, và **`startAt` + `endAt` → chỉ phòng trống** trong khung giờ |
| GET | `/api/v1/rooms/{id}` | USER, ADMIN | UC02 | Chi tiết phòng |
| POST | `/api/v1/rooms` | ADMIN | UC09 | Tạo phòng |
| PUT | `/api/v1/rooms/{id}` | ADMIN | UC09 | Sửa tên / vị trí / sức chứa |
| PATCH | `/api/v1/rooms/{id}/status` | ADMIN | UC09 | Bật / tắt phòng: `{"active": false}` |
| POST | `/api/v1/bookings` | USER, ADMIN | UC05 | Đặt phòng |
| PUT | `/api/v1/bookings/{id}` | chủ booking, ADMIN | UC06 | Sửa booking (gửi kèm `version`) |
| POST | `/api/v1/bookings/{id}/cancel` | chủ booking, ADMIN | UC07 | Huỷ, body tuỳ chọn `{"cancelReason": "..."}` |
| GET | `/api/v1/bookings` | USER, ADMIN | UC08 | Booking **của tôi**, phân trang. Lọc: `status`, `from` (mặc định: bây giờ), `to`, `page`, `size` (≤ 100) |
| GET | `/api/v1/bookings/calendar` | USER, ADMIN | UC04 | Lịch: mọi booking `CONFIRMED` trong `[from, to)` (≤ 31 ngày), lọc `roomId`; mỗi sự kiện kèm tên phòng và cờ `mine` |

### Định dạng lỗi

Mọi lỗi trả về [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) kèm `errorCode` để client xử lý mà không phải đọc message:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Room is already booked in this time range",
  "instance": "/api/v1/bookings",
  "errorCode": "BOOKING_OVERLAP"
}
```

| HTTP | Khi nào | `errorCode` ví dụ |
| --- | --- | --- |
| 400 | Dữ liệu thiếu / sai định dạng (kèm `fields`), tham số tìm kiếm sai | `VALIDATION_ERROR`, `INVALID_SEARCH_PARAMS` |
| 401 | Chưa đăng nhập, token sai / hết hạn, sai mật khẩu | `UNAUTHORIZED`, `INVALID_CREDENTIALS` |
| 403 | Không có quyền (USER gọi API ADMIN, sửa booking người khác) | `FORBIDDEN` |
| 404 | Không tìm thấy phòng / booking | `ROOM_NOT_FOUND`, `BOOKING_NOT_FOUND` |
| 409 | Trùng giờ, sửa đồng thời, trùng mã phòng, tắt phòng còn booking | `BOOKING_OVERLAP`, `BOOKING_VERSION_CONFLICT`, `ROOM_CODE_DUPLICATED`, `ROOM_HAS_FUTURE_BOOKINGS` |
| 422 | Vi phạm quy tắc nghiệp vụ (BR3–BR8) | `BOOKING_IN_PAST`, `BOOKING_INVALID_DURATION`, `BOOKING_TOO_FAR_AHEAD`, `BOOKING_EXCEEDS_CAPACITY`, `ROOM_INACTIVE`, `BOOKING_ALREADY_ENDED`, `BOOKING_ALREADY_CANCELLED`, `BOOKING_IN_PROGRESS` |

## Các quyết định quan trọng và lý do

| Quyết định | Lý do |
| --- | --- |
| **Chống trùng giờ bằng exclusion constraint của PostgreSQL** (`btree_gist` + `tstzrange ... WITH &&`), không kiểm tra bằng code | Kiểu "SELECT xem có trùng không rồi INSERT" sẽ lọt khi hai người bấm cùng lúc (cả hai cùng thấy "trống"). Constraint để DB đảm bảo **một** dòng thắng, mọi đường ghi (kể cả SQL tay) đều bị chặn. Code chỉ bắt lỗi và đổi thành `409`. Đã thử 20 request đặt cùng khung giờ cùng lúc: 1 thành công, 19 nhận `409` |
| **Khoảng giờ nửa mở `[start, end)`** | 09:00–10:00 và 10:00–11:00 không trùng (BR9). Cùng một quy ước cho constraint, tìm phòng trống, danh sách và lịch |
| **Khoá lạc quan (`@Version`) khi sửa booking** | Client gửi `version` đã đọc; lệch → `409`, phải tải lại. Tránh người sau âm thầm ghi đè thay đổi của người trước. Không khoá dòng nên không làm chậm người đọc |
| **Huỷ mềm** (`CANCELLED`) và **vô hiệu hoá phòng** thay vì xoá | Giữ lịch sử. Constraint chỉ áp cho `CONFIRMED` nên khung giờ của booking đã huỷ tự được giải phóng |
| **Lưu thời gian UTC (`TIMESTAMPTZ`)**, API nhận ISO-8601 có múi giờ | Không phụ thuộc múi giờ server. "Nửa đêm" (BR4) tính theo `BOOKING_ZONE` của công ty |
| **Quy tắc nghiệp vụ cấu hình được** qua biến môi trường | Thời lượng tối thiểu, bước, đặt trước bao xa là chính sách công ty, có thể đổi mà không build lại |
| **Flyway + `ddl-auto=validate`** | Schema có version, review được, chạy giống nhau ở mọi môi trường. App không khởi động nếu entity lệch với DB |
| **JWT stateless (HS256), không refresh token** | Đơn giản, không cần lưu session. Khi dùng thật sẽ thay bằng SSO công ty (xem Ngoài phạm vi) |
| **Kiểm tra quyền sở hữu trong service**, quyền theo vai trò bằng `@PreAuthorize` | "Chủ booking hoặc ADMIN" phụ thuộc dữ liệu nên phải kiểm sau khi đọc booking. Kiểm quyền **trước** kiểm version / trạng thái để người không có quyền không suy ra được thông tin booking |
| **Tìm phòng trống gộp vào `GET /rooms`** (`startAt` + `endAt`) | Dùng lại bộ lọc sẵn có (`minCapacity`, `location`) và cùng hình dạng response; không cần endpoint riêng |
| **`GET /bookings` (của tôi) tách khỏi `GET /bookings/calendar` (mọi người)** | Hai nghĩa khác nhau: danh sách cá nhân có phân trang; lịch cần đủ sự kiện trong khoảng giờ nên không phân trang mà giới hạn ≤ 31 ngày |
| **Danh sách phòng không phân trang** | ~10 phòng, gần như không tăng; FE cần cả danh sách để chọn |
| **Giảm sức chứa phòng không ảnh hưởng booking đã đặt** | Sức chứa là con số khai báo, thực tế phòng vẫn có thể chứa thêm vài người. Không huỷ hay chặn booking cũ chỉ vì admin sửa con số; BR6 áp dụng cho booking **mới** và khi **sửa** booking (lúc đó phải giảm số người cho vừa sức chứa mới) |
| **Mã lỗi riêng cho từng quy tắc** (`errorCode`) | Client biết chính xác sai gì mà không phải phân tích message |

### Giới hạn đã biết (trade-off)

- **Tắt phòng đúng lúc có người đang đặt phòng đó**: có thể lọt một booking vào phòng vừa bị tắt. Khả năng rất thấp (thao tác admin hiếm), hậu quả nhẹ (admin huỷ tay). Chặn hẳn được bằng khoá dòng phòng (`SELECT ... FOR UPDATE`) ở cả hai phía, đổi lại mọi lượt đặt cùng phòng phải xếp hàng.
- **`CREATE EXTENSION btree_gist`** cần quyền superuser lúc chạy migration (user `app` trong compose có quyền này). Trên DB do DBA quản lý thì cần cài extension trước.
- Chưa có test tự động; các luồng chính và trường hợp đồng thời đã được thử bằng script gọi API trên bản chạy Docker.

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
| UC04 | Xem lịch phòng theo ngày / tuần | USER, ADMIN |
| UC05 | Đặt phòng | USER, ADMIN |
| UC06 | Sửa booking (đổi giờ, đổi phòng, đổi tiêu đề / số người) | Chủ booking, ADMIN |
| UC07 | Huỷ booking | Chủ booking, ADMIN |
| UC08 | Xem danh sách booking của tôi | USER, ADMIN |
| UC09 | Quản lý phòng: tạo, sửa, vô hiệu hoá | ADMIN |

### Use case chi tiết

#### UC01 — Đăng nhập

`POST /auth/login` với email + mật khẩu → trả `accessToken` (JWT, hạn 1 giờ), kèm `tokenType` và `expiresIn` (giây). Các API khác gửi token qua header `Authorization: Bearer <token>`. Sai email / mật khẩu → `401` (không nói rõ sai email hay sai mật khẩu, tránh dò tài khoản). Token hết hạn → `401`, đăng nhập lại.

#### UC02 — Xem danh sách / chi tiết phòng

`GET /rooms`: mặc định chỉ phòng **đang hoạt động**, sắp theo mã phòng. Lọc tuỳ chọn, kết hợp được với nhau:

| Tham số | Ý nghĩa |
| --- | --- |
| `status` | `ACTIVE` (mặc định), `INACTIVE`, `ALL` — hai giá trị sau chỉ ADMIN dùng được, USER → `403` |
| `minCapacity` | Sức chứa tối thiểu |
| `location` | Một phần vị trí, không phân biệt hoa thường (`floor 2` khớp `Floor 2`) |

`GET /rooms/{id}`: chi tiết một phòng, kể cả phòng đã vô hiệu hoá (để xem lại phòng của booking cũ). Không tồn tại → `404`.

#### UC03 — Tìm phòng trống

`GET /rooms?startAt=...&endAt=...&minCapacity=...`: trả các phòng **đang hoạt động**, đủ sức chứa, **không có booking `CONFIRMED` nào chồng** lên khung giờ. `startAt` / `endAt` phải đi cùng nhau và `endAt > startAt` (sai → `400`). Không có phòng nào → `200` với `[]`.

#### UC04 — Xem lịch

`GET /bookings/calendar?from=...&to=...&roomId=...`: mọi booking `CONFIRMED` (của **mọi người**) chồng lên `[from, to)`, kèm mã / tên phòng; có `roomId` thì là lịch của một phòng. Xem ngày hay tuần do client chọn `from` / `to` (tối đa 31 ngày). Mỗi sự kiện có cờ `mine` để FE tô màu booking của chính mình.

#### UC05 — Đặt phòng

##### UC05 — Luồng chính

1. Người dùng tìm phòng trống (UC03) hoặc chọn một phòng cụ thể.
2. Gửi thông tin: phòng, tiêu đề, giờ bắt đầu, giờ kết thúc, số người tham dự.
3. Hệ thống kiểm tra các quy tắc nghiệp vụ và lưu booking với trạng thái `CONFIRMED`.
4. Trả về `201` cùng booking vừa tạo (có `version`).

##### UC05 — Luồng ngoại lệ

| Tình huống | Kết quả |
| --- | --- |
| Khung giờ đã có người đặt | Từ chối — `409 Conflict` |
| Hai người đặt cùng một khung giờ **cùng lúc** | Chỉ **một** người thành công, người còn lại nhận `409` |
| Giờ bắt đầu ≥ giờ kết thúc, thiếu / sai định dạng dữ liệu | `400 Bad Request` |
| Đặt vào quá khứ, quá xa, thời lượng không hợp lệ, vượt sức chứa, phòng đã vô hiệu | `422 Unprocessable Entity` |
| Phòng không tồn tại | `404 Not Found` |

#### UC06 — Sửa booking

Đổi phòng hoặc đổi khung giờ đều là sửa booking, và được **kiểm tra lại toàn bộ quy tắc như khi đặt mới** (trừ việc so trùng với chính nó). Client gửi lại toàn bộ booking kèm `version` đã đọc.

##### UC06 — Luồng ngoại lệ

| Tình huống | Kết quả |
| --- | --- |
| Không phải chủ booking và không phải ADMIN | `403 Forbidden` |
| Khung giờ / phòng mới bị trùng với booking khác | `409 Conflict` |
| `version` cũ, hoặc hai người sửa cùng một booking cùng lúc | Một người thành công, người còn lại nhận `409` (phải tải lại rồi sửa) |
| Booking đã kết thúc / đã huỷ | `422` |
| Booking đang diễn ra | Chỉ được sửa giờ kết thúc (gia hạn / kết thúc sớm, nhưng không lùi về quá khứ); đổi phòng hoặc giờ bắt đầu → `422` |

#### UC07 — Huỷ booking

Huỷ là **huỷ mềm**: booking chuyển sang `CANCELLED`, không bị xoá, và khung giờ được giải phóng cho người khác đặt. Có thể kèm lý do huỷ. Booking đang diễn ra vẫn huỷ được (kết thúc họp sớm).

**Luồng ngoại lệ:** không phải chủ booking / ADMIN → `403`; booking đã kết thúc hoặc đã huỷ → `422`.

#### UC08 — Booking của tôi

`GET /bookings`: chỉ booking của người đang đăng nhập, sắp theo giờ bắt đầu, phân trang (`page` từ 0, `size` mặc định 20, tối đa 100). Mặc định chỉ hiện booking **chưa kết thúc** (`from` = bây giờ); truyền `from` sớm hơn để xem lịch sử. Lọc thêm `status`, `to`.

#### UC09 — Quản lý phòng

ADMIN tạo / sửa phòng (mã phòng duy nhất → trùng thì `409`, kể cả khi hai admin tạo cùng lúc). Phòng không bị xoá mà chỉ chuyển sang không hoạt động (`PATCH /rooms/{id}/status`), để giữ lịch sử booking. Nếu phòng còn booking `CONFIRMED` **chưa kết thúc** (kể cả đang diễn ra) thì hệ thống từ chối (`409`); ADMIN phải huỷ các booking đó trước.

### Quy tắc nghiệp vụ

| # | Quy tắc | Vi phạm |
| --- | --- | --- |
| BR1 | Không có 2 booking `CONFIRMED` chồng giờ trên cùng một phòng | `409` |
| BR2 | Giờ bắt đầu < giờ kết thúc | `400` |
| BR3 | Không đặt / dời giờ bắt đầu về quá khứ | `422` |
| BR4 | Thời lượng tối thiểu 5 phút, theo bước 5 phút, không kéo qua nửa đêm (theo múi giờ công ty) | `422` |
| BR5 | Chỉ được đặt trước tối đa 90 ngày | `422` |
| BR6 | Số người tham dự ≤ sức chứa của phòng | `422` |
| BR7 | Chỉ đặt được phòng đang hoạt động | `422` |
| BR8 | Chỉ chủ booking hoặc ADMIN được sửa / huỷ; booking đã kết thúc thì không sửa / huỷ được | `403` / `422` |
| BR9 | Khung giờ tính theo kiểu nửa mở `[bắt đầu, kết thúc)`: 09:00–10:00 và 10:00–11:00 **không** bị coi là trùng | — |
| BR10 | Không vô hiệu hoá phòng còn booking chưa kết thúc | `409` |

Các giới hạn ở BR4 và BR5 cấu hình được qua biến môi trường (`BOOKING_*`, xem [Biến môi trường](#biến-môi-trường)).

### Giả định

- Công ty có một múi giờ (`BOOKING_ZONE`). Thời gian lưu theo UTC; API nhận ISO-8601 có múi giờ (vd `2026-10-28T09:00:00+07:00`) và trả về UTC (`2026-10-28T02:00:00Z`).
- Tài khoản người dùng có sẵn (seed khi triển khai), không có chức năng đăng ký. Khi đưa vào dùng thật sẽ thay bằng SSO của công ty.
- Booking được xác nhận ngay khi tạo, không cần ai phê duyệt.
- Chỉ lưu **số lượng** người tham dự, không lưu danh sách người tham dự.
- **Lịch phòng công khai**: mọi nhân viên thấy tiêu đề và người đặt của mọi booking (như lịch nội bộ công ty).

### Ngoài phạm vi (hướng mở rộng)

- Đăng ký / quản lý người dùng, đăng nhập SSO, refresh token
- Tiện ích của phòng (máy chiếu, TV…) và lọc phòng theo tiện ích
- Danh sách người tham dự, gửi thông báo email / Slack
- Check-in và tự giải phóng phòng khi không ai đến
- Đồng bộ với Google Calendar / Outlook
- Luồng phê duyệt booking
- ADMIN chỉnh quy tắc đặt phòng (thời lượng tối thiểu, bước, đặt trước tối đa) ngay trên hệ thống: lưu trong DB, có hiệu lực ngay, không cần sửa biến môi trường và khởi động lại như hiện tại
