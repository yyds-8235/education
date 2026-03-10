CREATE TABLE `courses` (
  `id`            VARCHAR(36)  NOT NULL,
  `name`          VARCHAR(128) NOT NULL,
  `description`   TEXT         NULL,
  `grade`         VARCHAR(16)  NOT NULL,
  `class_name`    VARCHAR(16)  NOT NULL,
  `subject`       VARCHAR(32)  NOT NULL,
  `teacher_id`    VARCHAR(36)  NOT NULL,
  `visibility`    ENUM('public','private') NOT NULL DEFAULT 'public',
  `cover_image`   VARCHAR(255) NULL,
  `student_count` INT          NOT NULL DEFAULT 0,
  `status`        ENUM('draft','active','archived') NOT NULL DEFAULT 'draft',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_teacher` (`teacher_id`),
  KEY `idx_course_grade_class` (`grade`, `class_name`),
  KEY `idx_course_subject` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `course_chapters` (
  `id`           VARCHAR(36)  NOT NULL,
  `course_id`    VARCHAR(36)  NOT NULL,
  `title`        VARCHAR(128) NOT NULL,
  `description`  TEXT         NULL,
  `sort_order`   INT          NOT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chapter_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `course_resources` (
  `id`               VARCHAR(36)  NOT NULL,
  `chapter_id`       VARCHAR(36)  NOT NULL,
  `name`             VARCHAR(255) NOT NULL,
  `type`             ENUM('video','ppt','word','pdf','other') NOT NULL,
  `url`              VARCHAR(500) NOT NULL,
  `bucket_name`      VARCHAR(64)  NULL,
  `object_key`       VARCHAR(255) NULL,
  `size`             BIGINT       NOT NULL,
  `duration`         INT          NULL,
  `sort_order`       INT          NOT NULL,
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resource_chapter` (`chapter_id`),
  KEY `idx_resource_object_key` (`object_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `course_students` (
  `id`           VARCHAR(36)   NOT NULL,
  `course_id`    VARCHAR(36)   NOT NULL,
  `student_id`   VARCHAR(36)   NOT NULL,
  `student_no`   VARCHAR(32)   NOT NULL,
  `joined_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `progress`     DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student` (`course_id`, `student_id`),
  KEY `idx_course_students_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
