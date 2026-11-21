package com.n7x.motionstudio.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProjectEntity::class, MediaClipEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun mediaClipDao(): MediaClipDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `media_clips` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`projectId` INTEGER NOT NULL, " +
                "`path` TEXT NOT NULL, " +
                "`mimeType` TEXT NOT NULL, " +
                "`isVideo` INTEGER NOT NULL, " +
                "`durationMs` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_clips_projectId` " +
                "ON `media_clips` (`projectId`)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `trimStartMs` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `trimEndMs` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `speed` REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `volume` REAL NOT NULL DEFAULT 1.0")
        db.execSQL("UPDATE `media_clips` SET `trimEndMs` = `durationMs`")
        db.execSQL("UPDATE `media_clips` SET `durationMs` = 30000 WHERE `isVideo` = 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `keyframes` TEXT NOT NULL DEFAULT '[]'")
    }
}
