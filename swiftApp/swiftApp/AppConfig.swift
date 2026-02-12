import Foundation

enum AppConfig {
    static var serverURL: String {
        if let value = Bundle.main.object(forInfoDictionaryKey: "PB_SERVER_URL") as? String, !value.isEmpty {
            return value
        }
        return "http://127.0.0.1:8090"
    }
}
