//
//  schoolTermsApp.swift
//  schoolTerms
//
//  Created by Sanzhar  Zhabagin  on 25.03.2025.
//

import SwiftUI

@main
struct schoolTermsApp: App {
    @StateObject var languageManager = LanguageManager()
    var body: some Scene {
        WindowGroup {
            ContentView().environmentObject(languageManager)
        }
    }
}
