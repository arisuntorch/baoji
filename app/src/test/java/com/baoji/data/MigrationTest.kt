package com.baoji.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.baoji.data.db.BaojiDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opens every exported schema version and runs all migrations up to the current one.
 * When VERSION is bumped, add `helper.runMigrationsAndValidate(NAME, n, true, MIGRATION_x_n)` here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    private val name = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), BaojiDatabase::class.java)

    @Test
    fun currentSchemaOpensWithAllMigrations() {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO weight_log (id, date, kg, updatedAt) VALUES ('w1', 20000, 70.5, 1)")
            close()
        }
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), BaojiDatabase::class.java, name)
            .addMigrations(*BaojiDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        val kg = db.openHelper.writableDatabase.query("SELECT kg FROM weight_log WHERE id = 'w1'").use {
            it.moveToFirst()
            it.getDouble(0)
        }
        assertThat(kg).isEqualTo(70.5)
        val activityRows = db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM activity_log").use {
            it.moveToFirst()
            it.getInt(0)
        }
        assertThat(activityRows).isEqualTo(0)
        db.close()
    }

    @Test
    fun migration1To2MatchesExportedSchema() {
        helper.createDatabase(name, 1).close()
        helper.runMigrationsAndValidate(name, 2, true, BaojiDatabase.MIGRATION_1_2).close()
    }
}
