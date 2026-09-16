package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ReelNote::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reelNoteDao(): ReelNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reel_notes_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.reelNoteDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: ReelNoteDao) {
                if (dao.getCount() > 0) return

                val now = System.currentTimeMillis()
                dao.insert(
                    ReelNote(
                        reelUrl = "https://www.instagram.com/reel/C8xg4NypM81/",
                        shortCode = "C8xg4NypM81",
                        title = "Crispy Chili Garlic Noodles",
                        useCase = "Quick 10-minute dinner for busy weeknights",
                        notes = "• Ingredients: Ramen noodles, 3 cloves garlic, 1 tbsp chili flakes, 1 tbsp soy sauce, hot oil splash\n• Tip: Pour screaming hot oil directly over minced garlic and scallions for the aroma!",
                        category = "Recipe",
                        actionStatus = "TO_TRY",
                        isFavorite = true,
                        createdAt = now - 3600000 * 24
                    )
                )
                dao.insert(
                    ReelNote(
                        reelUrl = "https://www.instagram.com/reel/C7yt9k3L45z/",
                        shortCode = "C7yt9k3L45z",
                        title = "Hidden Kyoto Bamboo Path & Teahouse",
                        useCase = "Add to Kyoto itinerary Day 3 early morning",
                        notes = "• Location: Adashino Nenbutsu-ji Temple\n• Avoid the crowded Arashiyama crowds; best time to visit is 8:30 AM right when gates open.",
                        category = "Travel",
                        actionStatus = "SAVED",
                        isFavorite = false,
                        createdAt = now - 3600000 * 12
                    )
                )
                dao.insert(
                    ReelNote(
                        reelUrl = "https://www.instagram.com/reel/C5ml14QPr90/",
                        shortCode = "C5ml14QPr90",
                        title = "Shoulder Mobility & Scapular Wall Slides",
                        useCase = "Warm-up drill before upper body bench press session",
                        notes = "• Keep lower back flat against wall\n• 3 sets of 10 slow reps\n• Really helps with impingement before overhead press!",
                        category = "Fitness",
                        actionStatus = "TRIED",
                        isFavorite = true,
                        createdAt = now - 3600000 * 4
                    )
                )
            }
        }
    }
}
