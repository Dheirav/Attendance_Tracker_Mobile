package com.example.lol.data

import kotlinx.coroutines.flow.Flow

class SubjectRepository(private val subjectDao: SubjectDao) {
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun insert(subject: Subject) {
        subjectDao.insertSubject(subject)
    }

    suspend fun delete(subject: Subject) {
        subjectDao.deleteSubject(subject)
    }

    suspend fun update(subject: Subject) {
        subjectDao.updateSubject(subject)
    }

    suspend fun refreshSubjects() {
        subjectDao.getAllSubjects()
    }
}
