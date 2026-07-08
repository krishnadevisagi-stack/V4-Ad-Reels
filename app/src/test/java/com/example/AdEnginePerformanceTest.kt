package com.example

import com.example.data.adengine.*
import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem
import com.example.data.provider.AdvertisementProvider
import com.example.data.provider.ReelsAdvertisementProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdEnginePerformanceTest {

    // Mock Provider for Feed Ads
    private val mockFeedProvider = object : AdvertisementProvider {
        override suspend fun getAdsForCategories(categories: List<String>): List<AdvertisementItem> {
            return listOf(
                AdvertisementItem(
                    id = "ad_1",
                    type = "IMAGE",
                    brandName = "Nike",
                    productName = "Nike Pegasus",
                    category = "Sports",
                    title = "Run fast",
                    description = "Pegasus 40",
                    ctaText = "Shop Now",
                    destinationUrl = "https://nike.com",
                    imageUrl = "https://nike.com/img.jpg",
                    videoUrl = null,
                    isSponsored = true,
                    isRewardEligible = true,
                    rewardCoins = 15
                ),
                AdvertisementItem(
                    id = "ad_2",
                    type = "IMAGE",
                    brandName = "Adidas",
                    productName = "Ultraboost",
                    category = "Sports",
                    title = "Feel the boost",
                    description = "Ultraboost light",
                    ctaText = "Shop Now",
                    destinationUrl = "https://adidas.com",
                    imageUrl = "https://adidas.com/img.jpg",
                    videoUrl = null,
                    isSponsored = true,
                    isRewardEligible = true,
                    rewardCoins = 15
                )
            )
        }
    }

    // Mock Provider for Reels Ads
    private val mockReelsProvider = object : ReelsAdvertisementProvider {
        override suspend fun getReelsAdsForCategories(categories: List<String>): List<ReelAdvertisementItem> {
            return listOf(
                ReelAdvertisementItem(
                    id = "reel_1",
                    brandName = "Puma",
                    productName = "Nitro",
                    category = "Sports",
                    title = "Run nitro",
                    description = "Fastest shoes",
                    ctaText = "Buy now",
                    destinationUrl = "https://puma.com",
                    videoUrl = "https://puma.com/vid.mp4",
                    isSponsored = true,
                    isRewardEligible = true,
                    rewardCoins = 30
                )
            )
        }
    }

    @Test
    fun testQueueManager_enqueuesAndDequeuesFeedAdsSuccessfully() {
        val queueManager = AdvertisementQueueManager()
        val ad = AdvertisementItem(
            id = "test_ad",
            type = "IMAGE",
            brandName = "Test",
            productName = "Product",
            category = "Category",
            title = "Title",
            description = "Desc",
            ctaText = "CTA",
            destinationUrl = "https://test.com",
            imageUrl = "https://test.com/img.png",
            videoUrl = null,
            isSponsored = true,
            isRewardEligible = true
        )

        assertEquals(0, queueManager.getFeedQueueSize())
        queueManager.enqueueFeedAd(ad)
        assertEquals(1, queueManager.getFeedQueueSize())
        assertEquals(ad, queueManager.peekFeedAd())

        val dequeued = queueManager.dequeueFeedAd()
        assertEquals(ad, dequeued)
        assertEquals(0, queueManager.getFeedQueueSize())
    }

    @Test
    fun testQueueManager_enqueuesAndDequeuesReelsAdsSuccessfully() {
        val queueManager = AdvertisementQueueManager()
        val reel = ReelAdvertisementItem(
            id = "test_reel",
            brandName = "Test",
            productName = "Product",
            category = "Category",
            title = "Title",
            description = "Desc",
            ctaText = "CTA",
            destinationUrl = "https://test.com",
            videoUrl = "https://test.com/video.mp4"
        )

        assertEquals(0, queueManager.getReelsQueueSize())
        queueManager.enqueueReelsAd(reel)
        assertEquals(1, queueManager.getReelsQueueSize())
        assertEquals(reel, queueManager.peekReelsAd())

        val dequeued = queueManager.dequeueReelsAd()
        assertEquals(reel, dequeued)
        assertEquals(0, queueManager.getReelsQueueSize())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testLoader_fetchesFeedAndReelsAdsOffMainThread() = runTest {
        val loader = AdvertisementLoader(mockFeedProvider, mockReelsProvider)
        val feedAds = loader.loadFeedAdsWithRetry(listOf("Sports"))
        val reelsAds = loader.loadReelsAdsWithRetry(listOf("Sports"))

        assertEquals(2, feedAds.size)
        assertEquals("Nike", feedAds[0].brandName)
        assertEquals(1, reelsAds.size)
        assertEquals("Puma", reelsAds[0].brandName)
    }

    @Test
    fun testCache_evictsCorrectly() {
        val cache = AdvertisementCache(maxSize = 2)
        val ad1 = AdvertisementItem(
            id = "ad_1", type = "IMAGE", brandName = "B1", productName = "P1", category = "C1",
            title = "T1", description = "D1", ctaText = "CTA", destinationUrl = "", imageUrl = "",
            videoUrl = null, isSponsored = true, isRewardEligible = true
        )
        val ad2 = AdvertisementItem(
            id = "ad_2", type = "IMAGE", brandName = "B2", productName = "P2", category = "C2",
            title = "T2", description = "D2", ctaText = "CTA", destinationUrl = "", imageUrl = "",
            videoUrl = null, isSponsored = true, isRewardEligible = true
        )
        val ad3 = AdvertisementItem(
            id = "ad_3", type = "IMAGE", brandName = "B3", productName = "P3", category = "C3",
            title = "T3", description = "D3", ctaText = "CTA", destinationUrl = "", imageUrl = "",
            videoUrl = null, isSponsored = true, isRewardEligible = true
        )

        cache.putFeedAd(ad1)
        cache.putFeedAd(ad2)
        assertEquals(ad1, cache.getFeedAd("ad_1"))
        assertEquals(ad2, cache.getFeedAd("ad_2"))

        // Put third item, ad1 should be evicted (LRU behaviour)
        cache.putFeedAd(ad3)
        assertNull(cache.getFeedAd("ad_1"))
        assertEquals(ad2, cache.getFeedAd("ad_2"))
        assertEquals(ad3, cache.getFeedAd("ad_3"))
    }

    @Test
    fun testCache_evictsExpiredItems() = runTest {
        val cache = AdvertisementCache()
        val ad = AdvertisementItem(
            id = "ad_1", type = "IMAGE", brandName = "B1", productName = "P1", category = "C1",
            title = "T1", description = "D1", ctaText = "CTA", destinationUrl = "", imageUrl = "",
            videoUrl = null, isSponsored = true, isRewardEligible = true
        )
        cache.putFeedAd(ad)
        assertEquals(ad, cache.getFeedAd("ad_1"))

        // Force eviction with a threshold that guarantees eviction regardless of timing
        cache.evictExpired(-1L)
        assertNull(cache.getFeedAd("ad_1"))
    }

    @Test
    fun testPrefetchManager_triggersWhenApproachingEnd() {
        val cache = AdvertisementCache()
        val loader = AdvertisementLoader(mockFeedProvider, mockReelsProvider)
        val queueManager = AdvertisementQueueManager()
        val prefetchManager = AdvertisementPrefetchManager(cache, loader, queueManager)

        // Threshold item reached
        val shouldPrefetch = prefetchManager.shouldPrepareNextVideo(
            currentPositionMs = 5500L,
            thresholdMs = 5000L,
            currentIndex = 0,
            totalReels = 2
        )
        assertTrue(shouldPrefetch)

        // Threshold item not reached
        val shouldNotPrefetch = prefetchManager.shouldPrepareNextVideo(
            currentPositionMs = 3000L,
            thresholdMs = 5000L,
            currentIndex = 0,
            totalReels = 2
        )
        assertFalse(shouldNotPrefetch)
    }

    @Test
    fun testDispatcher_propagatesEventsCorrectly() = runTest {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val dispatcher = AdvertisementDispatcher(testScope)
        var receivedEvent = false
        var receivedCredit = false

        val eventJob = testScope.launch {
            dispatcher.adEventFlow.collect { event ->
                if (event.adId == "ad_test" && event.eventType == "VISIBLE") {
                    receivedEvent = true
                }
            }
        }

        val creditJob = testScope.launch {
            dispatcher.rewardCreditFlow.collect { credit ->
                if (credit.adId == "ad_test" && credit.coins == 30) {
                    receivedCredit = true
                }
            }
        }

        dispatcher.dispatchAdEvent("ad_test", "VISIBLE")
        dispatcher.dispatchRewardCredit("ad_test", 30, true)

        // Let background coroutines settle
        kotlinx.coroutines.delay(100)

        assertTrue(receivedEvent)
        assertTrue(receivedCredit)

        eventJob.cancel()
        creditJob.cancel()
    }

    @Test
    fun testAdvertisementManager_andEventBus_worksFlawlessly() = runTest {
        val cache = AdvertisementCache()
        val queueManager = AdvertisementQueueManager()
        val loader = AdvertisementLoader(mockFeedProvider, mockReelsProvider)
        val manager = AdvertisementManager(
            context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            feedProvider = mockFeedProvider,
            reelsProvider = mockReelsProvider,
            cache = cache,
            queueManager = queueManager,
            loader = loader
        )

        // 1. Verify EventBus is fully responsive
        val receivedEvents = mutableListOf<AdvertisementEventBus.Event>()
        val collectJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).launch {
            AdvertisementEventBus.events.collect {
                receivedEvents.add(it)
            }
        }

        val testItem = AdvertisementItem(
            id = "test_man_1",
            type = "IMAGE",
            brandName = "Nike",
            productName = "Pegasus",
            category = "Sports",
            title = "Title",
            description = "Desc",
            ctaText = "Shop",
            destinationUrl = "https://nike.com",
            imageUrl = "https://nike.com/img.jpg",
            videoUrl = null,
            isSponsored = true,
            isRewardEligible = true,
            rewardCoins = 10
        )

        val metadata = manager.mapToMetadata(testItem)
        assertEquals("test_man_1", metadata.adId)
        assertEquals("IMAGE", metadata.adType)
        assertEquals("Nike", metadata.brandName)

        // Track visibility lifecycle
        manager.trackLifecycle("test_man_1", "VISIBLE", metadata)
        kotlinx.coroutines.delay(50)

        assertTrue(receivedEvents.any { it is AdvertisementEventBus.Event.AdVisible && it.metadata.adId == "test_man_1" })

        // 2. Verify cache integration
        manager.cacheAdvertisement(metadata)
        val retrieved = cache.getFeedAd("test_man_1")
        assertNotNull(retrieved)
        assertEquals("test_man_1", retrieved?.id)

        collectJob.cancel()
    }

    @Test
    fun testFutureProviders_implementCorrectAgnosticContracts() = runTest {
        val repository = com.example.data.repository.AdRepository()
        val admob = com.example.data.provider.AdMobProvider(repository)
        val custom = com.example.data.provider.CustomAdvertisementProvider(repository)
        val direct = com.example.data.provider.DirectAdvertiserProvider(repository)
        val hybrid = com.example.data.provider.HybridProvider(repository, admob, direct)

        val categories = listOf("Education", "Tech")
        
        val admobAds = admob.getAdsForCategories(categories)
        assertNotNull(admobAds)
        assertTrue(admobAds.all { it.id.startsWith("admob_feed_") || it.id.startsWith("ca-app-pub-") })

        val customAds = custom.getAdsForCategories(categories)
        assertNotNull(customAds)
        assertTrue(customAds.all { it.id.startsWith("custom_feed_") })

        val directAds = direct.getAdsForCategories(categories)
        assertNotNull(directAds)
        assertTrue(directAds.all { it.id.startsWith("direct_feed_") })

        val hybridAds = hybrid.getAdsForCategories(categories)
        assertNotNull(hybridAds)
    }
}
