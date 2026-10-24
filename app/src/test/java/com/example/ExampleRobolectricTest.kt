package com.example

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class ExampleRobolectricTest {

    @Test
    fun `launch MainActivity and navigate across all screens without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible()
        val activity = controller.get()
        assertNotNull(activity)

        // Test navigating across all tabs
        activity.switchToTab(MainActivity.TAB_FANDRAISANA)
        activity.switchToTab(MainActivity.TAB_HIKAROKA)
        activity.switchToTab(MainActivity.TAB_HIRA_TIAKO)
        activity.switchToTab(MainActivity.TAB_KAONTY)
        activity.switchToTab(MainActivity.TAB_FANDRAISANA)

        // Verify authentic Hymn 1 and Hymn 830 from real hymns.json
        val hymn1 = com.example.data.local.HymnCatalog.getSongByNumber(activity, 1)
        org.junit.Assert.assertEquals("MASINA NY LANITRA", hymn1?.title)

        val hymn830 = com.example.data.local.HymnCatalog.getSongByNumber(activity, 830)
        org.junit.Assert.assertEquals("RAHA MBOLA MAHARITRA NY FANILO MIREHITRA", hymn830?.title)

        // Verify empty state card exists on Fandraisana
        val emptyCard = activity.findViewById<View>(R.id.card_history_empty_state)
        assertNotNull(emptyCard)

        // Verify screen is NOT kept on by default on Fandraisana
        val flagsBefore = activity.window.attributes.flags
        org.junit.Assert.assertEquals(0, flagsBefore and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Test opening HymnReader for several hymns (including multi-verse hymns)
        activity.openHymnReader(1)
        val flagsInHymnReader = activity.window.attributes.flags
        org.junit.Assert.assertTrue((flagsInHymnReader and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        // Verify A- and A+ buttons remain visible and functional alongside Pinch-to-Zoom (.coerceIn(12f, 40f))
        val btnDecrease = activity.findViewById<View>(R.id.btn_font_decrease)
        val btnIncrease = activity.findViewById<View>(R.id.btn_font_increase)
        assertNotNull(btnDecrease)
        assertNotNull(btnIncrease)
        org.junit.Assert.assertEquals(View.VISIBLE, btnDecrease.visibility)
        org.junit.Assert.assertEquals(View.VISIBLE, btnIncrease.visibility)

        com.example.ui.theme.ThemeManager.setLyricsFontSizeSp(activity, 20f)
        org.junit.Assert.assertEquals(20f, com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value, 0.01f)
        btnIncrease.performClick()
        org.junit.Assert.assertEquals(22f, com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value, 0.01f)
        btnDecrease.performClick()
        org.junit.Assert.assertEquals(20f, com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value, 0.01f)

        val hymnReaderFrag = activity.supportFragmentManager.findFragmentByTag("TAG_HYMN_READER") as com.example.ui.fragments.HymnReaderFragment
        hymnReaderFrag.applyPinchZoomFactor(1.5f)
        org.junit.Assert.assertEquals(30f, com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value, 0.01f)
        hymnReaderFrag.applyPinchZoomFactor(2.0f)
        org.junit.Assert.assertEquals(40f, com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value, 0.01f)
        hymnReaderFrag.applyPinchZoomFactor(0.1f)
        org.junit.Assert.assertEquals(12f, com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value, 0.01f)
        com.example.ui.theme.ThemeManager.setLyricsFontSizeSp(activity, 18f)

        activity.openHymnReader(112)
        activity.openHymnReader(830)
        activity.closeHymnReader()

        val flagsAfterHymnReader = activity.window.attributes.flags
        org.junit.Assert.assertEquals(0, flagsAfterHymnReader and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Verify history has consulted hymns and at most 10 items
        val history = com.example.data.local.HymnCatalog.historySongsFlow.value
        org.junit.Assert.assertTrue(history.size in 1..10)
        org.junit.Assert.assertEquals(830, history.first().number)

        // Test opening Hikaroka with search and numpad + contextual snippet extraction & case-insensitive highlighting
        activity.openHikarokaWithSearch("12", openNumpad = true)
        activity.openHikarokaWithSearch("Jesosy", openNumpad = false)

        // Verify Rule 1: Dynamic contextual snippet extraction (first lyric line matching query, ignoreCase = true)
        val sampleLyrics = "Andininy voalohany eto\nNy famonjena lehibe dia avy amin'i Jesosy\nFiderana mandrakizay"
        val extracted = com.example.data.local.HymnCatalog.extractContextualSnippet(
            fullLyrics = sampleLyrics,
            defaultSnippet = "Andininy voalohany eto...",
            rawQuery = "famonjena"
        )
        org.junit.Assert.assertEquals("Ny famonjena lehibe dia avy amin'i Jesosy", extracted)

        val fallbackExtracted = com.example.data.local.HymnCatalog.extractContextualSnippet(
            fullLyrics = sampleLyrics,
            defaultSnippet = "Andininy voalohany eto...",
            rawQuery = "TsyAoAnatyTononkira"
        )
        org.junit.Assert.assertEquals("Andininy voalohany eto...", fallbackExtracted)

        // Verify Rule 2: Case-insensitive highlighting preserving original casing and applying ExtraBold + Yellow(0.3f)
        val annotated = com.example.ui.util.HighlightHelper.buildHighlightedAnnotatedString(
            text = "Jesosy no Mpamonjy, eny jesosy irery",
            query = "JESOSY"
        )
        org.junit.Assert.assertEquals("Jesosy no Mpamonjy, eny jesosy irery", annotated.text)
        org.junit.Assert.assertEquals(2, annotated.spanStyles.size)
        org.junit.Assert.assertEquals(0, annotated.spanStyles[0].start)
        org.junit.Assert.assertEquals(6, annotated.spanStyles[0].end)
        org.junit.Assert.assertEquals(
            androidx.compose.ui.text.font.FontWeight.ExtraBold,
            annotated.spanStyles[0].item.fontWeight
        )
        org.junit.Assert.assertEquals(24, annotated.spanStyles[1].start)
        org.junit.Assert.assertEquals(30, annotated.spanStyles[1].end)

        // Test opening Fandaharampotoana, Trakta, Fifandraisana and dynamic regional schedules
        val onboardingPrefs = com.example.data.local.OnboardingPreferencesManager(activity)

        onboardingPrefs.setRegionImmediate("Ambohipo foibe")
        activity.openFandaharampotoana()
        org.junit.Assert.assertEquals("08H30-09H30", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_1).text.toString())
        org.junit.Assert.assertEquals("10H-12H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_2).text.toString())
        org.junit.Assert.assertEquals("15H-17H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_3).text.toString())
        org.junit.Assert.assertEquals("05H-06H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_4).text.toString())
        org.junit.Assert.assertEquals("17H-18H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_5).text.toString())
        org.junit.Assert.assertFalse(activity.findViewById<View>(R.id.card_alahady_1).isClickable)
        org.junit.Assert.assertFalse(activity.findViewById<View>(R.id.card_andavanandro_1).isClickable)

        onboardingPrefs.setRegionImmediate("Fianarantsoa")
        org.junit.Assert.assertEquals("08H30-09H30", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_1).text.toString())
        org.junit.Assert.assertEquals("09H30-11H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_2).text.toString())
        org.junit.Assert.assertEquals("14H-15H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_3).text.toString())
        org.junit.Assert.assertEquals("05H30-06H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_4).text.toString())
        org.junit.Assert.assertEquals("16H30-17H15", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_5).text.toString())

        onboardingPrefs.setRegionImmediate("Toamasina")
        org.junit.Assert.assertEquals("08H30-09H30", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_1).text.toString())
        org.junit.Assert.assertEquals("09H30-11H30", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_2).text.toString())
        org.junit.Assert.assertEquals("15H-17H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_3).text.toString())
        org.junit.Assert.assertEquals("05H-06H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_4).text.toString())
        org.junit.Assert.assertEquals("17H-18H", activity.findViewById<android.widget.TextView>(R.id.tv_schedule_time_5).text.toString())
        activity.closeFandaharampotoana()

        // Verify CulteTimeChecker regional worship schedules
        val calSundayMorning = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 10)
            set(java.util.Calendar.MINUTE, 30)
        }
        org.junit.Assert.assertTrue(
            com.example.ui.fragments.CulteTimeChecker.isCulteTime("Ambohipo foibe", calSundayMorning)
        )
        org.junit.Assert.assertTrue(
            com.example.ui.fragments.CulteTimeChecker.isCulteTime("Fianarantsoa", calSundayMorning)
        )
        org.junit.Assert.assertTrue(
            com.example.ui.fragments.CulteTimeChecker.isCulteTime("Toamasina", calSundayMorning)
        )

        val calMondayAfternoon = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 0)
        }
        org.junit.Assert.assertFalse(
            com.example.ui.fragments.CulteTimeChecker.isCulteTime("Ambohipo foibe", calMondayAfternoon)
        )

        // Test opening Trakta during worship hours (list unloaded from memory) and outside worship hours
        val traktaFrag = activity.supportFragmentManager.findFragmentByTag("TAG_TRAKTA") as com.example.ui.fragments.TraktaFragment
        com.example.ui.fragments.CulteTimeChecker.forcedCulteTimeForTesting = true
        activity.openTrakta()
        traktaFrag.refreshCulteLockState()
        org.junit.Assert.assertFalse(traktaFrag.isTraktaListLoadedInMemory())

        com.example.ui.fragments.CulteTimeChecker.forcedCulteTimeForTesting = false
        traktaFrag.refreshCulteLockState()
        org.junit.Assert.assertTrue(traktaFrag.isTraktaListLoadedInMemory())
        com.example.ui.fragments.CulteTimeChecker.forcedCulteTimeForTesting = null
        val trakta1 = com.example.data.local.LocalContentCatalog.loadTraktaEntities(activity).first()
        activity.openTraktaReaderScreen(trakta1)
        val flagsInTraktaReader = activity.window.attributes.flags
        org.junit.Assert.assertTrue((flagsInTraktaReader and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
        activity.closeTraktaReaderScreen()
        val flagsAfterTraktaReader = activity.window.attributes.flags
        org.junit.Assert.assertEquals(0, flagsAfterTraktaReader and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        activity.openFifandraisana()
        activity.closeFifandraisana()

        // Test Dark Mode toggle and state verification
        activity.toggleThemeMode()
        activity.setThemeMode("nuit")
        org.junit.Assert.assertEquals("nuit", com.example.ui.theme.ThemeManager.themeMode.value)
        org.junit.Assert.assertTrue(com.example.ui.theme.ThemeManager.isDarkMode.value)

        activity.setThemeMode("jour")
        org.junit.Assert.assertEquals("jour", com.example.ui.theme.ThemeManager.themeMode.value)
        org.junit.Assert.assertFalse(com.example.ui.theme.ThemeManager.isDarkMode.value)

        // Verify RecyclerView binding on Hikaroka
        activity.switchToTab(MainActivity.TAB_HIKAROKA)
        val rv = activity.findViewById<RecyclerView>(R.id.rv_hikaroka_results)
        assertNotNull(rv)
        rv.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        rv.layout(0, 0, 1080, 1920)
        assertTrue((rv.adapter?.itemCount ?: 0) > 0)
    }
}
