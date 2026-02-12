//
//  swiftAppApp.swift
//  swiftApp
//
//  Created by Idan Aizik Nissim on 12/02/2026.
//

import SwiftUI

@main
struct swiftAppApp: App {
    @StateObject private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(container)
        }
    }
}
