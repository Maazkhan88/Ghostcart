package com.example.ghostcart.data

import com.ghostcart.app.R

data class AvatarPreset(val id: String, val drawableRes: Int)

// To add another preset avatar later: drop the PNG in res/drawable-nodpi and
// add one line here. Nothing else needs to change - the Profile screen's
// picker grid and the server-side validation both just read this list.
val AVATAR_PRESETS: List<AvatarPreset> = listOf(
    AvatarPreset("male", R.drawable.mascot_male),
    AvatarPreset("female", R.drawable.mascot_female),
    AvatarPreset("avatar_shopper", R.drawable.avatar_shopper),
    AvatarPreset("avatar_sleepy", R.drawable.avatar_sleepy),
    AvatarPreset("avatar_cool", R.drawable.avatar_cool),
    AvatarPreset("avatar_angry", R.drawable.avatar_angry),
    AvatarPreset("avatar_happy", R.drawable.avatar_happy),
    AvatarPreset("avatar_playful", R.drawable.avatar_playful),
    AvatarPreset("avatar_racer", R.drawable.avatar_racer),
    AvatarPreset("avatar_gamer", R.drawable.avatar_gamer),
    AvatarPreset("avatar_foodie", R.drawable.avatar_foodie),
)

fun avatarPresetById(id: String?): AvatarPreset? = id?.let { wanted -> AVATAR_PRESETS.find { it.id == wanted } }

// Rendering precedence when no explicit preset is set: an uploaded photo
// still wins over the gender default, since picking a photo is the more
// deliberate, more recent action in that case.
fun defaultAvatarPresetIdForGender(gender: String?): String? = when (gender?.lowercase()) {
    "male" -> "male"
    "female" -> "female"
    else -> null
}
