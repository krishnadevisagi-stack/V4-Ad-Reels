package com.example.data.config

object AdConfig {
    // REWARD SYSTEM VALUES
    const val INITIAL_SIGNON_BONUS_COINS = 5000 // 5000 coins initial sign-on bonus for easy testing

    
    // Feed reward configurations (impression-based)
    const val FEED_IMPRESSIONS_REQUIRED = 5 // 5 impressions = Base Reward
    const val FEED_BASE_REWARD_COINS = 15    // Base reward coins for 5 feed views (equal to Reels 5s watch)
    
    // Reels reward configurations (watch-duration based)
    const val REELS_QUALIFIED_WATCH_SECONDS = 5
    const val REELS_BASE_REWARD_COINS = 15
    
    const val REELS_LONGER_WATCH_SECONDS = 10
    const val REELS_LONGER_REWARD_COINS = 40
    
    // WALLET CONVERSION
    const val COINS_TO_USD_RATIO = 0.01 // 1 Coin = 0.01 USD (Editable ratio)
    
    // REDEMPTION OPTIONS (VOUCHERS)
    val REDEMPTION_VOUCHERS = listOf(
        RedeemVoucherOption("Amazon India Gift Card (₹100)", 1000, "Amazon", "₹100.00 Amazon.in E-Gift Card"),
        RedeemVoucherOption("Flipkart Shopping Voucher (₹100)", 1000, "Flipkart", "₹100.00 Flipkart Shopping e-Voucher"),
        RedeemVoucherOption("Google Play Store Code (₹100)", 1000, "Google Play", "₹100.00 Google Play redeem code"),
        RedeemVoucherOption("Swiggy Delivery Voucher (₹50)", 500, "Swiggy", "₹50.00 Swiggy Food & Instamart coupon"),
        RedeemVoucherOption("Uber Rides Voucher (₹50)", 500, "Uber", "₹50.00 Uber Ride promo discount voucher")
    )
}

data class RedeemVoucherOption(
    val title: String,
    val coinCost: Int,
    val provider: String,
    val description: String
)
