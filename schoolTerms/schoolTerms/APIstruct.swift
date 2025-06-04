//
//  APIstruct.swift
//  schoolTerms
//
//  Created by Sanzhar  Zhabagin  on 01.05.2025.
//

import Foundation

struct Term: Identifiable, Codable {
    let id = UUID()
    let kazakh: String
    let russian: String
    let english: String
    let description: String
}
struct GSheetResponse: Decodable {
    let values: [[String]]
}

class TermDataLoader: ObservableObject {
    @Published var terms: [Term] = []

    func fetchTerms(for subject: String, completion: @escaping ([Term]) -> Void) {
        let spreadsheetId = "1I9KZKiU5A51_iEd1Mmr4Acdr8zo8XIbvlfpINuUpn9U"
        let apiKey = "AIzaSyBJWakLzfodn_58xliYcB4FY_N8y6NHAgQ"
        let range = "\(subject)!A2:D"
        
        let urlString = "https://sheets.googleapis.com/v4/spreadsheets/\(spreadsheetId)/values/\(range)?key=\(apiKey)"
        
        guard let url = URL(string: urlString) else { return }

        URLSession.shared.dataTask(with: url) { data, response, error in
            guard
                let data = data,
                error == nil,
                let response = try? JSONDecoder().decode(GSheetResponse.self, from: data)
            else {
                print("Error fetching data")
                DispatchQueue.main.async {
                    completion([])
                }
                return
            }
            
            let terms = response.values.compactMap { row -> Term? in
                guard row.count >= 4 else { return nil }
                return Term(kazakh: row[0], russian: row[1], english: row[2], description: row[3])
            }

            DispatchQueue.main.async {
                completion(terms)
            }
        }.resume()
    }
}
extension TermDataLoader {
    func fetchAllSubjectsTerms(completion: @escaping ([Term]) -> Void) {
        let subjects = ["Математика", "Физика", "Биология", "Естествознание", "Информатика", "Химия", "География"]
        var allTerms: [Term] = []
        let group = DispatchGroup()

        for subject in subjects {
            group.enter()
            fetchTerms(for: subject) { terms in
                allTerms.append(contentsOf: terms)
                group.leave()
            }
        }

        group.notify(queue: .main) {
            completion(allTerms)
        }
    }
}

struct SheetResponse: Decodable {
    let range: String
    let majorDimension: String
    let values: [[String]]
}
