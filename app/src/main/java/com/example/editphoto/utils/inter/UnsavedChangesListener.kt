package com.example.editphoto.utils.inter


interface UnsavedChangesListener {
    fun hasUnsavedChanges(): Boolean
    fun revertUnsavedChanges()
}