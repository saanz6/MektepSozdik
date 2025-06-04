//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//
package com.example.bilimsoz.data.network

import com.example.bilimsoz.data.model.GoogleSheetsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleSheetsApi {
    @GET("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun getSheetData(
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("key") apiKey: String
    ): GoogleSheetsResponse
}