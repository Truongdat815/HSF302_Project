-- Sample data for HSF302 E-Learning.
-- Safe to run multiple times: it upserts fixed records and updates lesson content.
-- Default password for both users: 123456

BEGIN;

INSERT INTO roles (name)
VALUES
    ('ROLE_ADMIN'),
    ('ROLE_STUDENT')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (full_name, email, password, avatar_url, enabled, created_at)
VALUES
    (
        'Quan tri vien Demo',
        'admin@fpt.edu.vn',
        '$2a$10$O23pzoKILcAkVTfx7cadFOW4fXzuz0L.8Gt7R0d2FkWx6/zHwezx.',
        'https://res.cloudinary.com/demo/image/upload/v1700000000/elearning/admin-avatar.png',
        true,
        NOW()
    ),
    (
        'Hoc vien Demo',
        'student@fpt.edu.vn',
        '$2a$10$O23pzoKILcAkVTfx7cadFOW4fXzuz0L.8Gt7R0d2FkWx6/zHwezx.',
        'https://res.cloudinary.com/demo/image/upload/v1700000000/elearning/student-avatar.png',
        true,
        NOW()
    )
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    password = EXCLUDED.password,
    avatar_url = EXCLUDED.avatar_url,
    enabled = EXCLUDED.enabled;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.email = 'admin@fpt.edu.vn'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_STUDENT'
WHERE u.email = 'student@fpt.edu.vn'
ON CONFLICT DO NOTHING;

INSERT INTO categories (name, slug)
VALUES
    ('Lap trinh Backend', 'lap-trinh-backend'),
    ('Thiet ke giao dien', 'thiet-ke-giao-dien'),
    ('Ky nang hoc tap', 'ky-nang-hoc-tap')
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name;

WITH admin_user AS (
    SELECT id FROM users WHERE email = 'admin@fpt.edu.vn'
),
backend_category AS (
    SELECT id FROM categories WHERE slug = 'lap-trinh-backend'
),
design_category AS (
    SELECT id FROM categories WHERE slug = 'thiet-ke-giao-dien'
),
study_category AS (
    SELECT id FROM categories WHERE slug = 'ky-nang-hoc-tap'
)
INSERT INTO courses (
    title,
    slug,
    short_description,
    description,
    thumbnail_url,
    price,
    status,
    instructor_id,
    category_id,
    created_at
)
VALUES
    (
        'Spring Boot cho nguoi moi bat dau',
        'spring-boot-cho-nguoi-moi-bat-dau',
        'Xay dung ung dung web co ban voi Spring Boot, Thymeleaf va PostgreSQL.',
        '<h2>Ban se hoc gi?</h2><p>Khoa hoc huong dan tu cau truc project, controller, service, repository den ket noi database.</p><ul><li>Spring MVC</li><li>Spring Data JPA</li><li>Thymeleaf</li><li>Validation co ban</li></ul>',
        'https://images.unsplash.com/photo-1516321318423-f06f85e504b3',
        2000.00,
        'PUBLISHED',
        (SELECT id FROM admin_user),
        (SELECT id FROM backend_category),
        NOW()
    ),
    (
        'Thiet ke UI web voi Bootstrap va Thymeleaf',
        'thiet-ke-ui-web-voi-bootstrap-va-thymeleaf',
        'Tao giao dien web ro rang, responsive va de dung cho ung dung Java web.',
        '<h2>Noi dung chinh</h2><p>Khoa hoc tap trung vao layout, component Bootstrap, form, bang du lieu va trang chi tiet khoa hoc.</p><ul><li>Grid system</li><li>Form va validation message</li><li>Responsive co ban</li><li>Trang admin de quan ly noi dung</li></ul>',
        'https://images.unsplash.com/photo-1559028012-481c04fa702d',
        2000.00,
        'PUBLISHED',
        (SELECT id FROM admin_user),
        (SELECT id FROM design_category),
        NOW()
    ),
    (
        'Ky nang hoc online hieu qua',
        'ky-nang-hoc-online-hieu-qua',
        'Phuong phap lap ke hoach, ghi chu va theo doi tien do khi hoc truc tuyen.',
        '<h2>Danh cho ai?</h2><p>Khoa hoc phu hop voi sinh vien muon hoc online co ky luat hon va hoan thanh khoa hoc dung tien do.</p><ul><li>Lap muc tieu hoc tap</li><li>Chia nho noi dung</li><li>On tap bang quiz</li><li>Tu danh gia tien do</li></ul>',
        'https://images.unsplash.com/photo-1522202176988-66273c2fd55f',
        2000.00,
        'PUBLISHED',
        (SELECT id FROM admin_user),
        (SELECT id FROM study_category),
        NOW()
    )
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    short_description = EXCLUDED.short_description,
    description = EXCLUDED.description,
    thumbnail_url = EXCLUDED.thumbnail_url,
    price = EXCLUDED.price,
    status = EXCLUDED.status,
    instructor_id = EXCLUDED.instructor_id,
    category_id = EXCLUDED.category_id;

WITH course_ref AS (
    SELECT id, slug FROM courses
    WHERE slug IN (
        'spring-boot-cho-nguoi-moi-bat-dau',
        'thiet-ke-ui-web-voi-bootstrap-va-thymeleaf',
        'ky-nang-hoc-online-hieu-qua'
    )
),
lesson_seed (course_slug, title, content, video_url, order_index, preview) AS (
    VALUES
        (
            'spring-boot-cho-nguoi-moi-bat-dau',
            'Tong quan Spring Boot',
            '<h2>1. Spring Boot la gi?</h2><p>Spring Boot la mot phan mo rong cua Spring Framework, duoc tao ra de giup lap trinh vien xay dung ung dung Java nhanh hon, it cau hinh thu cong hon va de trien khai hon. Neu Spring Framework truyen thong yeu cau khai bao nhieu cau hinh XML hoac Java Config, Spring Boot su dung co che auto-configuration de tu cau hinh cac thanh phan pho bien dua tren dependency co trong project.</p><h2>2. Vi sao nen dung Spring Boot?</h2><p>Spring Boot phu hop cho ung dung web, REST API, he thong quan ly, e-learning, thuong mai dien tu va nhieu loai backend khac. Loi ich chinh la khoi tao nhanh, tich hop san Tomcat, quan ly dependency theo starter, ho tro cau hinh qua application.properties va co he sinh thai lon nhu Spring Data JPA, Spring Security, Validation, Thymeleaf.</p><h2>3. Cau truc project co ban</h2><ul><li><strong>Controller</strong>: nhan request tu trinh duyet, goi service va tra view hoac JSON.</li><li><strong>Service</strong>: chua logic nghiep vu, vi du tinh gia, tao don hang, ghi danh hoc vien.</li><li><strong>Repository</strong>: lam viec voi database thong qua Spring Data JPA.</li><li><strong>Entity</strong>: dai dien cho bang trong database.</li><li><strong>Template</strong>: giao dien Thymeleaf render HTML cho nguoi dung.</li></ul><h2>4. Luong xu ly request</h2><p>Mot request thong thuong di theo huong: trinh duyet gui HTTP request den Controller, Controller kiem tra input co ban va goi Service, Service xu ly nghiep vu va goi Repository, Repository truy van PostgreSQL, sau do ket qua duoc tra nguoc ve template de hien thi.</p><h2>5. Nguyen tac quan trong</h2><p>Controller nen mong, Service nen ro nghiep vu, Repository khong nen chua logic phuc tap cua business. Cach tach lop nay giup code de doc, de test va it bi roi khi project lon hon.</p>',
            'https://www.youtube.com/watch?v=vtPkZShrvXQ',
            1,
            true
        ),
        (
            'spring-boot-cho-nguoi-moi-bat-dau',
            'Tao Controller va hien thi Thymeleaf',
            '<h2>1. Controller trong Spring MVC</h2><p>Controller la noi tiep nhan request tu nguoi dung. Trong ung dung web dung Thymeleaf, controller thuong tra ve ten template thay vi tra JSON. Vi du khi nguoi dung vao trang danh sach khoa hoc, controller se lay danh sach khoa hoc tu service, dua vao Model, roi tra ve template HTML.</p><h2>2. Annotation thuong dung</h2><ul><li><strong>@Controller</strong>: danh dau class la controller tra ve view.</li><li><strong>@GetMapping</strong>: xu ly request GET, thuong dung de hien thi trang.</li><li><strong>@PostMapping</strong>: xu ly request POST, thuong dung khi submit form.</li><li><strong>@PathVariable</strong>: lay bien tu URL, vi du /courses/{id}.</li><li><strong>@RequestParam</strong>: lay tham so query hoac form don gian.</li><li><strong>@ModelAttribute</strong>: bind du lieu form vao object.</li></ul><h2>3. Model va Thymeleaf</h2><p>Model la noi controller dat du lieu de template su dung. Vi du model.addAttribute("courses", courses) cho phep file HTML dung th:each de lap qua danh sach khoa hoc. Thymeleaf giup render du lieu server-side, phu hop voi project Spring Boot MVC khong can frontend rieng.</p><h2>4. Xu ly form</h2><p>Khi submit form tao khoa hoc, controller nhan CourseForm, validate du lieu, neu co loi thi tra lai trang form kem thong bao loi. Neu hop le, controller goi CourseService de tao khoa hoc. Khong nen dua logic upload anh, tinh gia, kiem tra quyen phuc tap vao controller vi se lam controller kho bao tri.</p><h2>5. Loi thuc te can tranh</h2><ul><li>Tra entity truc tiep ra view khi co truong nhay cam.</li><li>Dat qua nhieu logic nghiep vu trong controller.</li><li>Khong xu ly empty state khi danh sach rong.</li><li>Khong validate du lieu dau vao truoc khi luu.</li></ul>',
            'https://www.youtube.com/watch?v=31KTdfRH6nY',
            2,
            false
        ),
        (
            'spring-boot-cho-nguoi-moi-bat-dau',
            'Ket noi PostgreSQL voi Spring Data JPA',
            '<h2>1. Spring Data JPA la gi?</h2><p>Spring Data JPA la thu vien giup thao tac database thong qua repository interface. Thay vi viet SQL cho cac thao tac CRUD co ban, lap trinh vien chi can tao interface ke thua JpaRepository. Spring se tu sinh implementation khi ung dung chay.</p><h2>2. Entity va bang du lieu</h2><p>Entity la class Java dai dien cho mot bang. Moi entity can co @Entity, khoa chinh @Id va chien luoc sinh id nhu GenerationType.IDENTITY. Cac quan he nhu @ManyToOne, @OneToMany, @ManyToMany giup mo ta lien ket giua cac bang. Vi du Course co nhieu Lesson, moi Lesson thuoc mot Course.</p><h2>3. Repository</h2><p>Repository cho phep goi cac ham nhu findById, findAll, save, deleteById. Ngoai ra co the khai bao method theo ten nhu findByEmail, findBySlug, findByStatus. Spring Data JPA se doc ten method va sinh query tuong ung.</p><h2>4. Transaction</h2><p>Transaction dam bao nhieu thao tac database thanh cong cung nhau hoac that bai cung nhau. Vi du khi thanh toan thanh cong, he thong can cap nhat Payment, Order, Enrollment va Cart. Neu mot buoc loi ma khong co transaction, du lieu co the bi lech trang thai.</p><h2>5. Luu y khi dung JPA</h2><ul><li>Dat unique constraint cho email, slug, coupon code neu nghiep vu yeu cau khong trung.</li><li>Can canh giac N+1 query khi doc quan he lazy trong danh sach lon.</li><li>Khong dung ddl-auto=update cho production neu chua co quy trinh migration ro rang.</li><li>Khong luu password plain text, phai ma hoa bang BCrypt.</li></ul>',
            'https://www.youtube.com/watch?v=8SGI_XS5OPw',
            3,
            false
        ),
        (
            'thiet-ke-ui-web-voi-bootstrap-va-thymeleaf',
            'Xay dung layout chung',
            '<h2>1. Layout chung la gi?</h2><p>Layout chung la khung giao dien duoc tai su dung o nhieu trang, thuong gom header, navigation, footer, khu vuc hien thi thong bao va phan noi dung chinh. Trong Thymeleaf, co the dung fragment de tach cac thanh phan lap lai thanh file rieng, giup giao dien dong nhat va de sua.</p><h2>2. Thanh phan nen co</h2><ul><li><strong>Header</strong>: logo, menu, nut dang nhap/dang xuat, gio hang.</li><li><strong>Navigation</strong>: lien ket den trang chu, khoa hoc, khoa hoc cua toi, admin.</li><li><strong>Main content</strong>: noi dung rieng cua tung trang.</li><li><strong>Footer</strong>: thong tin he thong, ban quyen, lien he.</li><li><strong>Alert area</strong>: hien thi thong bao thanh cong hoac loi.</li></ul><h2>3. Bootstrap grid</h2><p>Bootstrap su dung he thong container, row va col de chia cot responsive. Container giup can noi dung theo chieu ngang, row tao hang, col chia cot. Khi thiet ke trang danh sach khoa hoc, co the dung grid 3 cot tren desktop, 2 cot tren tablet va 1 cot tren mobile.</p><h2>4. Nguyen tac UI co ban</h2><p>Giao dien e-learning can de doc, de quet thong tin va khong lam nguoi hoc roi. Tieu de khoa hoc can ro, gia va nut hanh dong phai de thay, anh thumbnail can cung ti le de card khong bi lech. Khoang cach giua cac phan nen vua du, khong qua day dac nhung cung khong qua thua.</p><h2>5. Loi can tranh</h2><ul><li>Lap lai header/footer trong tung file template.</li><li>Khong co responsive cho mobile.</li><li>Nut hanh dong khong noi bat.</li><li>Thong bao loi/thanh cong khong dong nhat.</li></ul>',
            'https://www.youtube.com/watch?v=-qfEOE4vtxE',
            1,
            true
        ),
        (
            'thiet-ke-ui-web-voi-bootstrap-va-thymeleaf',
            'Thiet ke form than thien',
            '<h2>1. Vai tro cua form</h2><p>Form la noi nguoi dung nhap du lieu vao he thong, vi du dang ky tai khoan, tao khoa hoc, them bai hoc, nhap coupon hoac danh gia khoa hoc. Form tot giup nguoi dung hieu can nhap gi, nhap sai o dau va sua nhu the nao.</p><h2>2. Thanh phan cua form tot</h2><ul><li><strong>Label ro rang</strong>: khong chi dua vao placeholder vi placeholder bien mat khi nguoi dung go.</li><li><strong>Input phu hop</strong>: email dung type email, gia tien dung number, noi dung dai dung textarea hoac rich text editor.</li><li><strong>Validation message</strong>: loi nen nam gan field bi loi.</li><li><strong>Required state</strong>: field bat buoc nen duoc danh dau ro.</li><li><strong>Submit button</strong>: noi dung nut nen mo ta hanh dong, vi du Luu khoa hoc, Tao bai hoc.</li></ul><h2>3. Validation hai lop</h2><p>Frontend validation giup phan hoi nhanh, nhung khong du an toan vi nguoi dung co the bypass trinh duyet. Backend validation moi la lop bat buoc. Trong Spring Boot, co the dung @Valid va cac annotation nhu @NotBlank, @Size, @Email, @Min de kiem tra request.</p><h2>4. Giu lai du lieu khi loi</h2><p>Khi submit form that bai, he thong nen hien lai form voi du lieu nguoi dung da nhap, kem thong bao loi. Neu xoa het form, nguoi dung phai nhap lai tu dau va trai nghiem rat te.</p><h2>5. Bao mat voi form</h2><ul><li>Bat CSRF cho thao tac thay doi du lieu.</li><li>Khong tin du lieu tu client.</li><li>Sanitize HTML neu cho nhap rich text.</li><li>Khong hien stack trace ra giao dien production.</li></ul>',
            'https://www.youtube.com/watch?v=Jyvffr3aCp0',
            2,
            false
        ),
        (
            'thiet-ke-ui-web-voi-bootstrap-va-thymeleaf',
            'Responsive cho trang khoa hoc',
            '<h2>1. Responsive la gi?</h2><p>Responsive design la cach thiet ke de giao dien hien thi tot tren nhieu kich thuoc man hinh: desktop, laptop, tablet va mobile. Voi nen tang e-learning, nguoi hoc co the xem khoa hoc tren dien thoai, vi vay trang khoa hoc khong duoc chi dep tren man hinh lon.</p><h2>2. Uu tien noi dung</h2><p>Tren mobile, dien tich ngang bi han che. Can dua thong tin quan trong len truoc: ten khoa hoc, mo ta ngan, gia, nut mua/hoc tiep. Cac thong tin phu nhu danh muc, tac gia, danh gia co the dat ben duoi hoac trong cac khu vuc gon hon.</p><h2>3. Card khoa hoc</h2><p>Card khoa hoc nen co anh thumbnail cung ti le, tieu de khong tran khoi card, mo ta ngan co gioi han dong, gia de thay va nut hanh dong ro. Neu card cao thap khac nhau qua nhieu, danh sach se trong roi mat.</p><h2>4. Breakpoint voi Bootstrap</h2><ul><li>Mobile: 1 cot, uu tien doc tu tren xuong duoi.</li><li>Tablet: 2 cot neu noi dung vua.</li><li>Desktop: 3 hoac 4 cot tuy do rong va mat do thong tin.</li></ul><h2>5. Kiem tra thuc te</h2><p>Khong chi resize bang mat. Can test cac trang quan trong: trang chu, danh sach khoa hoc, chi tiet khoa hoc, gio hang, checkout, bai hoc va admin form. Kiem tra text co bi tran, nut co qua nho, anh co meo va layout co bi vo khong.</p>',
            'https://www.youtube.com/watch?v=Qhaz36TZG5Y',
            3,
            false
        ),
        (
            'ky-nang-hoc-online-hieu-qua',
            'Dat muc tieu hoc tap',
            '<h2>1. Vi sao can dat muc tieu?</h2><p>Hoc online de bi bo do vi khong co lich hoc co dinh va khong co nguoi nhac truc tiep. Muc tieu giup nguoi hoc biet minh dang hoc de lam gi, can hoan thanh trong bao lau va ket qua nao duoc xem la dat.</p><h2>2. Muc tieu tot theo SMART</h2><ul><li><strong>Specific</strong>: cu the, khong noi chung chung.</li><li><strong>Measurable</strong>: do duoc, vi du hoan thanh 3 bai moi tuan.</li><li><strong>Achievable</strong>: vua suc voi thoi gian va nang luc hien tai.</li><li><strong>Relevant</strong>: lien quan den nhu cau hoc tap hoac cong viec.</li><li><strong>Time-bound</strong>: co han hoan thanh ro rang.</li></ul><h2>3. Vi du muc tieu</h2><p>Thay vi noi "toi muon hoc Spring Boot", hay dat muc tieu: "Trong 2 tuan, toi hoan thanh 6 bai Spring Boot, lam du quiz dat tren 70% va tao duoc mot trang CRUD don gian". Muc tieu nay ro rang hon va de theo doi tien do.</p><h2>4. Chia nho muc tieu</h2><p>Muc tieu lon nen duoc chia thanh cac moc nho theo ngay hoac theo tuan. Moi moc nho nen co dau ra cu the, vi du doc xong ly thuyet, lam bai quiz, viet lai ghi chu, hoac ap dung vao mot bai tap nho.</p><h2>5. Tu danh gia</h2><p>Sau moi buoi hoc, nguoi hoc nen tu hoi: minh da hieu phan nao, phan nao con mo ho, can hoi ai hoac can xem lai bai nao. Thoi quen nay giup viec hoc chu dong hon.</p>',
            'https://www.youtube.com/watch?v=XpKvs-apvOs',
            1,
            true
        ),
        (
            'ky-nang-hoc-online-hieu-qua',
            'Ghi chu va on tap',
            '<h2>1. Ghi chu de hieu, khong phai de chep lai</h2><p>Ghi chu tot khong phai la chep lai toan bo noi dung bai hoc. Muc tieu cua ghi chu la tom tat y chinh bang ngon ngu cua minh, ghi lai vi du quan trong va danh dau nhung phan chua hieu de xem lai.</p><h2>2. Cau truc ghi chu don gian</h2><ul><li><strong>Y chinh</strong>: bai hoc noi ve van de gi.</li><li><strong>Khai niem</strong>: cac thuat ngu hoac dinh nghia quan trong.</li><li><strong>Vi du</strong>: tinh huong ap dung thuc te.</li><li><strong>Loi hay gap</strong>: nhung diem de nham lan.</li><li><strong>Cau hoi on tap</strong>: 3-5 cau tu dat sau khi hoc.</li></ul><h2>3. Phuong phap on tap chu dong</h2><p>Doc lai ghi chu mot cach thu dong thuong khong du. Nen che noi dung lai va tu tra loi cau hoi, lam quiz, giai thich lai cho nguoi khac hoac viet mot vi du moi. Khi phai tu lay kien thuc ra khoi tri nho, viec ghi nho se ben hon.</p><h2>4. On tap lap lai</h2><p>Thay vi hoc mot lan that lau, nen on lai theo chu ky: sau 1 ngay, sau 3 ngay, sau 1 tuan. Moi lan on chi can ngan hon nhung tap trung vao phan hay quen hoac lam sai.</p><h2>5. Ap dung vao e-learning</h2><p>Sau moi bai hoc, nguoi hoc nen doc phan ly thuyet, ghi lai 5 y chinh, lam quiz neu co, xem lai dap an sai va cap nhat ghi chu. Neu bai hoc co video, khong nen chi xem video ma khong tu tong hop.</p>',
            'https://www.youtube.com/watch?v=E7CwqNHn_Ns',
            2,
            false
        ),
        (
            'ky-nang-hoc-online-hieu-qua',
            'Theo doi tien do va hoan thanh khoa hoc',
            '<h2>1. Vi sao can theo doi tien do?</h2><p>Theo doi tien do giup nguoi hoc biet minh da hoan thanh bao nhieu, con bao nhieu bai va dang cham o dau. Neu khong theo doi, nguoi hoc rat de cam thay minh "dang hoc" nhung thuc te khong tien gan den ket qua.</p><h2>2. Cac chi so nen theo doi</h2><ul><li>So bai da hoan thanh tren tong so bai.</li><li>Diem quiz gan nhat va so lan lam lai.</li><li>Thoi gian hoc moi tuan.</li><li>Nhung chu de con yeu.</li><li>Moc ngay du kien hoan thanh khoa hoc.</li></ul><h2>3. Hoan thanh bai hoc dung nghia</h2><p>Danh dau hoan thanh khong nen chi la bam nut. Voi bai co quiz, nguoi hoc nen dat diem toi thieu, vi du 70%, de chung minh da nam noi dung co ban. Voi bai khong co quiz, nguoi hoc nen tu tom tat duoc noi dung hoac ap dung duoc vao bai tap nho.</p><h2>4. Xu ly khi bi tre tien do</h2><p>Bi tre tien do la binh thuong. Dieu quan trong la dieu chinh ke hoach: giam muc tieu moi ngay, uu tien bai quan trong, bo sung thoi gian on tap hoac hoi giang vien. Khong nen bo ca khoa hoc chi vi cham mot vai ngay.</p><h2>5. Ket thuc khoa hoc</h2><p>Khi hoan thanh 100% bai hoc, nguoi hoc nen xem lai ghi chu, lam lai cac quiz sai nhieu, tong hop mot san pham nho va luu chung chi. Chung chi co y nghia hon khi di kem bang chung rang nguoi hoc thuc su ap dung duoc kien thuc.</p>',
            'https://www.youtube.com/watch?v=Z-zNHHpXoMM',
            3,
            false
        )
)
INSERT INTO lessons (course_id, title, content, video_url, order_index, preview)
SELECT c.id, s.title, s.content, s.video_url, s.order_index, s.preview
FROM lesson_seed s
JOIN course_ref c ON c.slug = s.course_slug
WHERE NOT EXISTS (
    SELECT 1
    FROM lessons l
    WHERE l.course_id = c.id
      AND l.title = s.title
);

WITH course_ref AS (
    SELECT id, slug FROM courses
    WHERE slug IN (
        'spring-boot-cho-nguoi-moi-bat-dau',
        'thiet-ke-ui-web-voi-bootstrap-va-thymeleaf',
        'ky-nang-hoc-online-hieu-qua'
    )
),
lesson_seed (course_slug, title, content, video_url, order_index, preview) AS (
    VALUES
        ('spring-boot-cho-nguoi-moi-bat-dau', 'Tong quan Spring Boot', '<h2>1. Spring Boot la gi?</h2><p>Spring Boot la mot phan mo rong cua Spring Framework, duoc tao ra de giup lap trinh vien xay dung ung dung Java nhanh hon, it cau hinh thu cong hon va de trien khai hon. Neu Spring Framework truyen thong yeu cau khai bao nhieu cau hinh XML hoac Java Config, Spring Boot su dung co che auto-configuration de tu cau hinh cac thanh phan pho bien dua tren dependency co trong project.</p><h2>2. Vi sao nen dung Spring Boot?</h2><p>Spring Boot phu hop cho ung dung web, REST API, he thong quan ly, e-learning, thuong mai dien tu va nhieu loai backend khac. Loi ich chinh la khoi tao nhanh, tich hop san Tomcat, quan ly dependency theo starter, ho tro cau hinh qua application.properties va co he sinh thai lon nhu Spring Data JPA, Spring Security, Validation, Thymeleaf.</p><h2>3. Cau truc project co ban</h2><ul><li><strong>Controller</strong>: nhan request tu trinh duyet, goi service va tra view hoac JSON.</li><li><strong>Service</strong>: chua logic nghiep vu, vi du tinh gia, tao don hang, ghi danh hoc vien.</li><li><strong>Repository</strong>: lam viec voi database thong qua Spring Data JPA.</li><li><strong>Entity</strong>: dai dien cho bang trong database.</li><li><strong>Template</strong>: giao dien Thymeleaf render HTML cho nguoi dung.</li></ul><h2>4. Luong xu ly request</h2><p>Mot request thong thuong di theo huong: trinh duyet gui HTTP request den Controller, Controller kiem tra input co ban va goi Service, Service xu ly nghiep vu va goi Repository, Repository truy van PostgreSQL, sau do ket qua duoc tra nguoc ve template de hien thi.</p><h2>5. Nguyen tac quan trong</h2><p>Controller nen mong, Service nen ro nghiep vu, Repository khong nen chua logic phuc tap cua business. Cach tach lop nay giup code de doc, de test va it bi roi khi project lon hon.</p>', 'https://www.youtube.com/watch?v=vtPkZShrvXQ', 1, true),
        ('spring-boot-cho-nguoi-moi-bat-dau', 'Tao Controller va hien thi Thymeleaf', '<h2>1. Controller trong Spring MVC</h2><p>Controller la noi tiep nhan request tu nguoi dung. Trong ung dung web dung Thymeleaf, controller thuong tra ve ten template thay vi tra JSON. Vi du khi nguoi dung vao trang danh sach khoa hoc, controller se lay danh sach khoa hoc tu service, dua vao Model, roi tra ve template HTML.</p><h2>2. Annotation thuong dung</h2><ul><li><strong>@Controller</strong>: danh dau class la controller tra ve view.</li><li><strong>@GetMapping</strong>: xu ly request GET, thuong dung de hien thi trang.</li><li><strong>@PostMapping</strong>: xu ly request POST, thuong dung khi submit form.</li><li><strong>@PathVariable</strong>: lay bien tu URL, vi du /courses/{id}.</li><li><strong>@RequestParam</strong>: lay tham so query hoac form don gian.</li><li><strong>@ModelAttribute</strong>: bind du lieu form vao object.</li></ul><h2>3. Model va Thymeleaf</h2><p>Model la noi controller dat du lieu de template su dung. Vi du model.addAttribute("courses", courses) cho phep file HTML dung th:each de lap qua danh sach khoa hoc. Thymeleaf giup render du lieu server-side, phu hop voi project Spring Boot MVC khong can frontend rieng.</p><h2>4. Xu ly form</h2><p>Khi submit form tao khoa hoc, controller nhan CourseForm, validate du lieu, neu co loi thi tra lai trang form kem thong bao loi. Neu hop le, controller goi CourseService de tao khoa hoc. Khong nen dua logic upload anh, tinh gia, kiem tra quyen phuc tap vao controller vi se lam controller kho bao tri.</p><h2>5. Loi thuc te can tranh</h2><ul><li>Tra entity truc tiep ra view khi co truong nhay cam.</li><li>Dat qua nhieu logic nghiep vu trong controller.</li><li>Khong xu ly empty state khi danh sach rong.</li><li>Khong validate du lieu dau vao truoc khi luu.</li></ul>', 'https://www.youtube.com/watch?v=31KTdfRH6nY', 2, false),
        ('spring-boot-cho-nguoi-moi-bat-dau', 'Ket noi PostgreSQL voi Spring Data JPA', '<h2>1. Spring Data JPA la gi?</h2><p>Spring Data JPA la thu vien giup thao tac database thong qua repository interface. Thay vi viet SQL cho cac thao tac CRUD co ban, lap trinh vien chi can tao interface ke thua JpaRepository. Spring se tu sinh implementation khi ung dung chay.</p><h2>2. Entity va bang du lieu</h2><p>Entity la class Java dai dien cho mot bang. Moi entity can co @Entity, khoa chinh @Id va chien luoc sinh id nhu GenerationType.IDENTITY. Cac quan he nhu @ManyToOne, @OneToMany, @ManyToMany giup mo ta lien ket giua cac bang. Vi du Course co nhieu Lesson, moi Lesson thuoc mot Course.</p><h2>3. Repository</h2><p>Repository cho phep goi cac ham nhu findById, findAll, save, deleteById. Ngoai ra co the khai bao method theo ten nhu findByEmail, findBySlug, findByStatus. Spring Data JPA se doc ten method va sinh query tuong ung.</p><h2>4. Transaction</h2><p>Transaction dam bao nhieu thao tac database thanh cong cung nhau hoac that bai cung nhau. Vi du khi thanh toan thanh cong, he thong can cap nhat Payment, Order, Enrollment va Cart. Neu mot buoc loi ma khong co transaction, du lieu co the bi lech trang thai.</p><h2>5. Luu y khi dung JPA</h2><ul><li>Dat unique constraint cho email, slug, coupon code neu nghiep vu yeu cau khong trung.</li><li>Can canh giac N+1 query khi doc quan he lazy trong danh sach lon.</li><li>Khong dung ddl-auto=update cho production neu chua co quy trinh migration ro rang.</li><li>Khong luu password plain text, phai ma hoa bang BCrypt.</li></ul>', 'https://www.youtube.com/watch?v=8SGI_XS5OPw', 3, false),
        ('thiet-ke-ui-web-voi-bootstrap-va-thymeleaf', 'Xay dung layout chung', '<h2>1. Layout chung la gi?</h2><p>Layout chung la khung giao dien duoc tai su dung o nhieu trang, thuong gom header, navigation, footer, khu vuc hien thi thong bao va phan noi dung chinh. Trong Thymeleaf, co the dung fragment de tach cac thanh phan lap lai thanh file rieng, giup giao dien dong nhat va de sua.</p><h2>2. Thanh phan nen co</h2><ul><li><strong>Header</strong>: logo, menu, nut dang nhap/dang xuat, gio hang.</li><li><strong>Navigation</strong>: lien ket den trang chu, khoa hoc, khoa hoc cua toi, admin.</li><li><strong>Main content</strong>: noi dung rieng cua tung trang.</li><li><strong>Footer</strong>: thong tin he thong, ban quyen, lien he.</li><li><strong>Alert area</strong>: hien thi thong bao thanh cong hoac loi.</li></ul><h2>3. Bootstrap grid</h2><p>Bootstrap su dung he thong container, row va col de chia cot responsive. Container giup can noi dung theo chieu ngang, row tao hang, col chia cot. Khi thiet ke trang danh sach khoa hoc, co the dung grid 3 cot tren desktop, 2 cot tren tablet va 1 cot tren mobile.</p><h2>4. Nguyen tac UI co ban</h2><p>Giao dien e-learning can de doc, de quet thong tin va khong lam nguoi hoc roi. Tieu de khoa hoc can ro, gia va nut hanh dong phai de thay, anh thumbnail can cung ti le de card khong bi lech. Khoang cach giua cac phan nen vua du, khong qua day dac nhung cung khong qua thua.</p><h2>5. Loi can tranh</h2><ul><li>Lap lai header/footer trong tung file template.</li><li>Khong co responsive cho mobile.</li><li>Nut hanh dong khong noi bat.</li><li>Thong bao loi/thanh cong khong dong nhat.</li></ul>', 'https://www.youtube.com/watch?v=-qfEOE4vtxE', 1, true),
        ('thiet-ke-ui-web-voi-bootstrap-va-thymeleaf', 'Thiet ke form than thien', '<h2>1. Vai tro cua form</h2><p>Form la noi nguoi dung nhap du lieu vao he thong, vi du dang ky tai khoan, tao khoa hoc, them bai hoc, nhap coupon hoac danh gia khoa hoc. Form tot giup nguoi dung hieu can nhap gi, nhap sai o dau va sua nhu the nao.</p><h2>2. Thanh phan cua form tot</h2><ul><li><strong>Label ro rang</strong>: khong chi dua vao placeholder vi placeholder bien mat khi nguoi dung go.</li><li><strong>Input phu hop</strong>: email dung type email, gia tien dung number, noi dung dai dung textarea hoac rich text editor.</li><li><strong>Validation message</strong>: loi nen nam gan field bi loi.</li><li><strong>Required state</strong>: field bat buoc nen duoc danh dau ro.</li><li><strong>Submit button</strong>: noi dung nut nen mo ta hanh dong, vi du Luu khoa hoc, Tao bai hoc.</li></ul><h2>3. Validation hai lop</h2><p>Frontend validation giup phan hoi nhanh, nhung khong du an toan vi nguoi dung co the bypass trinh duyet. Backend validation moi la lop bat buoc. Trong Spring Boot, co the dung @Valid va cac annotation nhu @NotBlank, @Size, @Email, @Min de kiem tra request.</p><h2>4. Giu lai du lieu khi loi</h2><p>Khi submit form that bai, he thong nen hien lai form voi du lieu nguoi dung da nhap, kem thong bao loi. Neu xoa het form, nguoi dung phai nhap lai tu dau va trai nghiem rat te.</p><h2>5. Bao mat voi form</h2><ul><li>Bat CSRF cho thao tac thay doi du lieu.</li><li>Khong tin du lieu tu client.</li><li>Sanitize HTML neu cho nhap rich text.</li><li>Khong hien stack trace ra giao dien production.</li></ul>', 'https://www.youtube.com/watch?v=Jyvffr3aCp0', 2, false),
        ('thiet-ke-ui-web-voi-bootstrap-va-thymeleaf', 'Responsive cho trang khoa hoc', '<h2>1. Responsive la gi?</h2><p>Responsive design la cach thiet ke de giao dien hien thi tot tren nhieu kich thuoc man hinh: desktop, laptop, tablet va mobile. Voi nen tang e-learning, nguoi hoc co the xem khoa hoc tren dien thoai, vi vay trang khoa hoc khong duoc chi dep tren man hinh lon.</p><h2>2. Uu tien noi dung</h2><p>Tren mobile, dien tich ngang bi han che. Can dua thong tin quan trong len truoc: ten khoa hoc, mo ta ngan, gia, nut mua/hoc tiep. Cac thong tin phu nhu danh muc, tac gia, danh gia co the dat ben duoi hoac trong cac khu vuc gon hon.</p><h2>3. Card khoa hoc</h2><p>Card khoa hoc nen co anh thumbnail cung ti le, tieu de khong tran khoi card, mo ta ngan co gioi han dong, gia de thay va nut hanh dong ro. Neu card cao thap khac nhau qua nhieu, danh sach se trong roi mat.</p><h2>4. Breakpoint voi Bootstrap</h2><ul><li>Mobile: 1 cot, uu tien doc tu tren xuong duoi.</li><li>Tablet: 2 cot neu noi dung vua.</li><li>Desktop: 3 hoac 4 cot tuy do rong va mat do thong tin.</li></ul><h2>5. Kiem tra thuc te</h2><p>Khong chi resize bang mat. Can test cac trang quan trong: trang chu, danh sach khoa hoc, chi tiet khoa hoc, gio hang, checkout, bai hoc va admin form. Kiem tra text co bi tran, nut co qua nho, anh co meo va layout co bi vo khong.</p>', 'https://www.youtube.com/watch?v=Qhaz36TZG5Y', 3, false),
        ('ky-nang-hoc-online-hieu-qua', 'Dat muc tieu hoc tap', '<h2>1. Vi sao can dat muc tieu?</h2><p>Hoc online de bi bo do vi khong co lich hoc co dinh va khong co nguoi nhac truc tiep. Muc tieu giup nguoi hoc biet minh dang hoc de lam gi, can hoan thanh trong bao lau va ket qua nao duoc xem la dat.</p><h2>2. Muc tieu tot theo SMART</h2><ul><li><strong>Specific</strong>: cu the, khong noi chung chung.</li><li><strong>Measurable</strong>: do duoc, vi du hoan thanh 3 bai moi tuan.</li><li><strong>Achievable</strong>: vua suc voi thoi gian va nang luc hien tai.</li><li><strong>Relevant</strong>: lien quan den nhu cau hoc tap hoac cong viec.</li><li><strong>Time-bound</strong>: co han hoan thanh ro rang.</li></ul><h2>3. Vi du muc tieu</h2><p>Thay vi noi "toi muon hoc Spring Boot", hay dat muc tieu: "Trong 2 tuan, toi hoan thanh 6 bai Spring Boot, lam du quiz dat tren 70% va tao duoc mot trang CRUD don gian". Muc tieu nay ro rang hon va de theo doi tien do.</p><h2>4. Chia nho muc tieu</h2><p>Muc tieu lon nen duoc chia thanh cac moc nho theo ngay hoac theo tuan. Moi moc nho nen co dau ra cu the, vi du doc xong ly thuyet, lam bai quiz, viet lai ghi chu, hoac ap dung vao mot bai tap nho.</p><h2>5. Tu danh gia</h2><p>Sau moi buoi hoc, nguoi hoc nen tu hoi: minh da hieu phan nao, phan nao con mo ho, can hoi ai hoac can xem lai bai nao. Thoi quen nay giup viec hoc chu dong hon.</p>', 'https://www.youtube.com/watch?v=XpKvs-apvOs', 1, true),
        ('ky-nang-hoc-online-hieu-qua', 'Ghi chu va on tap', '<h2>1. Ghi chu de hieu, khong phai de chep lai</h2><p>Ghi chu tot khong phai la chep lai toan bo noi dung bai hoc. Muc tieu cua ghi chu la tom tat y chinh bang ngon ngu cua minh, ghi lai vi du quan trong va danh dau nhung phan chua hieu de xem lai.</p><h2>2. Cau truc ghi chu don gian</h2><ul><li><strong>Y chinh</strong>: bai hoc noi ve van de gi.</li><li><strong>Khai niem</strong>: cac thuat ngu hoac dinh nghia quan trong.</li><li><strong>Vi du</strong>: tinh huong ap dung thuc te.</li><li><strong>Loi hay gap</strong>: nhung diem de nham lan.</li><li><strong>Cau hoi on tap</strong>: 3-5 cau tu dat sau khi hoc.</li></ul><h2>3. Phuong phap on tap chu dong</h2><p>Doc lai ghi chu mot cach thu dong thuong khong du. Nen che noi dung lai va tu tra loi cau hoi, lam quiz, giai thich lai cho nguoi khac hoac viet mot vi du moi. Khi phai tu lay kien thuc ra khoi tri nho, viec ghi nho se ben hon.</p><h2>4. On tap lap lai</h2><p>Thay vi hoc mot lan that lau, nen on lai theo chu ky: sau 1 ngay, sau 3 ngay, sau 1 tuan. Moi lan on chi can ngan hon nhung tap trung vao phan hay quen hoac lam sai.</p><h2>5. Ap dung vao e-learning</h2><p>Sau moi bai hoc, nguoi hoc nen doc phan ly thuyet, ghi lai 5 y chinh, lam quiz neu co, xem lai dap an sai va cap nhat ghi chu. Neu bai hoc co video, khong nen chi xem video ma khong tu tong hop.</p>', 'https://www.youtube.com/watch?v=E7CwqNHn_Ns', 2, false),
        ('ky-nang-hoc-online-hieu-qua', 'Theo doi tien do va hoan thanh khoa hoc', '<h2>1. Vi sao can theo doi tien do?</h2><p>Theo doi tien do giup nguoi hoc biet minh da hoan thanh bao nhieu, con bao nhieu bai va dang cham o dau. Neu khong theo doi, nguoi hoc rat de cam thay minh "dang hoc" nhung thuc te khong tien gan den ket qua.</p><h2>2. Cac chi so nen theo doi</h2><ul><li>So bai da hoan thanh tren tong so bai.</li><li>Diem quiz gan nhat va so lan lam lai.</li><li>Thoi gian hoc moi tuan.</li><li>Nhung chu de con yeu.</li><li>Moc ngay du kien hoan thanh khoa hoc.</li></ul><h2>3. Hoan thanh bai hoc dung nghia</h2><p>Danh dau hoan thanh khong nen chi la bam nut. Voi bai co quiz, nguoi hoc nen dat diem toi thieu, vi du 70%, de chung minh da nam noi dung co ban. Voi bai khong co quiz, nguoi hoc nen tu tom tat duoc noi dung hoac ap dung duoc vao bai tap nho.</p><h2>4. Xu ly khi bi tre tien do</h2><p>Bi tre tien do la binh thuong. Dieu quan trong la dieu chinh ke hoach: giam muc tieu moi ngay, uu tien bai quan trong, bo sung thoi gian on tap hoac hoi giang vien. Khong nen bo ca khoa hoc chi vi cham mot vai ngay.</p><h2>5. Ket thuc khoa hoc</h2><p>Khi hoan thanh 100% bai hoc, nguoi hoc nen xem lai ghi chu, lam lai cac quiz sai nhieu, tong hop mot san pham nho va luu chung chi. Chung chi co y nghia hon khi di kem bang chung rang nguoi hoc thuc su ap dung duoc kien thuc.</p>', 'https://www.youtube.com/watch?v=Z-zNHHpXoMM', 3, false)
)
UPDATE lessons l
SET
    content = s.content,
    video_url = s.video_url,
    order_index = s.order_index,
    preview = s.preview
FROM lesson_seed s
JOIN course_ref c ON c.slug = s.course_slug
WHERE l.course_id = c.id
  AND l.title = s.title;

COMMIT;
