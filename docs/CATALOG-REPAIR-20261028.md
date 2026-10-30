# Sửa trang giáo viên và chủ đề trùng — 28/10/2026

## Nguyên nhân

- `.editor-form` căn giữa nhưng `.video-manager` chỉ giới hạn chiều rộng, nên hai
  khung lệch nhau. Nội dung textarea kế thừa font-weight từ label.
- `tools/http-regression.ps1` thêm mới chủ đề Đo lường mỗi lần chạy. MySQL có 15
  bản xuất bản cùng nội dung và chủ sở hữu admin@smartstudy.vn.

## Thay đổi

- Hai khung cùng rộng tối đa 920px, căn giữa, cách nhau 24px. Ô nhập có chữ thường
  15px, dòng 1.65; giới hạn chiều rộng và cho phép textarea co giãn dọc.
- CatalogService từ chối chủ đề đang sử dụng trùng tên và nhóm (chuẩn Unicode,
  không phân biệt hoa thường, gộp khoảng trắng). Sửa chính mục hiện có vẫn được.
- Script hồi quy tìm và cập nhật chủ đề hiện có thay vì tạo mới ở mỗi lần chạy.
- Migration tùy chọn 004 chỉ lưu trữ các bản fixture Đo lường giống hệt, giữ bản
  đầu tiên. Đã áp dụng: 14 archived, 1 published. Không DELETE, không sửa liên kết.
- Admin mặc định lọc Đang sử dụng; Lưu trữ và Tất cả vẫn xem được bản cũ.

## Bảo vệ dữ liệu

- Source trước sửa: backups/catalog-before-cleanup-20261028.zip.
- Export các chủ đề trước sửa: backups/topics-before-repair-20261028.sql.
- Không thay đổi bài học, nội dung video, tài khoản hay điểm số.

## Kiểm tra đã chạy

- Ant dist thành công; Jasper biên dịch JSP 0 lỗi.
- DomainWorkflowTest 55 kiểm tra đạt, gồm chống trùng và sửa mục hiện có.
- tools/test-catalog-repair.ps1: 19 kiểm tra HTTP đạt. Hai POST trùng bị từ chối
  HTTP 400, số bản ghi không tăng; Student bị chặn trang sửa của Teacher.
- Trình duyệt thực, Teacher lesson-motion: hai khung left=298.4, width=920 tại
  viewport1280, cách nhau 24px; textarea font-weight400, trang không tràn ngang.
- Admin: 5 mục đang dùng, chỉ 1 Đo lường; lọc Lưu trữ thấy 14 bản; tìm không dấu
  `do luong` với bộ lọc Đang sử dụng trả đúng 1 mục. Console không có lỗi/cảnh báo.
- Ảnh trong docs/screenshots/teacher-editor-aligned-20261028.png và
  admin-topics-clean-20261028.png.

Chưa chạy lại toàn bộ HTTP regression vì luồng đó còn tạo các bài đánh giá thử;
đã chạy kiểm tra tập trung cho thay đổi này và kiểm tra nghiệp vụ trong bộ nhớ.
