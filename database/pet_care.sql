-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: pet_care
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `pet_care`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `pet_care` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `pet_care`;

--
-- Table structure for table `adoption_request`
--

DROP TABLE IF EXISTS `adoption_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `adoption_request` (
  `adoption_request_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned NOT NULL,
  `animal_id` int(10) unsigned NOT NULL,
  `applicant_name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `address` text NOT NULL,
  `reason` text NOT NULL,
  `request_date` datetime NOT NULL DEFAULT current_timestamp(),
  `status` enum('pending','approved','rejected','cancelled') NOT NULL DEFAULT 'pending',
  `admin_note` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`adoption_request_id`),
  KEY `adoption_request_user_id_foreign` (`user_id`),
  KEY `adoption_request_animal_id_foreign` (`animal_id`),
  CONSTRAINT `adoption_request_animal_id_foreign` FOREIGN KEY (`animal_id`) REFERENCES `animal` (`animal_id`) ON DELETE CASCADE,
  CONSTRAINT `adoption_request_user_id_foreign` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `adoption_request`
--

LOCK TABLES `adoption_request` WRITE;
/*!40000 ALTER TABLE `adoption_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `adoption_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `animal`
--

DROP TABLE IF EXISTS `animal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `animal` (
  `animal_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `nama` varchar(100) NOT NULL,
  `species` varchar(50) NOT NULL,
  `breed` varchar(50) NOT NULL,
  `gender` varchar(20) NOT NULL,
  `age` varchar(50) NOT NULL,
  `status` enum('available','adopted','unavailable') NOT NULL DEFAULT 'available',
  `image` varchar(255) NOT NULL DEFAULT 'assets/img/meng.png',
  `description` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`animal_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `animal`
--

LOCK TABLES `animal` WRITE;
/*!40000 ALTER TABLE `animal` DISABLE KEYS */;
INSERT INTO `animal` VALUES (1,'Siti','kucing','Anggora','Betina','1 Tahun','available','assets/img/anggora.jpg','Kucing anggora jinak, aktif, dan sudah vaksin dasar.','2026-11-02 04:49:37','2026-11-02 04:49:37'),(2,'Jeno','lainnya','African Pygmy','Jantan','8 Bulan','available','assets/img/goat.jpg','Kambing mini ramah dan sangat suka bermain di halaman.','2026-11-02 04:49:37','2026-11-02 04:49:37'),(3,'Jubariyah','kucing','Bengal','Betina','13 Bulan','adopted','assets/img/bengal.jpg','Kucing Bengal lincah dengan corak bulu eksotis.','2026-11-02 04:49:37','2026-11-02 04:49:37'),(4,'Noe','kelinci','Rex','Jantan','10 Bulan','available','assets/img/rabbit.jpg','Kelinci Rex berbulu halus seperti beludru dan tenang.','2026-11-02 04:49:37','2026-11-02 04:49:37');
/*!40000 ALTER TABLE `animal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `health_booking`
--

DROP TABLE IF EXISTS `health_booking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_booking` (
  `booking_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned NOT NULL,
  `pet_id` int(10) unsigned DEFAULT NULL,
  `service_id` int(10) unsigned NOT NULL,
  `owner_name` varchar(100) NOT NULL,
  `pet_name` varchar(100) NOT NULL,
  `pet_species` varchar(50) NOT NULL,
  `booking_date` datetime NOT NULL,
  `status` enum('pending','approved','completed','rejected','cancelled') NOT NULL DEFAULT 'pending',
  `rejection_reason` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`booking_id`),
  KEY `health_booking_user_id_foreign` (`user_id`),
  KEY `health_booking_pet_id_foreign` (`pet_id`),
  KEY `health_booking_service_id_foreign` (`service_id`),
  CONSTRAINT `health_booking_pet_id_foreign` FOREIGN KEY (`pet_id`) REFERENCES `pet` (`pet_id`) ON DELETE SET NULL,
  CONSTRAINT `health_booking_service_id_foreign` FOREIGN KEY (`service_id`) REFERENCES `health_service` (`service_id`),
  CONSTRAINT `health_booking_user_id_foreign` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `health_booking`
--

LOCK TABLES `health_booking` WRITE;
/*!40000 ALTER TABLE `health_booking` DISABLE KEYS */;
/*!40000 ALTER TABLE `health_booking` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `health_service`
--

DROP TABLE IF EXISTS `health_service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_service` (
  `service_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `service_name` varchar(100) NOT NULL,
  `doctor_name` varchar(100) NOT NULL DEFAULT 'drh. Clara Wijaya',
  `practice_days` varchar(100) NOT NULL DEFAULT 'Senin - Sabtu',
  `time_slots` varchar(150) NOT NULL DEFAULT '09:00, 13:00, 16:00',
  `daily_quota` int(11) NOT NULL DEFAULT 5,
  `description` varchar(255) DEFAULT NULL,
  `icon` varchar(20) NOT NULL DEFAULT '?',
  `card_color` varchar(30) NOT NULL DEFAULT 'card-green',
  `price` decimal(12,2) NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`service_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `health_service`
--

LOCK TABLES `health_service` WRITE;
/*!40000 ALTER TABLE `health_service` DISABLE KEYS */;
INSERT INTO `health_service` VALUES (1,'Booking Jadwal Periksa','drh. Clara Wijaya','Senin - Sabtu','09:00, 11:00, 14:00, 16:00',5,'Pemeriksaan fisik menyeluruh & konsultasi kesehatan klinik','🩺','card-green',100000.00,'2026-11-02 04:49:37','2026-11-02 04:49:37'),(2,'Jadwalkan Vaksin','drh. Budi Pratama','Senin - Minggu','09:00, 13:00, 15:30',8,'Vaksinasi lengkap (Rabies, Tricat, Eurican) + buku vaksin','💉','card-blue',150000.00,'2026-11-02 04:49:37','2026-11-02 04:49:37'),(3,'Panggil Dokter Ke Rumah','drh. Siska Amelia','Selasa - Minggu','10:00, 14:00, 16:30',3,'Dokter kami datang langsung ke rumah Anda untuk Home Visit','🏥','card-purple',250000.00,'2026-11-02 04:49:37','2026-11-02 04:49:37');
/*!40000 ALTER TABLE `health_service` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `migrations`
--

DROP TABLE IF EXISTS `migrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `migrations` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `migration` varchar(255) NOT NULL,
  `batch` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `migrations`
--

LOCK TABLES `migrations` WRITE;
/*!40000 ALTER TABLE `migrations` DISABLE KEYS */;
INSERT INTO `migrations` VALUES (1,'0001_01_01_000000_create_pet_care_tables',1);
/*!40000 ALTER TABLE `migrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_detail`
--

DROP TABLE IF EXISTS `order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_detail` (
  `order_detail_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `order_id` int(10) unsigned NOT NULL,
  `product_id` int(10) unsigned NOT NULL,
  `quantity` int(11) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`order_detail_id`),
  KEY `order_detail_order_id_foreign` (`order_id`),
  KEY `order_detail_product_id_foreign` (`product_id`),
  CONSTRAINT `order_detail_order_id_foreign` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE,
  CONSTRAINT `order_detail_product_id_foreign` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_detail`
--

LOCK TABLES `order_detail` WRITE;
/*!40000 ALTER TABLE `order_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `order_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `orders` (
  `order_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `order_code` varchar(40) NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  `recipient_name` varchar(100) NOT NULL,
  `recipient_phone` varchar(30) NOT NULL,
  `delivery_method` enum('pickup','delivery') NOT NULL DEFAULT 'pickup',
  `shipping_address` text DEFAULT NULL,
  `shipping_fee` decimal(12,2) NOT NULL DEFAULT 0.00,
  `total_price` decimal(12,2) NOT NULL,
  `order_date` datetime NOT NULL DEFAULT current_timestamp(),
  `status` enum('pending','paid','processing','shipped','completed','cancelled') NOT NULL DEFAULT 'pending',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `orders_order_code_unique` (`order_code`),
  KEY `orders_user_id_foreign` (`user_id`),
  CONSTRAINT `orders_user_id_foreign` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payment` (
  `payment_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `order_id` int(10) unsigned NOT NULL,
  `payment_date` datetime NOT NULL DEFAULT current_timestamp(),
  `amount` decimal(12,2) NOT NULL,
  `payment_method` varchar(50) NOT NULL DEFAULT 'Simulasi Checkout',
  `payment_status` enum('pending','paid','failed') NOT NULL DEFAULT 'paid',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `payment_order_id_foreign` (`order_id`),
  CONSTRAINT `payment_order_id_foreign` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pet`
--

DROP TABLE IF EXISTS `pet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pet` (
  `pet_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned NOT NULL,
  `pet_name` varchar(100) NOT NULL,
  `species` varchar(50) NOT NULL,
  `breed` varchar(50) NOT NULL DEFAULT 'Tidak diketahui',
  `gender` varchar(20) NOT NULL DEFAULT 'Tidak diketahui',
  `age` varchar(30) NOT NULL DEFAULT '1 Tahun',
  `health_status` varchar(50) NOT NULL DEFAULT 'Sehat',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`pet_id`),
  KEY `pet_user_id_foreign` (`user_id`),
  CONSTRAINT `pet_user_id_foreign` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pet`
--

LOCK TABLES `pet` WRITE;
/*!40000 ALTER TABLE `pet` DISABLE KEYS */;
INSERT INTO `pet` VALUES (1,2,'Lumi','Kucing','Persia','Betina','2 Tahun','Sehat','2026-11-02 04:49:37','2026-11-02 04:49:37'),(2,2,'Milo','Anjing','Golden Retriever','Jantan','1 Tahun','Vaksin Rutin','2026-11-02 04:49:37','2026-11-02 04:49:37');
/*!40000 ALTER TABLE `pet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product` (
  `product_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `slug` varchar(100) NOT NULL,
  `product_name` varchar(150) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `stock` int(11) NOT NULL DEFAULT 0,
  `category` varchar(50) NOT NULL,
  `image` varchar(255) NOT NULL DEFAULT 'assets/img/shop/whiskas.png',
  `description` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `product_slug_unique` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,'whiskas','Whiskas Adult',75000.00,25,'makanan','assets/img/shop/whiskas.png','Makanan nutrisi lengkap untuk kucing dewasa.','2026-11-02 04:49:37','2026-11-02 04:49:37'),(2,'pedigree','Pedigree Adult',100000.00,20,'makanan','assets/img/shop/pedigree.png','Makanan bergizi tinggi untuk kesehatan anjing dewasa.','2026-11-02 04:49:37','2026-11-02 04:49:37'),(3,'shampoo-anjing','Shampoo Anjing',90000.00,15,'perawatan','assets/img/shop/shampoo-anjing.png','Shampoo lembut anti kutu & menjaga kilau bulu anjing.','2026-11-02 04:49:37','2026-11-02 04:49:37'),(4,'shampoo-kucing','Shampoo Kucing',85000.00,18,'perawatan','assets/img/shop/shampoo-kucing.png','Formula khusus kulit sensitif kucing kesayangan.','2026-11-02 04:49:37','2026-11-02 04:49:37'),(5,'sisir','Sisir Grooming',60000.00,30,'perawatan','assets/img/shop/sisir-grooming.png','Sisir ergonomis untuk merapikan bulu rontok.','2026-11-02 04:49:37','2026-11-02 04:49:37'),(6,'tulang','Mainan Tulang Karet',35000.00,40,'mainan','assets/img/shop/mainan-tulang.png','Mainan gigit elastis & aman untuk kesehatan gigi hewan.','2026-11-02 04:49:37','2026-11-02 04:49:37');
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `user_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('admin','user') NOT NULL DEFAULT 'user',
  `phone` varchar(30) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `remember_token` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_username_unique` (`username`),
  UNIQUE KEY `user_email_unique` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'Admin PawPaw','admin','admin@pawpaw.com','$2y$12$K7QjeA2JqA6cEFHAv5gM1O8O2.TSLXrp0r4AXqn2YxydT7PAttVjK','admin','081234567890','Jl. Panam Raya No. 12, Pekanbaru','Administrator PawPaw Pet Care',NULL,NULL,'2026-11-02 04:49:37','2026-11-02 04:49:37'),(2,'Arini Dewinda','arini','arini@pawpaw.com','$2y$12$d5qCmEGmBXGTz7Lw6yJ0Pu1G13a9OkWdKncXdfzSwjB9BsuHdWtJS','user','0812-3456-7890','Jl. Sudirman No. 12, Pekanbaru','Cat lover & proud pet parent ♡',NULL,NULL,'2026-11-02 04:49:37','2026-11-02 04:49:37');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-02 18:49:37
