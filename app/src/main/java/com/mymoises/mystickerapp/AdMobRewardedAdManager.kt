package com.mymoises.mystickerapp

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobRewardedAdManager {
    private const val TAG = "AdMobRewardedAdManager"

    // Official Google AdMob Test Rewarded Ad Unit ID for Android
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    var rewardedAdUnitId: String = TEST_REWARDED_AD_UNIT_ID

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) {
                loadAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization error", e)
        }
    }

    fun loadAd(context: Context) {
        if (rewardedAd != null || isLoading) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun isAdAvailable(): Boolean = rewardedAd != null

    fun showRewardedAd(
        activity: Activity,
        onRewardGranted: () -> Unit,
        onAdClosed: (Boolean) -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            Log.w(TAG, "Rewarded ad was not ready.")
            loadAd(activity)
            Toast.makeText(activity, "Ad is loading. Please try again in a moment.", Toast.LENGTH_SHORT).show()
            onAdClosed(false)
            return
        }

        var rewardEarned = false
        var actionExecuted = false

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                rewardedAd = null
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                Log.w(TAG, "Failed to show rewarded ad: ${error.message}")
                loadAd(activity)
                Toast.makeText(activity, "Ad failed to display. Please try again.", Toast.LENGTH_SHORT).show()
                onAdClosed(false)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd(activity)
                if (rewardEarned && !actionExecuted) {
                    actionExecuted = true
                    onRewardGranted()
                }
                onAdClosed(rewardEarned)
            }
        }

        currentAd.show(activity) { rewardItem ->
            rewardEarned = true
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
    }
}

data class RewardedAdOfferSpec(
    val actionType: String,
    val onExecuteAction: () -> Unit
)

/**
 * AdMob Policy Compliant Rewarded Ad Requirement Dialog
 * Discloses that watching a rewarded ad is required to perform the requested action
 * (Save Project, Save Image, Share Design, Send Design, Export Image, Download Image).
 * Opt-In: Users click "Watch Ad" to view the ad and execute the action upon completion.
 * Dismiss/Cancel: Closes the dialog and cancels the requested action without saving/sharing,
 * preserving current editing progress.
 */
@Composable
fun RewardedAdOfferDialog(
    actionType: String = "Export",
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = "Watch Ad to $actionType"
    val message = "A short rewarded ad is required to $actionType. Watch the ad to complete your action."

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF1E182E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFD1D5DB),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Color(0xFFD1D5DB)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "🎬 AdMob Rewarded Ad Required",
                    fontSize = 11.sp,
                    color = Color(0xFFA78BFA),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onWatchAd,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Watch Ad 🎬", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = Color.LightGray)
            }
        }
    )
}
