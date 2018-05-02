package com.cs565project.smart.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Database entity to hold details of an app.
 */
@Entity
class AppDetails(@field:PrimaryKey
                 val packageName: String, val appName: String, val category: String, val thresholdTime: Int)
