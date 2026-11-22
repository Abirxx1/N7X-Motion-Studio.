package com.n7x.motionstudio.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProjectEntity::class, MediaClipEntity::class, OverlayEntity::class, AudioEntity::class],
    version = 7,
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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `filterId` TEXT NOT NULL DEFAULT 'none'")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `brightness` REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `contrast` REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `saturation` REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `lutPath` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `transitionType` TEXT NOT NULL DEFAULT 'none'")
        db.execSQL("ALTER TABLE `media_clips` ADD COLUMN `transitionMs` INTEGER NOT NULL DEFAULT 600")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `overlays` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`projectId` INTEGER NOT NULL, " +
                "`kind` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`startMs` INTEGER NOT NULL, " +
                "`durationMs` INTEGER NOT NULL, " +
                "`x` REAL NOT NULL, " +
                "`y` REAL NOT NULL, " +
                "`size` REAL NOT NULL, " +
                "`rotation` REAL NOT NULL, " +
                "`colorArgb` INTEGER NOT NULL, " +
                "`bold` INTEGER NOT NULL, " +
                "`box` INTEGER NOT NULL, " +
                "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_overlays_projectId` ON `overlays` (`projectId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `audio_tracks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`projectId` INTEGER NOT NULL, " +
                "`path` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`sourceMs` INTEGER NOT NULL, " +
                "`startMs` INTEGER NOT NULL, " +
                "`trimStartMs` INTEGER NOT NULL, " +
                "`trimEndMs` INTEGER NOT NULL, " +
                "`volume` REAL NOT NULL, " +
                "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_tracks_projectId` ON `audio_tracks` (`projectId`)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `fontId` TEXT NOT NULL DEFAULT 'sans'")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `italic` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `letterSpacing` REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `opacity` REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `shadowSize` REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `shadowColor` INTEGER NOT NULL DEFAULT -16777216")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `outlineWidth` REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `outlineColor` INTEGER NOT NULL DEFAULT -16777216")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `boxColor` INTEGER NOT NULL DEFAULT -1728053248")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `inAnim` TEXT NOT NULL DEFAULT 'none'")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `inMs` INTEGER NOT NULL DEFAULT 600")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `outAnim` TEXT NOT NULL DEFAULT 'none'")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `outMs` INTEGER NOT NULL DEFAULT 600")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `loopAnim` TEXT NOT NULL DEFAULT 'none'")
        db.execSQL("ALTER TABLE `overlays` ADD COLUMN `loopSpeed` REAL NOT NULL DEFAULT 1.0")
    }
}
