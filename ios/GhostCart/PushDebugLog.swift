import Foundation
import SwiftUI
import os.log

// Structured, always-on (not #if DEBUG gated - TestFlight builds are exactly
// where this pipeline has been failing silently) logging for every step of
// the push-notification chain: permission -> APNs token -> FCM token ->
// backend upload -> received/tapped. Uses os_log so it shows up in Console/
// Xcode's device log, plus a persisted ring buffer (UserDefaults) so the
// last N lines survive without needing a live console session attached -
// they're readable from Profile > Reminders after the fact.
enum PushDebugLog {
    private static let logger = Logger(subsystem: "com.ghostcart.app", category: "PushDebug")
    private static let bufferKey = "ghostcart.push-debug-buffer"
    private static let maxLines = 200

    static func log(_ message: String) {
        let line = "\(ISO8601DateFormatter().string(from: Date())) \(message)"
        logger.notice("\(line, privacy: .public)")
        #if DEBUG
        print("========== PUSH DEBUG ==========\n\(line)\n================================")
        #endif
        var buffer = UserDefaults.standard.stringArray(forKey: bufferKey) ?? []
        buffer.append(line)
        if buffer.count > maxLines { buffer.removeFirst(buffer.count - maxLines) }
        UserDefaults.standard.set(buffer, forKey: bufferKey)
    }

    static func recentLines() -> [String] {
        UserDefaults.standard.stringArray(forKey: bufferKey) ?? []
    }

    static func clear() {
        UserDefaults.standard.removeObject(forKey: bufferKey)
    }
}

// Readable without a live Xcode console session attached - the whole reason
// this exists is that the push pipeline was failing silently on a
// TestFlight-distributed build, where nobody has a debugger attached.
struct PushDebugLogView: View {
    @State private var lines = PushDebugLog.recentLines()

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 6) {
                if lines.isEmpty {
                    Text("No push events logged yet on this device.")
                        .font(.caption)
                        .foregroundStyle(Color.secondary)
                        .padding()
                } else {
                    ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                        Text(line)
                            .font(.system(size: 11, design: .monospaced))
                            .textSelection(.enabled)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Push Debug Log")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                ShareLink(item: lines.joined(separator: "\n")) {
                    Image(systemName: "square.and.arrow.up")
                }
                Button("Clear") {
                    PushDebugLog.clear()
                    lines = []
                }
            }
        }
        .onAppear { lines = PushDebugLog.recentLines() }
    }
}
