import Foundation
import PocketBase

enum ServiceError: Error, LocalizedError {
    case invalidResponse
    case missingAuthToken
    case invalidAttachment

    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid server response."
        case .missingAuthToken: return "You must be logged in."
        case .invalidAttachment: return "Could not read selected file."
        }
    }
}

typealias PocketBaseService = PocketBase.ApplePocketBaseService

extension PocketBaseService {
    convenience init(url: String) {
        self.init(url: url, authCollection: "users", sessionKey: "pocketbase.session")
    }

    func decodeJSON<T: Decodable>(_ type: T.Type, from json: String) throws -> T {
        try JSONDecoder().decode(type, from: Data(json.utf8))
    }

    func encodeJSON<T: Encodable>(_ value: T) throws -> String {
        let data = try JSONEncoder().encode(value)
        return String(decoding: data, as: UTF8.self)
    }

    func makeFile(field: String, from fileURL: URL?) throws -> [PocketBase.File] {
        guard let fileURL else { return [] }
        let data = try Data(contentsOf: fileURL)
        let fileName = fileURL.lastPathComponent
        guard !fileName.isEmpty else { throw ServiceError.invalidAttachment }
        let kotlinData = data.toKotlinByteArray()
        return [PocketBase.File(field: field, data: kotlinData, fileName: fileName)]
    }
}

private extension Data {
    nonisolated func toKotlinByteArray() -> KotlinByteArray {
        let kotlinArray = KotlinByteArray(size: Int32(count))
        for (idx, byte) in enumerated() {
            kotlinArray.set(index: Int32(idx), value: Int8(bitPattern: byte))
        }
        return kotlinArray
    }
}
