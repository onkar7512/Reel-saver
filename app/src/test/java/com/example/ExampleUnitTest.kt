package com.example

import com.example.util.InstagramUrlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun parse_standardInstagramReelUrl() {
    val input = "https://www.instagram.com/reel/C8xg4NypM81/?igsh=MWQ1ZGUxMzBkMA=="
    val parsed = InstagramUrlParser.parse(input)

    assertTrue(parsed.isInstagramUrl)
    assertEquals("C8xg4NypM81", parsed.shortCode)
    assertEquals("https://www.instagram.com/reel/C8xg4NypM81/", parsed.cleanUrl)
  }

  @Test
  fun parse_sharedReelWithTextAndRecipeKeywords() {
    val input = "Check out this delicious ramen recipe https://www.instagram.com/reel/C7yt9k3L45z/?utm_source=ig_web"
    val parsed = InstagramUrlParser.parse(input)

    assertTrue(parsed.isInstagramUrl)
    assertEquals("C7yt9k3L45z", parsed.shortCode)
    assertEquals("https://www.instagram.com/reel/C7yt9k3L45z/", parsed.cleanUrl)
    assertEquals("Recipe", parsed.suggestedCategory)
    assertTrue(parsed.extractedCaption.contains("Check out this delicious ramen recipe"))
  }

  @Test
  fun parse_fitnessReelKeyword() {
    val input = "https://www.instagram.com/reel/C5ml14QPr90/ core workout routine"
    val parsed = InstagramUrlParser.parse(input)

    assertEquals("Fitness", parsed.suggestedCategory)
  }
}

