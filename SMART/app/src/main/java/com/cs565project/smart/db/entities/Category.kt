package com.cs565project.smart.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Database entity to hold categories of apps that user wants to restrict.
 */
@Entity
class Category(@field:PrimaryKey
               var name: String, var shouldRestrict: Boolean)
