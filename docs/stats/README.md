# Trang thống kê thẻ

`docs/stats/index.html` là trang tĩnh (không cần server, không thư viện ngoài)
đọc `data/stats.js` và hiển thị trên điện thoại:

- **Thẻ**: danh sách thẻ với tỉ lệ thắng (thanh lệch quanh 50%, vạch mảnh là
  khoảng tin cậy 95%), sắp xếp và lọc; chạm vào thẻ để xem chi tiết: chỉ số,
  đặc tính, so với thẻ cùng giá, đi một mình tới tháp, đối đầu tốt/tệ nhất,
  lịch sử tỉ lệ thắng. Nút "Dạng bảng" hiện mọi chỉ số trong một bảng.
- **So sánh**: tối đa 3 thẻ cạnh nhau, và đường tỉ lệ thắng qua các lần chạy.
- **Đối đầu**: bản đồ nhiệt ai thắng ai (tấn công qua cầu vào thẻ phòng thủ
  dưới tháp), chạm ô để xem số.
- **Trận**: số liệu bot đấu bot, xếp hạng tỉ lệ thắng, dải sức mạnh của
  `PowerCreepTest`, quân chỉ đánh tháp.

## Cập nhật số liệu

```
gradlew statsReport              # 200 trận bot
gradlew statsReport -Pmatches=500
```

Ghi `docs/stats/data/stats.js`, `stats.json` và thêm một dòng vào
`history.jsonl` (giữ 60 lần chạy gần nhất; chạy lại trên cùng commit thì thay
dòng cũ). Commit trước rồi mới chạy, để nhãn là mã commit (có dấu `+` khi còn
thay đổi chưa commit). Commit các file trong `data/` để lưu lịch sử.

## Xem trên điện thoại (cùng Wi-Fi)

```
gradlew statsServe               # cổng 8000
gradlew statsServe -Pport=8080
```

Lệnh in ra địa chỉ dạng `http://192.168.x.x:8000/docs/stats/`, mở bằng điện
thoại. Server chỉ phục vụ `docs/stats/` và ảnh thẻ, chỉ đọc; phần còn lại của
repo không truy cập được. Nếu điện thoại không vào được, cho phép Java qua
tường lửa Windows (mạng Private). Ctrl+C để dừng.

Trên máy tính có thể mở thẳng `docs/stats/index.html` bằng trình duyệt.

Có Python thì cũng dùng được: `python -m http.server 8000` ở thư mục repo, rồi
mở `http://<IP máy>:8000/docs/stats/` (cách này chia sẻ cả repo trong mạng).

## Các chỉ số

| Chỉ số | Ý nghĩa |
|---|---|
| Tỉ lệ thắng | phần trăm trận thắng của deck bot có thẻ này (hòa tính nửa) |
| Điểm đấu | elixir lời trung bình khi tấn công vào và phòng thủ trước mọi quân đánh nhau; dải cho phép −1,2 … +1,1 |
| Điểm tấn công / phòng thủ | hai nửa của điểm đấu |
| Sát thương/giây mỗi elixir | sát thương của cả thẻ mỗi giây, chia cho giá |
| Máu mỗi elixir | tổng máu (tính cả con tách ra) chia cho giá |
| Chịu tháp | số giây cả thẻ sống dưới một tháp công chúa |
| Phá tháp mỗi elixir | máu tháp phá được khi đi một mình, chia cho giá |
| Tới tháp sau | số giây từ đầu cầu tới đòn đầu tiên vào tháp |
| Giá trị spell mỗi elixir | giá theo công thức chia cho giá thật; dải 0,90 … 1,12 |
| Lượt đánh mỗi trận | bot đánh thẻ này bao nhiêu lần một trận |

Thẻ có nhãn ⚠ nằm ngoài dải sức mạnh, hoặc tỉ lệ thắng lệch hẳn khỏi 40–60%.
