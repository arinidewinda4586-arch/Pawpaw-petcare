-- =============================================================================
-- PAWPAW PET CARE - FULL MYSQL / PHPMYADMIN DATABASE DUMP
-- Database   : pet_care
-- Encoding   : UTF-8 (utf8mb4 - Supports Emoji & Full Schema)
-- =============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

CREATE DATABASE IF NOT EXISTS `pet_care` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pet_care`;

DROP TABLE IF EXISTS `payment`;
DROP TABLE IF EXISTS `order_detail`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `health_booking`;
DROP TABLE IF EXISTS `health_service`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `adoption_request`;
DROP TABLE IF EXISTS `animal`;
DROP TABLE IF EXISTS `pet`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `migrations`;

-- --------------------------------------------------------
-- Struktur tabel `user`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `user_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `name` varchar(150) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('admin','user') NOT NULL DEFAULT 'user',
  `phone` varchar(30) DEFAULT '-',
  `address` text DEFAULT NULL,
  `bio` text DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `remember_token` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `user` (12 baris)
INSERT INTO `user` (`user_id`, `username`, `name`, `email`, `password`, `role`, `phone`, `address`, `bio`, `avatar`, `remember_token`, `created_at`, `updated_at`) VALUES
(1, 'jesika_putri', 'Jesika Putri', 'jesika@pawpaw.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000001', 'Jl. Subrantas Panam No. 10, Pekanbaru', 'Pecinta kucing dan pelanggan setia PawPaw', 'assets/img/jesika.jfif', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, 'arin_maharani', 'Arin Maharani', 'arin@pawpaw.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000002', 'Jl. HR Soebrantas No. 22, Pekanbaru', 'Suka merawat anabul kesayangan', 'assets/img/puncak-lukisem.jfif', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, 'farras_akbar', 'Farras Akbar', 'farras@pawpaw.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000003', 'Jl. Garuda Sakti KM 2, Pekanbaru', 'Cat & dog lover Pekanbaru', 'assets/img/farras.jfif', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, 'fakhry_ramadhan', 'Fakhry Ramadhan', 'fakhry@pawpaw.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000004', 'Jl. Bina Krida Panam No. 5, Pekanbaru', 'Selalu sedia vitamin untuk anabul', 'assets/img/fakhry.jfif', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, 'rizky_pratama', 'Rizky Pratama', 'rizky.pratama@gmail.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000005', 'Jl. Riau Ujung No. 45, Pekanbaru', 'Pemilik anjing Golden & Kucing Persia', NULL, NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, 'nabila_azzahra', 'Nabila Azzahra', 'nabila.azzahra@gmail.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000006', 'Jl. Arifin Ahmad No. 18, Pekanbaru', 'Senang mengadopsi hewan terlantar', NULL, NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, 'dimas_aditya', 'Dimas Aditya', 'dimas.aditya@gmail.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000007', 'Jl. Sudirman No. 88, Pekanbaru', 'Rutin cek kesehatan hewan setiap bulan', NULL, NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, 'siti_nurhaliza', 'Siti Nurhaliza', 'siti.nurhaliza@gmail.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000008', 'Jl. Tuanku Tambusai No. 31, Pekanbaru', 'Cat mom dari 3 kucing lucu', NULL, NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, 'bagas_saputra', 'Bagas Saputra', 'bagas.saputra@gmail.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000009', 'Jl. Soekarno Hatta No. 14, Pekanbaru', 'Hobi bermain bersama anjing peliharaan', NULL, NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(10, 'citra_lestari', 'Citra Lestari', 'citra.lestari@gmail.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081210000010', 'Jl. Delima Panam No. 9, Pekanbaru', 'Penyuka kelinci dan kucing anggora', NULL, NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(11, 'admin_pawpaw', 'Admin PawPaw', 'admin@pawpaw.com', '$2y$10$wxyCXe1bKS63tX9WIRE2r.NLrxFhWiva4BSK7APq8AP.4NNrwsfDe', 'admin', '081122334455', 'Jl. Kemang Raya No. 88, Jakarta Selatan', 'Administrator Sistem PawPaw Pet Care', NULL, NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(12, 'arini_putri', 'Arini Putri', 'arini@pawpaw.com', '$2y$10$VF2CcxNFK/HLebU2phJ4reQSop.ZCSmmp5iq5HPk2ConI8t6xAFkW', 'user', '081298765432', 'Jl. Melati Indah No. 12, Bandung', 'Pecinta kucing dan anjing.', NULL, NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11');

-- --------------------------------------------------------
-- Struktur tabel `pet`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pet` (
  `pet_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `pet_name` varchar(100) NOT NULL,
  `species` varchar(50) NOT NULL,
  `breed` varchar(100) DEFAULT 'Campuran',
  `gender` varchar(20) NOT NULL DEFAULT 'Jantan',
  `age` varchar(50) NOT NULL DEFAULT '1 Tahun',
  `user_id` int(10) unsigned NOT NULL,
  `health_status` varchar(255) DEFAULT 'Sehat',
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`pet_id`),
  KEY `fk_pet_user` (`user_id`),
  CONSTRAINT `fk_pet_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `pet` (10 baris)
INSERT INTO `pet` (`pet_id`, `pet_name`, `species`, `breed`, `gender`, `age`, `user_id`, `health_status`, `created_at`, `updated_at`) VALUES
(1, 'Milo', 'Kucing', 'Domestik', 'Jantan', '2 Tahun', 1, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, 'Luna', 'Anjing', 'Golden Retriever', 'Betina', '3 Tahun', 2, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, 'Mochi', 'Kucing', 'Persia', 'Jantan', '4 Tahun', 3, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, 'Oreo', 'Anjing', 'Beagle', 'Betina', '1 Tahun', 4, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, 'Coco', 'Kucing', 'Anggora', 'Jantan', '2 Tahun', 5, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, 'Bella', 'Anjing', 'Pomeranian', 'Betina', '3 Tahun', 6, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, 'Leo', 'Kucing', 'British Shorthair', 'Jantan', '4 Tahun', 7, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, 'Nala', 'Anjing', 'Husky', 'Betina', '1 Tahun', 8, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, 'Mimi', 'Kucing', 'Bengal', 'Jantan', '2 Tahun', 9, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(10, 'Snowy', 'Kelinci', 'Holland Lop', 'Betina', '1 Tahun', 10, 'Sehat', '2026-11-06 10:01:11', '2026-11-06 10:01:11');

-- --------------------------------------------------------
-- Struktur tabel `animal`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `animal` (
  `animal_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `nama` varchar(100) NOT NULL,
  `species` varchar(50) NOT NULL,
  `breed` varchar(100) DEFAULT NULL,
  `gender` varchar(20) NOT NULL DEFAULT 'Jantan',
  `age` varchar(50) NOT NULL DEFAULT '1 Tahun',
  `status` varchar(30) DEFAULT 'available',
  `image` varchar(255) DEFAULT 'assets/img/meng.png',
  `description` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`animal_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `animal` (10 baris)
INSERT INTO `animal` (`animal_id`, `nama`, `species`, `breed`, `gender`, `age`, `status`, `image`, `description`, `created_at`, `updated_at`) VALUES
(1, 'Siti', 'kucing', 'Anggora', 'Betina', '1 Tahun', 'available', 'assets/img/anggora.jpg', 'Kucing anggora jinak, aktif, sudah divaksin dasar, dan siap diadopsi oleh keluarga penyayang.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, 'Jeno', 'lainnya', 'African Pygmy', 'Jantan', '8 Bulan', 'available', 'assets/img/goat.jpg', 'Kambing mini ramah, sehat, dan sangat suka bermain di halaman.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, 'Jubariyah', 'kucing', 'Bengal', 'Betina', '13 Bulan', 'available', 'assets/img/bengal.jpg', 'Kucing Bengal lincah dengan corak bulu eksotis dan sangat manja.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, 'Noe', 'kelinci', 'Rex', 'Jantan', '10 Bulan', 'available', 'assets/img/rabbit.jpg', 'Kelinci Rex berbulu halus seperti beludru, bersih, dan tenang.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, 'Miko', 'kucing', 'Domestik', 'Jantan', '2 Tahun', 'available', 'assets/img/kucing.png', 'Kucing domestik sehat, pintar, dan mudah beradaptasi di rumah.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, 'Kiko', 'anjing', 'Campuran', 'Betina', '1 Tahun', 'available', 'assets/img/dog.png', 'Anjing ramah, setia, sudah vaksin lengkap, dan senang bermain bola.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, 'Tomi', 'kucing', 'Persia', 'Jantan', '2 Tahun', 'available', 'assets/img/meng.png', 'Kucing Persia berbulu lebat, terawat, dan sangat menyukai perhatian.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, 'Boni', 'anjing', 'Golden', 'Jantan', '3 Tahun', 'available', 'assets/img/dog.png', 'Anjing keluarga yang tenang, bersahabat dengan anak-anak, dan patuh.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, 'Lili', 'kucing', 'Domestik', 'Betina', '1 Tahun', 'available', 'assets/img/cuking.png', 'Kucing mungil yang ceria, sehat, dan sudah terlatih menggunakan litter box.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(10, 'Piko', 'kelinci', 'Holland Lop', 'Betina', '6 Bulan', 'available', 'assets/img/kelinci.png', 'Kelinci telinga terkulai yang lucu, jinak, dan suka makan wortel segar.', '2026-11-06 10:01:11', '2026-11-06 10:01:11');

-- --------------------------------------------------------
-- Struktur tabel `adoption_request`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `adoption_request` (
  `adoption_request_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `request_date` datetime NOT NULL DEFAULT current_timestamp(),
  `status` varchar(30) DEFAULT 'pending',
  `user_id` int(10) unsigned NOT NULL,
  `animal_id` int(10) unsigned NOT NULL,
  `applicant_name` varchar(150) NOT NULL,
  `email` varchar(150) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `address` text NOT NULL,
  `reason` text NOT NULL,
  `admin_note` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`adoption_request_id`),
  KEY `fk_adoption_user` (`user_id`),
  KEY `fk_adoption_animal` (`animal_id`),
  CONSTRAINT `fk_adoption_animal` FOREIGN KEY (`animal_id`) REFERENCES `animal` (`animal_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_adoption_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `adoption_request` (10 baris)
INSERT INTO `adoption_request` (`adoption_request_id`, `request_date`, `status`, `user_id`, `animal_id`, `applicant_name`, `email`, `phone`, `address`, `reason`, `admin_note`, `created_at`, `updated_at`) VALUES
(1, '2026-10-01 10:00:00', 'pending', 1, 1, 'Jesika Putri', 'jesika@pawpaw.com', '081210000001', 'Jl. Subrantas Panam No. 10, Pekanbaru', 'Ingin merawat dan memberikan rumah yang penuh kasih sayang.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, '2026-10-02 11:00:00', 'pending', 2, 2, 'Arin Maharani', 'arin@pawpaw.com', '081210000002', 'Jl. HR Soebrantas No. 22, Pekanbaru', 'Halaman rumah luas dan siap merawat dengan sepenuh hati.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, '2026-10-03 13:00:00', 'pending', 3, 3, 'Farras Akbar', 'farras@pawpaw.com', '081210000003', 'Jl. Garuda Sakti KM 2, Pekanbaru', 'Sudah berpengalaman memelihara kucing dan punya perlengkapan lengkap.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, '2026-10-04 14:00:00', 'pending', 4, 4, 'Fakhry Ramadhan', 'fakhry@pawpaw.com', '081210000004', 'Jl. Bina Krida Panam No. 5, Pekanbaru', 'Ingin menjadikan teman bermain di rumah yang nyaman.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, '2026-10-05 09:30:00', 'pending', 5, 5, 'Rizky Pratama', 'rizky.pratama@gmail.com', '081210000005', 'Jl. Riau Ujung No. 45, Pekanbaru', 'Keluarga kami sangat menyayangi hewan peliharaan.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, '2026-10-06 15:00:00', 'pending', 6, 6, 'Nabila Azzahra', 'nabila.azzahra@gmail.com', '081210000006', 'Jl. Arifin Ahmad No. 18, Pekanbaru', 'Ingin mengadopsi dan menjamin jadwal vaksinasi rutinnya.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, '2026-10-07 10:15:00', 'pending', 7, 7, 'Dimas Aditya', 'dimas.aditya@gmail.com', '081210000007', 'Jl. Sudirman No. 88, Pekanbaru', 'Mencari teman untuk kucing saya di rumah agar tidak kesepian.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, '2026-10-08 16:00:00', 'pending', 8, 8, 'Siti Nurhaliza', 'siti.nurhaliza@gmail.com', '081210000008', 'Jl. Tuanku Tambusai No. 31, Pekanbaru', 'Siap memberikan perawatan dan nutrisi terbaik setiap hari.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, '2026-10-09 12:00:00', 'pending', 9, 9, 'Bagas Saputra', 'bagas.saputra@gmail.com', '081210000009', 'Jl. Soekarno Hatta No. 14, Pekanbaru', 'Lingkungan rumah aman dan sangat ramah hewan.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(10, '2026-10-10 14:30:00', 'pending', 10, 10, 'Citra Lestari', 'citra.lestari@gmail.com', '081210000010', 'Jl. Delima Panam No. 9, Pekanbaru', 'Sangat suka kelinci dan sudah menyiapkan kandang indoor yang bersih.', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11');

-- --------------------------------------------------------
-- Struktur tabel `product`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product` (
  `product_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `product_name` varchar(150) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `stock` int(11) NOT NULL DEFAULT 0,
  `category` varchar(100) NOT NULL,
  `slug` varchar(150) DEFAULT NULL,
  `image` varchar(255) DEFAULT 'assets/img/shop/whiskas.png',
  `description` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `product` (10 baris)
INSERT INTO `product` (`product_id`, `product_name`, `price`, `stock`, `category`, `slug`, `image`, `description`, `created_at`, `updated_at`) VALUES
(1, 'Whiskas Adult Tuna 1 kg', '65000.00', 50, 'makanan', 'whiskas', 'assets/img/shop/whiskas.png', 'Makanan nutrisi lengkap dan seimbang untuk kucing dewasa.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, 'Pedigree Adult Beef 1 kg', '75000.00', 40, 'makanan', 'pedigree', 'assets/img/shop/pedigree.png', 'Makanan bergizi tinggi untuk kesehatan tulang dan bulu anjing dewasa.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, 'Pasir Kucing Gumpal Wangi 5L', '40000.00', 60, 'perawatan', 'pasir-kucing', 'assets/img/other.png', 'Pasir kucing bentonite daya serap tinggi dan mengunci bau tidak sedap.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, 'Shampoo Kucing Kulit Sensitif 250 ml', '35000.00', 35, 'perawatan', 'shampoo-kucing', 'assets/img/shop/shampoo-kucing.png', 'Formula lembut khusus menjaga kelembapan kulit dan bulu kucing kesayangan.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, 'Vitamin Bulu & Imun Hewan 60 ml', '55000.00', 25, 'perawatan', 'vitamin-hewan', 'assets/img/vaksin.png', 'Suplemen multivitamin penambah nafsu makan dan pelebat bulu anabul.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, 'Shampoo Anjing Anti Kutu 250 ml', '90000.00', 70, 'perawatan', 'shampoo-anjing', 'assets/img/shop/shampoo-anjing.png', 'Shampoo lembut anti kutu & menjaga kilau sehat bulu anjing.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, 'Mainan Tulang Karet Elastis', '15000.00', 80, 'mainan', 'tulang', 'assets/img/shop/mainan-tulang.png', 'Mainan gigit elastis & aman untuk melatih kesehatan gigi hewan.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, 'Mangkuk Makan Double Bowl', '25000.00', 45, 'perlengkapan', 'mangkuk-makan', 'assets/img/food.png', 'Tempat makan dan minum anti selip yang mudah dibersihkan.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, 'Kandang Portabel Lipat', '250000.00', 15, 'perlengkapan', 'kandang-portabel', 'assets/img/other.png', 'Kandang besi lipat kokoh dan nyaman untuk perjalanan maupun di rumah.', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(10, 'Sisir Grooming Anti Rontok', '30000.00', 55, 'perawatan', 'sisir', 'assets/img/shop/sisir-grooming.png', 'Sisir ergonomis untuk merapikan dan mengangkat bulu rontok dengan nyaman.', '2026-11-06 10:01:11', '2026-11-06 10:01:11');

-- --------------------------------------------------------
-- Struktur tabel `orders`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
  `order_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `order_date` datetime NOT NULL DEFAULT current_timestamp(),
  `total_price` decimal(12,2) NOT NULL,
  `status` varchar(50) DEFAULT 'pending',
  `user_id` int(10) unsigned NOT NULL,
  `order_code` varchar(50) DEFAULT NULL,
  `recipient_name` varchar(150) DEFAULT NULL,
  `recipient_phone` varchar(30) DEFAULT NULL,
  `delivery_method` varchar(50) DEFAULT 'pickup',
  `shipping_address` text DEFAULT NULL,
  `shipping_fee` decimal(12,2) DEFAULT 0.00,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `order_code` (`order_code`),
  KEY `fk_order_user` (`user_id`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `orders` (10 baris)
INSERT INTO `orders` (`order_id`, `order_date`, `total_price`, `status`, `user_id`, `order_code`, `recipient_name`, `recipient_phone`, `delivery_method`, `shipping_address`, `shipping_fee`, `created_at`, `updated_at`) VALUES
(1, '2026-10-01 10:00:00', '205000.00', 'processing', 1, 'ORD-2026-0001', 'Jesika Putri', '081210000001', 'pickup', 'Jl. Subrantas Panam No. 10, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, '2026-10-02 11:00:00', '265000.00', 'processing', 2, 'ORD-2026-0002', 'Arin Maharani', '081210000002', 'pickup', 'Jl. HR Soebrantas No. 22, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, '2026-10-03 12:00:00', '75000.00', 'processing', 3, 'ORD-2026-0003', 'Farras Akbar', '081210000003', 'pickup', 'Jl. Garuda Sakti KM 2, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, '2026-10-04 13:00:00', '125000.00', 'processing', 4, 'ORD-2026-0004', 'Fakhry Ramadhan', '081210000004', 'pickup', 'Jl. Bina Krida Panam No. 5, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, '2026-10-05 14:00:00', '255000.00', 'processing', 5, 'ORD-2026-0005', 'Rizky Pratama', '081210000005', 'pickup', 'Jl. Riau Ujung No. 45, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, '2026-10-06 15:00:00', '105000.00', 'processing', 6, 'ORD-2026-0006', 'Nabila Azzahra', '081210000006', 'pickup', 'Jl. Arifin Ahmad No. 18, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, '2026-10-07 16:00:00', '55000.00', 'processing', 7, 'ORD-2026-0007', 'Dimas Aditya', '081210000007', 'pickup', 'Jl. Sudirman No. 88, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, '2026-10-08 09:00:00', '325000.00', 'processing', 8, 'ORD-2026-0008', 'Siti Nurhaliza', '081210000008', 'pickup', 'Jl. Tuanku Tambusai No. 31, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, '2026-10-09 10:30:00', '280000.00', 'processing', 9, 'ORD-2026-0009', 'Bagas Saputra', '081210000009', 'pickup', 'Jl. Soekarno Hatta No. 14, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(10, '2026-10-10 11:45:00', '125000.00', 'processing', 10, 'ORD-2026-0010', 'Citra Lestari', '081210000010', 'pickup', 'Jl. Delima Panam No. 9, Pekanbaru', '0.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11');

-- --------------------------------------------------------
-- Struktur tabel `order_detail`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_detail` (
  `order_detail_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `quantity` int(11) NOT NULL DEFAULT 1,
  `order_id` int(10) unsigned NOT NULL,
  `product_id` int(10) unsigned NOT NULL,
  `unit_price` decimal(12,2) DEFAULT 0.00,
  `subtotal` decimal(12,2) DEFAULT 0.00,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`order_detail_id`),
  KEY `fk_detail_order` (`order_id`),
  KEY `fk_detail_product` (`product_id`),
  CONSTRAINT `fk_detail_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_detail_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `order_detail` (20 baris)
INSERT INTO `order_detail` (`order_detail_id`, `quantity`, `order_id`, `product_id`, `unit_price`, `subtotal`, `created_at`, `updated_at`) VALUES
(1, 2, 1, 1, '65000.00', '130000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, 1, 1, 2, '75000.00', '75000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, 3, 2, 2, '75000.00', '225000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, 1, 2, 3, '40000.00', '40000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, 1, 3, 3, '40000.00', '40000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, 1, 3, 4, '35000.00', '35000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, 2, 4, 4, '35000.00', '70000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, 1, 4, 5, '55000.00', '55000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, 3, 5, 5, '55000.00', '165000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(10, 1, 5, 6, '90000.00', '90000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(11, 1, 6, 6, '90000.00', '90000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(12, 1, 6, 7, '15000.00', '15000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(13, 2, 7, 7, '15000.00', '30000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(14, 1, 7, 8, '25000.00', '25000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(15, 3, 8, 8, '25000.00', '75000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(16, 1, 8, 9, '250000.00', '250000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(17, 1, 9, 9, '250000.00', '250000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(18, 1, 9, 10, '30000.00', '30000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(19, 2, 10, 10, '30000.00', '60000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(20, 1, 10, 1, '65000.00', '65000.00', '2026-11-06 10:01:11', '2026-11-06 10:01:11');

-- --------------------------------------------------------
-- Struktur tabel `health_service`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `health_service` (
  `service_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `service_name` varchar(150) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `doctor_name` varchar(100) DEFAULT 'drh. Clara Wijaya',
  `practice_days` varchar(100) DEFAULT 'Senin - Sabtu',
  `time_slots` varchar(150) DEFAULT '09:00, 13:00, 16:00',
  `daily_quota` int(11) DEFAULT 5,
  `description` text DEFAULT NULL,
  `icon` varchar(30) DEFAULT '?',
  `card_color` varchar(50) DEFAULT 'card-green',
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`service_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `health_service` (10 baris)
INSERT INTO `health_service` (`service_id`, `service_name`, `price`, `doctor_name`, `practice_days`, `time_slots`, `daily_quota`, `description`, `icon`, `card_color`, `created_at`, `updated_at`) VALUES
(1, 'Pemeriksaan Umum', '100000.00', 'drh. Clara Wijaya', 'Senin - Sabtu', '09:00, 11:00, 14:00, 16:00', 8, 'Pemeriksaan fisik menyeluruh untuk memastikan hewan peliharaan Anda tetap prima.', '🩺', 'card-green', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, 'Vaksin Kucing', '150000.00', 'drh. Budi Santoso', 'Senin - Jumat', '09:00, 13:00, 15:00', 6, 'Vaksinasi lengkap kucing (Tricat, Tetracat, Rabies) untuk perlindungan dari virus berbahaya.', '💉', 'card-blue', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, 'Kunjungan Dokter ke Rumah', '250000.00', 'drh. Siti Rahma', 'Selasa - Sabtu', '10:00, 14:00, 16:00', 6, 'Layanan kunjungan dokter hewan langsung ke rumah Anda untuk pemeriksaan nyaman tanpa stres.', '🏥', 'card-pink', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, 'Vaksin Anjing', '180000.00', 'drh. Andi Pratama, Sp.Bedah', 'Rabu - Sabtu', '09:00, 13:00', 3, 'Vaksinasi lengkap anjing (Parvo, Distemper, Rabies) untuk kekebalan tubuh optimal.', '🐕', 'card-orange', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, 'Konsultasi Nutrisi', '80000.00', 'drh. Clara Wijaya', 'Senin - Minggu', '09:00, 11:00, 13:00, 15:00', 10, 'Konsultasi pola makan, diet khusus, dan kebutuhan nutrisi seimbang untuk tumbuh kembang anabul.', '🥗', 'card-green', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, 'Pemeriksaan Kulit', '120000.00', 'drh. Budi Santoso', 'Senin - Sabtu', '10:00, 14:00, 16:00', 6, 'Pemeriksaan mikroskopis dan penanganan masalah kulit, jamur, alergi, serta kerontokan bulu.', '🔬', 'card-blue', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, 'Pembersihan Telinga', '60000.00', 'drh. Siti Rahma', 'Senin - Minggu', '08:00, 12:00, 16:00', 10, 'Perawatan kebersihan telinga secara medis untuk mencegah tungau (ear mites) dan infeksi telinga.', '👂', 'card-pink', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, 'Pemeriksaan Gigi', '130000.00', 'drh. Andi Pratama, Sp.Bedah', 'Senin - Sabtu', '09:00, 11:00, 14:00', 5, 'Pembersihan karang gigi (scaling) dan perawatan kesehatan mulut serta gusi hewan kesayangan.', '🦷', 'card-orange', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, 'Pemberian Obat Cacing', '70000.00', 'drh. Clara Wijaya', 'Senin - Minggu', '08:00, 14:00, 19:00', 5, 'Pemberian obat cacing berkala dan vitamin penguat daya tahan tubuh agar bebas parasit.', '💊', 'card-green', '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(10, 'Pemeriksaan Mata', '110000.00', 'drh. Budi Santoso', 'Senin - Sabtu', '10:00, 13:00, 16:00', 4, 'Pemeriksaan menyeluruh kesehatan mata serta penanganan iritasi dan infeksi mata pada hewan.', '👁️', 'card-blue', '2026-11-06 10:01:11', '2026-11-06 10:01:11');

-- --------------------------------------------------------
-- Struktur tabel `health_booking`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `health_booking` (
  `booking_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `booking_date` datetime NOT NULL,
  `status` varchar(50) DEFAULT 'pending',
  `user_id` int(10) unsigned NOT NULL,
  `service_id` int(10) unsigned NOT NULL,
  `pet_id` int(10) unsigned DEFAULT NULL,
  `owner_name` varchar(150) DEFAULT NULL,
  `pet_name` varchar(100) DEFAULT NULL,
  `pet_species` varchar(50) DEFAULT NULL,
  `rejection_reason` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`booking_id`),
  KEY `fk_booking_user` (`user_id`),
  KEY `fk_booking_service` (`service_id`),
  KEY `fk_booking_pet` (`pet_id`),
  CONSTRAINT `fk_booking_pet` FOREIGN KEY (`pet_id`) REFERENCES `pet` (`pet_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_booking_service` FOREIGN KEY (`service_id`) REFERENCES `health_service` (`service_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `health_booking` (10 baris)
INSERT INTO `health_booking` (`booking_id`, `booking_date`, `status`, `user_id`, `service_id`, `pet_id`, `owner_name`, `pet_name`, `pet_species`, `rejection_reason`, `created_at`, `updated_at`) VALUES
(1, '2026-11-01 09:00:00', 'pending', 1, 1, 1, 'Jesika Putri', 'Milo', 'Kucing', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(2, '2026-11-02 13:00:00', 'pending', 2, 2, 2, 'Arin Maharani', 'Luna', 'Anjing', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(3, '2026-11-03 10:00:00', 'pending', 3, 3, 3, 'Farras Akbar', 'Mochi', 'Kucing', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(4, '2026-11-04 09:00:00', 'pending', 4, 4, 4, 'Fakhry Ramadhan', 'Oreo', 'Anjing', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(5, '2026-11-05 11:00:00', 'pending', 5, 5, 5, 'Rizky Pratama', 'Coco', 'Kucing', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(6, '2026-11-06 14:00:00', 'pending', 6, 6, 6, 'Nabila Azzahra', 'Bella', 'Anjing', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(7, '2026-11-07 08:00:00', 'pending', 7, 7, 7, 'Dimas Aditya', 'Leo', 'Kucing', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(8, '2026-11-08 09:00:00', 'pending', 8, 8, 8, 'Siti Nurhaliza', 'Nala', 'Anjing', NULL, '2026-11-06 10:01:11', '2026-11-06 10:01:11'),
(9, '2026-11-09 14:00:00', 'pending', 9, 9, 9, 'Bagas Saputra', 'Mimi', 'Kucing', NULL, '2026-11-06 10:01:12', '2026-11-06 10:01:12'),
(10, '2026-11-10 10:00:00', 'pending', 10, 10, 10, 'Citra Lestari', 'Snowy', 'Kelinci', NULL, '2026-11-06 10:01:12', '2026-11-06 10:01:12');

-- --------------------------------------------------------
-- Struktur tabel `payment`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `payment` (
  `payment_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `method` varchar(50) DEFAULT 'TRANSFER_BANK',
  `amount` decimal(12,2) NOT NULL,
  `status` varchar(50) DEFAULT 'BERHASIL',
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `paid_at` timestamp NULL DEFAULT current_timestamp(),
  `reference_number` varchar(100) DEFAULT NULL,
  `order_id` int(10) unsigned DEFAULT NULL,
  `booking_id` int(10) unsigned DEFAULT NULL,
  `payment_date` timestamp NULL DEFAULT current_timestamp(),
  `payment_method` varchar(50) DEFAULT 'Simulasi Checkout',
  `payment_status` varchar(50) DEFAULT 'paid',
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`payment_id`),
  KEY `fk_payment_order` (`order_id`),
  KEY `fk_payment_booking` (`booking_id`),
  CONSTRAINT `fk_payment_booking` FOREIGN KEY (`booking_id`) REFERENCES `health_booking` (`booking_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data untuk tabel `payment` (20 baris)
INSERT INTO `payment` (`payment_id`, `method`, `amount`, `status`, `created_at`, `paid_at`, `reference_number`, `order_id`, `booking_id`, `payment_date`, `payment_method`, `payment_status`, `updated_at`) VALUES
(1, 'TRANSFER_BANK', '205000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0001', 1, NULL, '2026-11-06 10:01:12', 'Transfer Bank', 'paid', '2026-11-06 10:01:12'),
(2, 'QRIS', '265000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0002', 2, NULL, '2026-11-06 10:01:12', 'QRIS', 'paid', '2026-11-06 10:01:12'),
(3, 'TRANSFER_BANK', '75000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0003', 3, NULL, '2026-11-06 10:01:12', 'Transfer Bank', 'paid', '2026-11-06 10:01:12'),
(4, 'QRIS', '125000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0004', 4, NULL, '2026-11-06 10:01:12', 'QRIS', 'paid', '2026-11-06 10:01:12'),
(5, 'TRANSFER_BANK', '255000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0005', 5, NULL, '2026-11-06 10:01:12', 'Transfer Bank', 'paid', '2026-11-06 10:01:12'),
(6, 'QRIS', '105000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0006', 6, NULL, '2026-11-06 10:01:12', 'QRIS', 'paid', '2026-11-06 10:01:12'),
(7, 'TRANSFER_BANK', '55000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0007', 7, NULL, '2026-11-06 10:01:12', 'Transfer Bank', 'paid', '2026-11-06 10:01:12'),
(8, 'QRIS', '325000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0008', 8, NULL, '2026-11-06 10:01:12', 'QRIS', 'paid', '2026-11-06 10:01:12'),
(9, 'TRANSFER_BANK', '280000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0009', 9, NULL, '2026-11-06 10:01:12', 'Transfer Bank', 'paid', '2026-11-06 10:01:12'),
(10, 'QRIS', '125000.00', 'BERHASIL', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-ORD-0010', 10, NULL, '2026-11-06 10:01:12', 'QRIS', 'paid', '2026-11-06 10:01:12'),
(11, 'QRIS', '100000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0001', NULL, 1, '2026-11-06 10:01:12', 'QRIS', 'pending', '2026-11-06 10:01:12'),
(12, 'TUNAI', '150000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0002', NULL, 2, '2026-11-06 10:01:12', 'COD', 'pending', '2026-11-06 10:01:12'),
(13, 'QRIS', '250000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0003', NULL, 3, '2026-11-06 10:01:12', 'QRIS', 'pending', '2026-11-06 10:01:12'),
(14, 'TUNAI', '180000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0004', NULL, 4, '2026-11-06 10:01:12', 'COD', 'pending', '2026-11-06 10:01:12'),
(15, 'QRIS', '80000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0005', NULL, 5, '2026-11-06 10:01:12', 'QRIS', 'pending', '2026-11-06 10:01:12'),
(16, 'TUNAI', '120000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0006', NULL, 6, '2026-11-06 10:01:12', 'COD', 'pending', '2026-11-06 10:01:12'),
(17, 'QRIS', '60000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0007', NULL, 7, '2026-11-06 10:01:12', 'QRIS', 'pending', '2026-11-06 10:01:12'),
(18, 'TUNAI', '130000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0008', NULL, 8, '2026-11-06 10:01:12', 'COD', 'pending', '2026-11-06 10:01:12'),
(19, 'QRIS', '70000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0009', NULL, 9, '2026-11-06 10:01:12', 'QRIS', 'pending', '2026-11-06 10:01:12'),
(20, 'TUNAI', '110000.00', 'MENUNGGU', '2026-11-06 10:01:12', '2026-11-06 10:01:12', 'PAY-BKG-0010', NULL, 10, '2026-11-06 10:01:12', 'COD', 'pending', '2026-11-06 10:01:12');

SET FOREIGN_KEY_CHECKS = 1;
