package com.example.data.repository

import com.example.data.model.DummyAd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * -----------------------------------------------------------------
 * ADVERTISEMENT DATA REPOSITORY
 * -----------------------------------------------------------------
 * Purpose: Manages all raw advertisement resources, models, and mock metadata lists.
 * Responsibilities:
 *   - Supply curated ad objects partitioned by category for feeds and reels.
 *   - Support dynamic lookups by interest tags.
 * Dependencies: Independent from external SDKs.
 * Future Extension: Query from real external Ad Server REST APIs or local Room caches.
 */
class AdRepository {

    private val _allAds = MutableStateFlow<List<DummyAd>>(emptyList())
    val allAds: StateFlow<List<DummyAd>> = _allAds.asStateFlow()

    init {
        generateDummyAds()
    }

    private fun generateDummyAds() {
        val categories = listOf(
            "Gaming", "Education", "Technology", "Finance", "Business", "Fashion",
            "Fitness", "Automobile", "Travel", "Food", "Movies", "Music",
            "Sports", "Health", "Books", "News"
        )

        val videoUrls = listOf(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        )

        val categoryImages = mapOf(
            "Gaming" to "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=600&auto=format&fit=crop",
            "Education" to "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600&auto=format&fit=crop",
            "Technology" to "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop",
            "Finance" to "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=600&auto=format&fit=crop",
            "Business" to "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600&auto=format&fit=crop",
            "Fashion" to "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600&auto=format&fit=crop",
            "Fitness" to "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop",
            "Automobile" to "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?w=600&auto=format&fit=crop",
            "Travel" to "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=600&auto=format&fit=crop",
            "Food" to "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600&auto=format&fit=crop",
            "Movies" to "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop",
            "Music" to "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop",
            "Sports" to "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=600&auto=format&fit=crop",
            "Health" to "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=600&auto=format&fit=crop",
            "Books" to "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop",
            "News" to "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=600&auto=format&fit=crop"
        )

        val logos = listOf(
            "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=100&auto=format&fit=crop&q=60",
            "https://images.unsplash.com/photo-1560179707-f14e90ef3623?w=100&auto=format&fit=crop&q=60",
            "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=100&auto=format&fit=crop&q=60",
            "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=100&auto=format&fit=crop&q=60"
        )

        val list = mutableListOf<DummyAd>()

        categories.forEachIndexed { catIdx, category ->
            val catImg = categoryImages[category] ?: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=600"
            
            // Generate 4 ads for each category: 2 images (Home feed) and 2 videos (Reels)
            
            // Ad 1: Image Feed Ad
            list.add(
                DummyAd(
                    id = "img_${category.lowercase()}_1",
                    category = category,
                    brandName = "${category} Prime",
                    logo = logos[catIdx % logos.size],
                    mediaUrl = catImg,
                    isVideo = false,
                    title = "Experience Premium ${category}",
                    description = "Discover the next level of innovation in $category with our exclusive, high-performance product lineups designed for you.",
                    rewardCoins = 15 + (catIdx * 2) % 30,
                    duration = 10,
                    ctaText = "Explore Now",
                    productName = "${category} Suite Elite"
                )
            )

            // Ad 2: Video Reels Ad
            list.add(
                DummyAd(
                    id = "vid_${category.lowercase()}_1",
                    category = category,
                    brandName = "${category} Studios",
                    logo = logos[(catIdx + 1) % logos.size],
                    mediaUrl = videoUrls[catIdx % videoUrls.size],
                    isVideo = true,
                    title = "Why $category Matters",
                    description = "Watch this exclusive highlight detailing our incredible breakthroughs in the world of $category. Don't blink!",
                    rewardCoins = 30 + (catIdx * 3) % 40,
                    duration = 15,
                    ctaText = "Join Today",
                    appName = "${category}Pro Mobile"
                )
            )

            // Ad 3: Image Feed Ad
            list.add(
                DummyAd(
                    id = "img_${category.lowercase()}_2",
                    category = category,
                    brandName = "Global ${category} Co.",
                    logo = logos[(catIdx + 2) % logos.size],
                    mediaUrl = catImg,
                    isVideo = false,
                    title = "Smart Solutions for $category",
                    description = "Unlock customized tools and specialized strategies to maximize your efficiency in $category. Thousands trust us.",
                    rewardCoins = 20 + (catIdx * 4) % 25,
                    duration = 12,
                    ctaText = "Learn More",
                    serviceName = "${category} Connect 360"
                )
            )

            // Ad 4: Video Reels Ad
            list.add(
                DummyAd(
                    id = "vid_${category.lowercase()}_2",
                    category = category,
                    brandName = "${category} Hub",
                    logo = logos[(catIdx + 3) % logos.size],
                    mediaUrl = videoUrls[(catIdx + 1) % videoUrls.size],
                    isVideo = true,
                    title = "The Future of $category is Here",
                    description = "Experience the thrilling, feature-complete release of our specialized $category product lines. Act fast!",
                    rewardCoins = 45 + (catIdx * 5) % 35,
                    duration = 20,
                    ctaText = "Get Started",
                    productName = "${category} Core X"
                )
            )
        }

        _allAds.value = list
    }

    /**
     * Get static image ads for the home feed.
     * Filter by user interests, if any are selected.
     */
    fun getHomeFeedAds(selectedCategories: List<String>): List<DummyAd> {
        val allImageAds = _allAds.value.filter { !it.isVideo }
        if (selectedCategories.isEmpty()) return allImageAds
        
        val filtered = allImageAds.filter { ad ->
            selectedCategories.any { cat -> ad.category.equals(cat, ignoreCase = true) }
        }
        return filtered.ifEmpty { allImageAds }
    }

    /**
     * Get short video ads for the Reels tab.
     * Filter by user interests, if any are selected.
     */
    fun getReelsAds(selectedCategories: List<String>): List<DummyAd> {
        val allVideoAds = _allAds.value.filter { it.isVideo }
        if (selectedCategories.isEmpty()) return allVideoAds

        val filtered = allVideoAds.filter { ad ->
            selectedCategories.any { cat -> ad.category.equals(cat, ignoreCase = true) }
        }
        return filtered.ifEmpty { allVideoAds }
    }
}
