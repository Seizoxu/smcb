-- MySQL dump 10.13  Distrib 8.0.42, for Linux (x86_64)
--
-- Host: localhost    Database: mowc
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `maps`
--

DROP TABLE IF EXISTS `maps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maps` (
  `map_id` int NOT NULL,
  `mapset_id` int NOT NULL,
  `end_date` date DEFAULT NULL,
  `title` varchar(512) DEFAULT NULL,
  `artist` varchar(255) DEFAULT NULL,
  `mapper` varchar(255) DEFAULT NULL,
  `difficulty_name` varchar(255) DEFAULT NULL,
  `banner_link` text,
  `star_rating` float DEFAULT NULL,
  `ar` float DEFAULT NULL,
  `od` float DEFAULT NULL,
  `hp` float DEFAULT NULL,
  `cs` float DEFAULT NULL,
  `length_seconds` int DEFAULT NULL,
  `bpm` float DEFAULT NULL,
  PRIMARY KEY (`map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scores`
--

DROP TABLE IF EXISTS `scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scores` (
  `score_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `map_id` int NOT NULL,
  `score` int DEFAULT NULL,
  `mods` varchar(255) DEFAULT NULL,
  `score_time` datetime DEFAULT NULL,
  UNIQUE KEY `unique_user_map` (`user_id`,`map_id`) USING BTREE,
  KEY `map_id` (`map_id`),
  CONSTRAINT `scores_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `scores_ibfk_2` FOREIGN KEY (`map_id`) REFERENCES `maps` (`map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` bigint NOT NULL,
  `username` varchar(255) NOT NULL,
  `country_code` char(2) DEFAULT NULL,
  `verified` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'mowc'
--
/*!50003 DROP PROCEDURE IF EXISTS `insert_or_update_score_if_higher` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = latin1 */ ;
/*!50003 SET character_set_results = latin1 */ ;
/*!50003 SET collation_connection  = latin1_swedish_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `insert_or_update_score_if_higher`(
    IN p_score_id BIGINT,
    IN p_user_id BIGINT,
    IN p_map_id INT,
    IN p_score INT,
    IN p_mods VARCHAR(255),
    IN p_score_time DATETIME
)
BEGIN
    DECLARE existing_score INT;

    SELECT score INTO existing_score
    FROM scores
    WHERE user_id = p_user_id AND map_id = p_map_id;

    IF existing_score IS NULL THEN
        INSERT INTO scores (score_id, user_id, map_id, score, mods, score_time)
        VALUES (p_score_id, p_user_id, p_map_id, p_score, p_mods, p_score_time);
    ELSEIF p_score > existing_score THEN
        UPDATE scores
        SET score = p_score,
            score_id = p_score_id,
            mods = p_mods,
            score_time = p_score_time
        WHERE user_id = p_user_id AND map_id = p_map_id;
    END IF;
END ;;
DELIMITER ;

/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-06-03  4:34:06
