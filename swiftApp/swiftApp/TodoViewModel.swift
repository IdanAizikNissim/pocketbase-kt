import Foundation
import Combine

@MainActor
final class TodoViewModel: ObservableObject {
    @Published var todos: [TodoRecord] = []
    @Published var selectedTodo: TodoRecord?
    @Published var isLoading = false
    @Published var errorMessage: String?

    let service: PocketBaseService

    init(service: PocketBaseService) {
        self.service = service
    }

    func start() {
        fetchTodos()
        Task {
            do {
                try await service.startRealtime { [weak self] in
                    Task { @MainActor in
                        self?.fetchTodos()
                    }
                }
            } catch {
                errorMessage = "Realtime subscribe failed: \(error.localizedDescription)"
            }
        }
    }

    func stop() {
        Task {
            await service.stopRealtime()
        }
    }

    func fetchTodos() {
        isLoading = true
        errorMessage = nil

        Task {
            defer { isLoading = false }
            do {
                todos = try await service.fetchTodos()
            } catch {
                errorMessage = "Load failed: \(error.localizedDescription)"
            }
        }
    }

    func loadTodo(id: String) async throws -> TodoRecord {
        try await service.getTodo(id: id)
    }

    func createTodo(text: String, attachmentURL: URL?) {
        Task {
            do {
                try await service.createTodo(text: text, attachmentURL: attachmentURL)
                fetchTodos()
            } catch {
                errorMessage = "Create failed: \(error.localizedDescription)"
            }
        }
    }

    func updateTodo(
        todo: TodoRecord,
        text: String,
        completed: Bool,
        newAttachmentURL: URL?,
        deleteAttachment: Bool
    ) {
        Task {
            do {
                try await service.updateTodo(
                    todo: todo,
                    text: text,
                    completed: completed,
                    newAttachmentURL: newAttachmentURL,
                    deleteAttachment: deleteAttachment
                )
                fetchTodos()
            } catch {
                errorMessage = "Update failed: \(error.localizedDescription)"
            }
        }
    }

    func toggleTodo(_ todo: TodoRecord) {
        Task {
            do {
                try await service.toggleTodo(todo)
                fetchTodos()
            } catch {
                errorMessage = "Toggle failed: \(error.localizedDescription)"
            }
        }
    }

    func deleteTodo(id: String) {
        Task {
            do {
                try await service.deleteTodo(id: id)
                fetchTodos()
            } catch {
                errorMessage = "Delete failed: \(error.localizedDescription)"
            }
        }
    }

    func attachmentURL(for todo: TodoRecord) async -> URL? {
        await service.attachmentURL(for: todo)
    }
}
