import Foundation

// Mirrors Android's AVATAR_PRESETS exactly (data/AvatarPresets.kt) - same
// ids, same order, same real raster assets (copied directly from
// android/app/src/main/res/drawable-nodpi/, not recreated).
struct AvatarPreset: Identifiable, Equatable {
    let id: String
    let imageName: String

    static let all: [AvatarPreset] = [
        AvatarPreset(id: "male", imageName: "MascotMale"),
        AvatarPreset(id: "female", imageName: "MascotFemale"),
        AvatarPreset(id: "avatar_shopper", imageName: "AvatarShopper"),
        AvatarPreset(id: "avatar_sleepy", imageName: "AvatarSleepy"),
        AvatarPreset(id: "avatar_cool", imageName: "AvatarCool"),
        AvatarPreset(id: "avatar_angry", imageName: "AvatarAngry"),
        AvatarPreset(id: "avatar_happy", imageName: "AvatarHappy"),
        AvatarPreset(id: "avatar_playful", imageName: "AvatarPlayful"),
        AvatarPreset(id: "avatar_racer", imageName: "AvatarRacer"),
        AvatarPreset(id: "avatar_gamer", imageName: "AvatarGamer"),
        AvatarPreset(id: "avatar_foodie", imageName: "AvatarFoodie"),
    ]

    static func byId(_ id: String?) -> AvatarPreset? {
        guard let id else { return nil }
        return all.first { $0.id == id }
    }

    // Rendering precedence when no explicit preset is set: an uploaded
    // photo still wins over the gender default (matches Android's
    // defaultAvatarPresetIdForGender - picking a photo is the more
    // deliberate, more recent action in that case).
    static func defaultId(forGender gender: ProfilePick?) -> String? {
        switch gender {
        case .male: return "male"
        case .female: return "female"
        case nil: return nil
        }
    }
}
