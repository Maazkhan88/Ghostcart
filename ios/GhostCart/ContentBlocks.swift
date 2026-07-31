import Foundation

// Admin-curated marketing imagery from GET /api/content-blocks, mirrors
// Android's ProductImportRepository.fetchContentBlocks (ProductDiscovery.kt).
// The endpoint ignores a `type` query filter server-side, so filtering by
// `type` ("banner" / "story") happens client-side here.
struct ContentBlock: Identifiable, Equatable {
    let id: Int
    let type: String
    let imageKey: String
    let mediaType: String
    let sortOrder: Int

    var imageURL: URL? {
        URL(string: "\(ApiConfig.baseURL)/api/content-blocks/image/\(imageKey)")
    }
}

enum ContentBlocksService {
    static func fetch() async -> [ContentBlock] {
        guard let object = try? await ApiClient.shared.getJSON(path: "/api/content-blocks"),
              let rawBlocks = object["contentBlocks"] as? [[String: Any]] else {
            return []
        }
        return rawBlocks.compactMap { raw -> ContentBlock? in
            guard let id = raw["id"] as? Int,
                  let type = raw["type"] as? String,
                  let imageKey = raw["imageKey"] as? String,
                  (raw["isActive"] as? Bool) != false else { return nil }
            return ContentBlock(
                id: id,
                type: type,
                imageKey: imageKey,
                mediaType: raw["mediaType"] as? String ?? "image",
                sortOrder: raw["sortOrder"] as? Int ?? 0
            )
        }
        .sorted { $0.sortOrder < $1.sortOrder }
    }

    static func banners(from blocks: [ContentBlock]) -> [ContentBlock] {
        blocks.filter { $0.type == "banner" }
    }

    static func stories(from blocks: [ContentBlock]) -> [ContentBlock] {
        blocks.filter { $0.type == "story" }
    }
}
