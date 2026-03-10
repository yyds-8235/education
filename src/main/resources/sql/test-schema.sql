CREATE TABLE `tests` (
  `id`           VARCHAR(36)  NOT NULL,
  `course_id`    VARCHAR(36)  NOT NULL,
  `title`        VARCHAR(255) NOT NULL,
  `description`  TEXT         NULL,
  `duration`     INT          NOT NULL,
  `total_score`  INT          NOT NULL DEFAULT 0,
  `show_answer`  TINYINT(1)   NOT NULL DEFAULT 0,
  `status`       ENUM('draft','published','ended') NOT NULL DEFAULT 'draft',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tests_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `test_questions` (
  `id`           VARCHAR(36)   NOT NULL,
  `test_id`      VARCHAR(36)   NOT NULL,
  `type`         ENUM('single_choice','fill_blank','short_answer') NOT NULL,
  `content`      TEXT          NOT NULL,
  `answer`       TEXT          NULL,
  `score`        INT           NOT NULL,
  `sort_order`   INT           NOT NULL,
  `analysis`     TEXT          NULL,
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_test_questions_test` (`test_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `test_question_options` (
  `id`           VARCHAR(36)   NOT NULL,
  `question_id`  VARCHAR(36)   NOT NULL,
  `label`        VARCHAR(16)   NOT NULL,
  `content`      TEXT          NOT NULL,
  `sort_order`   INT           NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_question_options_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `test_submissions` (
  `id`                VARCHAR(36)   NOT NULL,
  `test_id`           VARCHAR(36)   NOT NULL,
  `student_id`        VARCHAR(36)   NOT NULL,
  `total_score`       INT           NOT NULL DEFAULT 0,
  `status`            ENUM('submitted','graded') NOT NULL DEFAULT 'submitted',
  `submitted_at`      DATETIME      NULL,
  `graded_at`         DATETIME      NULL,
  `appeal_reason`     TEXT          NULL,
  `appeal_status`     ENUM('pending','accepted','rejected') NULL,
  `analysis_summary`  TEXT          NULL,
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_student` (`test_id`, `student_id`),
  KEY `idx_test_submissions_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `test_submission_answers` (
  `id`             VARCHAR(36)  NOT NULL,
  `submission_id`  VARCHAR(36)  NOT NULL,
  `question_id`    VARCHAR(36)  NOT NULL,
  `answer`         TEXT         NULL,
  `score`          INT          NULL,
  `feedback`       TEXT         NULL,
  `is_correct`     TINYINT(1)   NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_submission_answers_submission` (`submission_id`),
  KEY `idx_submission_answers_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
