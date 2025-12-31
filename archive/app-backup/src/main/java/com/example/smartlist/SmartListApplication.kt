package com.example.smartlist

import android.app.Application

class SmartListApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		// Initialize simple ServiceLocator so ViewModels and UI can access Repository without Hilt
		ServiceLocator.init(this)
	}
}
